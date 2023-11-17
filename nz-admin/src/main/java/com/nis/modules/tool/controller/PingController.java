package com.nis.modules.tool.controller;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.TimedCacheManager;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.AgentService;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.tool.utils.ToolConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@RestController
@RequestMapping("/tool")
public class PingController {

    private final static Log log = Log.get();

    /**
     * ping job timeout (seconds) 默认五分钟
     */
    @Value("${tool.ping.job.timeout:300}")
    private Long timeout;

    @Autowired
    private AgentService agentService;

    @Autowired
    private DcService dcService;

    static final List<Thread> ACTIVE_TASKS = Collections.synchronizedList(new ArrayList<>());

    @GetMapping("/ping")
    public R ping(@RequestParam Map<String, Object> params) {
        String ips = (String) params.get("ips");
        if (Tool.StrUtil.isEmpty(ips)) throw new NZException(RCode.TOOL_PING_IP_ISNULL);

        String dcIds = (String) params.get("dcIds");

        List<Dc> dcList = dcService.list(new LambdaQueryWrapper<Dc>().in(Tool.StrUtil.isNotEmpty(dcIds), Dc::getId, Tool.StrUtil.split(dcIds, ',')));

        // 根据 dcids 获取当前 state = up 的所有 agent
        List<Agent> agentList = agentService.list(new LambdaQueryWrapper<Agent>()
                .in(Tool.StrUtil.isNotEmpty(dcIds), Agent::getDcId, Tool.StrUtil.split(dcIds, ','))
                .eq(Agent::getStatus, 1));

        List<Dc> futureDcList = Tool.ListUtil.list(false);
        for (Dc dc : dcList) {
            Agent agentByDc = agentList.stream().filter(p -> dc.getId().equals(p.getDcId())).findFirst().orElse(null);
            dc.setAgent(agentByDc);
            futureDcList.add(dc);
        }

        Map<String, Object> requestParam = Tool.MapUtil.newHashMap();

        String packageSize = Tool.ObjectUtil.defaultIfEmpty((String) params.get("packageSize"), "64");
        String count = Tool.ObjectUtil.defaultIfEmpty((String) params.get("count"), "4");
        String pingCmdTimeout = Tool.ObjectUtil.defaultIfEmpty((String) params.get("timeout"), "100");
        String concurrent = Tool.ObjectUtil.defaultIfEmpty((String) params.get("concurrent"), "10");

        requestParam.put("packetsize", packageSize);
        requestParam.put("count", count);
        requestParam.put("timeout", pingCmdTimeout);
        requestParam.put("concurrent", concurrent);

        String tid = UUID.randomUUID().toString();
        List<String> ipList = Tool.StrUtil.split(ips, ',');

        Map m = Tool.MapUtil.newHashMap();
        m.put("tid", tid);
        Map task = Tool.MapUtil.newHashMap();
        task.put("total", (int) Tool.NumberUtil.mul(futureDcList.size(), ipList.size()));
        task.put("done", 0);
        task.put("process", 0);
        m.put("task", task);

        // ping stat in cache
        TimedCache<String, Object> cache = TimedCacheManager.getCache(ToolConstant.PING_CACHE_PREFIX, tid);
        String stat = Tool.StrUtil.concat(true, "test_ping_stat_", tid);
        cache.put(stat, JSONObject.toJSONString(task));

        ThreadUtil.execAsync(() -> {
            ReentrantReadWriteLock readWriteLock = Tool.LockUtil.createReadWriteLock(false);
            ReentrantReadWriteLock.WriteLock lock = readWriteLock.writeLock();
            for (Dc dc : futureDcList) {
                for (String ip : ipList) {
                    Map<String, Object> map = Tool.MapUtil.newHashMap();
                    map.putAll(requestParam);
                    map.put("ips", Tool.StrUtil.split(ip, ','));

                    PingRunnable pingRunnable = new PingRunnable(tid, lock, dc, Long.valueOf(timeout), TimeUnit.SECONDS, map);
                    pingRunnable.setName(Tool.StrUtil.concat(true, tid, "-", dc.getName()));
                    ThreadUtil.execAsync(pingRunnable);
                }
            }
        });
        return R.ok(m);
    }

