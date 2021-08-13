package com.nis.modules.agent.job;

import cn.hutool.core.lang.hash.MurmurHash;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.*;
import com.nis.modules.agent.dao.MonitorEventLogDao;
import com.nis.modules.agent.entity.MonitorEventLog;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.entity.AlertSeverityConf;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.alert.service.AlertSeverityService;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.module.service.MonitorModuleService;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * AgentJob
 *
 * @author shizhendong
 * @date 2021-04-06
 */
@DisallowConcurrentExecution
public class AgentJob extends QuartzJobBean {

    private static Log log = Log.get();

    @Value("${nezha.virtualAgentNum}")
    private Integer virtualAgentNum;

	@Value("${connect.timeout:30000}")
	private Integer connectTimeout;
	
    @Autowired
    private DcService dcService;

    @Autowired
    private PromserverService promService;

    @Autowired
    private AlertRuleService ruleService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MonitorEventLogDao confEventDao;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private AlertSeverityService alertSeverityService;

    @Autowired
    private MonitorEndpointService monitorEndpointService;

    @Autowired
    private MonitorModuleService monitorModuleService;
    
    @Autowired
    private AssetAssetService assetService;

    private static ExecutorService exec = Executors.newCachedThreadPool();

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 检查 haMode
    	String serverId = (String) redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
        if(!StrUtil.equals(serverId, Constant.SERVER_ID)) return;

