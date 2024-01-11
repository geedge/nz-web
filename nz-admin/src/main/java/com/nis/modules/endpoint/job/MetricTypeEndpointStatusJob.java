package com.nis.modules.endpoint.job;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.*;
import com.nis.common.utils.Constant.AgentType;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.AgentService;
import com.nis.modules.agent.service.PromApiService;
import com.nis.modules.election.dao.LeaderElectionDao;
import com.nis.modules.endpoint.entity.MonitorEndpointConfig;
import com.nis.modules.endpoint.entity.MonitorEndpointState;
import com.nis.modules.endpoint.service.MonitorEndpointConfigService;
import com.nis.modules.endpoint.service.MonitorEndpointStateService;
import com.nis.modules.sys.service.SysConfService;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

@DisallowConcurrentExecution
public class MetricTypeEndpointStatusJob extends QuartzJobBean {

    private static final Log log = Log.get();

    @Autowired
    private PromApiService promApiService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private SysConfService sysConfService;

    @Autowired
    private MonitorEndpointStateService endpointStateService;

    @Autowired
    private MonitorEndpointConfigService endpointConfigService;

    /**
     * 计数器，每 forceSync 个周期 执行一次强制执行
     */
    private volatile static Integer counter = 1;

    @Value("${endpoint.state.force.sync:8}")
    private Integer forceSync;

    private static ExecutorService threadPool = Executors.newCachedThreadPool();

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Thread.currentThread().setName("EndpointStateJob");

        // 检查 HA_LOCK
        LeaderElectionDao dao = LeaderElectionDao.getInstance();
        boolean isLeader = dao.isLeader(Constant.SYS_HA_LOCK, Constant.SERVER_ID);
        log.info("[MetricTypeEndpointStatusJob] [Get HA_LOCK] [isLeader: {}]", isLeader);
        if (BooleanUtil.negate(isLeader)) return;

