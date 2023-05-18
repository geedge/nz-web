package com.nis.modules.tool.controller;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.thread.NamedThreadFactory;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.AgentService;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.ipam.entity.IpamSubnet;
import com.nis.modules.ipam.service.IpamSubnetService;
import com.nis.modules.sys.entity.SnmpCredential;
import com.nis.modules.sys.service.SnmpCredentialService;
import com.nis.modules.sys.service.SysConfService;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Discover assets by subnet
 *
 * @author shizhendong
 */
@RestController
@RequestMapping("/tool")
public class AssetDiscoveryController {

    private final static Log log = Log.get();

    @Value("${discovery.corePoolSize:200}")
    private Integer discoveryCorePoolSize;

    @Value("${discovery.keepAliveTime:30}")
    private Long discoveryKeepAliveTime;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private AgentService agentService;

    @Autowired
    private SysConfService sysConfService;

    @Autowired
    private AssetAssetService assetService;

    @Autowired
    private IpamSubnetService ipamSubnetService;

    @Autowired
    private SnmpCredentialService snmpCredentialService;

    /**
     * discovery 执行线程池，无界+不抛弃
     */
    private ExecutorService discoveryThreadPool;
    /**
     * 存储 thread group ，用以中断线程
     */
    private static volatile ConcurrentHashMap<String, List<ThreadGroup>> DISCOVERY_CACHE = Tool.MapUtil.newConcurrentHashMap();

    private synchronized ExecutorService getDiscoveryThreadPool() {
        if (discoveryThreadPool == null) {
            discoveryThreadPool = new ThreadPoolExecutor(
                    discoveryCorePoolSize,
                    discoveryCorePoolSize,
                    discoveryKeepAliveTime,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(),
                    new NamedThreadFactory("DISCOVERY-", true),
                    new ThreadPoolExecutor.CallerRunsPolicy());
        }
        return discoveryThreadPool;
    }


