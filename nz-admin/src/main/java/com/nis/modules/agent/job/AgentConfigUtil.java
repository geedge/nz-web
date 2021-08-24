package com.nis.modules.agent.job;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.hash.MurmurHash;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.ObjectUtil;
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
import com.nis.modules.asset.service.DcService;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.entity.MonitorEndpointConfig;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.module.service.MonitorModuleService;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Component
public class AgentConfigUtil {

    private static Log log = Log.get();

    @Value("${nezha.lokiDefaultPort:13100}")
    private Integer lokiDefaultPort;

    @Value("${nezha.cortexDefaultPort:10093}")
    private Integer cortexDefaultPort;

    @Value("${nezha.virtualAgentNum}")
    private Integer virtualAgentNum;

    @Value("${connect.timeout:30000}")
    private Integer connectTimeout;

    @Autowired
    private DcService dcService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private AlertRuleService ruleService;

    @Autowired
    private PromserverService promService;

    @Autowired
    private MonitorEventLogDao confEventDao;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private AlertSeverityService alertSeverityService;

    @Autowired
    private MonitorModuleService monitorModuleService;

    private static ExecutorService exec = Executors.newCachedThreadPool();

    /**
     * 判断配置相关信息是否发生改变
     *
     * @param filterKeys 判断那些 key 发生变化， 为空则全部进行对比
     * @return
     */
    public boolean compareDataChanges(String... filterKeys) {
        boolean flag = false;
        Boolean hasKey = redisTemplate.hasKey(Constant.REDIS_KEY_CONF_EVENT);

        // 第一次时 为 true 全量更新
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
     * 生成 rule.yml
     *
     * @return
     */
    public Map generateRuleYml() {
        log.debug("rule.yml 开始生成");
        // sys_config 配置表信息
        List<SysConfigEntity> configEntityList = sysConfigService.list();
        Map<String, String> configMap = configEntityList.stream().collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));

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
        // alert 方式实现 endpoint state  --   该方案不再使用，兼容未删除
        String endpointAlertPush = configMap.get(Constant.ENDPOINT_ALERT_PUSH);
        if (StrUtil.equals(endpointAlertPush, "1")) {
            params.put("endpointAlert", true);
        } else {
            params.put("endpointAlert", false);
        }
        params.put("ruleList", ruleList);