    @GetMapping("/ping/result/{tid}")
    public R pingResult(@PathVariable(value = "tid", required = true) String tid) {
        TimedCache<String, Object> cache = TimedCacheManager.getCache(ToolConstant.PING_CACHE_PREFIX, tid);

        String pingStatKey = Tool.StrUtil.concat(true, "test_ping_stat_", tid);
        String pingStatStr = (String) cache.get(pingStatKey);

        // 任务已被取消或 key 超时
        ValidateUtils.is(pingStatStr).notNull(RCode.TOOL_PING_TASK_NOT_EXIST);

        String pingResultKey = Tool.StrUtil.concat(true, "test_ping_result_", tid);
        List<String> pingResultList = CollUtil.defaultIfEmpty((List<String>) cache.get(pingResultKey), CollUtil.list(true));
        int completedSize = pingResultList.size();

        Map pingStatMap = JSONObject.parseObject(pingStatStr, Map.class);
        Integer total = (Integer) pingStatMap.get("total");
        Integer done = (Integer) pingStatMap.get("done");

        // 增量返回
        pingResultList = ListUtil.sub(pingResultList, done, completedSize);
        List list = Tool.ListUtil.list(false);
        for (String jsonStr : pingResultList) {
            JSONArray jsonArray = JSONArray.parseArray(jsonStr);
            list.addAll(jsonArray);
        }

        pingStatMap.put("done", completedSize);
        String percent = NumberUtil.formatPercent((double) completedSize / total, 2);
        pingStatMap.put("process", percent.replace("%", ""));

        // 更新 cache test_ping_stat
        cache.put(pingStatKey, JSONObject.toJSONString(pingStatMap));

        Map result = Tool.MapUtil.newHashMap();
        result.put("task", pingStatMap);
        result.put("list", list);
        return R.ok(result);
    }

    @GetMapping("/ping/cancel/{tid}")
    public R pingCancel(@PathVariable(value = "tid", required = false) String tid) {
        if (Tool.CollectionUtil.isNotEmpty(ACTIVE_TASKS)) {
            ACTIVE_TASKS.stream().filter(thread -> Tool.StrUtil.startWith(thread.getName(), tid)).forEach(thread -> {
                log.info(String.format("Ping thread: %s has been canceled", thread.getName()));
                thread.interrupt();
            });
        }

        TimedCacheManager.removeCache(ToolConstant.PING_CACHE_PREFIX, tid);
        return R.ok();
    }

    public class PingRunnable extends Thread {
        private String tid;
        private Lock lock;
        private Dc dc;
        private long timeout;
        private TimeUnit timeUnit;
        private Map<String, Object> requestBody;

        public PingRunnable(String tid, Lock lock, Dc dc, long timeout, TimeUnit timeUnit, Map<String, Object> requestBody) {
            this.dc = dc;
            this.tid = tid;
            this.lock = lock;
            this.timeout = timeout;
            this.timeUnit = timeUnit;
            this.requestBody = requestBody;
        }