    /**
     * 创建任务
     *
     * @param requestBody
     * @return
     */
    @PostMapping("/discovery")
    public R createDiscoveryTask(@RequestBody Map<String, Object> requestBody) {
        requestBody = Tool.MapUtil.defaultIfEmpty(requestBody, Tool.MapUtil.newHashMap());
        log.info("[createDiscoveryTask] [begin] [body: {}]", JSONUtil.toJsonStr(requestBody));

        String subnetIds = (String) requestBody.get("subnetIds");
        String snmpCredentialIds = (String) requestBody.get("snmpCredentialIds");
        ValidateUtils.is(subnetIds).notNull(RCode.IPAM_SUBNET_ID_ISNULL)
                .and(snmpCredentialIds).notNull(RCode.SNMP_CREDENTIAL_ID_ISNULL);

        List<String> subnetIdList = Tool.StrUtil.split(subnetIds, ",");
        List<IpamSubnet> ipamSubnetList = ipamSubnetService.getBaseMapper().selectBatchIds(subnetIdList);
        ValidateUtils.is(ipamSubnetList).notNull(RCode.IPAM_IP_SUBNET_NOT_EXISTS);

        IpamSubnet ipv6TypeSubnet = ipamSubnetList.stream().filter(ipamSubnet -> Tool.ObjectUtil.equal(6, ipamSubnet.getType())).findFirst().orElse(null);
        if (Tool.ObjectUtil.isNotNull(ipv6TypeSubnet)) throw new NZException(RCode.TOOL_NOT_SUPPORTED_IPV6SUBNET);

        List<String> snmpCredentialIdList = Tool.StrUtil.split(snmpCredentialIds, ",");
        List<SnmpCredential> credentialList = snmpCredentialService.getBaseMapper().selectBatchIds(snmpCredentialIdList);
        ValidateUtils.is(credentialList).notNull(RCode.SNMP_CREDENTIAL_NOT_FOUND);

        // snmp 探测模式
        String discoverySnmpMode = sysConfService.getValueOrDefault("asset_discovery_snmp_mode", "1");
        Map<String, Object> confParams = Tool.MapUtil.newHashMap();
        confParams.put("timeout", requestBody.get("timeout"));
        confParams.put("retries", requestBody.get("retries"));
        confParams.put("discoverySnmpMode", discoverySnmpMode);

        List<AssetAsset> discoveredAssetList = assetService.getDiscoveredAssetInfoList();
        List<Agent> agentList = agentService.list(new LambdaQueryWrapper<Agent>().eq(Agent::getStatus, 1));
        if (BooleanUtil.and(
                StrUtil.equals("2", discoverySnmpMode),
                Tool.CollectionUtil.isEmpty(agentList))) {
            throw new NZException(RCode.AGENT_UNAVAILABLE);
        }

        String tid = StrUtil.uuid();
        ThreadUtil.execAsync(() -> {
            for (IpamSubnet ipamSubnet : ipamSubnetList) {
                ThreadUtil.execAsync(() -> {
                    List<ThreadGroup> threadGroupListForTask = DISCOVERY_CACHE.get(StrUtil.concat(true, "discovery_threadGroup_", tid));
                    threadGroupListForTask = Tool.CollUtil.defaultIfEmpty(threadGroupListForTask, Tool.ListUtil.list(true));

                    ThreadGroup threadGroup = new ThreadGroup(tid + "_" + ipamSubnet.getId());
                    threadGroupListForTask.add(threadGroup);

                    DISCOVERY_CACHE.put(StrUtil.concat(true, "discovery_threadGroup_", tid), threadGroupListForTask);

                    // 利用 SubnetUtils 计算 ip 范围
                    SubnetUtils subnetUtils = new SubnetUtils(ipamSubnet.getAddr() + "/" + ipamSubnet.getMask());
                    subnetUtils.setInclusiveHostCount(true);

                    // 这里不使用SubnetUtils.info 提供的 getAllAddresses 方法，使用内部方法自己生成
                    int low = ReflectUtil.invoke(subnetUtils.getInfo(), "low");
                    int high = ReflectUtil.invoke(subnetUtils.getInfo(), "high");
                    for (int add = low, j = 0; add <= high; ++add, ++j) {
                        String ip = format(toArray(add));
                        DiscoveryRunnable runnable = new DiscoveryRunnable(threadGroup, tid, ip, credentialList, discoveredAssetList, agentList, confParams);
                        runnable.setName(StrUtil.concat(true, tid, "-", ipamSubnet.getName()));

                        ReflectUtil.invoke(threadGroup, "add", runnable);
                        CompletableFuture.runAsync(runnable, getDiscoveryThreadPool());
                    }
                });
            }
        });
        long totalDiscoveryIpNum = ipamSubnetList.stream().mapToLong(subnet -> Long.valueOf(subnet.getCap())).sum();

        Map m = Tool.MapUtil.newHashMap();
        m.put("tid", tid);
        Map task = Tool.MapUtil.newHashMap();
        task.put("total", totalDiscoveryIpNum);
        task.put("done", 0);
        task.put("process", 0);
        m.put("task", task);
        log.info("[createDiscoveryTask] [finshed] [tid: {}] [subnet size: {}] [discovery ip size: {}]", tid, ipamSubnetList.size(), totalDiscoveryIpNum);

        // 设置 30 分钟超时，避免 redis 中存在垃圾数据
        redisTemplate.expire(StrUtil.concat(true, "discovery_result_", tid), 30, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(StrUtil.concat(true, "discovery_stat_", tid), JSONObject.toJSONString(task), 30, TimeUnit.MINUTES);
        return R.ok(m);
    }

    /**
     * 查询结果
     * 更新进度缓存
     *
     * @param tid
     * @return
     */
    @GetMapping("/discovery/{tid}")
    public R queryResult(@PathVariable(value = "tid", required = true) String tid) {
        String discoveryResultKey = StrUtil.concat(true, "discovery_result_", tid);
        List<String> discoveryResultList = Tool.ListUtil.list(false);
        while (true) {
            String obj = (String) redisTemplate.opsForList().rightPop(discoveryResultKey);
            if (obj == null) break;
            discoveryResultList.add(obj);
        }

        String discoveryStatStr = (String) redisTemplate.opsForValue().get(StrUtil.concat(true, "discovery_stat_", tid));
        // 任务已被取消或 key 超时
        if (Tool.StrUtil.isEmpty(discoveryStatStr)) {
            Map result = Tool.MapUtil.newHashMap();
            Map<String, Integer> task = Tool.MapUtil.of(Pair.of("total", 0), Pair.of("done", 0), Pair.of("process", 0));
            result.put("task", task);
            result.put("list", Tool.ListUtil.empty());
            return R.ok(result);
        }

        Map discoveryStatMap = JSONObject.parseObject(discoveryStatStr, Map.class);
        Integer total = (Integer) discoveryStatMap.get("total");
        Integer done = (Integer) discoveryStatMap.get("done");

        List list = Tool.ListUtil.list(false);
        for (String jsonStr : discoveryResultList) {
            cn.hutool.json.JSONObject jsonObject = JSONUtil.parseObj(jsonStr);
            list.add(jsonObject);
        }
        done = done + list.size();
        discoveryStatMap.put("done", done);
        String percent = NumberUtil.formatPercent((double) done / total, 2);
        discoveryStatMap.put("process", percent.replace("%", ""));

        // update
        redisTemplate.opsForValue().set(StrUtil.concat(true, "discovery_stat_", tid), JSONObject.toJSONString(discoveryStatMap), 30, TimeUnit.MINUTES);

        Map result = Tool.MapUtil.newHashMap();
        result.put("task", discoveryStatMap);
        result.put("list", list);
        return R.ok(result);
    }

    /**
     * 取消任务
     * 删除缓存数据
     *
     * @param tid
     * @return
     */
    @DeleteMapping("/discovery/{tid}")
    public R cancelTask(@PathVariable(value = "tid", required = false) String tid) {
        List<ThreadGroup> threadGroupListForTask = DISCOVERY_CACHE.get(StrUtil.concat(true, "discovery_threadGroup_", tid));
        threadGroupListForTask = Tool.CollUtil.defaultIfEmpty(threadGroupListForTask, Tool.ListUtil.list(true));

        if (Tool.CollUtil.isEmpty(threadGroupListForTask)) {
            log.warn("[cancelTask] [discovery task cancel error, the task does not exist or has expired] [task id: {}]", tid);
            return R.ok();
        } else {
            try {
                for (ThreadGroup threadGroup : threadGroupListForTask) {
                    log.info("[cancelTask] [discovery task cancel] [task id: {}] [canceled thread num: {}]", tid, threadGroup.activeCount());
                    // thread interrupt
                    threadGroup.interrupt();
                }
            } catch (Exception e) {
                log.error(e, "[cancelTask] [error] [tid: {}] [cache: {}]", tid, JSONUtil.toJsonStr(threadGroupListForTask));
            }
        }

        // remove cache
        List<String> keys = Tool.ListUtil.of(
                StrUtil.concat(true, "discovery_stat_", tid),
                StrUtil.concat(true, "discovery_result_", tid));
        redisTemplate.delete(keys);

        DISCOVERY_CACHE.remove(StrUtil.concat(true, "discovery_threadGroup_", tid));
        return R.ok();
    }

    /**
     * Convert a 4-element array into dotted decimal format
     *
     * @param octets
     * @return
     * @see org.apache.commons.net.util.SubnetUtils
     */
    public static String format(int[] octets) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < octets.length; ++i) {
            str.append(octets[i]);
            if (i != octets.length - 1) {
                str.append(".");
            }
        }
        return str.toString();
    }

    /**
     * Convert a packed integer address into a 4-element array
     *
     * @param val
     * @return
     * @see org.apache.commons.net.util.SubnetUtils
     */
    public static int[] toArray(int val) {
        int ret[] = new int[4];
        for (int j = 3; j >= 0; --j) {
            ret[j] |= ((val >>> 8 * (3 - j)) & (0xff));
        }
        return ret;
    }

    public class DiscoveryRunnable extends Thread {

        private final static String SYSNAME_OID = "1.3.6.1.2.1.1.5.0";
        private final static String SYSOBJECTID_OID = "1.3.6.1.2.1.1.2.0";
        private final static String SYSDESCR_OID = "1.3.6.1.2.1.1.1.0";
        private final static String SYSUPTIME_OID = "1.3.6.1.2.1.1.3.0";

        private ThreadGroup tg;

        private String ip;
        private String tid;
        private List<Agent> agentList;
        private List<SnmpCredential> credentialList;
        private List<AssetAsset> discoveredAssetList;
        private Map<String, Object> confParams;

        private Integer timeout;
        private Integer retries;
        private boolean isInterrupted = false;

        public DiscoveryRunnable(ThreadGroup tg, String tid, String ip, List<SnmpCredential> credentialList, List<AssetAsset> discoveredAssetList, List<Agent> agentList, Map<String, Object> confParams) {
            super(tg, String.format("discovery-%s-%s", tg.getName(), ip));
            this.tg = tg;

            this.ip = ip;
            this.tid = tid;
            this.agentList = agentList;
            this.credentialList = credentialList;
            this.discoveredAssetList = discoveredAssetList;
            this.confParams = confParams;
        }

        @Override
        public void run() {
            try {
                if (this.isInterrupted()) {
                    // interrupted
                } else {
                    Map m = Tool.MapUtil.newHashMap();
                    m.put("ip", ip);
                    m.put("imported", 0);
                    m.put("state", 0);
                    AssetAsset asset = discoveredAssetList.stream().filter(obj -> StrUtil.equals(obj.getManageIp(), ip)).findFirst().orElse(null);
                    if (ObjectUtil.isNotNull(asset)) {
                        m.put("imported", 1);
                        m.put("asset", Tool.MapUtil.builder().put("id", asset.getId()).put("name", asset.getName()).map());
                        m.put("model", Tool.MapUtil.builder().put("id", asset.getModel().getId()).put("name", asset.getModel().getName()).map());
                        m.put("dc", Tool.MapUtil.builder().put("id", asset.getDc().getId()).put("name", asset.getDc().getName()).map());
                    }
                    // ping
                    boolean ipIsReachable = this.checkIpIsReachable(ip, this.getTimeout());
                    m.put("ping", ipIsReachable ? 1 : 0);
                    if (ipIsReachable) {
                        // snmp
                        // discovery Snmp Mode, 1:nz-web 2:nz-agent
                        String discoverySnmpMode = (String) confParams.get("discoverySnmpMode");
                        List<Map> snmpGetResultList = null;
                        if (StrUtil.equals("1", discoverySnmpMode)) {
                            snmpGetResultList = this.snmpGetpViaWeb();
                        } else {
                            Agent server = agentList.get(RandomUtils.nextInt(0, agentList.size()));
                            snmpGetResultList = this.snmpGetpViaAgent(server);
                        }
                        if (Tool.CollUtil.isNotEmpty(snmpGetResultList)) {
                            m.put("state", 1);
                            snmpGetResultList.parallelStream().forEach(tmap -> {
                                String oid = tmap.get("oid").toString();
                                if (StrUtil.equals(SYSNAME_OID, oid))
                                    m.put("sysName", tmap.get("value"));
                                if (StrUtil.equals(SYSOBJECTID_OID, oid))
                                    m.put("sysObjectId", tmap.get("value"));
                                if (StrUtil.equals(SYSDESCR_OID, oid))
                                    m.put("sysDescr", tmap.get("value"));
                                if (StrUtil.equals(SYSUPTIME_OID, oid))
                                    m.put("sysUptime", tmap.get("value"));
                            });
                        }
                    }
                    if (!this.isInterrupted()) {
                        redisTemplate.opsForList().leftPush(StrUtil.concat(true, "discovery_result_", tid), JSONUtil.toJsonStr(m));
                    }
                }
            } catch (Exception e) {
                log.error(e, "[DiscoveryRunnable] [error] [ip: {}]", ip);
            } finally {
                ReflectUtil.invoke(tg, "remove", this);
            }
        }

        /**
         * check Ip IsR eachable
         *
         * @return a {@code boolean} indicating if the address is reachable.
         */
        private boolean checkIpIsReachable(String ip, Integer timeout) {
            boolean isReachable = false;
            try {
                InetAddress inetAddress = InetAddress.getByName(ip);
                // timeout the time, in milliseconds, before the call aborts
                isReachable = inetAddress.isReachable(timeout);
            } catch (Exception e) {
                log.error(e, "[checkIpIsReachable] [error in check process] [ip: {}]", ip);
            }
            return isReachable;
        }

        /**
         * snmpGetpViaWeb
         *
         * @return
         */
        private List<Map> snmpGetpViaWeb() {
            List<Map> snmpGetResultList = Tool.ListUtil.list(true);
            List<String> oidList = Tool.ListUtil.of(SYSNAME_OID, SYSOBJECTID_OID, SYSDESCR_OID, SYSUPTIME_OID);
            for (SnmpCredential credential : credentialList) {
                SnmpUtil snmpUtil1 = new SnmpUtil(ip, credential);
                try {
                    snmpUtil1.setTimeout(this.getTimeout());
                    snmpUtil1.setRetries(this.getRetries());
                    snmpUtil1.setOidList(oidList);
                    snmpGetResultList = snmpUtil1.snmpMulitGet();
                    if (Tool.CollUtil.isNotEmpty(snmpGetResultList)) {
                        break;
                    }
                } finally {
                    snmpUtil1.close();
                }
            }
            return snmpGetResultList;
        }

        /**
         * snmpGetpViaAgent
         *
         * @param server
         * @return
         */
        private List<Map> snmpGetpViaAgent(Agent server) {
            String path = "/cmd/snmp";
            Map requestBody = Tool.MapUtil.newHashMap();
            requestBody.put("ips", Tool.ListUtil.of(ip));
            requestBody.put("oids", Tool.ListUtil.of(SYSNAME_OID, SYSOBJECTID_OID, SYSDESCR_OID, SYSUPTIME_OID));
            requestBody.put("operation", "get");
            requestBody.put("timeout", this.getTimeout());
            requestBody.put("retries", this.getRetries());
            requestBody.put("snmpCredentialList", credentialList);
            String result = AgentUtil.request(server, HttpMethod.POST, path, null, requestBody, String.class);
            result = StrUtil.emptyToDefault(result, StrUtil.EMPTY_JSON);
            Object code = JSONPath.read(result, "code");
            if (ObjectUtil.notEqual(RCode.SUCCESS.getCode(), code)) {
                log.error("[DiscoveryRunnable] [request /cmd/snmp api error] [agent id: {}] [request body: {}] [result: {}]", server.getId(), JSONUtil.toJsonStr(requestBody), result);
                return Tool.ListUtil.list(true);
            } else {
                Object list = JSONPath.read(result, "data.list");
                List<Map> mapList = JSONArray.parseArray(JSONArray.toJSONString(list), Map.class);
                return mapList;
            }
        }

        @Override
        public void interrupt() {
            super.interrupt();
            isInterrupted = true;
            if (log.isDebugEnabled()) {
                log.debug("[DiscoveryRunnable] [interrupt] [name: {}] [ip: {}]", this.getName(), ip);
            }
        }

        @Override
        public boolean isInterrupted() {
            return isInterrupted;
        }

        /**
         * getTimeout
         * range: 1~60000
         *
         * @return
         */
        public Integer getTimeout() {
            // 超时时间，单位毫秒，默认：1000，范围：1~60000
            timeout = Tool.ObjectUtil.defaultIfNull((Integer) confParams.get("timeout"), 1000);
            timeout = timeout < 1 ? 1 : timeout;
            timeout = timeout > 60000 ? 60000 : timeout;
            return timeout;
        }

        /**
         * getRetries
         * range: 1~100
         *
         * @return
         */
        public Integer getRetries() {
            // 重试次数，默认：1，范围：1~100
            retries = Tool.ObjectUtil.defaultIfNull((Integer) confParams.get("retries"), 1);
            retries = retries < 1 ? 1 : retries;
            retries = retries > 100 ? 100 : retries;
            return retries;
        }
    }
}