        Map map = new HashMap();
        String content = "";
        try {
            Template template = TemplateUtil.stringToTemplate(configMap.get(Constant.SYSCONFIG_KEY_PROM_RULE_TMPL), Constant.SYSCONFIG_KEY_PROM_RULE_TMPL);
            content = FreeMarkerTemplateUtils.processTemplateIntoString(template, params);
            map = JSONObject.parseObject(content, LinkedHashMap.class, Feature.OrderedField);
        } catch (IOException | TemplateException | RuntimeException e) {
            log.error("rule 模板生成失败，模板内容:" + JSONObject.toJSONString(content), e);
        }
        log.debug("rule.yml 生成结束");
        return map;
    }

    /**
     * 生成 cortex.yml
     *
     * @param promserver
     * @param globalPromServerList
     * @return
     */
    public Map generateCortexYml(Promserver promserver, List<Promserver> globalPromServerList) {
        log.debug("cortex.yml 开始生成");
        Map params = new LinkedHashMap(8);

        params.put("ip", promserver.getHost());
        params.put("objectStroageUrl", sysConfigService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_ENDPOINT));
        params.put("bucketName", sysConfigService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_BUCKET));
        params.put("accessKey", sysConfigService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_ACCESS_KEY));
        params.put("secret", sysConfigService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_SECRET_ACCESS_KEY));
        params.put("insecure", sysConfigService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_INSECURE));
        //  object storage 标识  1: 本地文件存储，2：S3对象存储
        String objStroageEnable = sysConfigService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_TYPE);
        boolean isLocalStorage = StrUtil.equals("1", objStroageEnable);
        params.put("isLocalStorage", isLocalStorage);

        List<String> joinMemberIpList = new ArrayList<>();
        joinMemberIpList = globalPromServerList.stream().filter(entity -> !entity.getId().equals(promserver.getId())).map(Promserver::getHost).collect(Collectors.toList());
        params.put("joinMemberList", joinMemberIpList);
        params.put("isMemberlistStore", CollectionUtil.isNotEmpty(joinMemberIpList));

        Map map = new HashMap();
        String content = "";
        try {
            String cortexYmlTmpl = sysConfigService.getValue(Constant.SYSCONFIG_KEY_CORTEX_YML_TMPL);
            Template template = TemplateUtil.stringToTemplate(cortexYmlTmpl, Constant.SYSCONFIG_KEY_CORTEX_YML_TMPL);
            content = FreeMarkerTemplateUtils.processTemplateIntoString(template, params);
            map = JSONObject.parseObject(content, LinkedHashMap.class, Feature.OrderedField);
        } catch (IOException | TemplateException | RuntimeException e) {
            log.error("cortex 模板生成失败，模板内容:" + JSONObject.toJSONString(content), e);
        }
        log.debug("cortex.yml 生成结束");
        return map;
    }

    /**
     * 生成 loki.yml
     *
     * @return
     */
    public Map generateLokiYml() {
        log.debug("loki.yml 开始生成");
        Map params = new LinkedHashMap(16);

        params.put("schemaConfigFrom", DateUtil.format(new Date(), "yyyy-MM-dd"));
        String logsStorageType = sysConfigService.getValue(Constant.SYSCONFIG_KEY_LOGS_STORAGE_TYPE);
        params.put("openAws", StrUtil.equals("2", logsStorageType));
        String logsStorageRetention = sysConfigService.getValue(Constant.SYSCONFIG_KEY_LOGS_STORAGE_RETENTION);
        params.put("storageRetention", Integer.valueOf(logsStorageRetention) * 24);
        params.put("storageLocalPath", sysConfigService.getValue(Constant.SYSCONFIG_KEY_LOGS_STORAGE_LOCAL_PATH));


        params.put("objectStroageUrl", sysConfigService.getValue(Constant.SYSCONFIG_KEY_LOGS_STORAGE_S3_ENDPOINT));
        params.put("bucketName", sysConfigService.getValue(Constant.SYSCONFIG_KEY_LOGS_STORAGE_S3_BUCKET));
        params.put("accessKey", sysConfigService.getValue(Constant.SYSCONFIG_KEY_LOGS_STORAGE_S3_ACCESS_KEY));
        params.put("secret", sysConfigService.getValue(Constant.SYSCONFIG_KEY_LOGS_STORAGE_S3_SECRET_ACCESS_KEY));
        params.put("insecure", sysConfigService.getValue(Constant.SYSCONFIG_KEY_LOGS_STORAGE_S3_INSECURE));

        Map map = new HashMap();
        String content = "";
        try {
            String lokiYmlTmpl = sysConfigService.getValue(Constant.SYSCONFIG_KEY_LOKI_YML_TMPL);
            Template template = TemplateUtil.stringToTemplate(lokiYmlTmpl, Constant.SYSCONFIG_KEY_LOKI_YML_TMPL);
            content = FreeMarkerTemplateUtils.processTemplateIntoString(template, params);
            map = JSONObject.parseObject(content, LinkedHashMap.class, Feature.OrderedField);
        } catch (IOException | TemplateException | RuntimeException e) {
            log.error("loki 模板生成失败，模板内容:" + JSONObject.toJSONString(content), e);
        }
        log.debug("loki.yml 生成结束");
        return map;
    }

    /**
     * 获取当前支持 alert 的 agent id , 如果获取失败 则获取 global 第一个 agent
     *
     * @param healthyPromServers
     * @return
     */
    public Integer getSupportAlertPromId(List<Promserver> healthyPromServers) {
        Object supportAlertPromId = redisTemplate.opsForValue().get(Constant.REDIS_KEY_SUPPORT_ALERT_PROMSERVER_ID);
        supportAlertPromId = ToolUtil.isEmpty(supportAlertPromId) ? "" : supportAlertPromId;

        // 当前支持 alert 的 agent ， 判断是否存在,不存在则重新 找一个 agent 下发配置
        if (StrUtil.isNotEmpty(supportAlertPromId.toString())) {
            Promserver promServerById = promService.getById(supportAlertPromId.toString());
            if (promServerById == null) {
                supportAlertPromId = "";
            }
        }

        // 第一个 健康的 gloabl cortex 支持 alert
        if (StrUtil.isEmpty(supportAlertPromId.toString())) {
            Promserver supportAlertAgent = healthyPromServers.stream().filter(promserver -> Constant.PromserverType.GLOBAL.getValue().equals(promserver.getType())).sorted(Comparator.comparing(Promserver::getId)).findFirst().orElse(null);
            supportAlertPromId = supportAlertAgent.getId();
            redisTemplate.opsForValue().set(Constant.REDIS_KEY_SUPPORT_ALERT_PROMSERVER_ID, supportAlertPromId.toString());
        }
        return Integer.valueOf(supportAlertPromId.toString());
    }

    /**
     * 组装 snmp.yml
     *
     * @return
     */
    public Map generateSnmpYml() {
        log.debug("snmp.yml 开始生成");
        List<MonitorModule> snmpTypeModule = monitorModuleService.list(new LambdaQueryWrapper<MonitorModule>().isNotNull(MonitorModule::getSnmpYml));
        Set<String> snmpYmls = snmpTypeModule.stream().filter(monitorModule -> {
            Map map = JSONObject.parseObject(monitorModule.getSnmpYml(), Map.class);
            if (Tool.MapUtil.isEmpty(map)) {
                return false;
            }
            return true;
        }).map(MonitorModule::getSnmpYml).collect(Collectors.toSet());
        Map resultMap = new LinkedHashMap();
        snmpYmls.stream().forEach(s -> resultMap.putAll(JSONObject.parseObject(s)));
        log.debug("snmp.yml 生成结束");
        return resultMap;
    }

    /**
     * Prometheus cmdline 模板生成
     *
     * @return
     */
    public Map<String, String> generatePrometheusCmdLine() {
        Map params = new HashMap(2);
        params.put("storageDays", sysConfigService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_RETENTION));

        String content = "";
        try {
            Template template = TemplateUtil.stringToTemplate(sysConfigService.getValue(Constant.SYSCONFIG_KEY_PROM_CMD_TMPL), Constant.SYSCONFIG_KEY_PROM_CMD_TMPL);
            content = FreeMarkerTemplateUtils.processTemplateIntoString(template, params);
        } catch (IOException | TemplateException | RuntimeException e) {
            log.error("cmdLine 模板生成失败" + JSONObject.toJSONString(content), e);
        }

        Map<String, String> cmdMap = AgentUtil.loadServiceConfigStr(content);
        return cmdMap;
    }

    /**
     * 生成 promtail.yml
     *
     * @param currentAgent
     * @param globalAgentList
     * @param logEndpointConfigInfoEntity
     * @return
     */
    public Map generatePromtailYml(Promserver currentAgent, List<Promserver> globalAgentList, MonitorEndpoint logEndpointConfigInfoEntity) {
        log.debug("promtail.yml 开始生成");
        Map<String, Object> params = new LinkedHashMap();

        List<MonitorEndpoint> endpointList = new ArrayList<>();
        String configStr = logEndpointConfigInfoEntity.getEndpointConfig().getConfig();
        List<Map> logConfigs = JSONObject.parseArray(configStr, Map.class);
        for (Map configMap : logConfigs) {
            MonitorEndpoint clone = ObjectUtil.cloneByStream(logEndpointConfigInfoEntity);
            MonitorEndpointConfig endpointConfig = clone.getEndpointConfig();
            endpointConfig.setConfig(JSONObject.toJSONString(configMap));
            endpointList.add(clone);
        }

        // 处理 endpoint 配置项 为了适应模板
        this.handleDataConfigForTmpl(currentAgent, endpointList, Collections.emptyList());

        params.put("endpointList", endpointList);

        List<Map> clientsMapList = new ArrayList<>();
        // 日志代理推送模式
        String logsProxyPushType = sysConfigService.getValue(Constant.SYSCONFIG_KEY_LOGS_PROXY_PUSH_TYPE);
        switch (StrUtil.emptyToDefault(logsProxyPushType, "1")) {
            case "1":  // 全部使用 agent proxy 方式
            case "2": {// 联邦开启模式下，promtail 先推送至 per-datacenter agent 代理接口，再 push global loki
                Map clientEntity = new HashMap(4);
                clientEntity.put("url", String.format("http://%s:%s%s%s", currentAgent.getHost(), currentAgent.getPort(), sysConfigService.getValue(Constant.CONFAGENT_PATH), "/loki/proxy/loki/api/v1/push"));
                clientEntity.put("token", currentAgent.getToken());
                clientsMapList.add(clientEntity);
                break;
            }
            case "3": {
                // promtail push global loki
                for (Promserver globalAgent : globalAgentList) {
                    Map clientEntity = new HashMap(4);
                    clientEntity.put("url", String.format("http://%s:%s%s", globalAgent.getHost(), lokiDefaultPort, "/loki/api/v1/push"));
                    clientEntity.put("token", globalAgent.getToken());
                    clientsMapList.add(clientEntity);
                }
                break;
            }
        }

        params.put("clientsMapList", clientsMapList);

        Map map = null;
        String content = "";
        try {
            Template template = TemplateUtil.stringToTemplate(sysConfigService.getValue(Constant.SYSCONFIG_KEY_PROMTAIL_YML_TMPL), Constant.SYSCONFIG_KEY_PROMTAIL_YML_TMPL);
            content = FreeMarkerTemplateUtils.processTemplateIntoString(template, params);
            map = JSONObject.parseObject(content, LinkedHashMap.class, Feature.OrderedField);
        } catch (IOException | TemplateException | RuntimeException e) {
            log.error("promtail.yml 模板生成失败, 模板内容: " + JSONObject.toJSONString(content), e);
        }
        log.debug("promtail.yml 生成结束");
        return map;
    }

    /**
     * 生成 per-datacenter prometheus.yml
     *
     * @param promserver
     * @param endpointList
     * @param assetList
     * @param globalPromServers
     * @return
     */
    public Map generatePerdatacenterYml(Promserver promserver, List<MonitorEndpoint> endpointList, List<AssetAsset> assetList, List<Promserver> globalPromServers) {
        log.debug("per-datacenter prometheus.yml 开始生成");
        Map<String, Object> params = new LinkedHashMap();
        params.put("scrape_interval", sysConfigService.getValue(Constant.SYSCONFIG_KEY_SCRAPEINTERVAL));
        params.put("scrape_timeout", sysConfigService.getValue(Constant.SYSCONFIG_KEY_SCRAPETIMEOUT));
        params.put("path_prefix", "");
        params.put("alert_url", "");

        params.put("isAlert", false);
        params.put("isGlobal", false);
        params.put("isLocalStorage", false);

        params.put("assetList", assetList);

        // 处理 endpoint 配置项 为了适应模板
        this.handleDataConfigForTmpl(promserver, endpointList, assetList);

        params.put("endpointList", endpointList);
        // remote_write url and token
        List<Map> remoteWriteUrlInfoMapList = new ArrayList<>();
        //  object storage 标识  1: 本地文件存储，2：S3对象存储
        String objStroageEnable = sysConfigService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_TYPE);
        boolean isLocalStorage = StrUtil.equals("1", objStroageEnable);

        String agentPath = sysConfigService.getValue(Constant.CONFAGENT_PATH);

        // 指标代理模式 metrics_proxy_push_type 1: 经过 agent 代理推送 cortex 2: 直接推送 cortex
        String metricsProxyPushType = sysConfigService.getValue(Constant.SYSCONFIG_KEY_METRICS_PROXY_PUSH_TYPE);
        switch (StrUtil.emptyToDefault(metricsProxyPushType, "1")) {
            case "1": {
                if (isLocalStorage) {
                    // 本地存储 and agent 代理，per-datacenter 配置 remote_write 所有 global agent
                    for (Promserver globalPromServer : globalPromServers) {
                        Map map = new HashMap();// http://127.0.0.1:${agentPort?c}${agentPath}/cortex/proxy/api/v1/push
                        map.put("url", String.format("http://%s:%s%s/cortex/proxy/api/v1/push", globalPromServer.getHost(), globalPromServer.getPort(), agentPath));
                        map.put("token", globalPromServer.getToken());
                        remoteWriteUrlInfoMapList.add(map);
                    }
                } else {
                    // s3 存储 and agent 代理，任选一个 global 即可
                    Map map = new HashMap();// http://127.0.0.1:${agentPort?c}${agentPath}/cortex/proxy/api/v1/push
                    map.put("url", String.format("http://%s:%s%s/cortex/proxy/api/v1/push", globalPromServers.get(0).getHost(), globalPromServers.get(0).getPort(), agentPath));
                    map.put("token", globalPromServers.get(0).getToken());
                    remoteWriteUrlInfoMapList.add(map);
                }
                break;
            }
            case "2": {
                if (isLocalStorage) {
                    // 本地存储 and cortex，per-datacenter 配置 remote_write 所有 global cortex
                    for (Promserver globalPromServer : globalPromServers) {
                        Map map = new HashMap();// http://127.0.0.1:${cortexDefaultPort?c}/api/v1/push
                        map.put("url", String.format("http://%s:%s/api/v1/push", globalPromServer.getHost(), cortexDefaultPort));
                        map.put("token", globalPromServer.getToken());
                        remoteWriteUrlInfoMapList.add(map);
                    }
                } else {
                    // s3 存储 and cortex，任选一个 global cortex 即可
                    Map map = new HashMap();// http://127.0.0.1:${agentPort?c}${agentPath}/cortex/proxy/api/v1/push
                    map.put("url", String.format("http://%s:%s/api/v1/push", globalPromServers.get(0).getHost(), cortexDefaultPort));
                    map.put("token", globalPromServers.get(0).getToken());
                    remoteWriteUrlInfoMapList.add(map);
                }
                break;
            }
        }
        params.put("remoteWriteUrlInfos", remoteWriteUrlInfoMapList);

        Map map = null;
        String content = "";
        try {
            Template template = TemplateUtil.stringToTemplate(sysConfigService.getValue(Constant.SYSCONFIG_KEY_PROM_YML_TMPL), Constant.SYSCONFIG_KEY_PROM_YML_TMPL);
            content = FreeMarkerTemplateUtils.processTemplateIntoString(template, params);
            map = JSONObject.parseObject(content, LinkedHashMap.class, Feature.OrderedField);
        } catch (IOException | TemplateException | RuntimeException e) {
            log.error("per-datacenter prometheus.yml 模板生成失败, 模板内容: " + JSONObject.toJSONString(content), e);
        }
        log.debug("per-datacenter prometheus.yml 生成结束");
        return map;
    }

    /**
     * 生成 global prometheus.yml 配置
     *
     * @param currentAgent
     * @param globalPromServers
     * @param perDataCenterPromServers
     * @param assetList
     * @param endpointList
     * @param isAlert
     * @param isSupportSysteMetrics
     * @return
     */
    public Map generateGlobalYml(Promserver currentAgent, List<Promserver> globalPromServers, List<Promserver> perDataCenterPromServers, List<AssetAsset> assetList, List<MonitorEndpoint> endpointList, Boolean isAlert, Boolean isSupportSysteMetrics) {
        log.debug("global prometheus.yml 开始生成");
        // sys_config 配置表信息
        List<SysConfigEntity> configEntityList = sysConfigService.list();
        Map<String, String> configMap = configEntityList.stream().collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));

        boolean fedeEnabled = StrUtil.equals("1", configMap.get(Constant.SYSCONFIG_KEY_PROM_FEDER_ENABLED));
        // 关闭联邦 不启用 per-datecenter
        if (!fedeEnabled) perDataCenterPromServers = new ArrayList<>();

        List<Promserver> allPromserverList = new ArrayList<>();
        allPromserverList.addAll(globalPromServers);
        allPromserverList.addAll(perDataCenterPromServers);

        // agent 代理接口前缀
        String proxyPath = configMap.get(Constant.CONFAGENT_PATH) + "/prometheus/proxy";
        // federate 废弃
        List<Map> federateMapList = new ArrayList<>();
        // blackbox job  1: global   2: per-datacenter
        String assetPingFrom = configMap.get(Constant.SYSCONFIG_KEY_ASSET_PING_FROM);
        for (Promserver server : allPromserverList) {
            // 联邦开启
            if (fedeEnabled) {
                // global 做 ping 检测，联邦排除自己
                if ("1".equals(assetPingFrom)) {
                    if (server.getId().equals(currentAgent.getId())) continue;
                } else {
                    // per-datacenter 做 ping 检测，global 角色排除掉，不需要加入到 federate 中
                    if (Constant.PromserverType.GLOBAL.getValue().equals(server.getType())) continue;
                }
            } else {
                // 联邦关闭
                if (server.getId().equals(currentAgent.getId())) continue;
            }
            Map federateMap = new LinkedHashMap();
            federateMap.put("job_name", "federate_" + server.getId());
            federateMap.put("metrics_path", proxyPath + "/federate");
            federateMap.put("isFederate", false);
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
            federateMap.put("excludeAgentId", currentAgent.getId());
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
            Dc dc = dcService.getById(entity.getDcId());
            labels.put("datacenter", ToolUtil.string2Json(dc.getName()));
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

        //  object storage 标识  1: 本地文件存储，2：S3对象存储
        String objStroageEnable = configMap.get(Constant.SYSCONFIG_KEY_METRICS_STORAGE_TYPE);
        boolean isLocalStorage = StrUtil.equals("1", objStroageEnable);

        List<Map> jobList = new ArrayList<>();

        /**
         * 指标代理模式 metrics_proxy_push_type
         * 1: 经过 agent 代理推送 cortex
         * 2: 直接推送 cortex
         */
        String metricsProxyPushType = sysConfigService.getValue(Constant.SYSCONFIG_KEY_METRICS_PROXY_PUSH_TYPE);
        boolean isProxyPush = StrUtil.equalsIgnoreCase("1", StrUtil.emptyToDefault(metricsProxyPushType, "1"));

        // remote_write url and token
        List<Map> remoteWriteUrlInfoMapList = new ArrayList<>();
        if (fedeEnabled) {
            // 联邦开启
            // 本地存储  global 接收 per-datacenter 推过来得数据，每一个 global 支持 nzweb 系统指标
            // s3 对象存储 global 接收 per-datacenter 推过来得数据,第一个支持 nzweb 系统模板即可
            Map map = new HashMap();
            map.put("url", String.format("http://%s:%s%s", currentAgent.getHost(), isProxyPush ? currentAgent.getPort() : cortexDefaultPort, isProxyPush ? configMap.get(Constant.CONFAGENT_PATH) + "/cortex/proxy/api/v1/push" : "/api/v1/push"));
            map.put("token", currentAgent.getToken());
            remoteWriteUrlInfoMapList.add(map);
            if (isLocalStorage) {
                jobList.add(nezhaMetricsMap);
                jobList.add(promMetricsMap);
            } else {
                if (isSupportSysteMetrics) {
                    jobList.add(nezhaMetricsMap);
                    jobList.add(promMetricsMap);
                }
            }
        } else {
            // 联邦关闭
            if (isLocalStorage) {
                // 本地配置所有 global remote_write info
                for (Promserver promserver : globalPromServers) {
                    Map map = new HashMap();
                    map.put("url", String.format("http://%s:%s%s", promserver.getHost(), isProxyPush ? promserver.getPort() : cortexDefaultPort, isProxyPush ? configMap.get(Constant.CONFAGENT_PATH) + "/cortex/proxy/api/v1/push" : "/api/v1/push"));
                    map.put("token", currentAgent.getToken());
                    remoteWriteUrlInfoMapList.add(map);
                }
            } else {
                // s3 配置 配置自己即可
                Map map = new HashMap();
                map.put("url", String.format("http://%s:%s%s", currentAgent.getHost(), isProxyPush ? currentAgent.getPort() : cortexDefaultPort, isProxyPush ? configMap.get(Constant.CONFAGENT_PATH) + "/cortex/proxy/api/v1/push" : "/api/v1/push"));
                map.put("token", currentAgent.getToken());
                remoteWriteUrlInfoMapList.add(map);
            }
            // 不论 本地还是对象存储 ，有一个支持 nzweb 系统指标即可
            if (isSupportSysteMetrics) {
                jobList.add(nezhaMetricsMap);
                jobList.add(promMetricsMap);
            }
        }

        params.put("remoteWriteUrlInfos", remoteWriteUrlInfoMapList);

        // 处理 endpoint 配置项 为了适应模板
        this.handleDataConfigForTmpl(currentAgent, endpointList, assetList);

        params.put("assetList", assetList);
        params.put("endpointList", endpointList);

        // federate 废弃
        /*for (Map map : federateMapList) {
            jobList.add(map);
        }*/

        params.put("jobList", jobList);

        Map map = null;
        String content = "";
        try {
            Template template = TemplateUtil.stringToTemplate(configMap.get(Constant.SYSCONFIG_KEY_PROM_YML_TMPL), Constant.SYSCONFIG_KEY_PROM_YML_TMPL);
            content = FreeMarkerTemplateUtils.processTemplateIntoString(template, params);
            map = JSONObject.parseObject(content, LinkedHashMap.class, Feature.OrderedField);
        } catch (IOException | TemplateException | RuntimeException e) {
            log.error("global prometheus.yml 模板生成失败" + JSONObject.toJSONString(content), e);
        }
        log.debug("global prometheus.yml 生成结束");
        return map;
    }

    /**
     * 处理生成模板中 数据格式问题
     *
     * @param promserver
     * @param endpointList
     * @param assetList
     */
    private void handleDataConfigForTmpl(Promserver promserver, List<MonitorEndpoint> endpointList, List<AssetAsset> assetList) {
        for (AssetAsset asset : assetList) {
            // dc name 处理
            Dc dc = asset.getDc();
            dc.setName(ToolUtil.string2Json(dc.getName()));
            asset.setDc(dc);

            // nz_agent_id，对应 promserver id
            asset.setPromserver(promserver);
        }
        for (MonitorEndpoint endpoint : endpointList) {
            Map endpointConfigFieldMap = JSONObject.parseObject(endpoint.getEndpointConfig().getConfig(), Map.class);
            endpoint.setConfig(endpointConfigFieldMap);

            List endpointRelabelConfig = ObjectUtil.defaultIfNull((List<Map>) endpointConfigFieldMap.get("relabel_config"), Collections.emptyList());
            if (CollectionUtil.isEmpty(endpointRelabelConfig)) {
                List<Map> moduleConfigs = JSONObject.parseArray(endpoint.getModule().getConfigs(), Map.class);
                Map moduleMetricsConfigMap = moduleConfigs.stream().filter(map -> {
                    String configType = (String) map.get("type");
                    return StrUtil.equals(Constant.MonitorConfType.METRICS.getValue(), configType);
                }).findFirst().orElse(Collections.emptyMap());

                Map moduleConfigMap = (Map) moduleMetricsConfigMap.get("config");
                moduleConfigMap = ObjectUtil.defaultIfNull(moduleConfigMap, Collections.emptyMap());
                endpoint.setRelabelConfigListJsonString(JSONObject.toJSONString(ObjectUtil.defaultIfNull((List<Map>) moduleConfigMap.get("relabel_config"), Collections.emptyList())));
            } else {
                endpoint.setRelabelConfigListJsonString(JSONObject.toJSONString(endpointRelabelConfig));
            }

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
    }


    /**
     * 获取 global lokipushurl config map
     *
     * @param promserver
     * @param globalPromServers
     * @return
     */
    public Map<String, Object> getGlobalLokiPushConfig(Promserver promserver, List<Promserver> globalPromServers) {
        List<Map> lokiPushUrls = new ArrayList<>();
        // 对象存储开启  global 都需要下发 lokiPushUrls config 指向自己
        // 对象存储关闭  global 都需要下发 lokiPushUrls config 指向自己 和 remote 其他 global
        String logsStorageType = sysConfigService.getValue(Constant.SYSCONFIG_KEY_LOGS_STORAGE_TYPE);
        String logsProxyPushType = sysConfigService.getValue(Constant.SYSCONFIG_KEY_LOGS_PROXY_PUSH_TYPE);
        switch (StrUtil.emptyToDefault(logsProxyPushType, "1")) {
            // 全部使用 agent proxy 方式
            case "1": {
                if (StrUtil.equals("1", logsStorageType)) {
                    for (Promserver globalPromServer : globalPromServers) {
                        Map map = new HashMap(4);
                        // 指向自己 agent
                        if (globalPromServer.getId().equals(promserver.getId())) {
                            map.put("mode", "local");
                        } else {
                            // 指向其他 agent
                            map.put("token", globalPromServer.getToken());
                            map.put("target", String.format("%s:%s%s/loki/proxy/loki/api/v1/push", globalPromServer.getHost(), globalPromServer.getPort(), sysConfigService.getValue(Constant.CONFAGENT_PATH)));
                            map.put("mode", "remote");
                        }
                        lokiPushUrls.add(map);
                    }
                } else {
                    // s3 存储 global 推向自己
                    Map tempMap = new HashMap(4);
                    tempMap.put("token", promserver.getToken());
                    tempMap.put("target", String.format("%s:%s%s/loki/proxy/loki/api/v1/push", promserver.getHost(), promserver.getPort(), sysConfigService.getValue(Constant.CONFAGENT_PATH)));
                    tempMap.put("mode", "local");
                    lokiPushUrls.add(tempMap);
                }
                break;
            }
            case "2":
            case "3":
                // 啥也不做 per-datecenter / promtail 推送到 loki
                break;
        }
        Map<String, Object> lokiPushUrlConfigBody = new LinkedHashMap(4);
        lokiPushUrlConfigBody.put("lokiPushUrls", lokiPushUrls);
        return lokiPushUrlConfigBody;
    }

    /**
     * 获取 per-dataceneter lokipushurl config map
     *
     * @param globalPromServers
     * @return
     */
    public Map<String, Object> getPerdatacenterLokiPushConfig(List<Promserver> globalPromServers) {
        List<Map> lokiPushUrls = new ArrayList<>();
        // 判断是否开启 对象存储
        String logsStorageType = sysConfigService.getValue(Constant.SYSCONFIG_KEY_LOGS_STORAGE_TYPE);
        // 日志代理推送模式
        String logsProxyPushType = sysConfigService.getValue(Constant.SYSCONFIG_KEY_LOGS_PROXY_PUSH_TYPE);
        if (StrUtil.equals("2", logsStorageType)) {
            // 开启 /  per-datacenter 选择任意一个 global 推送即可
            Promserver firstGlobalAgent = globalPromServers.stream().findFirst().get();
            switch (StrUtil.emptyToDefault(logsProxyPushType, "1")) {
                case "1": {
                    // 全部使用 agent proxy 方式 agent 推送 global proxy
                    Map tempMap = new HashMap(4);
                    tempMap.put("token", firstGlobalAgent.getToken());
                    tempMap.put("target", String.format("%s:%s%s/loki/proxy/loki/api/v1/push", firstGlobalAgent.getHost(), firstGlobalAgent.getPort(), sysConfigService.getValue(Constant.CONFAGENT_PATH)));
                    tempMap.put("mode", "remote");
                    lokiPushUrls.add(tempMap);
                    break;
                }
                case "2": {
                    // promtail 先推送至 per-datacenter agent 代理接口，agent 推送 global loki
                    Map tempMap = new HashMap(4);
                    tempMap.put("token", firstGlobalAgent.getToken());
                    tempMap.put("target", String.format("http://%s:%s%s", firstGlobalAgent.getHost(), lokiDefaultPort, "/loki/api/v1/push"));
                    tempMap.put("mode", "remote");
                    lokiPushUrls.add(tempMap);
                    break;
                }
                case "3":  // 啥也不做 promtail 直接推送到 global loki
                    break;
            }
        } else {
            // 关闭 / per-datacenter 给所有 global  推送
            switch (StrUtil.emptyToDefault(logsProxyPushType, "1")) {
                case "1": {
                    // 全部使用 agent proxy 方式 agent 推送 global proxy
                    for (Promserver globalPromServer : globalPromServers) {
                        Map tempMap = new HashMap(4);
                        tempMap.put("token", globalPromServer.getToken());
                        tempMap.put("target", String.format("%s:%s%s/loki/proxy/loki/api/v1/push", globalPromServer.getHost(), globalPromServer.getPort(), sysConfigService.getValue(Constant.CONFAGENT_PATH)));
                        tempMap.put("mode", "remote");
                        lokiPushUrls.add(tempMap);
                    }
                    break;
                }
                case "2": {
                    // promtail 先推送至 per-datacenter agent 代理接口，agent 推送 global loki
                    for (Promserver globalPromServer : globalPromServers) {
                        Map tempMap = new HashMap(4);
                        tempMap.put("token", globalPromServer.getToken());
                        tempMap.put("target", String.format("http://%s:%s%s", globalPromServer.getHost(), lokiDefaultPort, "/loki/api/v1/push"));
                        tempMap.put("mode", "remote");
                        lokiPushUrls.add(tempMap);
                    }
                    break;
                }
                // 啥也不做 promtail 直接推送到 global loki
                case "3":
                    break;
            }
        }
        Map<String, Object> lokiPushUrlConfigBody = new LinkedHashMap(4);
        lokiPushUrlConfigBody.put("lokiPushUrls", lokiPushUrls);
        return lokiPushUrlConfigBody;
    }

    /**
     * 一致性 hash 分配 endpoint
     *
     * @param promserverList
     * @param endpointList
     * @return
     */
    public Map<Integer, Integer> consistentHashingAssignEndpoint(List<Promserver> promserverList, List<MonitorEndpoint> endpointList) {
        // 1. 真实节点
        Map<String, Integer> realNodeList = promserverList.stream().collect(Collectors.toMap(promserver -> promserver.getHost() + ":" + promserver.getPort(), Promserver::getId));
        // 2. 通过 真实节点 加上虚拟节点获取 hash 环
        SortedMap<Long, String> nodeMap = this.getHashRingByRealList(realNodeList);

        if (log.isDebugEnabled()) log.debug("endpoint hash 环节点信息: " + JSONObject.toJSONString(nodeMap));

        // endpoint id 和 promerver id 对应 map
        Map<Integer, Integer> endpointIdAndPromIdMap = new LinkedHashMap(16);

        // endpoint hash 处理
        Map<Integer, String> endpointIdAndHashMap = endpointList.stream().collect(Collectors.toMap(MonitorEndpoint::getId, endpoint -> endpoint.getEndpointConfig().getHash()));
        for (Map.Entry<Integer, String> entry : endpointIdAndHashMap.entrySet()) {
            Integer endpointId = entry.getKey();
            String endpointConfigHash = entry.getValue();
            // 顺时针最近的节点
            SortedMap<Long, String> tailMap = nodeMap.tailMap(Long.valueOf(endpointConfigHash));
            // 可能为空, 取环中的第一个节点
            if (tailMap.isEmpty()) {
                Long nodeIndex = nodeMap.firstKey();
                endpointIdAndPromIdMap.put(endpointId, realNodeList.get(nodeMap.get(nodeIndex)));
                if (log.isDebugEnabled())
                    log.debug("endpoint: " + endpointId + "\t" + "node: " + nodeMap.get(nodeIndex) + "\t" + "promserver id: " + realNodeList.get(nodeMap.get(nodeIndex)));
            } else {
                Long nodeIndex = tailMap.firstKey();
                endpointIdAndPromIdMap.put(endpointId, realNodeList.get(nodeMap.get(nodeIndex)));
                if (log.isDebugEnabled())
                    log.debug("endpoint: " + endpointId + "\t" + "node: " + nodeMap.get(nodeIndex) + "\t" + "promserver id: " + realNodeList.get(nodeMap.get(nodeIndex)));
            }
        }
        return endpointIdAndPromIdMap;
    }

    /**
     * 一致性 hash 分配 asset
     *
     * @param promserverList
     * @param assetList
     * @return
     */
    public Map<Integer, Integer> consistentHashingAssignAsset(List<Promserver> promserverList, List<AssetAsset> assetList) {
        Map<String, Integer> realNodeList = promserverList.stream().collect(Collectors.toMap(promserver -> promserver.getHost() + ":" + promserver.getPort(), Promserver::getId));
        SortedMap<Long, String> nodeMap = this.getHashRingByRealList(realNodeList);

        if (log.isDebugEnabled()) log.debug("asset hash 环节点信息:" + JSONObject.toJSONString(nodeMap));

        Map<Integer, Integer> assetIdAndPromIdMap = new LinkedHashMap(16);

        Map<Integer, String> assetIdAndHashMap = assetList.stream().collect(Collectors.toMap(AssetAsset::getId, asset -> String.valueOf(Math.abs(MurmurHash.hash64(asset.getManageIp() + ":" + asset.getDcId())))));
        for (Map.Entry<Integer, String> entry : assetIdAndHashMap.entrySet()) {
            Integer assetId = entry.getKey();
            String assetHash = entry.getValue();
            SortedMap<Long, String> tailMap = nodeMap.tailMap(Long.valueOf(assetHash));
            if (tailMap.isEmpty()) {
                Long nodeIndex = nodeMap.firstKey();
                assetIdAndPromIdMap.put(assetId, realNodeList.get(nodeMap.get(nodeIndex)));
                if (log.isDebugEnabled())
                    log.debug("asset: " + assetId + "\t" + "node: " + nodeMap.get(nodeIndex) + "\t" + "promserver id: " + realNodeList.get(nodeMap.get(nodeIndex)));
            } else {
                Long nodeIndex = tailMap.firstKey();
                assetIdAndPromIdMap.put(assetId, realNodeList.get(nodeMap.get(nodeIndex)));
                if (log.isDebugEnabled())
                    log.debug("asset: " + assetId + "\t" + "node: " + nodeMap.get(nodeIndex) + "\t" + "promserver id: " + realNodeList.get(nodeMap.get(nodeIndex)));
            }
        }
        return assetIdAndPromIdMap;
    }


    /**
     * 通过 真实节点获取 hash 环， 真实节点 + 虚拟节点
     *
     * @param realNodeList
     * @return
     */
    private SortedMap<Long, String> getHashRingByRealList(Map<String, Integer> realNodeList) {
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
     * 更新 redis 中 endpointid 和　promserver id对应关系 endpoint_prom
     *
     * @param endpointIdAndPromIdMap
     */
    public void updateEndpointPromInRedis(Map<Integer, Integer> endpointIdAndPromIdMap) {
        // 先删后增
        Map<String, String> tempStringMap = new LinkedHashMap<>();
        endpointIdAndPromIdMap.forEach((k, v) -> tempStringMap.put(k.toString(), v.toString()));

        redisTemplate.delete(Constant.REDIS_KEY_ENDPOINT_PROM);
        redisTemplate.opsForHash().putAll(Constant.REDIS_KEY_ENDPOINT_PROM, tempStringMap);
    }

    /**
     * 调用 agent、healthy 更新 prom_server 数据状态
     *
     * @return healthy prom_server
     */
    public List<Promserver> updateAgentStatus() {
        List<Promserver> allPromServers = promService.list();
        allPromServers.stream().forEach(promserver -> promserver.setStatus(null));
        String agentPrefixPath = sysConfigService.getValue(Constant.CONFAGENT_PATH);

        List<Callable<Promserver>> tasks = new ArrayList<>();
        allPromServers.stream().forEach(promServer -> {
            Callable<Promserver> task = new Callable<Promserver>() {
                @Override
                public Promserver call() throws Exception {
                    UrlBuilder urlBuilder = UrlBuilder.create();
                    urlBuilder.setScheme("http").setHost(promServer.getHost()).setPort(promServer.getPort()).appendPath(agentPrefixPath).appendPath("healthy");
                    String result = null;
                    try {
                        result = Tool.HttpUtil.createGet(urlBuilder.build()).timeout(connectTimeout).auth(promServer.getToken()).execute().body();
                    } catch (RuntimeException e) {
                        log.error(String.format("请求 %s 接口失败", urlBuilder.toString()), e);
                    }
                    log.debug(String.format("请求接口是%s，相应内容是:%s", urlBuilder.toString(), result));
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
                                String cortexState = (String) data.get("cortex");
                                String lokiState = (String) data.get("loki");
                                String authState = (String) data.get("auth");
                                String unavailable = "UNAVAILABLE", up = "UP", down = "DOWN";
                                if (down.equals(prometheusState) || down.equals(confagentState) || down.equals(snmpExporterState) || down.equals(blackboxExporterState) || down.equals(cortexState) || down.equals(lokiState)) {
                                    // 异常
                                    promServer.setStatus(0);
                                } else {
                                    if ("FALSE".equals(authState)) {
                                        // 认证失败
                                        promServer.setStatus(-1);
                                    } else {
                                        // 状态不为 down ，继续判断 可用状态
                                        if (unavailable.equals(cortexState)) {
                                            Boolean cortexUpdateExist = redisTemplate.hasKey(Constant.REDIS_KEY_CORTEX_UPDATE);
                                            // 返回为 UNAVAILABLE 但不存在更新标识，认为其不可用
                                            if (!cortexUpdateExist) promServer.setStatus(0);
                                        }
                                        if (unavailable.equals(lokiState)) {
                                            Boolean lokiUpdateExist = redisTemplate.hasKey(Constant.REDIS_KEY_LOKI_UPDATE);
                                            // 返回为 UNAVAILABLE 但不存在更新标识，认为其不可用
                                            if (!lokiUpdateExist) promServer.setStatus(0);
                                        }
                                        // 服务可用
                                        if (promServer.getStatus() == null) promServer.setStatus(1);
                                    }
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
     *
     * @return
     */
    private static String getPromStatusInfoErrorMsg() {
        Map errorData = new HashMap(4);
        errorData.put("prometheus", "DOWN");
        errorData.put("confagent", "DOWN");
        errorData.put("snmp_exporter", "DOWN");
        errorData.put("loki", "DOWN");
        errorData.put("cortex", "DOWN");
        return JSONObject.toJSONString(errorData);
    }

    /**
     * 请求 agent 状态码 ! 200 ，更新 agent 状态 造成 event_log 变动，下周期继续下发配置
     *
     * @param requestUrl
     * @param result
     * @param promserver
     */
    public void afterRequestApi(String requestUrl, String result, Promserver promserver) {
        try {
            R parseObject = JSONObject.parseObject(result, R.class);
            if (!RCode.SUCCESS.getCode().equals((Integer) parseObject.get("code"))) {
                log.error(String.format("请求 Agent 接口异常，接口地址: %s 错误信息: %s, Agent: %s", requestUrl, result, JSONObject.toJSONString(promserver)));
                this.updatePromServerStatus(promserver, 0);
            }
        } catch (Exception e) {
            log.error(String.format("请求 Agent 接口异常，接口地址: %s 错误信息: %s, Agent: %s", requestUrl, result, JSONObject.toJSONString(promserver)), e);
        }
    }

    /**
     * 修改 promservers 状态
     *
     * @param promserver
     * @param status
     */
    public void updatePromServerStatus(Promserver promserver, Integer status) {
        promserver.setStatus(status);
        promserver.setStatusInfo(getPromStatusInfoErrorMsg());
        promserver.setCheckTime(new Date());
        promService.updateById(promserver);
    }
}