        @Override
        public void run() {
            ACTIVE_TASKS.add(this);
            // agent 存在，向 agent 发起请求获取数据
            if (Optional.ofNullable(dc).map(Dc::getAgent).isPresent()) {
                Agent agent = dc.getAgent();
                CompletableFuture<String> future = this.createCompletableFuture(agent);
                try {
                    String result = future.get(timeout, timeUnit);
                    // update result
                    if (!Thread.currentThread().isInterrupted())
                        this.updateResult(result);
                } catch (InterruptedException | ExecutionException e) {
                    log.error(e);
                } catch (TimeoutException e) {
                    boolean cancel = future.cancel(true);
                    log.error(String.format("%s agent ping %s timeout, Task cancel status: %s", agent.getName(), requestBody.get("ips").toString(), cancel));
                    if (!Thread.currentThread().isInterrupted()) {
                        log.info(String.format("%s agent ping %s timeout, Timeout data write to cache", agent.getName(), requestBody.get("ips").toString()));
                        // 超时也需要向 cache 中写入 timeout 消息
                        Map agentMap = Tool.MapUtil.newHashMap(4);
                        agentMap.put("id", agent.getId());
                        agentMap.put("name", agent.getName());

                        Map dcMap = Tool.MapUtil.newHashMap(4);
                        dcMap.put("id", dc.getId());
                        dcMap.put("name", dc.getName());
                        List<String> timeOutIpList = (List<String>) requestBody.get("ips");
                        List<Map> list = Tool.ListUtil.list(false);
                        for (String timeOutIp : timeOutIpList) {
                            Map timeoutInfoMap = Tool.MapUtil.of(
                                    Pair.of("ip", timeOutIp),
                                    Pair.of("state", 2),
                                    Pair.of("rate", null),
                                    Pair.of("min", null),
                                    Pair.of("avg", null),
                                    Pair.of("max", null),
                                    Pair.of("source", agent.getHost()),
                                    Pair.of("raw", "Timeout"),
                                    Pair.of("agent", agentMap),
                                    Pair.of("dc", dcMap)
                            );
                            list.add(timeoutInfoMap);
                        }
                        this.updateResult(JSONObject.toJSONString(list));
                    } else {
                        log.info(String.format("%s agent ping %s timeout, But the thread is interrupted, and the timeout data is not write to cache.", agent.getName(), requestBody.get("ips").toString()));
                    }
                }
            } else {
                // agent 不存在（DC 下没有可用 agent,记录错误数据至 cache 中）
                if (!Thread.currentThread().isInterrupted()) {
                    log.info(String.format("Dc id : %s , No PromServer available", dc.getId()));
                    Map dcMap = Tool.MapUtil.newHashMap(4);
                    dcMap.put("id", dc.getId());
                    dcMap.put("name", dc.getName());
                    List<String> ipList = (List<String>) requestBody.get("ips");
                    List<Map> list = Tool.ListUtil.list(false);
                    for (String ip : ipList) {
                        Map noAgentInfo = Tool.MapUtil.of(
                                Pair.of("ip", ip),
                                Pair.of("state", 2),
                                Pair.of("rate", null),
                                Pair.of("min", null),
                                Pair.of("avg", null),
                                Pair.of("max", null),
                                Pair.of("source", ""),
                                Pair.of("raw", "No agent available"),
                                Pair.of("agent", Tool.MapUtil.empty()),
                                Pair.of("dc", dcMap)
                        );
                        list.add(noAgentInfo);
                    }
                    this.updateResult(JSONObject.toJSONString(list));
                } else {
                    log.info(String.format("Dc id : %s , No PromServer available, But the thread is interrupted, not write to cache.", dc.getId()));
                }
            }
            ACTIVE_TASKS.remove(this);
        }

        /**
         * createCompletableFuture
         *
         * @param agent
         * @return
         */
        private CompletableFuture<String> createCompletableFuture(Agent agent) {
            return CompletableFuture.supplyAsync(() -> {
                String path = "/cmd/ping";
                try {
                    String resultJsonStr = AgentUtil.request(agent, HttpMethod.POST, path, null, requestBody, String.class);
                    Object list = JSONPath.read(resultJsonStr, "data.list");
                    List<Map> mapList = JSONArray.parseArray(JSONArray.toJSONString(list), Map.class);
                    for (Map map : mapList) {
                        Map dcMap = Tool.MapUtil.newHashMap(4);
                        dcMap.put("id", dc.getId());
                        dcMap.put("name", dc.getName());

                        Map agentMap = Tool.MapUtil.newHashMap(4);
                        agentMap.put("id", agent.getId());
                        agentMap.put("name", agent.getName());

                        map.put("dc", dcMap);
                        map.put("agent", agentMap);
                        // source 赋值为 agent ip
                        map.put("source", agent.getHost());
                    }
                    return JSONArray.toJSONString(mapList);
                } catch (Exception e) {
                    log.error(e, "host: {}, path: {}", agent.getHost(), path);
                }
                return StrUtil.EMPTY;
            });
        }

        /**
         * updateResult
         *
         * @param resultData
         */
        private void updateResult(String resultData) {
            if (StrUtil.isEmpty(resultData))
                return;

            lock.lock();
            try {
                TimedCache<String, Object> cache = TimedCacheManager.getCache(ToolConstant.PING_CACHE_PREFIX, tid);

                // result cache
                String resultKey = Tool.StrUtil.concat(true, "test_ping_result_", tid);
                List<String> resultList = (List<String>) cache.get(resultKey);
                resultList = Tool.CollUtil.defaultIfEmpty(resultList, Tool.ListUtil.list(true));

                resultList.add(resultData);
                cache.put(resultKey, resultList, TimeUnit.SECONDS.toMillis(60));
            } catch (Exception e) {
                log.error(e);
            } finally {
                lock.unlock();
            }
        }
    }

}