        log.info("[EndpointStateJob] [Begin]");
        StopWatch sw = new StopWatch();
        sw.start();
        try {
            // agents For Scrape Endpoints
            List<Agent> agentsForScrapeEndpoints = this.getAgentsForScrapeEndpointsByConfig();

            if (Tool.CollUtil.isEmpty(agentsForScrapeEndpoints)) {
                log.warn("[MetricTypeEndpointStatusJob] [no agent available. update all metrics type endpoint to down]");
                this.updateAllMetricTypeEndpointsToDown();
            } else {
                // update Endpoint Status
                this.updateEndpointStatus(agentsForScrapeEndpoints);
            }
        } catch (Exception e) {
            log.error(e, "[EndpointStateJob Error]");
        } finally {
            sw.stop();
        }
        log.info("[EndpointStateJob] [Finshed] [Run Time: {}]", sw.toString());
    }

    /**
     * get Agents For Scrape Endpoints By Config
     * sys_config.prometheus_federation_enabled
     *
     * @return
     */
    private List<Agent> getAgentsForScrapeEndpointsByConfig() {
        // federation enable
        String federationEnabled = sysConfService.getValueOrDefault(Constant.SYSCONFIG_KEY_PROM_FEDER_ENABLED, "1");
        log.info("[getAgentsForScrapeEndpointsByConfig] [prometheus_federation_enabled] [value: {}]", federationEnabled);

        List<Agent> allAgentsForScrapeEndpoints = Tool.ListUtil.list(true);

        if (StrUtil.equals("0", federationEnabled)) {
            // 关闭 federation ，Global scrape endpoint jobs
            log.info("[getAgentsForScrapeEndpointsByConfig] [federation is not enabled. get global agents]");
            allAgentsForScrapeEndpoints = agentService.list(new LambdaQueryWrapper<Agent>().eq(Agent::getType, AgentType.GLOBAL.getValue()));
        } else if (StrUtil.equals("1", federationEnabled)) {
            // 开启 federation ，Per-datacenter scrape endpoint jobs
            log.info("[getAgentsForScrapeEndpointsByConfig] [federation is enabled. get per-datacenter agents]");
            allAgentsForScrapeEndpoints = agentService.list(new LambdaQueryWrapper<Agent>().eq(Agent::getType, AgentType.PER_DATACENTER.getValue()));
        }

        List<Agent> healthyAgents = allAgentsForScrapeEndpoints.stream().filter(agent -> Tool.ObjectUtil.equal(agent.getStatus(), 1)).collect(Collectors.toList());

        if (Tool.ObjectUtil.notEqual(allAgentsForScrapeEndpoints.size(), healthyAgents.size())) {
            log.warn("[getAgentsForScrapeEndpointsByConfig] [has error status agent] [total size: {}] [error size: {}]", allAgentsForScrapeEndpoints.size(), allAgentsForScrapeEndpoints.size() - healthyAgents.size());
            if (log.isDebugEnabled()) {
                List<Agent> errorAgents = allAgentsForScrapeEndpoints.stream().filter(agent -> Tool.ObjectUtil.notEqual(agent.getStatus(), 1)).collect(Collectors.toList());
                log.debug("[getAgentsForScrapeEndpointsByConfig] [has error status agent] [error agents: {}]", Tool.JSONUtil.toJsonStr(errorAgents));
            }
        }

        log.info("[getAgentsForScrapeEndpointsByConfig] [agents size: {}]", healthyAgents.size());
        return healthyAgents;
    }

    @SuppressWarnings("unlikely-arg-type")
    @Transactional(rollbackFor = Exception.class)
    public void updateEndpointStatus(List<Agent> agentsForScrapeEndpoints) {

        List<MonitorEndpointState> updateMetricsEndpointStateList = this.getCurrentMetricsEndpointStatusList(agentsForScrapeEndpoints);
        log.info("[updateEndpointStatus] [update metrics endpoints] [total size: {}]", updateMetricsEndpointStateList.size());

        //  判断查询数据与缓存中变化的信息
        TimedCache<String, Object> metricsTypeEndpointStateCache = LocalCacheManager.getTimedCache(Constant.SYS_STATE_CACHE_PREFIX, Constant.ENDPOINT_METRICS_STATE_CACHE_KEY);
        if (BooleanUtil.negate(metricsTypeEndpointStateCache.isEmpty()) && counter % forceSync != 0) {
            log.info("[updateEndpointStatus] [incrementally update the metrics endpoint state in redis]");
            for (int i = updateMetricsEndpointStateList.size() - 1; i >= 0; i--) {
                MonitorEndpointState endpointState = updateMetricsEndpointStateList.get(i);
                if (metricsTypeEndpointStateCache.containsKey(endpointState.getEndpointId().toString())) {
                    MonitorEndpointState cacheEndpointState = JSON.parseObject((String) metricsTypeEndpointStateCache.get(endpointState.getEndpointId().toString()), MonitorEndpointState.class);
                    metricsTypeEndpointStateCache.put(endpointState.getEndpointId().toString(), JSON.toJSONString(endpointState));
                    if (cacheEndpointState.getState().equals(endpointState.getState())) {
                        // 如果状态相同则只更新缓存中的数据信息
                        updateMetricsEndpointStateList.remove(i);
                    }
                } else {
                    // 数据库存在 缓存中不存在
                    metricsTypeEndpointStateCache.put(endpointState.getEndpointId().toString(), JSON.toJSONString(endpointState));
                }
            }
        } else {
            log.info("[updateEndpointStatus] [Full update cache]");
            Map<String, String> updateMetricsEndpointsInRedis = updateMetricsEndpointStateList.stream().collect(Collectors.toMap(state -> StrUtil.toString(state.getEndpointId()), state -> JSONUtil.toJsonStr(state)));
            for (Map.Entry<String, String> entry : updateMetricsEndpointsInRedis.entrySet()) {
                metricsTypeEndpointStateCache.put(entry.getKey(), entry.getValue());
            }
        }

        // 保存 根据类型以及endpointId进行新增或者修改
        if (ToolUtil.isNotEmpty(updateMetricsEndpointStateList)) {
            // 将数据分为新增或者修改
            List<Integer> endpointIds = updateMetricsEndpointStateList.stream().map(MonitorEndpointState::getEndpointId).collect(Collectors.toList());
            List<MonitorEndpointState> existStates = endpointStateService.list(new LambdaQueryWrapper<MonitorEndpointState>()
                    .in(MonitorEndpointState::getEndpointId, endpointIds).eq(MonitorEndpointState::getType, Constant.MonitorConfType.METRICS.getValue()));
            List<Integer> existIds = existStates.stream().map(MonitorEndpointState::getEndpointId).collect(Collectors.toList());
            List<MonitorEndpointState> batchUpdateStates = updateMetricsEndpointStateList.stream().filter(s -> existIds.contains(s.getEndpointId())).collect(Collectors.toList());
            List<MonitorEndpointState> batchAddStates = updateMetricsEndpointStateList.stream().filter(s -> !existIds.contains(s.getEndpointId())).collect(Collectors.toList());
            if (ToolUtil.isNotEmpty(batchAddStates)) {
                if (log.isDebugEnabled()) {
                    log.debug("[updateEndpointStatus] [add endpoint status] [values: {}]", JSON.toJSON(batchAddStates));
                }
                endpointStateService.saveBatch(batchAddStates);
            }
            if (ToolUtil.isNotEmpty(batchUpdateStates)) {
                if (log.isDebugEnabled()) {
                    log.debug("[updateEndpointStatus] [update endpoint status] [values: {}]", JSON.toJSON(batchAddStates));
                }
                endpointStateService.modifyBatchEndpointState(batchUpdateStates, Constant.MonitorConfType.METRICS.getValue());
            }
        }
        if (counter % forceSync == 0) {
            counter = 1;
        } else {
            counter++;
        }
        log.info("[updateEndpointStatus] [update state finshed] [counter: {}] [forceSync: {}]", counter, forceSync);
    }

    /**
     * get Current Metrics Endpoint Status List
     * 通过查询 prometheus /api/v1/targets 获得 endpoint 状态信息，结合 数据库 中 Endpoint 相关逻辑内容，返回当前所有 Metrics Endpoint State 实体
     *
     * @param agentsForScrapeEndpoints
     * @return
     */
    private List<MonitorEndpointState> getCurrentMetricsEndpointStatusList(List<Agent> agentsForScrapeEndpoints) {
        // metric Type Endpoint Config List
        List<MonitorEndpointConfig> metricTypeEndpointConfigList = endpointConfigService.getEndpointConfigInfoListByType(Constant.MonitorConfType.METRICS.getValue());
        Map<Integer, MonitorEndpointConfig> endpointIdAndConfigMapping = metricTypeEndpointConfigList.stream().collect(Collectors.toMap(MonitorEndpointConfig::getEndpointId, Function.identity()));

        // everything but up is down
        Map<Integer, MonitorEndpointConfig> downMetricsEndpointIdAndConfigMapping = ObjectUtil.cloneByStream(endpointIdAndConfigMapping);

        // active Target List
        List<JSONObject> activeTargetList = this.queryTargetsApiToGetEndpointStatus(agentsForScrapeEndpoints);
        log.info("[getCurrentMetricsEndpointStatusList] [query active Target] [szie: {}]", activeTargetList.size());

        // up
        List<MonitorEndpointState> upMetricsEndpointStateList = Tool.ListUtil.list(true);
        activeTargetList.stream()
                .filter(jsonObject -> StrUtil.equalsIgnoreCase("up", JSONPath.read(jsonObject.toJSONString(), "health").toString()))
                .map(jsonObject -> JSONPath.read(jsonObject.toJSONString(), "labels.endpoint_id").toString())
                .forEach(endpointIdStr -> {
                    Integer endpointId = Integer.valueOf(endpointIdStr);
                    MonitorEndpointConfig confObj = endpointIdAndConfigMapping.get(endpointId);
                    if (ObjectUtil.isNull(confObj)) {
                        log.warn("[getCurrentMetricsEndpointStatusList] [Can't get endpoint config by activeTarget's endpoint_id value] [activeTarget's endpoint_id: {}]", endpointId);
                        return;
                    }
                    // up endpoint state
                    upMetricsEndpointStateList.add(new MonitorEndpointState(endpointId, confObj.getId(), Constant.MonitorConfType.METRICS.getValue(), 1, confObj.getVersion(), StrUtil.EMPTY, DateTime.now()));

                    // remove up endpoint
                    downMetricsEndpointIdAndConfigMapping.remove(endpointId);
                });

        // down & suspended
        Map<Integer, String> downMetricsEndpointErrorMsgMapping = activeTargetList.stream()
                .filter(jsonObject -> StrUtil.equalsIgnoreCase("down", JSONPath.read(jsonObject.toJSONString(), "health").toString()))
                .collect(Collectors.toMap(
                        jsonObject -> Integer.valueOf(JSONPath.read(jsonObject.toJSONString(), "labels.endpoint_id").toString()),
                        jsonObject -> JSONPath.read(jsonObject.toJSONString(), "lastError").toString()
                ));
        List<MonitorEndpointState> downMetricsEndpointStateList = Tool.ListUtil.list(true);
        List<MonitorEndpointState> suspendedMetricsEndpointStateList = Tool.ListUtil.list(true);
        for (MonitorEndpointConfig conf : downMetricsEndpointIdAndConfigMapping.values()) {
            Integer state = 0;
            // 0：关闭 1：开启
            Integer monitor = conf.getStateConf().getMonitor();
            // 0：停用 1：启用
            Integer enabled = conf.getEnable();
            // 开启：ON, 关闭：OFF
            String dcStateStr = conf.getDc().getState();
            boolean dcState = StrUtil.equals("ON", dcStateStr) ? true : false;
            if (!dcState && monitor.equals(0) && enabled.equals(1)) {
                state = 2;
            } else if (!dcState && monitor.equals(1) && enabled.equals(0)) {
                state = 4;
            } else if (dcState && monitor.equals(0) && enabled.equals(0)) {
                state = 8;
            } else if (!dcState && monitor.equals(1) && enabled.equals(1)) {
                state = 6;
            } else if (dcState && monitor.equals(0) && enabled.equals(1)) {
                state = 10;
            } else if (dcState && monitor.equals(1) && enabled.equals(0)) {
                state = 12;
            }

            // lastError msg in prometheus target api return data
            String lastError = downMetricsEndpointErrorMsgMapping.get(conf.getEndpointId());
            MonitorEndpointState endpointState = new MonitorEndpointState(conf.getEndpointId(), conf.getId(), conf.getType(), state, conf.getVersion(), StrUtil.emptyToDefault(lastError, StrUtil.EMPTY), DateTime.now());
            if (ObjectUtil.equal(0, state)) {
                downMetricsEndpointStateList.add(endpointState);
            } else {
                suspendedMetricsEndpointStateList.add(endpointState);
            }
        }

        List<MonitorEndpointState> updateEndpointStateList = Tool.ListUtil.list(true);
        updateEndpointStateList.addAll(upMetricsEndpointStateList);
        updateEndpointStateList.addAll(downMetricsEndpointStateList);
        updateEndpointStateList.addAll(suspendedMetricsEndpointStateList);
        log.info("[getCurrentMetricsEndpointStatusList] [metrics endpoint state size] [total: {}] [up: {}] [down: {}] [suspended: {}]", updateEndpointStateList.size(), upMetricsEndpointStateList.size(), downMetricsEndpointStateList.size(), suspendedMetricsEndpointStateList.size());
        return updateEndpointStateList;
    }

    /**
     * query Targets Api To Get Endpoint Status
     *
     * @param agentsForScrapeEndpoints
     * @return prometheus targets API result data
     */
    private List<JSONObject> queryTargetsApiToGetEndpointStatus(List<Agent> agentsForScrapeEndpoints) {
        log.info("[queryTargetsApiToGetEndpointStatus] [begin] [query agent size: {}]", agentsForScrapeEndpoints.size());

        StopWatch sw = new StopWatch();
        sw.start();

        // all Active Target List
        List<JSONObject> allActiveTargetList = Tool.ListUtil.list(true);

        try {
            List<Callable<JSONObject>> tasks = Tool.ListUtil.list(true);
            for (Agent agent : agentsForScrapeEndpoints) {
                tasks.add(() -> {
                    JSONObject targets = new JSONObject();
                    try {
                        targets = promApiService.targets(null, StrUtil.toString(agent.getId()), null, null, Tool.ListUtil.empty(), null);
                    } catch (Exception e) {
                        log.error(e, "[queryTargetsApiToGetEndpointStatus] [query /targets error] [agent: {}]", JSONUtil.toJsonStr(agent));
                    }
                    return targets;
                });
            }

            List<Future<JSONObject>> futures = threadPool.invokeAll(tasks);
            for (Future<JSONObject> future : futures) {
                if (!future.isCancelled()) {
                    JSONObject jsonObject = future.get();
                    // activeTargets
                    Object activeTargets = JSONPath.read(jsonObject.toJSONString(), "data.activeTargets");
                    List list = (List) Tool.ObjectUtil.defaultIfNull(activeTargets, Tool.ListUtil.empty());
                    allActiveTargetList.addAll(list);
                }
            }

        } catch (Exception e) {
            log.error(e, "[queryTargetsApiToGetEndpointStatus] [get result error]");
        } finally {
            sw.stop();
        }

        allActiveTargetList = allActiveTargetList.stream()
                .filter(Objects::nonNull)
                .filter(jsonObject -> ObjectUtil.isNotNull(JSONPath.read(jsonObject.toJSONString(), "labels.endpoint_id")))
                .collect(Collectors.toList());
        log.info("[queryTargetsApiToGetEndpointStatus] [finshed] [Active Target List: {}] [RunTime: {}]", allActiveTargetList.size(), sw.toString());
        return allActiveTargetList;
    }


    /**
     * update All Metric Type Endpoints To Down
     * db & redis
     */
    private void updateAllMetricTypeEndpointsToDown() {
        log.info("[updateAllMetricTypeEndpointsToDown] [begin]");

        List<MonitorEndpointConfig> metricsEndpointConfigList = endpointConfigService.list(new LambdaQueryWrapper<MonitorEndpointConfig>().eq(MonitorEndpointConfig::getType, Constant.MonitorConfType.METRICS.getValue()));
        log.info("[updateAllMetricTypeEndpointsToDown] [metrics endpoint size: {}]", metricsEndpointConfigList.size());
        if (Tool.CollUtil.isNotEmpty(metricsEndpointConfigList)) {
            // db
            // remove
            endpointStateService.remove(new LambdaQueryWrapper<MonitorEndpointState>().eq(MonitorEndpointState::getType, Constant.MonitorConfType.METRICS.getValue()));
            // insert
            List<MonitorEndpointState> saveBatchEndpointStatusList = Tool.ListUtil.list(true);
            for (MonitorEndpointConfig endpointConfig : metricsEndpointConfigList) {
                MonitorEndpointState endpointState = new MonitorEndpointState();
                endpointState.setEndpointId(endpointConfig.getEndpointId());
                endpointState.setConfigId(endpointConfig.getId());
                endpointState.setType(endpointConfig.getType());
                endpointState.setVer(endpointConfig.getVersion());
                endpointState.setState(0);
                endpointState.setMsg(RCode.AGENT_UNAVAILABLE.getMsg());
                saveBatchEndpointStatusList.add(endpointState);
            }
            endpointStateService.saveBatch(saveBatchEndpointStatusList);

            // cache
            TimedCache<String, Object> metricsTypeEndpointStateCache = LocalCacheManager.getTimedCache(Constant.SYS_STATE_CACHE_PREFIX, Constant.ENDPOINT_METRICS_STATE_CACHE_KEY);
            Map<String, String> updateMetricsEndpointsInRedis = saveBatchEndpointStatusList.stream().collect(Collectors.toMap(state -> StrUtil.toString(state.getEndpointId()), state -> JSONUtil.toJsonStr(state)));
            for (Map.Entry<String, String> entry : updateMetricsEndpointsInRedis.entrySet()) {
                metricsTypeEndpointStateCache.put(entry.getKey(), entry.getValue());
            }
        }
        log.info("[updateAllMetricTypeEndpointsToDown] [finshed]");
    }

}
