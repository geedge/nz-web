package com.nis.modules.agent.job;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.*;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.entity.MonitorEventLog;
import com.nis.modules.agent.service.AgentService;
import com.nis.modules.agent.service.MonitorEventLogService;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.election.dao.LeaderElectionDao;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import lombok.Data;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AgentJob
 *
 * @author shizhendong
 * @date 2021-04-06
 */
@DisallowConcurrentExecution
@Data
public class AutoConfigAgentJob extends QuartzJobBean {

    private static final Log log = Log.get();

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private AgentConfigUtil configUtil;

    @Autowired
    private AgentService agentService;

    @Autowired
    private AssetAssetService assetService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private MonitorEndpointService endpointService;

    @Autowired
    private MonitorEventLogService eventLogService;

    // sys_config map
    Map<String, String> sysConfigMap = new HashMap();

    @SuppressWarnings("unchecked")
    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Thread.currentThread().setName("AutoConfigAgentJob");

        // 检查 HA_LOCK
        LeaderElectionDao dao = LeaderElectionDao.getInstance();
        boolean isLeader = dao.isLeader();
        log.info("[AutoConfigAgentJob] [Get HA_LOCK] [isLeader: {}]", isLeader);
        if (BooleanUtil.negate(isLeader)) return;

        log.info("[AutoConfigAgentJob] [Begin]");
        try {
            List<Agent> healthyAgents = Tool.ListUtil.empty();
            List<String> agentCaches = redisTemplate.opsForHash().values(Constant.AGENT_STATE_CACHE_KEY);
            if (ToolUtil.isNotEmpty(agentCaches)) {
                healthyAgents = agentCaches.stream().map(agentCache -> JSON.parseObject(agentCache, Agent.class)).filter(p -> p.getStatus().equals(1)).collect(Collectors.toList());
                log.info("[AutoConfigAgentJob] [Get healthy agent list from redis] [size: {}]", healthyAgents.size());
            } else {
                healthyAgents = agentService.list(new LambdaQueryWrapper<Agent>().eq(Agent::getStatus, 1));
                log.info("[AutoConfigAgentJob] [Get healthy agent list from db] [size: {}]", healthyAgents.size());
            }

            // healthy agent info
            Map<Integer, String> healthyAgentInfoMap = healthyAgents.stream().collect(Collectors.toMap(Agent::getId, Agent::getName));
            log.info("[AutoConfigAgentJob] [Healthy agent] [size: {}] [detail: {}]", healthyAgentInfoMap.size(), JSONUtil.toJsonStr(healthyAgentInfoMap));
            if (log.isDebugEnabled()) {
                log.debug("[AutoConfigAgentJob] [Healthy agents] [detail: {}]", JSONUtil.toJsonStr(healthyAgents));
            }

            List<SysConfigEntity> sysConfigList = sysConfigService.list();
            sysConfigMap = sysConfigList.stream().collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));

            // get global agent
            List<Agent> globalAgentList = healthyAgents.stream().filter(pojo -> ObjectUtil.equal(Constant.AgentType.GLOBAL.getValue(), pojo.getType())).collect(Collectors.toList());
            log.info("[AutoConfigAgentJob] [healthy global agent list] [size: {}]", globalAgentList.size());

            if (Tool.CollectionUtil.isEmpty(globalAgentList)) {
                // no healthy global agent
                log.info("[AutoConfigAgentJob] [try To Set Up Agent Config]");
                this.trySetUpAgentConfig();
            } else {
                // config agent
                log.info("[AutoConfigAgentJob] [Set Up Agent Config]");
                this.setUpAgentConfig(healthyAgents);
            }
        } catch (Exception e) {
            log.error(e, "[AutoConfigAgentJob] [Error]");
        }
        log.info("[AutoConfigAgentJob] [Finshed]");
    }


    /**
     * no healthy global agent
     * try To Set Up Agent Config
     */
    private void trySetUpAgentConfig() {
        // get Cortex Component Down Agent List
        List<Agent> cortexComponentDownAgentList = this.getCortexComponentDownAgentList();
        log.info("[trySetUpAgentConfig] [cortex Component Down Agent List] [size: {}]", cortexComponentDownAgentList.size());

        if (Tool.CollUtil.isNotEmpty(cortexComponentDownAgentList)) {
            ThreadUtil.execAsync(() -> {
                boolean cortexChangeFlag = configUtil.compareDataChanges("cortex");
                log.info("[trySetUpAgentConfig] [conf_event cortex state] [change: {}]", cortexChangeFlag);

                if (cortexChangeFlag) {
                    log.info("[trySetUpAgentConfig] [try to set up config to the down agent of cortex] [size: {}]", cortexComponentDownAgentList.size());
                    if (log.isDebugEnabled()) {
                        log.debug("[trySetUpAgentConfig] [try to set up config to the down agent of cortex] [size: {}] [agent info: {}]", cortexComponentDownAgentList.size(), JSONUtil.toJsonStr(cortexComponentDownAgentList));
                    }

                    // try Set Up Config To Down Agent Of Cortex
                    this.trySetUpConfigToDownAgentOfCortex(cortexComponentDownAgentList);
                }
            });
        }

        // get Loki Component Down Agent List
        List<Agent> lokiComponentDownAgentList = this.getLokiComponentDownAgentList();
        log.info("[trySetUpAgentConfig] [loki Component Down Agent List] [size: {}]", lokiComponentDownAgentList.size());
        if (Tool.CollUtil.isNotEmpty(lokiComponentDownAgentList)) {
            ThreadUtil.execAsync(() -> {
                boolean lokiChangeFlag = configUtil.compareDataChanges("loki");
                log.info("[trySetUpAgentConfig] [conf_event loki state] [change: {}]", lokiChangeFlag);

                if (lokiChangeFlag) {
                    log.info("[trySetUpAgentConfig] [try to set up config to the down agent of loki] [size: {}]", lokiComponentDownAgentList.size());
                    if (log.isDebugEnabled()) {
                        log.debug("[trySetUpAgentConfig] [try to set up config to the down agent of loki] [size: {}] [agent info: {}]", lokiComponentDownAgentList.size(), JSONUtil.toJsonStr(lokiComponentDownAgentList));
                    }

                    // try Set Up Config To Down Agent Of Loki
                    this.trySetUpConfigToDownAgentOfLoki(lokiComponentDownAgentList);
                }
            });
        }
    }

    /**
     * set Up to healeht Agent Config
     *
     * @param healthyPromServers
     */
    public void setUpAgentConfig(List<Agent> healthyPromServers) {
        ThreadUtil.execAsync(() -> {
        	boolean promtailChangeFlag = configUtil.compareDataChanges("promtail");
        	MonitorEventLog monitorEventLog = eventLogService.getOne(new LambdaQueryWrapper<MonitorEventLog>().eq(MonitorEventLog::getEvent, "promtail"));
            log.info("[setUpAgentConfig] [conf_event promtail state] [change: {}] [value: {}]", promtailChangeFlag, monitorEventLog.getCounter());
            this.udpatePromtailsConf(healthyPromServers, monitorEventLog.getCounter());
        });

        ThreadUtil.execAsync(() -> {
            boolean snmpChangeFlag = configUtil.compareDataChanges("snmp");
            log.info("[setUpAgentConfig] [conf_event snmp state] [change: {}]", snmpChangeFlag);
            if (snmpChangeFlag)
                this.updateSnmpExporterConf(healthyPromServers);
        });

        ThreadUtil.execAsync(() -> {
            boolean prometheusChangeFlag = configUtil.compareDataChanges("prometheus");
            log.info("[setUpAgentConfig] [conf_event prometheus state] [change: {}]", prometheusChangeFlag);
            if (prometheusChangeFlag)
                this.updatePrometheusConf(healthyPromServers);
        });

        ThreadUtil.execAsync(() -> {
            boolean lokiChangeFlag = configUtil.compareDataChanges("loki");
            log.info("[setUpAgentConfig] [conf_event loki state] [change: {}]", lokiChangeFlag);
            if (lokiChangeFlag)
                this.updateLokiConf(healthyPromServers);
        });


        ThreadUtil.execAsync(() -> {
            boolean cortexChangeFlag = configUtil.compareDataChanges("cortex");
            log.info("[setUpAgentConfig] [conf_event cortex state] [change: {}]", cortexChangeFlag);

            boolean ruleChangeFlag = configUtil.compareDataChanges("rule");
            log.info("[setUpAgentConfig] [conf_event rule state] [change: {}]", ruleChangeFlag);

            // 是否更新 rule.yml 配置
            String alertConfUpdate = sysConfigMap.get("nz_alert_rule_eval_enable");
//            Map alertYmlMap = configUtil.generateRuleYml();
//            if (ruleChangeFlag && StrUtil.equals("0", StrUtil.emptyToDefault(alertConfUpdate, "1"))) {
//                // 生成 alert.yml
//                // 如果 cortex 没有变更 就只更新 rule
//                if (!cortexChangeFlag) {
//                    this.justUpdateRuleConf(alertYmlMap, healthyPromServers);
//                }
//            }
            if (cortexChangeFlag) {
                this.updateCortexConf(healthyPromServers);
            }
        });
    }

    /**
     * 更新 promtail 配置
     *
     * @param healthyPromServers
     * @param version
     */
    private void udpatePromtailsConf(List<Agent> healthyPromServers, Integer version) {
        List<Agent> perDataCenterPromServers = healthyPromServers.stream().filter(pojo -> Constant.AgentType.PER_DATACENTER.getValue().equals(pojo.getType())).collect(Collectors.toList());
        List<Agent> globalPromServers = healthyPromServers.stream().filter(pojo -> Constant.AgentType.GLOBAL.getValue().equals(pojo.getType())).collect(Collectors.toList());

        this.handlePerDatacenterLogs(globalPromServers, perDataCenterPromServers, version);
        this.handleGlobalLogs(globalPromServers, version);
    }

    /**
     * global logs 配置
     *
     * @param globalPromServers
     * @param version
     */
    private void handleGlobalLogs(List<Agent> globalPromServers, Integer version) {
        // 对象存储开启  global 都需要下发 lokiPushUrls config 指向自己
        // 对象存储关闭  global 都需要下发 lokiPushUrls config 指向自己 和 remote 其他 global
        for (Agent globalProm : globalPromServers) {
            Map<String, Object> lokiPushUrlConfigMap = configUtil.getLokiPushConfig(globalProm, globalPromServers);

            String result = null;
            try {
            	result = AgentUtil.request(globalProm, HttpMethod.POST, "/config", null, lokiPushUrlConfigMap, String.class);
            } catch (RuntimeException e) {
            	log.error(e,"request error,agent:{}",Tool.JSONUtil.toJsonStr(globalProm));
            }
            
            if (result == null) {
                configUtil.updateAgentStatus(globalProm, 0);
                continue;
            } else {
                configUtil.afterRequestApi("/config", result, globalProm);
            }
        }

        String fedeEnabledStr = sysConfigMap.get(Constant.SYSCONFIG_KEY_PROM_FEDER_ENABLED);
        log.info(String.format("prometheus 联邦状态: %s", StrUtil.equals("0", fedeEnabledStr) ? "关闭" : "开启"));
        if (StrUtil.equals("0", fedeEnabledStr)) {
            // 用于生成配置的 endpoint 信息
            List<MonitorEndpoint> enableEndpointInfoList = endpointService.getEnableLogsEndpointInfoListForAgent(version);
            log.info("sync logs config size: {}" , Tool.CollUtil.size(enableEndpointInfoList));
            if(Tool.CollUtil.isEmpty(enableEndpointInfoList)) {
            	return;
            }
            // 联邦关闭 由 global 负载 promtail 配置下发
            List<MonitorEndpoint> distinctByAssetId = enableEndpointInfoList.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(MonitorEndpoint::getAssetId))), ArrayList::new));
            Map<Integer, Integer> endpointIdAndPromIdMap = configUtil.consistentHashingAssignEndpoint(globalPromServers, distinctByAssetId);

            Map<Integer, Agent> agentIdAndEntityMap = globalPromServers.stream().collect(Collectors.toMap(Agent::getId, Function.identity()));
            List talonConfigList = new ArrayList();
            Map<Integer, List<MonitorEndpoint>> endpointThroughAssetGroupMap = enableEndpointInfoList.stream().collect(Collectors.groupingBy(MonitorEndpoint::getAssetId));
            for (Map.Entry<Integer, List<MonitorEndpoint>> entry : endpointThroughAssetGroupMap.entrySet()) {
                List<MonitorEndpoint> logsEndpointForAsset = entry.getValue();
                Map talonConfig = new HashMap();
                Agent agent = null;
                for (MonitorEndpoint endpoint : logsEndpointForAsset) {
                    agent = agentIdAndEntityMap.get(endpointIdAndPromIdMap.get(endpoint.getId()));
                    if (agent != null) break;
                }

                Map promtailYml = configUtil.generatePromtailYml(agent, globalPromServers, logsEndpointForAsset);

                AssetAsset asset = logsEndpointForAsset.get(0).getAsset();
                talonConfig.put("assetId", asset.getId());
                talonConfig.put("target", String.format("%s:%s", asset.getManageIp(), asset.getClientPort()));
                talonConfig.put("token", asset.getClientToken());
                Map params = new HashMap(2);
                params.put("config", promtailYml);
                // 当前 promtail 值
                params.put("version", version);
                talonConfig.put("params", params);
                talonConfigList.add(talonConfig);
            }

            // 下发 promtail 配置
            Agent firstGlobalAgent = globalPromServers.stream().findFirst().get();
            
            String result = null;
            String path = "/talon/proxy/promtail/config";
            try {
            	result = AgentUtil.request(firstGlobalAgent, HttpMethod.POST, "/talon/proxy/promtail/config", null, talonConfigList, String.class);
            } catch (RuntimeException e) {
            	log.error(e,"request error,agent:{}",Tool.JSONUtil.toJsonStr(firstGlobalAgent));
            }
            
            log.debug("Promtail 下发结果 " + result);

            if (result == null) {
                configUtil.updateAgentStatus(firstGlobalAgent, 0);
            } else {
                configUtil.afterRequestApi(path, result, firstGlobalAgent);
            }
        }
    }

    /**
     * per-datacenter logs 配置
     *  @param globalPromServers
     * @param perDataCenterPromServers
     * @param version
     */
    private void handlePerDatacenterLogs(List<Agent> globalPromServers, List<Agent> perDataCenterPromServers, Integer version) {
        String fedeEnabledStr = sysConfigMap.get(Constant.SYSCONFIG_KEY_PROM_FEDER_ENABLED);
        log.debug(String.format("prometheus 联邦状态: %s", StrUtil.equals("0", fedeEnabledStr) ? "关闭" : "开启"));
        // 联邦关闭 禁用所有 per-datacenter 角色配置
        if (StrUtil.equals("0", fedeEnabledStr)) return;
        // 按照 dc 分组
        Map<Integer, List<Agent>> dcIdAndAgents = perDataCenterPromServers.stream().collect(Collectors.groupingBy(Agent::getDcId));

        // 用于生成配置的 endpoint 信息
        List<MonitorEndpoint> enableEndpointInfoList = endpointService.getEnableLogsEndpointInfoListForAgent(version);
        log.info("sync logs config size: {}" , Tool.CollUtil.size(enableEndpointInfoList));
        if(Tool.CollUtil.isEmpty(enableEndpointInfoList)) {
        	return;
        }
        for (Map.Entry<Integer, List<Agent>> entry : dcIdAndAgents.entrySet()) {
            Integer dcId = entry.getKey();
            // dc 下的 endpoint 信息
            List<MonitorEndpoint> endpointListByDc = enableEndpointInfoList.stream().filter(endpoint -> dcId.equals(endpoint.getDc().getId())).collect(Collectors.toList());

            List<Agent> agentListByDc = entry.getValue();
            Map<Integer, Agent> agentIdAndEntityMap = agentListByDc.stream().collect(Collectors.toMap(Agent::getId, Function.identity()));

            List<MonitorEndpoint> distinctByAssetId = endpointListByDc.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(MonitorEndpoint::getAssetId))), ArrayList::new));
            Map<Integer, Integer> endpointIdAndPromIdMap = configUtil.consistentHashingAssignEndpoint(agentListByDc, distinctByAssetId);

            Map<Integer, List<MonitorEndpoint>> endpointThroughAssetGroupMap = endpointListByDc.stream().collect(Collectors.groupingBy(MonitorEndpoint::getAssetId));

            List talonConfigList = new ArrayList();
            for (Map.Entry<Integer, List<MonitorEndpoint>> logConfEnery : endpointThroughAssetGroupMap.entrySet()) {
                List<MonitorEndpoint> logsEndpointForAsset = logConfEnery.getValue();

                Map talonConfig = new HashMap();
                // endpoint 对应的 per-datacenter
                Agent agent = null;
                for (MonitorEndpoint endpoint : logsEndpointForAsset) {
                    agent = agentIdAndEntityMap.get(endpointIdAndPromIdMap.get(endpoint.getId()));
                    if (agent != null) break;
                }
                // 以 asset endpoint 为维度去生成 promtail.yml
                Map promtailYml = configUtil.generatePromtailYml(agent, globalPromServers, logsEndpointForAsset);

                AssetAsset asset = logsEndpointForAsset.get(0).getAsset();
                talonConfig.put("assetId", asset.getId());
                talonConfig.put("target", String.format("%s:%s", asset.getManageIp(), asset.getClientPort()));
                talonConfig.put("token", asset.getClientToken());
                Map params = new HashMap(2);
                params.put("config", promtailYml);
                params.put("version", version);
                talonConfig.put("params", params);
                talonConfigList.add(talonConfig);
            }

            // 1. 更新 per-datacenter 角色 lokiPushUrls config
            for (Agent perDataCenterPromServer : perDataCenterPromServers) {
            	//获取 lokipushurl config
            	Map<String, Object> lokiPushUrlConfigMap = configUtil.getLokiPushConfig(perDataCenterPromServer,globalPromServers);

                String result = null;
                String path = "/config";
                try {
                	result = AgentUtil.request(perDataCenterPromServer, HttpMethod.POST,path, null, lokiPushUrlConfigMap, String.class);
                } catch (RuntimeException e) {
                	log.error(e,"request error,path:{},agent:{}",path,Tool.JSONUtil.toJsonStr(perDataCenterPromServer));
                }
                
                if (result == null) {
                    configUtil.updateAgentStatus(perDataCenterPromServer, 0);
                    continue;
                } else {
                    configUtil.afterRequestApi(path, result, perDataCenterPromServer);
                }
            }
            // 2. global lokipushurl 更新  TODO handleGlobal logs handle

            // 3. 选任意一台 global agent 进行下发 promtail 配置
            Agent firstGlobalAgent = globalPromServers.stream().findFirst().get();
            String result = null;
            String path = "/talon/proxy/promtail/config";
            try {
            	result = AgentUtil.request(firstGlobalAgent, HttpMethod.POST,path, null, talonConfigList, String.class);
            } catch (RuntimeException e) {
            	log.error(e,"request error,path:{},agent:{}",path,Tool.JSONUtil.toJsonStr(firstGlobalAgent));
            }
            
            log.debug("Promtail 下发结果 " + result);

            if (result == null) {
                configUtil.updateAgentStatus(firstGlobalAgent, 0);
            } else {
                configUtil.afterRequestApi(path, result, firstGlobalAgent);
            }
        }
    }

    /**
     * prometheus.yml 配置下发
     *
     * @param healthyPromServers
     */
    private void updatePrometheusConf(List<Agent> healthyPromServers) {
        List<Agent> perDataCenterPromServers = healthyPromServers.stream().filter(pojo -> Constant.AgentType.PER_DATACENTER.getValue().equals(pojo.getType())).collect(Collectors.toList());
        List<Agent> globalPromServers = healthyPromServers.stream().filter(pojo -> Constant.AgentType.GLOBAL.getValue().equals(pojo.getType())).collect(Collectors.toList());

        this.handlePerDatacenterMtericsConf(perDataCenterPromServers, globalPromServers);
        this.handleGlobalMtericsConf(globalPromServers, perDataCenterPromServers);
    }

    /**
     * global metrics 配置下发
     *
     * @param globalPromServers
     * @param perDataCenterPromServers
     */
    private void handleGlobalMtericsConf(List<Agent> globalPromServers, List<Agent> perDataCenterPromServers) {
        boolean fedeEnabled = StrUtil.equals("1", sysConfigMap.get(Constant.SYSCONFIG_KEY_PROM_FEDER_ENABLED));

        List<MonitorEndpoint> endpointList = new ArrayList<>();
        Map<Integer, Integer> assetIdAndPromIdMap, endpointIdAndPromIdMap = new HashMap<>();
        List<AssetAsset> blackboxAssetInfoList = assetService.getBlackboxAssetInfoList();
        // 开启联邦
        if (fedeEnabled) {
            String assetPingFrom = sysConfigMap.get(Constant.SYSCONFIG_KEY_ASSET_PING_FROM);
            // blackbox 源为 global
            assetIdAndPromIdMap = StrUtil.equals("1", assetPingFrom) ? configUtil.consistentHashingAssignAsset(globalPromServers, blackboxAssetInfoList) : new HashMap<>(2);
        } else {
            // 联邦关闭 global 直接进行 black box 采集  , endpoint 信息
            assetIdAndPromIdMap = configUtil.consistentHashingAssignAsset(globalPromServers, blackboxAssetInfoList);
            endpointList = endpointService.getEnableMetricsEndpointInfoListForAgent();
            // 分配 endpoint
            endpointIdAndPromIdMap = configUtil.consistentHashingAssignEndpoint(globalPromServers, endpointList);
            // 保存 redis 中 endpointid 和　agent id对应关系
            configUtil.updateEndpointPromInRedis(endpointIdAndPromIdMap);
        }

        // global config 配置文件处理 联邦
        for (int i = 0; i < globalPromServers.size(); i++) {
            Agent agent = globalPromServers.get(i);

            List<Integer> assetIdListForThisAgent = assetIdAndPromIdMap.entrySet().stream().filter(entry -> agent.getId().equals(entry.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
            List<AssetAsset> assetListForThisAgent = blackboxAssetInfoList.stream().filter(asset -> assetIdListForThisAgent.contains(asset.getId())).collect(Collectors.toList());

            List<Integer> endpointIdListForThisAgent = endpointIdAndPromIdMap.entrySet().stream().filter(entry -> agent.getId().equals(entry.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
            List<MonitorEndpoint> endpointListForThisAgent = endpointList.stream().filter(endpoint -> endpointIdListForThisAgent.contains(endpoint.getId())).collect(Collectors.toList());

            Map gloablAgentYmlMap = configUtil.generateGlobalYml(agent, globalPromServers, perDataCenterPromServers, assetListForThisAgent, endpointListForThisAgent, false, i == 0);

            Map<String, Object> globalConfigBody = new LinkedHashMap(4);
            globalConfigBody.put("config", gloablAgentYmlMap);

            Map<String, String> sysCmdLineMap = configUtil.generatePrometheusCmdLine(agent);
            boolean cmdFlag = this.compareCmdLineChanges(agent, sysCmdLineMap);
            if (cmdFlag) {
                log.debug("cmdline 配置下发" + JSONObject.toJSONString(agent));
                globalConfigBody.put("cmdline", sysCmdLineMap);
            }

            String result = null;
            String path = "/prometheus/config";
            try {
                result = AgentUtil.request(agent, HttpMethod.POST, path, null, globalConfigBody, String.class);
            } catch (RuntimeException e) {
                log.error(e, "request error,path:{},agent:{}", path, Tool.JSONUtil.toJsonStr(agent));
            }

            // 下发失败,更新 prom 状态，造成conf_event 变更，下个周期继续下发配置
            if (result == null) {
                configUtil.updateAgentStatus(agent, 0);
                continue;
            } else {
                configUtil.afterRequestApi(path, result, agent);
            }
        }
    }

    /**
     * per-datacenter metrics 配置下发
     *
     * @param perDataCenterPromServers
     * @param globalPromServers
     */
    private void handlePerDatacenterMtericsConf(List<Agent> perDataCenterPromServers, List<Agent> globalPromServers) {
        String fedeEnabledStr = sysConfigMap.get(Constant.SYSCONFIG_KEY_PROM_FEDER_ENABLED);
        log.debug(String.format("prometheus 联邦状态: %s", StrUtil.equals("0", fedeEnabledStr) ? "关闭" : "开启"));
        // 联邦关闭 禁用所有 per-datacenter 角色配置
        if (StrUtil.equals("0", fedeEnabledStr)) return;

        // 按照 dc 分组
        Map<Integer, List<Agent>> dcIdAndAgents = perDataCenterPromServers.stream().collect(Collectors.groupingBy(Agent::getDcId));

        // 用于生成配置的 endpoint 信息
        List<MonitorEndpoint> enableEndpointInfoList = endpointService.getEnableMetricsEndpointInfoListForAgent();

        // blackbox 所需 asset 信息
        List<AssetAsset> blackboxAssetInfoList = assetService.getBlackboxAssetInfoList();

        // 维护 redis 中 endpoint id and agent id map
        Map<Integer, Integer> updateRedisEndpointPromMap = new LinkedHashMap<>(16);
        for (Map.Entry<Integer, List<Agent>> entry : dcIdAndAgents.entrySet()) {
            Integer dcId = entry.getKey();
            List<Agent> agentListByDc = entry.getValue();
            List<MonitorEndpoint> endpointListByDc = enableEndpointInfoList.stream().filter(endpoint -> dcId.equals(endpoint.getDc().getId())).collect(Collectors.toList());

            // endpoint id 和 promerver id 对应 map
            Map<Integer, Integer> endpointIdAndPromIdMap = configUtil.consistentHashingAssignEndpoint(agentListByDc, endpointListByDc);


            // per-datacenter 支持 asset ping job
            List<AssetAsset> assetListByDc = blackboxAssetInfoList.stream().filter(asset -> dcId.equals(asset.getDcId())).collect(Collectors.toList());
            Map<Integer, Integer> assetIdAndPromIdMap = new HashMap<>();
            String assetPingFrom = sysConfigMap.get(Constant.SYSCONFIG_KEY_ASSET_PING_FROM);
            if (StrUtil.equals("2", assetPingFrom)) {
                assetIdAndPromIdMap = configUtil.consistentHashingAssignAsset(agentListByDc, assetListByDc);
            }

            // 生成 per-datacenter 配置信息,并下发配置
            this.updatePerDatacenterYml(agentListByDc, endpointListByDc, endpointIdAndPromIdMap, assetListByDc, assetIdAndPromIdMap, globalPromServers);

            updateRedisEndpointPromMap.putAll(endpointIdAndPromIdMap);
        }
        // 保存 redis 中 endpointid 和　agent id对应关系
        configUtil.updateEndpointPromInRedis(updateRedisEndpointPromMap);
    }

    /**
     * per-datacenter 配置下发
     *
     * @param agents
     * @param endpoints
     * @param endpointIdAndPromIdMap
     * @param assets
     * @param assetIdAndPromIdMap
     * @param globalPromServers
     */
    private void updatePerDatacenterYml(List<Agent> agents, List<MonitorEndpoint> endpoints, Map<Integer, Integer> endpointIdAndPromIdMap, List<AssetAsset> assets, Map<Integer, Integer> assetIdAndPromIdMap, List<Agent> globalPromServers) {
        for (Agent agent : agents) {
            List<Integer> endpointIdListForThisAgent = endpointIdAndPromIdMap.entrySet().stream().filter(entry -> agent.getId().equals(entry.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
            List<MonitorEndpoint> endpointListForThisAgent = endpoints.stream().filter(endpoint -> endpointIdListForThisAgent.contains(endpoint.getId())).collect(Collectors.toList());

            List<Integer> assetIdListForThisAgent = assetIdAndPromIdMap.entrySet().stream().filter(entry -> agent.getId().equals(entry.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
            List<AssetAsset> assetListForThisAgent = assets.stream().filter(asset -> assetIdListForThisAgent.contains(asset.getId())).collect(Collectors.toList());

            Map<String, Object> perDatacenterConfigBody = new HashMap<>(4);

            // 生成配置模板
            Map perdatacenterYmlMap = configUtil.generatePerdatacenterYml(agent, endpointListForThisAgent, assetListForThisAgent, globalPromServers);
            perDatacenterConfigBody.put("config", perdatacenterYmlMap);

            Map<String, String> sysCmdLineMap = configUtil.generatePrometheusCmdLine(agent);
            // 启动项涉及
            boolean cmdFlag = this.compareCmdLineChanges(agent, sysCmdLineMap);
            if (cmdFlag) {
                log.debug("cmdline 配置下发" + JSONObject.toJSONString(agent));
                perDatacenterConfigBody.put("cmdline", sysCmdLineMap);
            }

            // 调用 config 接口 下发 perdatacenter 配置
            String result = null;
            String path = "/prometheus/config";
            try {
            	result = AgentUtil.request(agent, HttpMethod.POST,path, null, perDatacenterConfigBody, String.class);
            } catch (RuntimeException e) {
            	log.error(e,"request error,path:{},agent:{}",path,Tool.JSONUtil.toJsonStr(agent));
            }
            
            if (result == null) {
                configUtil.updateAgentStatus(agent, 0);
                continue;
            } else {
                configUtil.afterRequestApi(path, result, agent);
            }
        }
    }

    /**
     * loki.yml 配置下发
     *
     * @param healthyPromServers
     */
    private void updateLokiConf(List<Agent> healthyPromServers) {
        // try Set Up Config To Down Agent Of Loki
        ThreadUtil.execAsync(() -> {
            // get Loki Component Down Agent List
            List<Agent> lokiComponentDownAgentList = this.getLokiComponentDownAgentList();
            log.info("[updateLokiConf] [loki Component Down Agent List] [size: {}]", lokiComponentDownAgentList.size());

            // try Set Up Config To Down Agent Of Loki
            this.trySetUpConfigToDownAgentOfLoki(lokiComponentDownAgentList);
        });

        // healthy agent config loki.yml
        ThreadUtil.execAsync(() -> {
            // 无论联邦与否 最终数据都会存在  Global  所以 per-datacenter 不必下发 loki 配置
            List<Agent> globalPromServerList = healthyPromServers.stream().filter(pojo -> Constant.AgentType.GLOBAL.getValue().equals(pojo.getType())).collect(Collectors.toList());
            for (Agent agent : globalPromServerList) {
                // generate Loki Yml
                Map lokiYml = configUtil.generateLokiYml(agent, globalPromServerList);
                Map<String, Object> lokiConfigBody = new HashMap<>(2);
                lokiConfigBody.put("config", lokiYml);

                String result = null;
                String path = "/loki/config";
                try {
                    log.info("[updateLokiConf] [request agent /loki/config api begin] [agent id: {}]", agent.getId());
                    if (log.isDebugEnabled()) {
                        log.info("[updateLokiConf] [request agent /loki/config api begin] [agent id: {}] [request body: {}]", agent.getId(), JSONUtil.toJsonStr(lokiConfigBody));
                    }
                    result = AgentUtil.request(agent, HttpMethod.POST, path, null, lokiConfigBody, String.class);
                    log.info("[updateLokiConf] [request agent /loki/config api finshed] [agent id: {}]", agent.getId());
                } catch (RuntimeException e) {
                    log.error(e, "request error,path:{},agent:{}", path, Tool.JSONUtil.toJsonStr(agent));
                }

                // 下发失败,更新 prom 状态，造成 conf_event 变更，下个周期继续下发配置
                if (result == null) {
                    configUtil.updateAgentStatus(agent, 0);
                } else {
                    configUtil.afterRequestApi(path, result, agent);
                }
            }

            // 配置下发后向 redis 记录配置更新  默认五分钟 单位 s
            String cortexUpdateTimeout = Tool.ObjectUtil.defaultIfNull(sysConfigMap.get(Constant.SYSCONFIG_KEY_LOKI_UPDATE_TIMEOUT), "300");
            redisTemplate.opsForValue().set(Constant.REDIS_KEY_LOKI_UPDATE, "1", Integer.valueOf(cortexUpdateTimeout), TimeUnit.SECONDS);
        });
    }

    /**
     * 更新 snmp_exporter 组件 snmp.yml 配置文件
     *
     * @param healthyPromServers
     */
    private void updateSnmpExporterConf(List<Agent> healthyPromServers) {
        Map snmpYml = configUtil.generateSnmpYml();
        for (Agent agent : healthyPromServers) {
            
            String result = null;
            String path = "/snmpExporter/config";
            try {
                result = AgentUtil.request(agent, HttpMethod.POST, path, null, JSONObject.toJSONString(snmpYml), String.class);
            } catch (RuntimeException e) {
            	log.error(e,"request error,path:{},agent:{}",path,Tool.JSONUtil.toJsonStr(agent));
            }
            
            log.debug("snmpExporter 请求返回结果 {}", result);
            if (result == null) {
                configUtil.updateAgentStatus(agent, 0);
            } else {
                configUtil.afterRequestApi(path, result, agent);
            }
        }
    }

    /**
     * prometheus cortex 都没有变更 ， 只更新 rule.yml
     *
     * @param alertYmlMap
     * @param healthyPromServers
     */
    private void justUpdateRuleConf(Map alertYmlMap, List<Agent> healthyPromServers) {
        // 先判断 redis 中是否存在 alert 告警标识
        Boolean aBoolean = redisTemplate.hasKey(Constant.REDIS_KEY_SUPPORT_ALERT_AGENT_ID);
        Integer supportAlertPromId = configUtil.getSupportAlertPromId(healthyPromServers);
        Agent agent = agentService.getById(supportAlertPromId);


       // 无论对象存储与否  全部由于 cortex 支持 rule
        Map<String, Object> updateRuleConfigBody = new HashMap<>(2);
        updateRuleConfigBody.put("rule", alertYmlMap);
        // 告警标识不存在 则需要同时下发 cortex 配置
        if (!aBoolean) {
            List<Agent> globalPromServerList = healthyPromServers.stream().filter(agentEntity -> Constant.AgentType.GLOBAL.getValue().equals(agentEntity.getType())).collect(Collectors.toList());
            Map cortexYml = configUtil.generateCortexYml(agent, globalPromServerList, true);
            updateRuleConfigBody.put("config", cortexYml);
        }
        
        String result = null;
        String path = "/cortex/config";
        try {
        	result = AgentUtil.request(agent, HttpMethod.POST,path, null, updateRuleConfigBody, String.class);
        } catch (RuntimeException e) {
        	log.error(e,"request error,path:{},agent:{}",path,Tool.JSONUtil.toJsonStr(agent));
        }
        
        log.debug("rule 更新请求返回结果 {}", result);
        if (result == null) {
            configUtil.updateAgentStatus(agent, 0);
        } else {
            configUtil.afterRequestApi(path, result, agent);
        }
    }

    /**
     * cortex 配置下发
     *
     * @param alertYmlMap
     * @param healthyAgents
     */
    private void updateCortexConf(List<Agent> healthyAgents) {
        // try Set Up Config To Down Agent Of Cortex
        ThreadUtil.execAsync(() -> {
            // get Cortex Component Down Agent List
            List<Agent> cortexComponentDownAgentList = this.getCortexComponentDownAgentList();
            log.info("[updateCortexConf] [cortex Component Down Agent List] [size: {}]", cortexComponentDownAgentList.size());

            // try Set Up Config To Down Agent Of Cortex
            this.trySetUpConfigToDownAgentOfCortex(cortexComponentDownAgentList);
        });

        // healthy agent config cortex.yml
        ThreadUtil.execAsync(() -> {
            // 获取当前支持 alert 的 agent id
            Integer supportAlertPromId = configUtil.getSupportAlertPromId(healthyAgents);
            // 调用 agent/cortex 接口 更新 cortex config and rule.yml
            List<Agent> globalAgentList = healthyAgents.stream().filter(pojo -> ObjectUtil.equal(Constant.AgentType.GLOBAL.getValue(), pojo.getType())).collect(Collectors.toList());
            for (Agent agent : globalAgentList) {
                Boolean isAlert = ObjectUtil.equal(agent.getId(), supportAlertPromId);
                // generate Cortex Yml
                Map cortexYml = configUtil.generateCortexYml(agent, globalAgentList, isAlert);

                Map<String, Object> cortexConfigBody = new HashMap<>(4);
                cortexConfigBody.put("config", cortexYml);
//                if ((supportAlertPromId == null || agent.getId().equals(supportAlertPromId)) && MapUtil.isNotEmpty(alertYmlMap)) {
//                    cortexConfigBody.put("rule", alertYmlMap);
//                }
                String result = null;
                String path = "/cortex/config";
                try {
                    log.info("[updateCortexConf] [request agent /cortex/config api begin] [agent id: {}]", agent.getId());
                    if (log.isDebugEnabled()) {
                        log.info("[updateCortexConf] [request agent /cortex/config api begin] [agent id: {}] [request body: {}]", agent.getId(), JSONUtil.toJsonStr(cortexConfigBody));
                    }
                    result = AgentUtil.request(agent, HttpMethod.POST, path, null, cortexConfigBody, String.class);
                    log.info("[updateCortexConf] [request agent /cortex/config api finshed] [agent id: {}]", agent.getId());
                } catch (RuntimeException e) {
                    log.error(e, "request error,path:{},agent:{}", path, Tool.JSONUtil.toJsonStr(agent));
                }
                // 下发失败,更新 prom 状态，造成 conf_event 变更，下个周期继续下发配置
                if (result == null) {
                    configUtil.updateAgentStatus(agent, 0);
                } else {
                    configUtil.afterRequestApi(path, result, agent);
                }
            }

            // 配置下发后向 redis 记录配置更新  默认五分钟 单位 s
            String cortexUpdateTimeout = Tool.ObjectUtil.defaultIfNull(sysConfigMap.get(Constant.SYSCONFIG_KEY_CORTEX_UPDATE_TIMEOUT), "300");
            redisTemplate.opsForValue().set(Constant.REDIS_KEY_CORTEX_UPDATE, "1", Integer.valueOf(cortexUpdateTimeout), TimeUnit.SECONDS);
        });
    }

    /**
     * get Cortex Component Down Agent List
     *
     * @return
     */
    private List<Agent> getCortexComponentDownAgentList() {
        // allAgentList
        List<Agent> allAgentList = agentService.list();

        List<Agent> cortexComponentDownAgentList = allAgentList.stream().filter(agent -> {
            String statusInfoJsonStr = agent.getStatusInfo();
            JSONObject jsonObject = JSONObject.parseObject(StrUtil.emptyToDefault(statusInfoJsonStr, StrUtil.EMPTY_JSON));

            String confagentState = (String) jsonObject.get("confagent");
            String cortexState = (String) jsonObject.get("cortex");

            return BooleanUtil.and(
                    StrUtil.equals("UP", confagentState),
                    StrUtil.equals("DOWN", cortexState)
            );
        }).collect(Collectors.toList());
        log.info("[getCortexComponentDownAgentList] [cortex Component Down Agent List] [size: {}]", cortexComponentDownAgentList.size());
        return cortexComponentDownAgentList;
    }

    /**
     * get Loki Component Down Agent List
     *
     * @return
     */
    private List<Agent> getLokiComponentDownAgentList() {
        // allAgentList
        List<Agent> allAgentList = agentService.list();

        List<Agent> lokiComponentDownAgentList = allAgentList.stream().filter(agent -> {
            String statusInfoJsonStr = agent.getStatusInfo();
            JSONObject jsonObject = JSONObject.parseObject(StrUtil.emptyToDefault(statusInfoJsonStr, StrUtil.EMPTY_JSON));

            String confagentState = (String) jsonObject.get("confagent");
            String lokiState = (String) jsonObject.get("loki");

            return BooleanUtil.and(
                    StrUtil.equals("UP", confagentState),
                    StrUtil.equals("DOWN", lokiState)
            );
        }).collect(Collectors.toList());
        log.info("[getLokiComponentDownAgentList] [loki Component Down Agent List] [size: {}]", lokiComponentDownAgentList.size());
        return lokiComponentDownAgentList;
    }

    /**
     * try Set Up Config To Down Agent Of Cortex
     * 尝试给因为 cortex down 导致 Agent 状态异常的 Agent 下发 init cortex.yml 配置，尝试使 cortex 正常
     *
     * @param cortexComponentDownAgentList
     */
    private void trySetUpConfigToDownAgentOfCortex(List<Agent> cortexComponentDownAgentList) {
        for (Agent agent : cortexComponentDownAgentList) {
            // generate Init Cortex Yml
            Map initCortexYml = configUtil.generateInitCortexYml();

            Map<Object, Object> requestBody = Tool.MapUtil.builder()
                    .put("config", initCortexYml)
                    .map();

            String path = "/cortex/config";
            try {
                log.info("[trySetUpConfigToDownAgentOfCortex] [request agent /cortex/config api begin] [agent id: {}]", agent.getId());
                if (log.isDebugEnabled()) {
                    log.info("[trySetUpConfigToDownAgentOfCortex] [request agent /cortex/config api begin] [agent id: {}] [request body: {}]", agent.getId(), JSONUtil.toJsonStr(requestBody));
                }

                // request
                String result = AgentUtil.request(agent, HttpMethod.POST, path, null, requestBody, String.class);

                // after Request api
                if (StrUtil.isEmpty(result)) {
                    // increment cortex
                    eventLogService.increment("cortex");

                    log.error("[trySetUpConfigToDownAgentOfCortex] [request error] [path: {}] [result: {}] [agent: {}]", path, result, JSONUtil.toJsonStr(agent));
                } else {
                    R parseObject = JSONObject.parseObject(result, R.class);
                    if (ObjectUtil.notEqual(RCode.SUCCESS.getCode(), parseObject.get("code"))) {
                        // increment cortex
                        eventLogService.increment("cortex");

                        log.error("[trySetUpConfigToDownAgentOfCortex] [request error] [path: {}] [result: {}] [agent: {}]", path, result, JSONUtil.toJsonStr(agent));
                    }
                }
                log.info("[trySetUpConfigToDownAgentOfCortex] [request agent /cortex/config api finshed] [agent id: {}]", agent.getId());
            } catch (Exception e) {
                // increment cortex
                eventLogService.increment("cortex");

                log.error(e, "[trySetUpConfigToDownAgentOfCortex] [request error] [path: {}] [agent: {}]", path, JSONUtil.toJsonStr(agent));
            }
        }
    }

    /**
     * try Set Up Config To Down Agent Of Loki
     * 尝试给因为 loki down 导致 Agent 状态异常的 Agent 下发 init loki.yml 配置，尝试使 loki 正常
     *
     * @param lokiComponentDownAgentList
     */
    private void trySetUpConfigToDownAgentOfLoki(List<Agent> lokiComponentDownAgentList) {
        for (Agent agent : lokiComponentDownAgentList) {
            // generate Init Loki Yml
            Map initLokiYml = configUtil.generateInitLokiYml();

            Map<Object, Object> requestBody = Tool.MapUtil.builder()
                    .put("config", initLokiYml)
                    .map();

            String path = "/loki/config";
            try {
                log.info("[trySetUpConfigToDownAgentOfLoki] [request agent /loki/config api begin] [agent id: {}]", agent.getId());
                if (log.isDebugEnabled()) {
                    log.info("[trySetUpConfigToDownAgentOfLoki] [request agent /loki/config api begin] [agent id: {}] [request body: {}]", agent.getId(), JSONUtil.toJsonStr(requestBody));
                }

                // request
                String result = AgentUtil.request(agent, HttpMethod.POST, path, null, requestBody, String.class);

                // after Request api
                if (StrUtil.isEmpty(result)) {
                    // increment loki
                    eventLogService.increment("loki");

                    log.error("[trySetUpConfigToDownAgentOfLoki] [request error] [path: {}] [result: {}] [agent: {}]", path, result, JSONUtil.toJsonStr(agent));
                } else {
                    R parseObject = JSONObject.parseObject(result, R.class);
                    if (ObjectUtil.notEqual(RCode.SUCCESS.getCode(), parseObject.get("code"))) {
                        // increment loki
                        eventLogService.increment("loki");

                        log.error("[trySetUpConfigToDownAgentOfLoki] [request error] [path: {}] [result: {}] [agent: {}]", path, result, JSONUtil.toJsonStr(agent));
                    }
                }
                log.info("[trySetUpConfigToDownAgentOfLoki] [request agent /loki/config api finshed] [agent id: {}]", agent.getId());
            } catch (Exception e) {
                // increment loki
                eventLogService.increment("loki");

                log.error(e, "[trySetUpConfigToDownAgentOfLoki] [request error] [path: {}] [agent: {}]", path, JSONUtil.toJsonStr(agent));
            }
        }
    }

    /**
     * 对比 cmd 启动项
     *
     * @param agent
     * @param sysCmdLineMap
     * @return
     */
    private boolean compareCmdLineChanges(Agent agent, Map<String, String> sysCmdLineMap) {
        String result = null;
        String path ="/prometheus/config";
        try {
        	result = AgentUtil.requestGet(agent,path, null, String.class);
        } catch (RuntimeException e) {
        	log.error(e,"request error,agent:{}",Tool.JSONUtil.toJsonStr(agent));
        }
        // 请求接口失败 返回 false 表示本次启动项对比成功，修改 agent 状态，造成下次继续执行对比
        if (result == null) {
            configUtil.updateAgentStatus(agent, 0);
            return false;
        } else {
            configUtil.afterRequestApi(path, result, agent);

            R resultObj = JSONObject.parseObject(result, R.class);
            if (RCode.SUCCESS.getCode().equals((Integer) resultObj.get("code"))) {
                Map<String, Object> data = (Map) resultObj.get("data");
                Map<String, String> promCmdLineMap = (Map<String, String>) data.get("cmdline");

                if (sysCmdLineMap.size() != promCmdLineMap.size()) {
                    return true;
                }

                // 以系统生成 cmdline 模板为主，对比查询出来的 prom cmd line 信息
                for (Map.Entry<String, String> entry : sysCmdLineMap.entrySet()) {
                    String val = promCmdLineMap.get(entry.getKey());
                    if (!entry.getValue().equals(val)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
