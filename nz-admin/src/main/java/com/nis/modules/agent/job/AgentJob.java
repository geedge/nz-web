package com.nis.modules.agent.job;

import cn.hutool.core.lang.hash.MurmurHash;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.*;
import com.nis.modules.agent.dao.MonitorConfEventDao;
import com.nis.modules.agent.entity.MonitorConfEvent;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.entity.AlertSeverityConf;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.alert.service.AlertSeverityService;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.lang3.time.StopWatch;
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
import java.util.stream.Collectors;

/**
 * AgentJob
 *
 * @author shizhendong
 * @date 2021-04-06
 */
public class AgentJob extends QuartzJobBean {

    private static Log log = Log.get();

    @Value("${nezha.virtualAgentNum}")
    private Integer virtualAgentNum;

    @Autowired
    private DcService dcService;

    @Autowired
    private PromserverService promService;

    @Autowired
    private AlertRuleService ruleService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MonitorConfEventDao confEventDao;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private AlertSeverityService alertSeverityService;

    @Autowired
    private MonitorEndpointService monitorEndpointService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 检查 haMode
        boolean haMode = sysConfigService.checkHaMode();
        if (!haMode) return;

        StopWatch sw = new StopWatch();
        sw.start();
        try {
            // 配置下发
            this.setUpAgentConfig();
        } catch (RuntimeException e) {
            log.error("agent 同步异常", e);
        } finally {
            sw.stop();
        }
        log.info("agent 执行结束，Run Time: {}", sw.toString());
    }

    public void setUpAgentConfig() {
        // 对比数据变化
        boolean changeFlag = this.compareDataChanges("cmd", "global", "per-datacenter");
        log.debug(String.format("conf_event 数据%s发生变化", changeFlag ? "" : "没有"));

        if (changeFlag) {
            // 配置信息
            List<SysConfigEntity> configEntityList = sysConfigService.list();
            Map<String, String> configMap = configEntityList.stream().collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));

            // 全部 prom_server
            List<Promserver> allPromServers = promService.list();

            // 健康的 prom_server
            List<Promserver> healthyPromServers = this.getHealthyPromServers(allPromServers, configMap);
            log.debug("健康的 Promservers: " + JSONObject.toJSONString(healthyPromServers));

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
     *
     * @param globalPromServers
     * @param perDataCenterPromServers
     * @param configMap
     * @param sysCmdLineMap
     */
    private void handleGlobal(List<Promserver> globalPromServers, List<Promserver> perDataCenterPromServers, Map<String, String> configMap, Map<String, String> sysCmdLineMap) {
        // 按照 id 升序，方便 alert 设置
        globalPromServers = globalPromServers.stream().sorted(Comparator.comparing(Promserver::getId)).collect(Collectors.toList());

        // global config 配置文件处理 联邦 告警 rule.yml 等，
        for (int i = 0; i < globalPromServers.size(); i++) {
            Promserver promserver = globalPromServers.get(i);
            Boolean isAlert = false;
            if (i == 0) {
                isAlert = true;
                log.debug("支持告警的 agent :" + JSONObject.toJSONString(promserver));
            }

            Map<String, Object> globalConfigBody = new LinkedHashMap(4);

            Map gloablAgentYmlMap = this.generateGlobalYml(isAlert, promserver, globalPromServers, perDataCenterPromServers, configMap);
            globalConfigBody.put("config", gloablAgentYmlMap);
            if (isAlert) {
                // 生成告警配置文件
                Map alertYmlMap = this.generateRuleYml(configMap);
                globalConfigBody.put("rule", alertYmlMap);
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
     * global yml
     *
     * @param isAlert
     * @param promserver
     * @param globalPromServers
     * @param perDataCenterPromServers
     * @param configMap
     * @return
     */
    private Map generateGlobalYml(Boolean isAlert, Promserver promserver, List<Promserver> globalPromServers, List<Promserver> perDataCenterPromServers, Map<String, String> configMap) {
        List<Dc> dcList = dcService.list();
        Map<Integer, String> dcIdAndNameMap = dcList.stream().collect(Collectors.toMap(Dc::getId, Dc::getName));

        List<Promserver> allPromserverList = new ArrayList<>();
        allPromserverList.addAll(globalPromServers);
        allPromserverList.addAll(perDataCenterPromServers);

        // federate
        List<Map> federateMapList = new ArrayList<>();
        for (Promserver subInfo : perDataCenterPromServers) {
            Map federateMap = new LinkedHashMap();
            federateMap.put("job_name", "federate_" + subInfo.getId());
            federateMap.put("metrics_path", "/federate");
            federateMap.put("isFederate", true);
            federateMap.put("instanceRelabel", true);
            // 该标签为直接取值，不作为判断条件，所以布尔值用字符串表示
            federateMap.put("honor_labels", "true");
            federateMap.put("token", subInfo.getToken());

            List federateStaticConfigList = new ArrayList();
            Map federateStaticConfig = new LinkedHashMap(2);
            List federateTargets = new ArrayList();
            federateTargets.add(subInfo.getHost() + ":" + subInfo.getPort());
            federateStaticConfig.put("targets", federateTargets);
            federateStaticConfigList.add(federateStaticConfig);
            federateMap.put("staticConfigList", federateStaticConfigList);
            federateMapList.add(federateMap);
        }

        // prometheus_metric
        Map promMetricsMap = new LinkedHashMap();
        promMetricsMap.put("job_name", "prometheus_metric");
        promMetricsMap.put("metrics_path", "/metrics");
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
        Map agentMetricsMap = new LinkedHashMap();
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

        jobList.add(agentMetricsMap);
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
        params.put("storageDays", configMap.get(Constant.SYSCONFIG_KEY_STORAGE_LOCAL_RETENTION));

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
     *
     * @param endpointList
     * @return
     */
    private Map generateSnmpYml(List<MonitorEndpoint> endpointList) {
        List<MonitorModule> snmpTypeModule = endpointList.stream().filter(endpoint -> Constant.MODULE_TYPE_SNMP.equals(endpoint.getModule().getType())).map(MonitorEndpoint::getModule).collect(Collectors.toList());
        Set<String> snmpYmls = snmpTypeModule.stream().map(MonitorModule::getSnmpYml).collect(Collectors.toSet());
        Map resultMap = new LinkedHashMap();
        snmpYmls.stream().forEach(s -> resultMap.putAll(JSONObject.parseObject(s)));
        return resultMap;
    }

    /**
     * 处理 per-datacenter 角色 配置
     *
     * @param perDataCenterPromServers
     * @param configMap
     * @param sysCmdLineMap
     */
    private void handlePerDatacenter(List<Promserver> perDataCenterPromServers, Map<String, String> configMap, Map<String, String> sysCmdLineMap) {
        // 按照 dc 分组
        Map<Integer, List<Promserver>> dcIdAndPromservers = perDataCenterPromServers.stream().collect(Collectors.groupingBy(Promserver::getDcId));

        // 用于生成配置的 endpoint 信息
        List<MonitorEndpoint> enableEndpointInfoList = monitorEndpointService.getEnableEndpointInfoList();

        boolean snmpChangeFlag = this.compareDataChanges("snmp");
        log.debug(String.format("conf_event snmp 数据%s发生变化", snmpChangeFlag ? "" : "没有"));

        // 维护 redis 中 endpoint id and promserver id map
        Map<Integer, Integer> updateRedisEndpointPromMap = new LinkedHashMap<>(16);
        for (Map.Entry<Integer, List<Promserver>> entry : dcIdAndPromservers.entrySet()) {
            Integer dcId = entry.getKey();
            List<Promserver> promserverListByDc = entry.getValue();
            List<MonitorEndpoint> endpointListByDc = enableEndpointInfoList.stream().filter(endpoint -> dcId.equals(endpoint.getDc().getId())).collect(Collectors.toList());

            // endpoint id 和 promerver id 对应 map
            Map<Integer, Integer> endpointIdAndPromIdMap = this.hashAllocationEndpoind(promserverListByDc, endpointListByDc);

            // 组装 per-datacenter 配置信息,并下发配置
            this.reloadPerDatacenterYml(promserverListByDc, endpointListByDc, endpointIdAndPromIdMap, configMap, sysCmdLineMap, snmpChangeFlag);

            updateRedisEndpointPromMap.putAll(endpointIdAndPromIdMap);
        }

        // 先删后增
        Map<String, String> tempStringMap = new LinkedHashMap<>();
        updateRedisEndpointPromMap.forEach((k, v) -> tempStringMap.put(k.toString(), v.toString()));

        redisTemplate.delete(Constant.REDIS_KEY_ENDPOINT_PROM);
        redisTemplate.opsForHash().putAll(Constant.REDIS_KEY_ENDPOINT_PROM, tempStringMap);
    }

    /**
     * 组装 per-datacenter 配置信息,并下发配置
     *
     * @param promserverListByDc
     * @param endpointListByDc
     * @param endpointIdAndPromIdMap
     * @param configMap
     * @param sysCmdLineMap
     * @param snmpChangeFlag
     */
    private void reloadPerDatacenterYml(List<Promserver> promserverListByDc, List<MonitorEndpoint> endpointListByDc, Map<Integer, Integer> endpointIdAndPromIdMap, Map<String, String> configMap, Map<String, String> sysCmdLineMap, boolean snmpChangeFlag) {
        for (Promserver promserver : promserverListByDc) {
            List<Integer> endpointIdListByPromServer = endpointIdAndPromIdMap.entrySet().stream().filter(entry -> promserver.getId().equals(entry.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
            List<MonitorEndpoint> endpointListByPromServer = endpointListByDc.stream().filter(endpoint -> endpointIdListByPromServer.contains(endpoint.getId())).collect(Collectors.toList());

            Map<String, Object> perDatacenterConfigBody = new LinkedHashMap(4);

            // 生成配置模板
            Map perdatacenterYmlMap = this.generatePerdatacenterYml(endpointListByPromServer, configMap);
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
                Map snmpExporterYmlMap = this.generateSnmpYml(endpointListByPromServer);

                UrlBuilder urlBuilder1 = UrlBuilder.create();
                urlBuilder1.setScheme("http").setHost(promserver.getHost()).setPort(promserver.getPort()).appendPath(configMap.get(Constant.CONFAGENT_PATH)).appendPath("snmpExporter").appendPath("config");
                String snmpConfResult = null;
                try {
                    snmpConfResult = Tool.HttpUtil.createPost(urlBuilder1.build()).auth(promserver.getToken()).body(JSONObject.toJSONString(snmpExporterYmlMap)).execute().body();
                } catch (RuntimeException e) {
                    log.error(String.format("请求 %s 接口失败", urlBuilder.toString()), e);
                }

                if (snmpConfResult == null) {
                    this.updatePromServerStatus(promserver, 0);
                    continue;
                }

                this.afterRequestApi(snmpConfResult, promserver);
            }
        }
    }

    private Map generatePerdatacenterYml(List<MonitorEndpoint> endpointList, Map<String, String> configMap) {
        Map<String, Object> params = new LinkedHashMap();
        params.put("scrape_interval", configMap.get(Constant.SYSCONFIG_KEY_SCRAPEINTERVAL));
        params.put("scrape_timeout", configMap.get(Constant.SYSCONFIG_KEY_SCRAPETIMEOUT));
        params.put("path_prefix", "");
        params.put("alert_url", "");

        params.put("isAlert", false);
        params.put("isGlobal", false);

        for (MonitorEndpoint endpoint : endpointList) {
            endpoint.setConfig(JSONObject.parseObject(endpoint.getConfigs(), Map.class));

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
        }

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
     * 一致性 hash 分配 endpoint
     *
     * @param promserverListByDc
     * @param endpointListByDc
     * @return
     */
    private Map<Integer, Integer> hashAllocationEndpoind(List<Promserver> promserverListByDc, List<MonitorEndpoint> endpointListByDc) {
        // 初始化, 将节点 hash 映射到 hash 环上
        SortedMap<Long, String> nodeMap = new TreeMap<>();

        Map<String, Integer> realNodeList = promserverListByDc.stream().collect(Collectors.toMap(promserver -> promserver.getHost() + ":" + promserver.getPort(), Promserver::getId));
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

        if (log.isDebugEnabled()) log.debug("虚节点信息" + JSONObject.toJSONString(nodeMap));

        // endpoint id 和 promerver id 对应 map
        Map<Integer, Integer> endpointIdAndPromIdMap = new LinkedHashMap(16);

        // endpoint hash 处理
        Map<Integer, String> endpointIdAndHashMap = endpointListByDc.stream().collect(Collectors.toMap(MonitorEndpoint::getId, MonitorEndpoint::getHash));
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
            List<MonitorConfEvent> eventList = confEventDao.selectList();
            if (ToolUtil.isNotEmpty(filterKeys)) {
                eventList = eventList.stream().filter(monitorConfEvent -> Arrays.asList(filterKeys).contains(monitorConfEvent.getTable())).collect(Collectors.toList());
            }
            Map<String, String> map = eventList.stream().collect(Collectors.toMap(MonitorConfEvent::getTable, monitorConfEvent -> String.valueOf(monitorConfEvent.getValue())));
            redisTemplate.opsForHash().putAll(Constant.REDIS_KEY_CONF_EVENT, map);
            return flag;
        }

        HashOperations opsForHash = redisTemplate.opsForHash();
        Map<String, String> confEvent = opsForHash.entries(Constant.REDIS_KEY_CONF_EVENT);

        List<MonitorConfEvent> eventList = confEventDao.selectList();
        if (ToolUtil.isNotEmpty(filterKeys)) {
            eventList = eventList.stream().filter(monitorConfEvent -> Arrays.asList(filterKeys).contains(monitorConfEvent.getTable())).collect(Collectors.toList());
        }
        for (MonitorConfEvent data : eventList) {
            if (ToolUtil.isEmpty(confEvent.get(data.getTable())) || !data.getValue().equals(Integer.valueOf(confEvent.get(data.getTable())))) {
                flag = true;
            }
        }
        // 更新 redis 中 conf_event 数据
        Map<String, String> map = eventList.stream().collect(Collectors.toMap(MonitorConfEvent::getTable, monitorConfEvent -> String.valueOf(monitorConfEvent.getValue())));
        opsForHash.delete(Constant.REDIS_KEY_CONF_EVENT, map.keySet().toArray());
        opsForHash.putAll(Constant.REDIS_KEY_CONF_EVENT, map);
        return flag;
    }

    /**
     * 获取健康的 promservers
     *
     * @param allPromServers
     * @param configMap
     * @return
     */
    private List<Promserver> getHealthyPromServers(List<Promserver> allPromServers, Map<String, String> configMap) {
        List<Promserver> healthyPromServers = new ArrayList<>();
        for (Promserver promServer : allPromServers) {
            UrlBuilder urlBuilder = UrlBuilder.create();
            urlBuilder.setScheme("http").setHost(promServer.getHost()).setPort(promServer.getPort()).appendPath(configMap.get(Constant.CONFAGENT_PATH)).appendPath("healthy");
            //  调用 healthy 接口
            String result = null;
            try {
                result = Tool.HttpUtil.createGet(urlBuilder.build()).auth(promServer.getToken()).execute().body();
            } catch (RuntimeException e) {
                log.error(String.format("请求 %s 接口失败", urlBuilder.toString()), e);
            }

            if (result == null) continue;
            R resultObj = JSONObject.parseObject(result, R.class);
            if (RCode.SUCCESS.getCode().equals((Integer) resultObj.get("code"))) {
                Map<String, Object> data = (Map) resultObj.get("data");
                String prometheusState = (String) data.get("prometheus");
                String confagentState = (String) data.get("confagent");
                String authState = (String) data.get("auth");
                if ("UP".equals(prometheusState) && "UP".equals(confagentState) && "TRUE".equals(authState)) {
                    healthyPromServers.add(promServer);
                }
            }
        }
        return healthyPromServers;
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
        // 暂时不处理
        //promserver.setStatusInfo("{}");
        promserver.setCheckTime(DateUtil.getUTCTimeByConfigTimeZone());
        promService.updateById(promserver);
    }
}
