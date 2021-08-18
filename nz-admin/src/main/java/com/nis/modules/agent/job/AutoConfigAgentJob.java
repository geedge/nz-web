package com.nis.modules.agent.job;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.Constant;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import lombok.Data;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.*;
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

    private static Log log = Log.get();

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private AgentConfigUtil configUtil;

    @Autowired
    private PromserverService promService;

    @Autowired
    private AssetAssetService assetService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private MonitorEndpointService endpointService;


    static Map agentConfigMap = new HashMap();

    // sys_config map
    Map<String, String> sysConfigMap = new HashMap();

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 检查 haMode
        String serverId = (String) redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
        if (!StrUtil.equals(serverId, Constant.SERVER_ID)) return;

        log.info("agent job 开始执行");
        StopWatch sw = new StopWatch();
        sw.start();
        try {
            // prom_server 状态更新
            List<Promserver> healthyPromServers = configUtil.updateAgentStatus();
            log.debug("健康的 Promservers: " + JSONObject.toJSONString(healthyPromServers));

            // 没有可用的 global 不下发配置
            List<Promserver> globalPromServerList = healthyPromServers.stream().filter(promserver -> Constant.PromserverType.GLOBAL.getValue().equals(promserver.getType())).collect(Collectors.toList());
            if (Tool.CollectionUtil.isEmpty(globalPromServerList)) return;

            // sys_config 配置表信息
            List<SysConfigEntity> configEntityList = sysConfigService.list();
            sysConfigMap = configEntityList.stream().collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));

            // 配置下发
            this.setUpAgentConfig(healthyPromServers);
        } catch (RuntimeException e) {
            log.error("agent 同步异常", e);
        } finally {
            sw.stop();
        }
        log.info("agent 执行结束，Run Time: {}", sw.toString());
    }

    // 配置下发
    public void setUpAgentConfig(List<Promserver> healthyPromServers) {
        try {
            boolean prometheusChangeFlag = configUtil.compareDataChanges("prometheus");
            log.debug(String.format("conf_event prometheus 数据 %s 发生变化", prometheusChangeFlag ? "" : "没有"));

            boolean cortexChangeFlag = configUtil.compareDataChanges("cortex");
            log.debug(String.format("conf_event cortex 数据 %s 发生变化", cortexChangeFlag ? "" : "没有"));

            boolean ruleChangeFlag = configUtil.compareDataChanges("rule");
            log.debug(String.format("conf_event rule 数据 %s 发生变化", ruleChangeFlag ? "" : "没有"));
            if (ruleChangeFlag) {
                // 生成 alert.yml
                Map alertYmlMap = configUtil.generateRuleYml();
                // Prometheus cortex 支持 rule
                if (prometheusChangeFlag) {
                    // 1. prometheus 改变，在 prometheus 配置下发的时候进行 rule 下发
                    agentConfigMap.put("alertYml", alertYmlMap);
                }

                if (!prometheusChangeFlag && !cortexChangeFlag) {
                    // 2. prometheus cortex 都没有变更 只有 alert 变更，查询 redis supportAlertPromId 进行下发 alert rule 配置变化
                    this.justUpdateRuleConf(alertYmlMap, healthyPromServers);
                }
            }
            if (cortexChangeFlag) {
                // 4. cortex 改变 alert 没有改变
                this.updateCortexConf((Map) agentConfigMap.get("alertYml"), healthyPromServers);
            }

            boolean snmpChangeFlag = configUtil.compareDataChanges("snmp");
            log.debug(String.format("conf_event snmp 数据 %s 发生变化", snmpChangeFlag ? "" : "没有"));
            // 更新 snmp_exporter 组件 snmp.yml 配置
            if (snmpChangeFlag)
                this.updateSnmpExporterConf(healthyPromServers);

            boolean lokiChangeFlag = configUtil.compareDataChanges("loki");
            log.debug(String.format("conf_event loki 数据 %s 发生变化", lokiChangeFlag ? "" : "没有"));
            // 更新 loki 组件 loki.yml 配置
            if (lokiChangeFlag)
                this.updateLokiConf(healthyPromServers);

            // 更新 prometheus 组件 prometheus.yml 配置
            if (prometheusChangeFlag)
                this.updatePrometheusConf(healthyPromServers);

            boolean promtailChangeFlag = configUtil.compareDataChanges("promtail");
            log.debug(String.format("conf_event promtail 数据 %s 发生变化", promtailChangeFlag ? "" : "没有"));
            // 更新 promtail 组件 promtail.yml 配置
            if (promtailChangeFlag)
                this.udpatePromtailsConf(healthyPromServers);
        } finally {
            MapUtil.clear(agentConfigMap);
        }
    }

    /**
     * 更新 promtail 配置
     *
     * @param healthyPromServers
     */
    private void udpatePromtailsConf(List<Promserver> healthyPromServers) {
        List<Promserver> perDataCenterPromServers = healthyPromServers.stream().filter(promserver -> Constant.PromserverType.PER_DATACENTER.getValue().equals(promserver.getType())).collect(Collectors.toList());
        List<Promserver> globalPromServers = healthyPromServers.stream().filter(promserver -> Constant.PromserverType.GLOBAL.getValue().equals(promserver.getType())).collect(Collectors.toList());

        this.handlePerDatacenterLogs(globalPromServers, perDataCenterPromServers);
        this.handleGlobalLogs(globalPromServers);
    }

    /**
     * global logs 配置
     *
     * @param globalPromServers
     */
    private void handleGlobalLogs(List<Promserver> globalPromServers) {
        // 对象存储开启  global 都需要下发 lokiPushUrls config 指向自己
        // 对象存储关闭  global 都需要下发 lokiPushUrls config 指向自己 和 remote 其他 global
        for (Promserver globalProm : globalPromServers) {
            Map<String, Object> lokiPushUrlConfigMap = configUtil.getGlobalLokiPushConfig(globalProm, globalPromServers);

            UrlBuilder urlBuilder = UrlBuilder.create().setScheme("http").setHost(globalProm.getHost()).setPort(globalProm.getPort()).appendPath(sysConfigMap.get(Constant.CONFAGENT_PATH)).appendPath("config");
            String result = null;
            try {
                result = Tool.HttpUtil.createPost(urlBuilder.build()).auth(globalProm.getToken()).body(JSONObject.toJSONString(lokiPushUrlConfigMap), "application/json;charset=UTF-8").execute().body();
            } catch (RuntimeException e) {
                log.error(String.format("请求 %s 接口失败", urlBuilder.toString()), e);
            }

            if (result == null) {
                configUtil.updatePromServerStatus(globalProm, 0);
                continue;
            } else {
                configUtil.afterRequestApi(urlBuilder.toString(), result, globalProm);
            }
        }

        String fedeEnabledStr = sysConfigMap.get(Constant.SYSCONFIG_KEY_PROM_FEDER_ENABLED);
        log.debug(String.format("prometheus 联邦状态: %s", StrUtil.equals("0", fedeEnabledStr) ? "关闭" : "开启"));
        if (StrUtil.equals("0", fedeEnabledStr)) {
            // 用于生成配置的 endpoint 信息
            List<MonitorEndpoint> enableEndpointInfoList = endpointService.getEnableLogsEndpointInfoListForAgent();

            // 联邦关闭 由 global 负载 promtail 配置下发
            Map<Integer, Integer> endpointIdAndPromIdMap = configUtil.consistentHashingAssignEndpoint(globalPromServers, enableEndpointInfoList);

            Map<Integer, Promserver> promserverIdAndEntityMap = globalPromServers.stream().collect(Collectors.toMap(Promserver::getId, Function.identity()));
            List talonConfigList = new ArrayList();
            for (MonitorEndpoint endpoint : enableEndpointInfoList) {
                Map talonConfig = new HashMap();
                Promserver promserver = promserverIdAndEntityMap.get(endpointIdAndPromIdMap.get(endpoint.getId()));

                Map promtailYml = configUtil.generatePromtailYml(promserver, globalPromServers, endpoint);

                AssetAsset asset = endpoint.getAsset();
                talonConfig.put("assetId", asset.getId());
                talonConfig.put("target", String.format("%s:%s", asset.getManageIp(), asset.getClientPort()));
                talonConfig.put("token", asset.getClientToken());
                Map params = new HashMap(2);
                params.put("config", promtailYml);
                // 请求 talon promtail config version 必填
                params.put("version", endpoint.getEndpointConfig().getVersion());
                talonConfig.put("params", params);
                talonConfigList.add(talonConfig);
            }

            // 下发 promtail 配置
            Promserver firstGlobalAgent = globalPromServers.stream().findFirst().get();
            UrlBuilder urlBuilder = UrlBuilder.create().setScheme("http").setHost(firstGlobalAgent.getHost()).setPort(firstGlobalAgent.getPort()).appendPath(sysConfigMap.get(Constant.CONFAGENT_PATH)).appendPath("talon").appendPath("proxy").appendPath("promtail").appendPath("config");
            String result = null;
            try {
                result = Tool.HttpUtil.createPost(urlBuilder.build()).auth(firstGlobalAgent.getToken()).body(JSONObject.toJSONString(talonConfigList), "application/json;charset=UTF-8").execute().body();
            } catch (RuntimeException e) {
                log.error(String.format("请求 %s 接口失败", urlBuilder.toString()), e);
            }
            log.debug("Promtail 下发结果 " + result);

            if (result == null) {
                configUtil.updatePromServerStatus(firstGlobalAgent, 0);
            } else {
                configUtil.afterRequestApi(urlBuilder.toString(), result, firstGlobalAgent);
            }
        }
    }

    /**
     * per-datacenter logs 配置
     *
     * @param globalPromServers
     * @param perDataCenterPromServers
     */
    private void handlePerDatacenterLogs(List<Promserver> globalPromServers, List<Promserver> perDataCenterPromServers) {
        String fedeEnabledStr = sysConfigMap.get(Constant.SYSCONFIG_KEY_PROM_FEDER_ENABLED);
        log.debug(String.format("prometheus 联邦状态: %s", StrUtil.equals("0", fedeEnabledStr) ? "关闭" : "开启"));
        // 联邦关闭 禁用所有 per-datacenter 角色配置
        if (StrUtil.equals("0", fedeEnabledStr)) return;
        // 按照 dc 分组
        Map<Integer, List<Promserver>> dcIdAndPromservers = perDataCenterPromServers.stream().collect(Collectors.groupingBy(Promserver::getDcId));

        // 用于生成配置的 endpoint 信息
        List<MonitorEndpoint> enableEndpointInfoList = endpointService.getEnableLogsEndpointInfoListForAgent();

        for (Map.Entry<Integer, List<Promserver>> entry : dcIdAndPromservers.entrySet()) {
            Integer dcId = entry.getKey();
            // dc 下的 endpoint 信息
            List<MonitorEndpoint> endpointListByDc = enableEndpointInfoList.stream().filter(endpoint -> dcId.equals(endpoint.getDc().getId())).collect(Collectors.toList());

            List<Promserver> promserverListByDc = entry.getValue();
            Map<Integer, Promserver> promserverIdAndEntityMap = promserverListByDc.stream().collect(Collectors.toMap(Promserver::getId, Function.identity()));

            Map<Integer, Integer> endpointIdAndPromIdMap = configUtil.consistentHashingAssignEndpoint(promserverListByDc, enableEndpointInfoList);
            List talonConfigList = new ArrayList();
            for (MonitorEndpoint endpoint : endpointListByDc) {
                Map talonConfig = new HashMap();
                // endpoint 对应的 per-datacenter
                Promserver promserver = promserverIdAndEntityMap.get(endpointIdAndPromIdMap.get(endpoint.getId()));
                // 以 asset endpoint 为维度去生成 promtail.yml
                Map promtailYml = configUtil.generatePromtailYml(promserver, globalPromServers, endpoint);

                AssetAsset asset = endpoint.getAsset();
                talonConfig.put("assetId", asset.getId());
                talonConfig.put("target", String.format("%s:%s", asset.getManageIp(), asset.getClientPort()));
                talonConfig.put("token", asset.getClientToken());
                Map params = new HashMap(2);
                params.put("config", promtailYml);
                params.put("version", endpoint.getEndpointConfig().getVersion());
                talonConfig.put("params", params);
                talonConfigList.add(talonConfig);
            }

            Map<String, Object> lokiPushUrlConfigMap = configUtil.getPerdatacenterLokiPushConfig(globalPromServers);

            // 1. 更新 per-datacenter 角色 lokiPushUrls config
            for (Promserver perDataCenterPromServer : perDataCenterPromServers) {
                UrlBuilder urlBuilder = UrlBuilder.create().setScheme("http").setHost(perDataCenterPromServer.getHost()).setPort(perDataCenterPromServer.getPort()).appendPath(sysConfigMap.get(Constant.CONFAGENT_PATH)).appendPath("config");
                String result = null;
                try {
                    result = Tool.HttpUtil.createPost(urlBuilder.build()).auth(perDataCenterPromServer.getToken()).body(JSONObject.toJSONString(lokiPushUrlConfigMap), "application/json;charset=UTF-8").execute().body();
                } catch (RuntimeException e) {
                    log.error(String.format("请求 %s 接口失败", urlBuilder.toString()), e);
                }

                if (result == null) {
                    configUtil.updatePromServerStatus(perDataCenterPromServer, 0);
                    continue;
                } else {
                    configUtil.afterRequestApi(urlBuilder.toString(), result, perDataCenterPromServer);
                }
            }
            // 2. global lokipushurl 更新  TODO handleGlobal logs handle

            // 3. 选任意一台 global agent 进行下发 promtail 配置
            Promserver firstGlobalAgent = globalPromServers.stream().findFirst().get();
            UrlBuilder urlBuilder = UrlBuilder.create().setScheme("http").setHost(firstGlobalAgent.getHost()).setPort(firstGlobalAgent.getPort()).appendPath(sysConfigMap.get(Constant.CONFAGENT_PATH)).appendPath("talon").appendPath("proxy").appendPath("promtail").appendPath("config");

            String result = null;
            try {
                result = Tool.HttpUtil.createPost(urlBuilder.build()).auth(firstGlobalAgent.getToken()).body(JSONObject.toJSONString(talonConfigList), "application/json;charset=UTF-8").execute().body();
            } catch (RuntimeException e) {
                log.error(String.format("请求 %s 接口失败", urlBuilder.toString()), e);
            }
            log.debug("Promtail 下发结果 " + result);

            if (result == null) {
                configUtil.updatePromServerStatus(firstGlobalAgent, 0);
            } else {
                configUtil.afterRequestApi(urlBuilder.toString(), result, firstGlobalAgent);
            }
        }
    }

    /**
     * prometheus.yml 配置下发
     *
     * @param healthyPromServers
     */
    private void updatePrometheusConf(List<Promserver> healthyPromServers) {
        List<Promserver> perDataCenterPromServers = healthyPromServers.stream().filter(promserver -> Constant.PromserverType.PER_DATACENTER.getValue().equals(promserver.getType())).collect(Collectors.toList());
        this.handlePerDatacenterMtericsConf(perDataCenterPromServers);

        List<Promserver> globalPromServers = healthyPromServers.stream().filter(promserver -> Constant.PromserverType.GLOBAL.getValue().equals(promserver.getType())).collect(Collectors.toList());
        this.handleGlobalMtericsConf(globalPromServers, perDataCenterPromServers);
    }

    /**
     * global metrics 配置下发
     *
     * @param globalPromServers
     * @param perDataCenterPromServers
     */
    private void handleGlobalMtericsConf(List<Promserver> globalPromServers, List<Promserver> perDataCenterPromServers) {
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
            // 保存 redis 中 endpointid 和　promserver id对应关系
            configUtil.updateEndpointPromInRedis(endpointIdAndPromIdMap);
        }

        //  object storage 标识  1: 本地文件存储，2：S3对象存储
        String objStroageEnable = sysConfigService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_TYPE);
        boolean isLocalStorage = StrUtil.equals("1", objStroageEnable);

        // global config 配置文件处理 联邦 告警 rule.yml cortex
        for (int i = 0; i < globalPromServers.size(); i++) {
            Promserver promserver = globalPromServers.get(i);

            Map<String, Object> globalConfigBody = new LinkedHashMap(4);

            List<Integer> assetIdListForThisAgent = assetIdAndPromIdMap.entrySet().stream().filter(entry -> promserver.getId().equals(entry.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
            List<AssetAsset> assetListForThisAgent = blackboxAssetInfoList.stream().filter(asset -> assetIdListForThisAgent.contains(asset.getId())).collect(Collectors.toList());

            List<Integer> endpointIdListForThisAgent = endpointIdAndPromIdMap.entrySet().stream().filter(entry -> promserver.getId().equals(entry.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
            List<MonitorEndpoint> endpointListForThisAgent = endpointList.stream().filter(endpoint -> endpointIdListForThisAgent.contains(endpoint.getId())).collect(Collectors.toList());

            Boolean isAlert = false;
            // prometheus 本地存储 && alert rule.yml 配置变更 && first global
            if (isLocalStorage && agentConfigMap.get("alertYml") != null && i == 0) {
                isAlert = true;
                log.debug("支持告警的 agent :" + JSONObject.toJSONString(promserver));
            }

            Map gloablAgentYmlMap = configUtil.generateGlobalYml(promserver, globalPromServers, perDataCenterPromServers, assetListForThisAgent, endpointListForThisAgent, isAlert);

            globalConfigBody.put("config", gloablAgentYmlMap);
            if (isAlert) {
                globalConfigBody.put("rule", (Map) agentConfigMap.get("alertYml"));
                redisTemplate.opsForValue().set(Constant.REDIS_KEY_SUPPORT_ALERT_PROMSERVER_ID, promserver.getId().toString());
            }

            Map<String, String> sysCmdLineMap = configUtil.generatePrometheusCmdLine();
            boolean cmdFlag = this.compareCmdLineChanges(promserver, sysCmdLineMap);
            if (cmdFlag) {
                log.debug("cmdline 配置下发" + JSONObject.toJSONString(promserver));
                globalConfigBody.put("cmdline", sysCmdLineMap);
            }

            UrlBuilder urlBuilder = UrlBuilder.create();
            urlBuilder.setScheme("http").setHost(promserver.getHost()).setPort(promserver.getPort()).appendPath(sysConfigMap.get(Constant.CONFAGENT_PATH)).appendPath("prometheus").appendPath("config");

            String result = null;
            try {
                result = Tool.HttpUtil.createPost(urlBuilder.build()).auth(promserver.getToken()).body(JSONObject.toJSONString(globalConfigBody), "application/json;charset=UTF-8").execute().body();
            } catch (RuntimeException e) {
                log.error(String.format("请求 %s 接口失败", urlBuilder.toString()), e);
            }

            // 下发失败,更新 prom 状态，造成conf_event 变更，下个周期继续下发配置
            if (result == null) {
                configUtil.updatePromServerStatus(promserver, 0);
                continue;
            } else {
                configUtil.afterRequestApi(urlBuilder.toString(), result, promserver);
            }
        }
    }

    /**
     * per-datacenter metrics 配置下发
     *
     * @param perDataCenterPromServers
     */
    private void handlePerDatacenterMtericsConf(List<Promserver> perDataCenterPromServers) {
        String fedeEnabledStr = sysConfigMap.get(Constant.SYSCONFIG_KEY_PROM_FEDER_ENABLED);
        log.debug(String.format("prometheus 联邦状态: %s", StrUtil.equals("0", fedeEnabledStr) ? "关闭" : "开启"));
        // 联邦关闭 禁用所有 per-datacenter 角色配置
        if (StrUtil.equals("0", fedeEnabledStr)) return;

        // 按照 dc 分组
        Map<Integer, List<Promserver>> dcIdAndPromservers = perDataCenterPromServers.stream().collect(Collectors.groupingBy(Promserver::getDcId));

        // 用于生成配置的 endpoint 信息
        List<MonitorEndpoint> enableEndpointInfoList = endpointService.getEnableMetricsEndpointInfoListForAgent();

        // blackbox 所需 asset 信息
        List<AssetAsset> blackboxAssetInfoList = assetService.getBlackboxAssetInfoList();

        // 维护 redis 中 endpoint id and promserver id map
        Map<Integer, Integer> updateRedisEndpointPromMap = new LinkedHashMap<>(16);
        for (Map.Entry<Integer, List<Promserver>> entry : dcIdAndPromservers.entrySet()) {
            Integer dcId = entry.getKey();
            List<Promserver> promserverListByDc = entry.getValue();
            List<MonitorEndpoint> endpointListByDc = enableEndpointInfoList.stream().filter(endpoint -> dcId.equals(endpoint.getDc().getId())).collect(Collectors.toList());

            // endpoint id 和 promerver id 对应 map
            Map<Integer, Integer> endpointIdAndPromIdMap = configUtil.consistentHashingAssignEndpoint(promserverListByDc, endpointListByDc);


            // per-datacenter 支持 asset ping job
            List<AssetAsset> assetListByDc = blackboxAssetInfoList.stream().filter(asset -> dcId.equals(asset.getDcId())).collect(Collectors.toList());
            Map<Integer, Integer> assetIdAndPromIdMap = new HashMap<>();
            String assetPingFrom = sysConfigMap.get(Constant.SYSCONFIG_KEY_ASSET_PING_FROM);
            if (StrUtil.equals("2", assetPingFrom)) {
                assetIdAndPromIdMap = configUtil.consistentHashingAssignAsset(promserverListByDc, assetListByDc);
            }

            // 生成 per-datacenter 配置信息,并下发配置
            this.updatePerDatacenterYml(promserverListByDc, endpointListByDc, endpointIdAndPromIdMap, assetListByDc, assetIdAndPromIdMap);

            updateRedisEndpointPromMap.putAll(endpointIdAndPromIdMap);
        }
        // 保存 redis 中 endpointid 和　promserver id对应关系
        configUtil.updateEndpointPromInRedis(updateRedisEndpointPromMap);
    }

    /**
     * per-datacenter 配置下发
     *
     * @param promservers
     * @param endpoints
     * @param endpointIdAndPromIdMap
     * @param assets
     * @param assetIdAndPromIdMap
     */
    private void updatePerDatacenterYml(List<Promserver> promservers, List<MonitorEndpoint> endpoints, Map<Integer, Integer> endpointIdAndPromIdMap, List<AssetAsset> assets, Map<Integer, Integer> assetIdAndPromIdMap) {
        for (Promserver promserver : promservers) {
            List<Integer> endpointIdListForThisAgent = endpointIdAndPromIdMap.entrySet().stream().filter(entry -> promserver.getId().equals(entry.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
            List<MonitorEndpoint> endpointListForThisAgent = endpoints.stream().filter(endpoint -> endpointIdListForThisAgent.contains(endpoint.getId())).collect(Collectors.toList());

            List<Integer> assetIdListForThisAgent = assetIdAndPromIdMap.entrySet().stream().filter(entry -> promserver.getId().equals(entry.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
            List<AssetAsset> assetListForThisAgent = assets.stream().filter(asset -> assetIdListForThisAgent.contains(asset.getId())).collect(Collectors.toList());

            Map<String, Object> perDatacenterConfigBody = new HashMap<>(4);

            // 生成配置模板
            Map perdatacenterYmlMap = configUtil.generatePerdatacenterYml(promserver, endpointListForThisAgent, assetListForThisAgent);
            perDatacenterConfigBody.put("config", perdatacenterYmlMap);

            Map<String, String> sysCmdLineMap = configUtil.generatePrometheusCmdLine();
            // 启动项涉及
            boolean cmdFlag = this.compareCmdLineChanges(promserver, sysCmdLineMap);
            if (cmdFlag) {
                log.debug("cmdline 配置下发" + JSONObject.toJSONString(promserver));
                perDatacenterConfigBody.put("cmdline", sysCmdLineMap);
            }

            // 调用 config 接口 下发 perdatacenter 配置
            UrlBuilder urlBuilder = UrlBuilder.create();
            urlBuilder.setScheme("http").setHost(promserver.getHost()).setPort(promserver.getPort()).appendPath(sysConfigMap.get(Constant.CONFAGENT_PATH)).appendPath("prometheus").appendPath("config");
            String result = null;
            try {
                result = Tool.HttpUtil.createPost(urlBuilder.build()).auth(promserver.getToken()).body(JSONObject.toJSONString(perDatacenterConfigBody), "application/json;charset=UTF-8").execute().body();
            } catch (RuntimeException e) {
                log.error(String.format("请求 %s 接口失败", urlBuilder.toString()), e);
            }

            if (result == null) {
                configUtil.updatePromServerStatus(promserver, 0);
                continue;
            } else {
                configUtil.afterRequestApi(urlBuilder.toString(), result, promserver);
            }
        }
    }

    /**
     * loki.yml 配置下发
     *
     * @param healthyPromServers
     */
    private void updateLokiConf(List<Promserver> healthyPromServers) {
        // 生成 loki 配置模板
        Map lokiYml = configUtil.generateLokiYml();
        // 无论联邦与否 最终数据都会存在  Global  所以 per-datacenter 不必下发 loki 配置
        List<Promserver> globalPromServerList = healthyPromServers.stream().filter(promserver -> Constant.PromserverType.GLOBAL.getValue().equals(promserver.getType())).collect(Collectors.toList());
        for (Promserver promserver : globalPromServerList) {
            UrlBuilder urlBuilder = UrlBuilder.create().setScheme("http").setHost(promserver.getHost()).setPort(promserver.getPort()).appendPath(sysConfigMap.get(Constant.CONFAGENT_PATH)).appendPath("loki").appendPath("config");

            Map<String, Object> lokiConfigBody = new HashMap<>(2);
            lokiConfigBody.put("config", lokiYml);
            String result = null;
            try {
                result = Tool.HttpUtil.createPost(urlBuilder.build()).auth(promserver.getToken()).body(JSONObject.toJSONString(lokiConfigBody), "application/json;charset=UTF-8").execute().body();
            } catch (RuntimeException e) {
                log.error(String.format("请求 %s 接口失败", urlBuilder.toString()), e);
            }
            // 下发失败,更新 prom 状态，造成 conf_event 变更，下个周期继续下发配置
            if (result == null) {
                configUtil.updatePromServerStatus(promserver, 0);
            } else {
                configUtil.afterRequestApi(urlBuilder.toString(), result, promserver);
            }
        }
    }

    /**
     * 更新 snmp_exporter 组件 snmp.yml 配置文件
     *
     * @param healthyPromServers
     */
    private void updateSnmpExporterConf(List<Promserver> healthyPromServers) {
        Map snmpYml = configUtil.generateSnmpYml();
        for (Promserver promserver : healthyPromServers) {
            UrlBuilder urlBuilder = UrlBuilder.create();
            urlBuilder.setScheme("http").setHost(promserver.getHost()).setPort(promserver.getPort()).appendPath(sysConfigMap.get(Constant.CONFAGENT_PATH)).appendPath("snmpExporter").appendPath("config");
            String snmpConfResult = null;
            try {
                snmpConfResult = Tool.HttpUtil.createPost(urlBuilder.build()).auth(promserver.getToken()).body(JSONObject.toJSONString(snmpYml)).execute().body();
            } catch (RuntimeException e) {
                log.error(String.format("请求 %s 接口失败", urlBuilder.toString()), e);
            }
            log.debug("snmpExporter 请求返回结果 {}", snmpConfResult);
            if (snmpConfResult == null) {
                configUtil.updatePromServerStatus(promserver, 0);
            } else {
                configUtil.afterRequestApi(urlBuilder.toString(), snmpConfResult, promserver);
            }
        }
    }

    /**
     * prometheus cortex 都没有变更 ， 只更新 rule.yml
     *
     * @param alertYmlMap
     * @param healthyPromServers
     */
    private void justUpdateRuleConf(Map alertYmlMap, List<Promserver> healthyPromServers) {
        Integer supportAlertPromId = configUtil.getSupportAlertPromId(healthyPromServers);
        Promserver promserver = promService.getById(supportAlertPromId);

        String objStroageEnable = sysConfigService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_TYPE);
        UrlBuilder urlBuilder;
        if (StrUtil.equals("2", objStroageEnable)) {
            // 开启对象存储 更新  cortex rule 配置
            urlBuilder = UrlBuilder.create().setScheme("http").setHost(promserver.getHost()).setPort(promserver.getPort()).appendPath(sysConfigMap.get(Constant.CONFAGENT_PATH)).appendPath("cortex").appendPath("config");
        } else {
            // 没有开启对象存储 更新  prometheus rule 配置
            urlBuilder = UrlBuilder.create().setScheme("http").setHost(promserver.getHost()).setPort(promserver.getPort()).appendPath(sysConfigMap.get(Constant.CONFAGENT_PATH)).appendPath("prometheus").appendPath("config");
        }

        Map<String, Object> updateRuleConfigBody = new HashMap<>(2);
        updateRuleConfigBody.put("rule", alertYmlMap);
        // 因为 cortex 配置文件必填，所以 set cortex config
        List<Promserver> globalPromServerList = healthyPromServers.stream().filter(agentEntity -> Constant.PromserverType.GLOBAL.getValue().equals(agentEntity.getType())).collect(Collectors.toList());
        Map cortexYml = configUtil.generateCortexYml(promserver, globalPromServerList);
        updateRuleConfigBody.put("config", cortexYml);
        String result = null;
        try {
            result = Tool.HttpUtil.createPost(urlBuilder.build()).auth(promserver.getToken()).body(JSONObject.toJSONString(updateRuleConfigBody), "application/json;charset=UTF-8").execute().body();
        } catch (RuntimeException e) {
            log.error(String.format("请求 %s 接口失败", urlBuilder.toString()), e);
        }
        log.debug("rule 更新请求返回结果 {}", result);
        if (result == null) {
            configUtil.updatePromServerStatus(promserver, 0);
        } else {
            configUtil.afterRequestApi(urlBuilder.toString(), result, promserver);
        }
    }

    /**
     * cortex 配置下发
     *
     * @param alertYmlMap
     * @param healthyPromServers
     */
    private void updateCortexConf(Map alertYmlMap, List<Promserver> healthyPromServers) {
        // 1. 判断是否开启  object storage
        //String objStroageEnable = sysConfigService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_TYPE);
        //if (StrUtil.equals("2", objStroageEnable)) {
        // 获取当前支持 alert 的 agent id
        Integer supportAlertPromId = configUtil.getSupportAlertPromId(healthyPromServers);
        // 调用 agent/cortex 接口 更新 cortex config and rule.yml
        List<Promserver> globalPromServerList = healthyPromServers.stream().filter(promserver -> Constant.PromserverType.GLOBAL.getValue().equals(promserver.getType())).collect(Collectors.toList());
        for (Promserver promserver : globalPromServerList) {
            UrlBuilder urlBuilder = UrlBuilder.create().setScheme("http").setHost(promserver.getHost()).setPort(promserver.getPort()).appendPath(sysConfigMap.get(Constant.CONFAGENT_PATH)).appendPath("cortex").appendPath("config");

            // 生成 cortex 配置模板
            Map cortexYml = configUtil.generateCortexYml(promserver, globalPromServerList);

            Map<String, Object> cortexConfigBody = new HashMap<>(4);
            cortexConfigBody.put("config", cortexYml);
            if ((supportAlertPromId == null || promserver.getId().equals(supportAlertPromId)) && MapUtil.isNotEmpty(alertYmlMap)) {
                cortexConfigBody.put("rule", alertYmlMap);
            }
            String result = null;
            try {
                result = Tool.HttpUtil.createPost(urlBuilder.build()).auth(promserver.getToken()).body(JSONObject.toJSONString(cortexConfigBody), "application/json;charset=UTF-8").execute().body();
            } catch (RuntimeException e) {
                log.error(String.format("请求 %s 接口失败", urlBuilder.toString()), e);
            }
            // 下发失败,更新 prom 状态，造成 conf_event 变更，下个周期继续下发配置
            if (result == null) {
                configUtil.updatePromServerStatus(promserver, 0);
            } else {
                configUtil.afterRequestApi(urlBuilder.toString(), result, promserver);
            }
        }
        /*} else {
            // 没有开启 对象存储 啥也不做
        }*/
    }


    /**
     * 对比 cmd 启动项
     *
     * @param promserver
     * @param sysCmdLineMap
     * @return
     */
    private boolean compareCmdLineChanges(Promserver promserver, Map<String, String> sysCmdLineMap) {
        UrlBuilder urlBuilder = UrlBuilder.create();
        urlBuilder.setScheme("http").setHost(promserver.getHost()).setPort(promserver.getPort()).appendPath(sysConfigMap.get(Constant.CONFAGENT_PATH)).appendPath("prometheus").appendPath("config");
        //  调用 /config 接口
        String result = null;
        try {
            result = Tool.HttpUtil.createGet(urlBuilder.build()).auth(promserver.getToken()).execute().body();
        } catch (RuntimeException e) {
            log.error(String.format("请求 %s 接口失败", urlBuilder.toString()), e);
        }

        // 请求接口失败 返回 false 表示本次启动项对比成功，修改 promserver 状态，造成下次继续执行对比
        if (result == null) {
            configUtil.updatePromServerStatus(promserver, 0);
            return false;
        } else {
            configUtil.afterRequestApi(urlBuilder.toString(), result, promserver);

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