        log.info("agent job 开始执行");
        StopWatch sw = new StopWatch();
        sw.start();
        try {
            // 配置下发
            // this.setUpAgentConfig();
        } catch (RuntimeException e) {
            log.error("agent 同步异常", e);
        } finally {
            sw.stop();
        }
        log.info("agent 执行结束，Run Time: {}", sw.toString());
    }

    public void setUpAgentConfig() {
        // 对比数据变化
        boolean changeFlag = this.compareDataChanges("prometheus");
        log.debug(String.format("conf_event 数据%s发生变化", changeFlag ? "" : "没有"));

        // sys_config 配置表信息
        List<SysConfigEntity> configEntityList = sysConfigService.list();
        Map<String, String> configMap = configEntityList.stream().collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));

        // 全部 prom_server  GLOBAL PER_DATACENTER
        List<Promserver> allPromServers = promService.list(new LambdaQueryWrapper<Promserver>().in(Promserver::getType, Constant.PromserverType.GLOBAL.getValue(),Constant.PromserverType.PER_DATACENTER.getValue()));

        // 健康的 prom_server
        List<Promserver> healthyPromServers = this.getHealthyPromServers(allPromServers, configMap);
        log.debug("健康的 Promservers: " + JSONObject.toJSONString(healthyPromServers));

        if (changeFlag) {
            // 系统 cmdline 模板
            Map<String, String> sysCmdLineMap = this.generateCmdLine(configMap);

            // 配置发生改变
            List<Promserver> perDataCenterPromServers = healthyPromServers.stream().filter(promserver -> Constant.PromserverType.PER_DATACENTER.getValue().equals(promserver.getType())).collect(Collectors.toList());
            this.handlePerDatacenter(perDataCenterPromServers, configMap, sysCmdLineMap);

            List<Promserver> globalPromServers = healthyPromServers.stream().filter(promserver -> Constant.PromserverType.GLOBAL.getValue().equals(promserver.getType())).collect(Collectors.toList());
            this.handleGlobal(globalPromServers, perDataCenterPromServers, configMap, sysCmdLineMap);
        }
    }

    /**
     * global 处理
     *  @param globalPromServers
     * @param perDataCenterPromServers
     * @param configMap
     * @param sysCmdLineMap
     */
    private void handleGlobal(List<Promserver> globalPromServers, List<Promserver> perDataCenterPromServers, Map<String, String> configMap, Map<String, String> sysCmdLineMap) {
        // 按照 id 升序，方便 alert 设置
        globalPromServers = globalPromServers.stream().sorted(Comparator.comparing(Promserver::getId)).collect(Collectors.toList());

        // blackbox job  1: global   2: per-datacenter
        String assetPingFrom = configMap.get(Constant.SYSCONFIG_KEY_ASSET_PING_FROM);
        // blackbox 所需 asset 信息
        List<AssetAsset> blackboxAssetInfoList = assetService.getBlackboxAssetInfoList();
        boolean fedeEnabled = "1".equalsIgnoreCase(configMap.get(Constant.SYSCONFIG_KEY_PROM_FEDER_ENABLED));
        List<MonitorEndpoint> endpointList = null;
        Map<Integer, Integer> assetIdAndPromIdMap, endpointIdAndPromIdMap = new HashMap<>();
        // 开启联邦
        if (fedeEnabled) {
            assetIdAndPromIdMap = "1".equals(assetPingFrom) ? this.hashAllocationAsset(globalPromServers, blackboxAssetInfoList) : new HashMap<>(2);
        } else {
            // 联邦关闭 global 直接进行 black box 采集  , endpoint 信息
            assetIdAndPromIdMap = this.hashAllocationAsset(globalPromServers, blackboxAssetInfoList);
            endpointList = monitorEndpointService.getEnableMetricsEndpointInfoListForAgent();
            endpointIdAndPromIdMap = this.hashAllocationEndpoind(globalPromServers, endpointList);

            // 保存 redis 中 endpointid 和　promserver id对应关系
            this.saveEndpointPromInRedis(endpointIdAndPromIdMap);
        }

        boolean ruleChange = this.compareDataChanges("rule");
        // global config 配置文件处理 联邦 告警 rule.yml 等，
        for (int i = 0; i < globalPromServers.size(); i++) {
            Promserver promserver = globalPromServers.get(i);
            Boolean isAlert = false;
            if (i == 0) {
                isAlert = true;
                log.debug("支持告警的 agent :" + JSONObject.toJSONString(promserver));
            }

            Map<String, Object> globalConfigBody = new LinkedHashMap(4);

            List<Integer> assetIdListByPromServer = assetIdAndPromIdMap.entrySet().stream().filter(entry -> promserver.getId().equals(entry.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
            List<AssetAsset> assets = blackboxAssetInfoList.stream().filter(asset -> assetIdListByPromServer.contains(asset.getId())).collect(Collectors.toList());
            assets.stream().forEach(asset -> asset.setPromserver(promserver));

            List<MonitorEndpoint> endpoints = this.getAssigEndpoints(endpointList, endpointIdAndPromIdMap, promserver.getId());
            Map gloablAgentYmlMap = this.generateGlobalYml(isAlert, promserver, globalPromServers, perDataCenterPromServers, assets, endpoints, configMap);
            globalConfigBody.put("config", gloablAgentYmlMap);
            if (isAlert) {
                Object supportAlertPromId = redisTemplate.opsForValue().get(Constant.REDIS_KEY_SUPPORT_ALERT_PROMSERVER_ID);
                supportAlertPromId = ToolUtil.isEmpty(supportAlertPromId) ? "" : supportAlertPromId;
                // 告警内容 或者 告警角色发生变更
                if (!promserver.getId().toString().equals(supportAlertPromId.toString()) || ruleChange) {
                    Map alertYmlMap = this.generateRuleYml(configMap);
                    globalConfigBody.put("rule", alertYmlMap);
                }
                redisTemplate.opsForValue().set(Constant.REDIS_KEY_SUPPORT_ALERT_PROMSERVER_ID, promserver.getId().toString());
            }

            boolean cmdFlag = this.compareCmdLineChanges(promserver, sysCmdLineMap, configMap);
            if (cmdFlag) {
                log.debug("cmdline 配置下发" + JSONObject.toJSONString(promserver));
                globalConfigBody.put("cmdline", sysCmdLineMap);
            }

            UrlBuilder urlBuilder = UrlBuilder.create();
            urlBuilder.setScheme("http").setHost(promserver.getHost()).setPort(promserver.getPort()).appendPath(configMap.get(Constant.CONFAGENT_PATH)).appendPath("prometheus").appendPath("config");

            String result = null;
            try {
                result = Tool.HttpUtil.createPost(urlBuilder.build()).auth(promserver.getToken()).body(JSONObject.toJSONString(globalConfigBody), "application/json;charset=UTF-8").execute().body();
            } catch (RuntimeException e) {
                log.error(String.format("请求 %s 接口失败", urlBuilder.toString()), e);
            }

            // 下发失败,更新 prom 状态，造成conf_event 变更，下个周期继续下发配置
            if (result == null) {
                this.updatePromServerStatus(promserver, 0);
                continue;
            }

            this.afterRequestApi(result, promserver);
        }
    }

    /**
     * 保存 redis 中 endpointid 和　promserver id对应关系
     * @param endpointIdAndPromIdMap
     */
    private void saveEndpointPromInRedis(Map<Integer, Integer> endpointIdAndPromIdMap) {
        // 先删后增
        Map<String, String> tempStringMap = new LinkedHashMap<>();
        endpointIdAndPromIdMap.forEach((k, v) -> tempStringMap.put(k.toString(), v.toString()));

        redisTemplate.delete(Constant.REDIS_KEY_ENDPOINT_PROM);
        redisTemplate.opsForHash().putAll(Constant.REDIS_KEY_ENDPOINT_PROM, tempStringMap);
    }


    /**
     * 获取 分配给 promserver 的 endpoint 集合
     * @param endpointList
     * @param endpointIdAndPromIdMap
     * @param promId
     * @return
     */
    private List<MonitorEndpoint> getAssigEndpoints(List<MonitorEndpoint> endpointList, Map<Integer, Integer> endpointIdAndPromIdMap, Integer promId) {
        endpointIdAndPromIdMap = (endpointIdAndPromIdMap == null) ? new HashMap<>() : endpointIdAndPromIdMap;
        endpointList = (endpointList == null) ? new ArrayList<>() : endpointList;
        List<Integer> endpointIdListByPromServer = endpointIdAndPromIdMap.entrySet().stream().filter(entry -> promId.equals(entry.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
        List<MonitorEndpoint> endpointListByPromServer = endpointList.stream().filter(endpoint -> endpointIdListByPromServer.contains(endpoint.getId())).collect(Collectors.toList());
        return endpointListByPromServer;
    }

    /**
     * global yml
     * @param isAlert
     * @param promserver
     * @param globalPromServers
     * @param perDataCenterPromServers
     * @param assets
     * @param endpoints
     * @param configMap
     * @return
     */
    private Map generateGlobalYml(Boolean isAlert, Promserver promserver, List<Promserver> globalPromServers, List<Promserver> perDataCenterPromServers, List<AssetAsset> assets, List<MonitorEndpoint> endpoints,Map<String, String> configMap) {
        String fedeEnabledStr = configMap.get(Constant.SYSCONFIG_KEY_PROM_FEDER_ENABLED);
        boolean fedeEnabled = fedeEnabledStr.equals("1");
        // 关闭联邦 不启用 per-datecenter
        if (!fedeEnabled) perDataCenterPromServers = new ArrayList<>();

        List<Dc> dcList = dcService.list();
        Map<Integer, String> dcIdAndNameMap = dcList.stream().collect(Collectors.toMap(Dc::getId, Dc::getName));

        List<Promserver> allPromserverList = new ArrayList<>();
        allPromserverList.addAll(globalPromServers);
        allPromserverList.addAll(perDataCenterPromServers);

        // agent 代理接口前缀
        String proxyPath = configMap.get(Constant.CONFAGENT_PATH) + "/prometheus/proxy";
        // federate
        List<Map> federateMapList = new ArrayList<>();
        // blackbox job  1: global   2: per-datacenter
        String assetPingFrom = configMap.get(Constant.SYSCONFIG_KEY_ASSET_PING_FROM);
        for (Promserver server : allPromserverList) {
            // 联邦开启
            if (fedeEnabled) {
                // global 做 ping 检测，联邦排除自己
                if ("1".equals(assetPingFrom)) {
                    if (server.getId().equals(promserver.getId())) continue;
                } else {
                    // per-datacenter 做 ping 检测，global 角色排除掉，不需要加入到 federate 中
                    if (Constant.PromserverType.GLOBAL.getValue().equals(server.getType())) continue;
                }
            } else {
                // 联邦关闭
                if (server.getId().equals(promserver.getId())) continue;
            }
            Map federateMap = new LinkedHashMap();
            federateMap.put("job_name", "federate_" + server.getId());
            federateMap.put("metrics_path", proxyPath + "/federate");
            federateMap.put("isFederate", true);
            // global 做 ping 检测，且 当前角色为 global
            if ("1".equals(assetPingFrom) && Constant.PromserverType.GLOBAL.getValue().equals(server.getType())) {
                federateMap.put("scrapePing", true);
            } else {
                federateMap.put("scrapePing", false);
            }
            federateMap.put("instanceRelabel", true);
            // 该标签为直接取值，不作为判断条件，所以布尔值用字符串表示
            federateMap.put("honor_labels", "true");
            federateMap.put("token", server.getToken());
            federateMap.put("excludeAgentId",promserver.getId());
            // 联邦开关
            federateMap.put("fedeEnabled", fedeEnabled);

            List federateStaticConfigList = new ArrayList();
            Map federateStaticConfig = new LinkedHashMap(2);
            List federateTargets = new ArrayList();
            federateTargets.add(server.getHost() + ":" + server.getPort());
            federateStaticConfig.put("targets", federateTargets);
            federateStaticConfigList.add(federateStaticConfig);
            federateMap.put("staticConfigList", federateStaticConfigList);
            federateMapList.add(federateMap);
        }

        // prometheus_metric
        Map promMetricsMap = new LinkedHashMap();
        promMetricsMap.put("job_name", "prometheus_metric");
        promMetricsMap.put("metrics_path", proxyPath + "/metrics");
        promMetricsMap.put("isFederate", false);
        promMetricsMap.put("instanceRelabel", false);

        List promMetricsStaticConfigList = new ArrayList();
        for (Promserver entity : allPromserverList) {
            Map promMetricsStaticConfig = new LinkedHashMap(2);
            List<String> promMetricsTargets = new ArrayList();
            promMetricsTargets.add(entity.getHost() + ":" + entity.getPort());
            promMetricsStaticConfig.put("targets", promMetricsTargets);

            Map labels = new LinkedHashMap(4);
            labels.put("datacenter", dcIdAndNameMap.get(entity.getDcId()));
            labels.put("type", Constant.PromserverType.getPromserverType(entity.getType()).getName());
            labels.put("application", "prometheus");
            promMetricsStaticConfig.put("labels", labels);

            promMetricsStaticConfigList.add(promMetricsStaticConfig);
        }
        promMetricsMap.put("staticConfigList", promMetricsStaticConfigList);

        // nezha_metrics
        Map nezhaMetricsMap = new LinkedHashMap();
        nezhaMetricsMap.put("job_name", "nezha_metrics");
        nezhaMetricsMap.put("metrics_path", configMap.get(Constant.SYSCONFIG_KEY_ALERT_PATH_PREFIX) + "/metrics");
        nezhaMetricsMap.put("isFederate", false);
        nezhaMetricsMap.put("instanceRelabel", true);

        List nezhaMetricsStaticConfigList = new ArrayList();

        Map nezhaMetricsStaticConfig = new LinkedHashMap(2);
        List nezhaMetricsTargets = new ArrayList();
        nezhaMetricsTargets.add(configMap.get(Constant.SYSCONFIG_KEY_ALERT_API));
        nezhaMetricsStaticConfig.put("targets", nezhaMetricsTargets);
        nezhaMetricsStaticConfigList.add(nezhaMetricsStaticConfig);
        nezhaMetricsMap.put("staticConfigList", nezhaMetricsStaticConfigList);

        Map<String, Object> params = new LinkedHashMap();
        params.put("scrape_interval", configMap.get(Constant.SYSCONFIG_KEY_SCRAPEINTERVAL));
        params.put("scrape_timeout", configMap.get(Constant.SYSCONFIG_KEY_SCRAPETIMEOUT));
        params.put("path_prefix", configMap.get(Constant.SYSCONFIG_KEY_ALERT_PATH_PREFIX));
        params.put("alert_url", configMap.get(Constant.SYSCONFIG_KEY_ALERT_API));
        params.put("isGlobal", true);
        params.put("isAlert", isAlert);

        params.put("assetList", assets);

        // 处理 endpoint 配置项 为了适应模板
        endpoints = this.handleEndpointConfigForTmpl(endpoints, promserver);
        params.put("endpointList", endpoints);

        List<Map> jobList = new ArrayList<>();
        // federate
        for (Map map : federateMapList) {
            jobList.add(map);
        }
        // prometheus_metric
        jobList.add(promMetricsMap);
        // nezha_metrics
        jobList.add(nezhaMetricsMap);

        // confagent_metrics
        /*Map agentMetricsMap = new LinkedHashMap();
        agentMetricsMap.put("job_name", "confagent_metrics");
        agentMetricsMap.put("metrics_path", configMap.get(Constant.CONFAGENT_PATH) + "/metrics");
        agentMetricsMap.put("isFederate", false);
        agentMetricsMap.put("instanceRelabel", true);

        List agentMetricsStaticConfigList = new ArrayList();
        Map agentMetricsStaticConfig = new LinkedHashMap(2);
        List<String> agentMetricsTargets = new ArrayList();
        agentMetricsTargets.add(promserver.getHost() + ":" + configMap.get(Constant.CONFAGENT_PORT));
        for (Promserver subInfo : perDataCenterPromServers) {
            agentMetricsTargets.add(subInfo.getHost() + ":" + configMap.get(Constant.CONFAGENT_PORT));
        }
        agentMetricsStaticConfig.put("targets", agentMetricsTargets);
        agentMetricsStaticConfigList.add(agentMetricsStaticConfig);
        agentMetricsMap.put("staticConfigList", agentMetricsStaticConfigList);

        jobList.add(agentMetricsMap);*/
        params.put("jobList", jobList);

        Map map = null;
        String content = "";
        try {
            Template template = TemplateUtil.stringToTemplate(configMap.get(Constant.SYSCONFIG_KEY_PROM_YML_TMPL), "prom_yaml_tmpl");
            content = FreeMarkerTemplateUtils.processTemplateIntoString(template, params);
            map = JSONObject.parseObject(content, LinkedHashMap.class, Feature.OrderedField);
        } catch (IOException | TemplateException | RuntimeException e) {
            log.error("global prometheus.yml 模板生成失败" + JSONObject.toJSONString(content), e);
        }
        return map;
    }

    /**
     * 生成 rule.yml
     *
     * @param configMap
     * @return
     */
    private Map generateRuleYml(Map<String, String> configMap) {
        List<AlertRuleEntity> alertRuleEntities = ruleService.list(new LambdaQueryWrapper<AlertRuleEntity>().ne(AlertRuleEntity::getBuildIn, 1));
        List<AlertSeverityConf> severityConfs = alertSeverityService.list();
        Map<Integer, String> alertSeverityIdAndNameMap = severityConfs.stream().collect(Collectors.toMap(AlertSeverityConf::getId, AlertSeverityConf::getName));

        List<Map> ruleList = new ArrayList<>();
        for (AlertRuleEntity entity : alertRuleEntities) {
            Map map = new LinkedHashMap(16);
            map.put("id", entity.getId());
            map.put("expr", ToolUtil.string2Json(entity.getExpr()));
            map.put("operator", entity.getOperator());
            // 以字符串方式替换 double 类型，保证不丢失精度
            map.put("threshold", entity.getThreshold() + "");
            map.put("last", entity.getLast());

            String severityName = alertSeverityIdAndNameMap.get(entity.getSeverityId());
            map.put("severity_name", ToolUtil.string2Json(severityName));
            map.put("severity_id", entity.getSeverityId());
            map.put("summary", ToolUtil.string2Json(entity.getSummary()));
            map.put("description", ToolUtil.string2Json(entity.getDescription()));
            ruleList.add(map);
        }

        Map params = new LinkedHashMap(4);
        String endpointAlertPush = configMap.get(Constant.ENDPOINT_ALERT_PUSH);
        if (StrUtil.equals(endpointAlertPush, "1")) {
            params.put("endpointAlert", true);
        } else {
            params.put("endpointAlert", false);
        }
        params.put("ruleList", ruleList);

        Map map = null;
        String content = "";
        try {
            Template template = TemplateUtil.stringToTemplate(configMap.get(Constant.SYSCONFIG_KEY_PROM_RULE_TMPL), "prom_rule_tmpl");
            content = FreeMarkerTemplateUtils.processTemplateIntoString(template, params);
            map = JSONObject.parseObject(content, LinkedHashMap.class, Feature.OrderedField);
        } catch (IOException | TemplateException | RuntimeException e) {
            log.error("rule 模板生成失败，模板内容:" + JSONObject.toJSONString(content), e);
        }
        log.debug("rule.yml 生成结束");
        return map;
    }

    /**
     * cmdline 模板生成
     *
     * @param configMap
     * @return
     */
    private Map<String, String> generateCmdLine(Map<String, String> configMap) {
        Map params = new HashMap(2);
        // params.put("storageDays", configMap.get(Constant.SYSCONFIG_KEY_STORAGE_LOCAL_RETENTION)); // deprecated

        String content = "";
        try {
            Template template = TemplateUtil.stringToTemplate(configMap.get(Constant.SYSCONFIG_KEY_PROM_CMD_TMPL), Constant.SYSCONFIG_KEY_PROM_CMD_TMPL);
            content = FreeMarkerTemplateUtils.processTemplateIntoString(template, params);
        } catch (IOException | TemplateException | RuntimeException e) {
            log.error("cmdLine 模板生成失败" + JSONObject.toJSONString(content), e);
        }

        Map<String, String> cmdMap = AgentUtil.loadServiceConfigStr(content);
        return cmdMap;
    }

    /**
     * 组装 snmp.yml
     * @return
     */
    private Map generateSnmpYml() {
        List<MonitorModule> snmpTypeModule = monitorModuleService.list(new LambdaQueryWrapper<MonitorModule>().eq(MonitorModule::getType, Constant.MODULE_TYPE_SNMP));
        Set<String> snmpYmls = snmpTypeModule.stream().map(MonitorModule::getSnmpYml).collect(Collectors.toSet());
        Map resultMap = new LinkedHashMap();
        snmpYmls.stream().forEach(s -> resultMap.putAll(JSONObject.parseObject(s)));
        return resultMap;
    }

    /**
     * 处理 per-datacenter 角色 配置
     * @param perDataCenterPromServers
     * @param configMap
     * @param sysCmdLineMap
     */
    private void handlePerDatacenter(List<Promserver> perDataCenterPromServers, Map<String, String> configMap, Map<String, String> sysCmdLineMap) {
        String fedeEnabledStr = configMap.get(Constant.SYSCONFIG_KEY_PROM_FEDER_ENABLED);
        log.debug(String.format("prometheus 联邦状态: %s", "0".equalsIgnoreCase(fedeEnabledStr) ? "关闭" : "开启"));
        // 联邦关闭 禁用所有 per-datacenter 角色配置
        if (fedeEnabledStr.equals("0")) return;
        // 按照 dc 分组
        Map<Integer, List<Promserver>> dcIdAndPromservers = perDataCenterPromServers.stream().collect(Collectors.groupingBy(Promserver::getDcId));

        // 用于生成配置的 endpoint 信息
        List<MonitorEndpoint> enableEndpointInfoList = monitorEndpointService.getEnableMetricsEndpointInfoListForAgent();

        boolean snmpChangeFlag = this.compareDataChanges("snmp");
        log.debug(String.format("conf_event snmp 数据%s发生变化", snmpChangeFlag ? "" : "没有"));

        // blackbox 所需 asset 信息
        List<AssetAsset> blackboxAssetInfoList = assetService.getBlackboxAssetInfoList();

        // 维护 redis 中 endpoint id and promserver id map
        Map<Integer, Integer> updateRedisEndpointPromMap = new LinkedHashMap<>(16);
        for (Map.Entry<Integer, List<Promserver>> entry : dcIdAndPromservers.entrySet()) {
            Integer dcId = entry.getKey();
            List<Promserver> promserverListByDc = entry.getValue();
            List<MonitorEndpoint> endpointListByDc = enableEndpointInfoList.stream().filter(endpoint -> dcId.equals(endpoint.getDc().getId())).collect(Collectors.toList());

            // endpoint id 和 promerver id 对应 map
            Map<Integer, Integer> endpointIdAndPromIdMap = this.hashAllocationEndpoind(promserverListByDc, endpointListByDc);

            List<AssetAsset> assetListByDc = blackboxAssetInfoList.stream().filter(asset -> dcId.equals(asset.getDcId())).collect(Collectors.toList());
            // asset id 和 promerver id 对应 map
            String assetPingFrom = configMap.get(Constant.SYSCONFIG_KEY_ASSET_PING_FROM);
            Map<Integer, Integer> assetIdAndPromIdMap = "2".equals(assetPingFrom) ? this.hashAllocationAsset(promserverListByDc, assetListByDc) : new HashMap<>(2);

            // 组装 per-datacenter 配置信息,并下发配置
            this.reloadPerDatacenterYml(promserverListByDc, endpointListByDc, endpointIdAndPromIdMap, assetListByDc, assetIdAndPromIdMap, configMap, sysCmdLineMap, snmpChangeFlag);

            updateRedisEndpointPromMap.putAll(endpointIdAndPromIdMap);
        }

        // 保存 redis 中 endpointid 和　promserver id对应关系
        this.saveEndpointPromInRedis(updateRedisEndpointPromMap);
    }

    /**
     * 组装 per-datacenter 配置信息,并下发配置
     *  @param promserverListByDc
     * @param endpointListByDc
     * @param endpointIdAndPromIdMap
     * @param assetListByDc
     * @param assetIdAndPromIdMap
     * @param configMap
     * @param sysCmdLineMap
     * @param snmpChangeFlag
     */
    private void reloadPerDatacenterYml(List<Promserver> promserverListByDc, List<MonitorEndpoint> endpointListByDc, Map<Integer, Integer> endpointIdAndPromIdMap, List<AssetAsset> assetListByDc, Map<Integer, Integer> assetIdAndPromIdMap, Map<String, String> configMap, Map<String, String> sysCmdLineMap, boolean snmpChangeFlag) {
        for (Promserver promserver : promserverListByDc) {
            List<Integer> endpointIdListByPromServer = endpointIdAndPromIdMap.entrySet().stream().filter(entry -> promserver.getId().equals(entry.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
            List<MonitorEndpoint> endpointListByPromServer = endpointListByDc.stream().filter(endpoint -> endpointIdListByPromServer.contains(endpoint.getId())).collect(Collectors.toList());

            List<Integer> assetIdListByPromServer = assetIdAndPromIdMap.entrySet().stream().filter(entry -> promserver.getId().equals(entry.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
            List<AssetAsset> assetList = assetListByDc.stream().filter(asset -> assetIdListByPromServer.contains(asset.getId())).collect(Collectors.toList());
            assetList.stream().forEach(asset -> asset.setPromserver(promserver));

            Map<String, Object> perDatacenterConfigBody = new LinkedHashMap(4);

            // 生成配置模板
            Map perdatacenterYmlMap = this.generatePerdatacenterYml(promserver,endpointListByPromServer, assetList, configMap);
            perDatacenterConfigBody.put("config", perdatacenterYmlMap);

            boolean cmdFlag = this.compareCmdLineChanges(promserver, sysCmdLineMap, configMap);
            if (cmdFlag) {
                log.debug("cmdline 配置下发" + JSONObject.toJSONString(promserver));
                perDatacenterConfigBody.put("cmdline", sysCmdLineMap);
            }

            // 调用 config 接口 下发 perdatacenter 配置
            UrlBuilder urlBuilder = UrlBuilder.create();
            urlBuilder.setScheme("http").setHost(promserver.getHost()).setPort(promserver.getPort()).appendPath(configMap.get(Constant.CONFAGENT_PATH)).appendPath("prometheus").appendPath("config");
            String result = null;
            try {
                result = Tool.HttpUtil.createPost(urlBuilder.build()).auth(promserver.getToken()).body(JSONObject.toJSONString(perDatacenterConfigBody), "application/json;charset=UTF-8").execute().body();
            } catch (RuntimeException e) {
                log.error(String.format("请求 %s 接口失败", urlBuilder.toString()), e);
            }

            // 下发失败,更新 prom 状态，造成conf_event 变更，下个周期继续下发配置
            if (result == null) {
                this.updatePromServerStatus(promserver, 0);
                continue;
            }

            this.afterRequestApi(result, promserver);

            if (snmpChangeFlag) {
                log.debug("snmp.yml 配置下发" + JSONObject.toJSONString(promserver));
                // snmp_exporter 模板信息
                Map snmpExporterYmlMap = this.generateSnmpYml();

                UrlBuilder urlBuilder1 = UrlBuilder.create();
                urlBuilder1.setScheme("http").setHost(promserver.getHost()).setPort(promserver.getPort()).appendPath(configMap.get(Constant.CONFAGENT_PATH)).appendPath("snmpExporter").appendPath("config");
                String snmpConfResult = null;
                try {
                    snmpConfResult = Tool.HttpUtil.createPost(urlBuilder1.build()).auth(promserver.getToken()).body(JSONObject.toJSONString(snmpExporterYmlMap)).execute().body();
                } catch (RuntimeException e) {
                    log.error(String.format("请求 %s 接口失败", urlBuilder1.toString()), e);
                }
                log.debug("snmp 请求返回结果 {}", snmpConfResult);
                if (snmpConfResult == null) {
                    this.updatePromServerStatus(promserver, 0);
                    continue;
                }

                this.afterRequestApi(snmpConfResult, promserver);
            }
        }
    }

    private Map generatePerdatacenterYml(Promserver promserver, List<MonitorEndpoint> endpointList, List<AssetAsset> assetList, Map<String, String> configMap) {
        Map<String, Object> params = new LinkedHashMap();
        params.put("scrape_interval", configMap.get(Constant.SYSCONFIG_KEY_SCRAPEINTERVAL));
        params.put("scrape_timeout", configMap.get(Constant.SYSCONFIG_KEY_SCRAPETIMEOUT));
        params.put("path_prefix", "");
        params.put("alert_url", "");

        params.put("isAlert", false);
        params.put("isGlobal", false);

        params.put("assetList", assetList);

        // 处理 endpoint 配置项 为了适应模板
        endpointList = this.handleEndpointConfigForTmpl(endpointList, promserver);

        params.put("endpointList", endpointList);

        Map map = null;
        String content = "";
        try {
            Template template = TemplateUtil.stringToTemplate(configMap.get(Constant.SYSCONFIG_KEY_PROM_YML_TMPL), "prom_yaml_tmpl");
            content = FreeMarkerTemplateUtils.processTemplateIntoString(template, params);
            map = JSONObject.parseObject(content, LinkedHashMap.class, Feature.OrderedField);
        } catch (IOException | TemplateException | RuntimeException e) {
            log.error("per-datacenter prometheus.yml 模板生成失败, 模板内容: " + JSONObject.toJSONString(content), e);
        }
        return map;
    }

    /**
     * 处理 endpoint 配置信息 为了适应模板，防止模板生成内容有误
     * @param endpointList
     * @param promserver
     * @return
     */
    private List<MonitorEndpoint> handleEndpointConfigForTmpl(List<MonitorEndpoint> endpointList, Promserver promserver) {
        for (MonitorEndpoint endpoint : endpointList) {
            //endpoint.setConfig(JSONObject.parseObject(endpoint.getConfigs(), Map.class));

            // project name 处理
            MonitorProject project = endpoint.getProject();
            project.setName(ToolUtil.string2Json(project.getName()));
            endpoint.setProject(project);

            // module name 处理
            MonitorModule module = endpoint.getModule();
            module.setName(ToolUtil.string2Json(module.getName()));
            endpoint.setModule(module);

            // dc name 处理
            Dc dc = endpoint.getDc();
            dc.setName(ToolUtil.string2Json(dc.getName()));
            endpoint.setDc(dc);

            // asset name 处理
            AssetAsset asset = endpoint.getAsset();
            asset.setName(ToolUtil.string2Json(asset.getName()));
            endpoint.setAsset(asset);

            // endpoint name 处理
            endpoint.setName(ToolUtil.string2Json(endpoint.getName()));

            // nz_agent_id，对应 promserver id
            endpoint.setPromserver(promserver);
        }
        return endpointList;
    }

    /**
     * 通过 真实节点获取 hash 环， 真实节点 + 虚拟节点
     * @param realNodeList
     * @return
     */
    private SortedMap<Long, String> getHashRingByRealList(Map<String, Integer> realNodeList){
        // 初始化, 将节点 hash 映射到 hash 环上
        SortedMap<Long, String> nodeMap = new TreeMap<>();

        for (String node : realNodeList.keySet()) {
            // 节点信息  IP+PORT
            long nodeHash = Math.abs(MurmurHash.hash64(node));
            nodeMap.put(nodeHash, node);

            // 虚节点
            for (int i = 0; i < virtualAgentNum; i++) {
                long virtualHash = Math.abs(MurmurHash.hash64((node + "#" + i)));
                nodeMap.put(virtualHash, node);
            }
        }
        return nodeMap;
    }

    /**
     * 一致性 hash 分配 endpoint
     *
     * @param promserverListByDc
     * @param endpointListByDc
     * @return
     */
    private Map<Integer, Integer> hashAllocationEndpoind(List<Promserver> promserverListByDc, List<MonitorEndpoint> endpointListByDc) {
        // 1. 真实节点
        Map<String, Integer> realNodeList = promserverListByDc.stream().collect(Collectors.toMap(promserver -> promserver.getHost() + ":" + promserver.getPort(), Promserver::getId));
        // 2. 通过 真实节点 加上虚拟节点获取 hash 环
        SortedMap<Long, String> nodeMap = this.getHashRingByRealList(realNodeList);

        if (log.isDebugEnabled()) log.debug("endpoint hash 环节点信息: " + JSONObject.toJSONString(nodeMap));

        // endpoint id 和 promerver id 对应 map
        Map<Integer, Integer> endpointIdAndPromIdMap = new LinkedHashMap(16);

        // endpoint hash 处理
//        Map<Integer, String> endpointIdAndHashMap = endpointListByDc.stream().collect(Collectors.toMap(MonitorEndpoint::getId, MonitorEndpoint::getHash));
        Map<Integer, String> endpointIdAndHashMap = new HashMap<>();
        for (Map.Entry<Integer, String> entry : endpointIdAndHashMap.entrySet()) {
            Integer endpointId = entry.getKey();
            String endpointHash = entry.getValue();
            // 顺时针最近的节点
            SortedMap<Long, String> tailMap = nodeMap.tailMap(Long.valueOf(endpointHash));
            // 可能为空, 取环中的第一个节点
            if (tailMap.isEmpty()) {
                Long nodeIndex = nodeMap.firstKey();
                endpointIdAndPromIdMap.put(endpointId, realNodeList.get(nodeMap.get(nodeIndex)));
                if (log.isDebugEnabled())
                    log.debug("endpoint: " + endpointId + "\t" + "agent: " + nodeMap.get(nodeIndex) + "\t" + "promserver id: " + realNodeList.get(nodeMap.get(nodeIndex)));
            } else {
                Long nodeIndex = tailMap.firstKey();
                endpointIdAndPromIdMap.put(endpointId, realNodeList.get(nodeMap.get(nodeIndex)));
                if (log.isDebugEnabled())
                    log.debug("endpoint: " + endpointId + "\t" + "agent: " + nodeMap.get(nodeIndex) + "\t" + "promserver id: " + realNodeList.get(nodeMap.get(nodeIndex)));
            }
        }
        return endpointIdAndPromIdMap;
    }


    /**
     * 一致性 hash 分配 asset
     * @param promserverList
     * @param assetList
     * @return
     */
    private Map<Integer, Integer> hashAllocationAsset(List<Promserver> promserverList, List<AssetAsset> assetList) {
        // 1. 真实节点
        Map<String, Integer> realNodeList = promserverList.stream().collect(Collectors.toMap(promserver -> promserver.getHost() + ":" + promserver.getPort(), Promserver::getId));
        // 2. 通过 真实节点 加上虚拟节点获取 hash 环
        SortedMap<Long, String> nodeMap = this.getHashRingByRealList(realNodeList);

        if (log.isDebugEnabled()) log.debug("asset hash 环节点信息:" + JSONObject.toJSONString(nodeMap));

        // asset id 和 promerver id 对应 map
        Map<Integer, Integer> assetIdAndPromIdMap = new LinkedHashMap(16);

        // asset hash 处理
        Map<Integer, String> assetIdAndHashMap = assetList.stream().collect(Collectors.toMap(AssetAsset::getId, asset -> String.valueOf(Math.abs(MurmurHash.hash64(asset.getManageIp() + ":" + asset.getDcId())))));
        for (Map.Entry<Integer, String> entry : assetIdAndHashMap.entrySet()) {
            Integer assetId = entry.getKey();
            String assetHash = entry.getValue();
            // 顺时针最近的节点
            SortedMap<Long, String> tailMap = nodeMap.tailMap(Long.valueOf(assetHash));
            // 可能为空, 取环中的第一个节点
            if (tailMap.isEmpty()) {
                Long nodeIndex = nodeMap.firstKey();
                assetIdAndPromIdMap.put(assetId, realNodeList.get(nodeMap.get(nodeIndex)));
                if (log.isDebugEnabled())
                    log.debug("asset: " + assetId + "\t" + "agent: " + nodeMap.get(nodeIndex) + "\t" + "promserver id: " + realNodeList.get(nodeMap.get(nodeIndex)));
            } else {
                Long nodeIndex = tailMap.firstKey();
                assetIdAndPromIdMap.put(assetId, realNodeList.get(nodeMap.get(nodeIndex)));
                if (log.isDebugEnabled())
                    log.debug("asset: " + assetId + "\t" + "agent: " + nodeMap.get(nodeIndex) + "\t" + "promserver id: " + realNodeList.get(nodeMap.get(nodeIndex)));
            }
        }
        return assetIdAndPromIdMap;
    }

    /**
     * 对比 cmd 启动项
     *
     * @param promserver
     * @param sysCmdLineMap
     * @param configMap
     * @return
     */
    private boolean compareCmdLineChanges(Promserver promserver, Map<String, String> sysCmdLineMap, Map<String, String> configMap) {
        UrlBuilder urlBuilder = UrlBuilder.create();
        urlBuilder.setScheme("http").setHost(promserver.getHost()).setPort(promserver.getPort()).appendPath(configMap.get(Constant.CONFAGENT_PATH)).appendPath("prometheus").appendPath("config");
        //  调用 /config 接口
        String result = null;
        try {
            result = Tool.HttpUtil.createGet(urlBuilder.build()).auth(promserver.getToken()).execute().body();
        } catch (RuntimeException e) {
            log.error(String.format("请求 %s 接口失败", urlBuilder.toString()), e);
        }

        // 请求接口失败 返回 false 表示本次启动项对比成功，修改 promserver 状态，造成下次继续执行对比
        if (result == null) {
            this.updatePromServerStatus(promserver, 0);
            return false;
        }

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
        return false;
    }

    /**
     * 判断配置相关信息是否发生改变
     *
     * @param filterKeys 判断那些 key 发生变化， 为空则全部进行对比
     * @return
     */
    private boolean compareDataChanges(String... filterKeys) {
        boolean flag = false;
        Boolean hasKey = redisTemplate.hasKey(Constant.REDIS_KEY_CONF_EVENT);

        // 第一次时 为true 全量更新
        if (!hasKey) {
            flag = true;
            List<MonitorEventLog> eventList = confEventDao.selectList(null);
            if (ToolUtil.isNotEmpty(filterKeys)) {
                eventList = eventList.stream().filter(monitorConfEvent -> Arrays.asList(filterKeys).contains(monitorConfEvent.getEvent())).collect(Collectors.toList());
            }
            Map<String, String> map = eventList.stream().collect(Collectors.toMap(MonitorEventLog::getEvent, monitorConfEvent -> String.valueOf(monitorConfEvent.getCounter())));
            redisTemplate.opsForHash().putAll(Constant.REDIS_KEY_CONF_EVENT, map);
            return flag;
        }

        HashOperations opsForHash = redisTemplate.opsForHash();
        Map<String, String> confEvent = opsForHash.entries(Constant.REDIS_KEY_CONF_EVENT);

        List<MonitorEventLog> eventList = confEventDao.selectList(null);
        if (ToolUtil.isNotEmpty(filterKeys)) {
            eventList = eventList.stream().filter(monitorConfEvent -> Arrays.asList(filterKeys).contains(monitorConfEvent.getEvent())).collect(Collectors.toList());
        }
        for (MonitorEventLog data : eventList) {
            if (ToolUtil.isEmpty(confEvent.get(data.getEvent())) || !data.getCounter().equals(Integer.valueOf(confEvent.get(data.getEvent())))) {
                flag = true;
            }
        }
        // 更新 redis 中 conf_event 数据
        Map<String, String> map = eventList.stream().collect(Collectors.toMap(MonitorEventLog::getEvent, monitorConfEvent -> String.valueOf(monitorConfEvent.getCounter())));
        opsForHash.delete(Constant.REDIS_KEY_CONF_EVENT, map.keySet().toArray());
        opsForHash.putAll(Constant.REDIS_KEY_CONF_EVENT, map);
        return flag;
    }

    /**
     * 获取健康的 promservers
     * @param allPromServers
     * @param configMap
     * @return
     */
    private List<Promserver> getHealthyPromServers(List<Promserver> allPromServers, Map<String, String> configMap) {
        List<Callable<Promserver>> tasks = new ArrayList<>();
        allPromServers.stream().forEach(promServer -> {
            Callable<Promserver> task = new Callable<Promserver>() {
                @Override
                public Promserver call() throws Exception {
                    UrlBuilder urlBuilder = UrlBuilder.create();
                    urlBuilder.setScheme("http").setHost(promServer.getHost()).setPort(promServer.getPort()).appendPath(configMap.get(Constant.CONFAGENT_PATH)).appendPath("healthy");
                    String result = null;
                    try {
                        result = Tool.HttpUtil.createGet(urlBuilder.build()).timeout(connectTimeout).auth(promServer.getToken()).execute().body();
                    } catch (RuntimeException e) {
                        log.error(String.format("请求 %s 接口失败", urlBuilder.toString()), e);
                    }
                    // 接口请求失败
                    if (result == null) {
                        promServer.setStatus(0);
                        promServer.setStatusInfo(getPromStatusInfoErrorMsg());
                    } else {
                        try {
                            R resultObj = JSONObject.parseObject(result, R.class);
                            if (RCode.SUCCESS.getCode().equals((Integer) resultObj.get("code"))) {
                                Map<String, Object> data = (Map) resultObj.get("data");

                                String prometheusState = (String) data.get("prometheus");
                                String confagentState = (String) data.get("confagent");
                                String snmpExporterState = (String) data.get("snmp_exporter");
                                String blackboxExporterState = (String) data.get("blackbox_exporter");
                                String authState = (String) data.get("auth");
                                if ("UP".equals(prometheusState) && "UP".equals(confagentState) && "UP".equals(snmpExporterState) && "UP".equals(blackboxExporterState) && "TRUE".equals(authState)) {
                                    // 服务正常  认证成功
                                    promServer.setStatus(1);
                                } else if ("UP".equals(prometheusState) && "UP".equals(confagentState) && "UP".equals(snmpExporterState) && "UP".equals(blackboxExporterState) && "FALSE".equals(authState)) {
                                    // 服务正常  认证失败
                                    promServer.setStatus(-1);
                                } else {
                                    // 异常
                                    promServer.setStatus(0);
                                }
                                data.remove("auth");
                                promServer.setStatusInfo(JSONObject.toJSONString(data));
                            } else {
                                promServer.setStatus(0);
                                promServer.setStatusInfo(getPromStatusInfoErrorMsg());
                            }
                        } catch (Exception e) {
                            log.error(String.format("%s 接口相应内容有误", urlBuilder.toString()), e);
                            promServer.setStatus(0);
                            promServer.setStatusInfo(getPromStatusInfoErrorMsg());
                        }
                    }
                    promServer.setCheckTime(new Date());
                    return promServer;
                }
            };
            tasks.add(task);
        });

        List<Promserver> healthyPromServers = new ArrayList<>();
        try {
            List<Future<Promserver>> futures = exec.invokeAll(tasks);
            for (Future<Promserver> future : futures) {
                if (!future.isCancelled()) {
                    Promserver promserver = future.get();
                    if (promserver.getStatus().equals(1)) {
                        healthyPromServers.add(promserver);
                    }
                    promService.updateStatusInfo(promserver);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error(e);
        }
        return healthyPromServers;
    }


    /**
     * 请求接口异常时,status info 信息
     * @return
     */
    private static String getPromStatusInfoErrorMsg() {
        Map errorData = new HashMap(4);
        errorData.put("prometheus", "DOWN");
        errorData.put("confagent", "DOWN");
        errorData.put("snmp_exporter", "DOWN");
        return JSONObject.toJSONString(errorData);
    }

    /**
     * 请求失败，更新 promserver 状态
     *
     * @param result
     * @param promserver
     */
    private void afterRequestApi(String result, Promserver promserver) {
        R snmpResultObj = JSONObject.parseObject(result, R.class);
        if (!RCode.SUCCESS.getCode().equals((Integer) snmpResultObj.get("code"))) {
            log.debug("请求失败，promserver 状态变更为 0 {}", JSONObject.toJSONString(promserver));
            this.updatePromServerStatus(promserver, 0);
        }
    }

    /**
     * 修改 promservers 状态
     *
     * @param promserver
     * @param status
     */
    private void updatePromServerStatus(Promserver promserver, Integer status) {
        promserver.setStatus(status);
        promserver.setStatusInfo(getPromStatusInfoErrorMsg());
        promserver.setCheckTime(new Date());
        promService.updateById(promserver);
    }
}
