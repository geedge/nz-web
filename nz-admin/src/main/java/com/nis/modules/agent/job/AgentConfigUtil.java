package com.nis.modules.agent.job;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.hash.MurmurHash;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.*;
import com.nis.modules.agent.dao.MonitorEventLogDao;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.entity.MonitorEventLog;
import com.nis.modules.agent.service.AgentService;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.alert.service.AlertSeverityService;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.entity.MonitorEndpointConfig;
import com.nis.modules.endpoint.entity.MonitorEndpointState;
import com.nis.modules.endpoint.service.MonitorEndpointStateService;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.module.service.MonitorModuleService;
import com.nis.modules.project.entity.MonitorProject;
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
import java.util.stream.Collectors;
@SuppressWarnings("all")
@Component
public class AgentConfigUtil {

	private static final Log log = Log.get();

	@Value("${nezha.lokiDefaultPort:13100}")
	private Integer lokiDefaultPort;

	@Value("${nezha.cortexDefaultPort:10093}")
	private Integer cortexDefaultPort;

	@Value("${nezha.prometheusDefaultPort:10091}")
	private Integer prometheusDefaultPort;

	@Value("${nezha.virtualAgentNum}")
	private Integer virtualAgentNum;

	@Autowired
	private DcService dcService;

	@Autowired
	private RedisTemplate redisTemplate;

	@Autowired
	private AlertRuleService ruleService;

	@Autowired
	private AgentService agentService;

	@Autowired
	private MonitorEventLogDao confEventDao;

	@Autowired
	private SysConfigService sysConfigService;

	@Autowired
	private AlertSeverityService alertSeverityService;

	@Autowired
	private MonitorModuleService monitorModuleService;

	@Autowired
	private MonitorEndpointStateService endpointStateService;

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
				eventList = eventList.stream()
						.filter(monitorConfEvent -> Arrays.asList(filterKeys).contains(monitorConfEvent.getEvent()))
						.collect(Collectors.toList());
			}
			Map<String, String> map = eventList.stream().collect(Collectors.toMap(MonitorEventLog::getEvent,
					monitorConfEvent -> String.valueOf(monitorConfEvent.getCounter())));
			redisTemplate.opsForHash().putAll(Constant.REDIS_KEY_CONF_EVENT, map);
			return flag;
		}

		HashOperations opsForHash = redisTemplate.opsForHash();
		Map<String, String> confEvent = opsForHash.entries(Constant.REDIS_KEY_CONF_EVENT);

		List<MonitorEventLog> eventList = confEventDao.selectList(null);
		if (ToolUtil.isNotEmpty(filterKeys)) {
			eventList = eventList.stream()
					.filter(monitorConfEvent -> Arrays.asList(filterKeys).contains(monitorConfEvent.getEvent()))
					.collect(Collectors.toList());
		}
		for (MonitorEventLog data : eventList) {
			if (ToolUtil.isEmpty(confEvent.get(data.getEvent()))
					|| !data.getCounter().equals(Integer.valueOf(confEvent.get(data.getEvent())))) {
				flag = true;
			}
		}
		// 更新 redis 中 conf_event 数据
		Map<String, String> map = eventList.stream().collect(Collectors.toMap(MonitorEventLog::getEvent,
				monitorConfEvent -> String.valueOf(monitorConfEvent.getCounter())));
		opsForHash.delete(Constant.REDIS_KEY_CONF_EVENT, map.keySet().toArray());
		opsForHash.putAll(Constant.REDIS_KEY_CONF_EVENT, map);
		return flag;
	}

	/**
	 * 对比 promtail 版本号是否发生变化
	 *
	 * @return
	 */
	public boolean comparePromtailVersionChanges() {
		MonitorEventLog monitorEventLog = confEventDao
				.selectOne(new LambdaQueryWrapper<MonitorEventLog>().eq(MonitorEventLog::getEvent, "promtail"));
		List<MonitorEndpointState> tStateList = endpointStateService.list(new LambdaQueryWrapper<MonitorEndpointState>()
				.select(MonitorEndpointState::getEndpointId)
				.eq(MonitorEndpointState::getType, Constant.MonitorConfType.LOGS.getValue())
				.eq(MonitorEndpointState::getState, 1).ne(MonitorEndpointState::getVer, monitorEventLog.getCounter())
				.last("limit 1"));
		return Tool.CollUtil.isNotEmpty(tStateList);
	}

	/**
	 * 生成 rule.yml
	 *
	 * @return
	 */
//	public Map generateRuleYml() {
//		log.debug("rule.yml 开始生成");
//		// sys_config 配置表信息
//		List<SysConfigEntity> configEntityList = sysConfigService.list();
//		Map<String, String> configMap = configEntityList.stream()
//				.collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));
//
//		List<AlertRuleEntity> alertRuleEntities = ruleService.list(new LambdaQueryWrapper<AlertRuleEntity>()
//				.ne(AlertRuleEntity::getBuildIn, 1).eq(AlertRuleEntity::getState, 1));
//		List<AlertSeverityConf> severityConfs = alertSeverityService.list();
//		Map<Integer, String> alertSeverityIdAndNameMap = severityConfs.stream()
//				.collect(Collectors.toMap(AlertSeverityConf::getId, AlertSeverityConf::getName));
//
//		List<Map> ruleList = new ArrayList<>();
//		for (AlertRuleEntity entity : alertRuleEntities) {
//			Map map = new LinkedHashMap(16);
//			map.put("id", entity.getId());
//			map.put("expr", ToolUtil.string2Json(entity.getExpr()));
//			map.put("operator", entity.getOperator());
//			// 以字符串方式替换 double 类型，保证不丢失精度
//			map.put("threshold", entity.getThreshold() + "");
//			map.put("last", entity.getLast());
//
//			String severityName = alertSeverityIdAndNameMap.get(entity.getSeverityId());
//			map.put("severity_name", ToolUtil.string2Json(severityName));
//			map.put("severity_id", entity.getSeverityId());
//			map.put("summary", ToolUtil.string2Json(entity.getSummary()));
//			map.put("description", ToolUtil.string2Json(entity.getDescription()));
//			ruleList.add(map);
//		}
//
//		Map params = new LinkedHashMap(4);
//		// alert 方式实现 endpoint state -- 该方案不再使用，兼容未删除
//		String endpointAlertPush = configMap.get(Constant.ENDPOINT_ALERT_PUSH);
//		if (StrUtil.equals(endpointAlertPush, "1")) {
//			params.put("endpointAlert", true);
//		} else {
//			params.put("endpointAlert", false);
//		}
//		params.put("ruleList", ruleList);
//
//		Map map = new HashMap();
//		String content = "";
//		try {
//			Template template = TemplateUtil.stringToTemplate(configMap.get(Constant.SYSCONFIG_KEY_PROM_RULE_TMPL),
//					Constant.SYSCONFIG_KEY_PROM_RULE_TMPL);
//			content = FreeMarkerTemplateUtils.processTemplateIntoString(template, params);
//			map = JSONObject.parseObject(content, LinkedHashMap.class, Feature.OrderedField);
//		} catch (IOException | TemplateException | RuntimeException e) {
//			log.error("rule 模板生成失败，模板内容:" + JSONObject.toJSONString(content), e);
//		}
//		log.debug("rule.yml 生成结束");
//		return map;
//	}

	/**
	 * 生成 cortex.yml
	 *
	 * @param agent
	 * @param globalAgentList
	 * @param isAlert
	 * @return
	 */
	public Map generateCortexYml(Agent agent, List<Agent> globalAgentList, Boolean isAlert) {
		log.debug("cortex.yml 开始生成");
		Map params = new LinkedHashMap(8);
		
		params.put("ip", agent.getHost());
		params.put("localAgent", agent);
		params.put("objectStroageUrl", sysConfigService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_ENDPOINT));
		params.put("bucketName", sysConfigService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_BUCKET));
		params.put("accessKey", sysConfigService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_ACCESS_KEY));
		params.put("secret", sysConfigService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_SECRET_ACCESS_KEY));
		params.put("insecure", sysConfigService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_INSECURE));
		// object storage 标识 1: 本地文件存储，2：S3对象存储
		String objStroageEnable = sysConfigService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_TYPE);
		boolean isLocalStorage = StrUtil.equals("1", objStroageEnable);
		params.put("isLocalStorage", isLocalStorage);

		List<String> joinMemberIpList = new ArrayList<>();
		joinMemberIpList = globalAgentList.stream().filter(entity -> !entity.getId().equals(agent.getId()))
				.map(Agent::getHost).collect(Collectors.toList());
		params.put("joinMemberList", joinMemberIpList);
		params.put("isMemberlistStore", CollectionUtil.isNotEmpty(joinMemberIpList));

		String interfaceNames = sysConfigService.getValue("interface_name");
		if(StrUtil.isBlank(interfaceNames)) {
			interfaceNames = "eth0";
		}
		List<String> interfaceNameList = Arrays.asList(interfaceNames.split(","));
		params.put("interfaceNameList", interfaceNameList);
		params.put("instanceInterfaceNames", CollectionUtil.isNotEmpty(interfaceNameList));
		
		List<SysConfigEntity> sysConfigList = sysConfigService.list();
		Map<String, String> sysConfiMap = sysConfigList.stream()
				.collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));
		params.put("systemConfig", sysConfiMap);

		params.put("isAlert", isAlert);

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
	public Map generateLokiYml(Agent agent,List<Agent> globalAgentList) {
		log.debug("loki.yml 开始生成");
		Map params = new LinkedHashMap(16);
		params.put("localAgent", agent);
		
		String interfaceNames = sysConfigService.getValue("interface_name");
		if(StrUtil.isBlank(interfaceNames)) {
			interfaceNames = "eth0";
		}
		List<String> interfaceNameList = Arrays.asList(interfaceNames.split(","));
		params.put("interfaceNameList", interfaceNameList);
		
		List<String> joinMemberIpList = new ArrayList<>();
		joinMemberIpList = globalAgentList.stream().filter(entity -> !entity.getId().equals(agent.getId()))
				.map(Agent::getHost).collect(Collectors.toList());
		params.put("joinMemberList", joinMemberIpList);
		params.put("isMemberlistStore", CollectionUtil.isNotEmpty(joinMemberIpList));
		
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

		List<SysConfigEntity> sysConfigList = sysConfigService.list();
		Map<String, String> sysConfiMap = sysConfigList.stream()
				.collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));
		params.put("systemConfig", sysConfiMap);

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
	 * @param healthyAgents
	 * @return
	 */
	public Integer getSupportAlertPromId(List<Agent> healthyAgents) {
		Object supportAlertPromId = redisTemplate.opsForValue().get(Constant.REDIS_KEY_SUPPORT_ALERT_AGENT_ID);
		supportAlertPromId = ToolUtil.isEmpty(supportAlertPromId) ? "" : supportAlertPromId;

		// 当前支持 alert 的 agent ， 判断是否存在,不存在则重新 找一个 agent 下发配置
		if (StrUtil.isNotEmpty(supportAlertPromId.toString())) {
			Agent agentById = agentService.getById(supportAlertPromId.toString());
			if (agentById == null || agentById.getStatus() != 1) {
				supportAlertPromId = "";
			}
		}

		// 第一个 健康的 gloabl cortex 支持 alert
		if (StrUtil.isEmpty(supportAlertPromId.toString())) {
			Agent supportAlertAgent = healthyAgents.stream()
					.filter(agent -> Constant.AgentType.GLOBAL.getValue().equals(agent.getType()))
					.sorted(Comparator.comparing(Agent::getId)).findFirst().orElse(null);
			supportAlertPromId = supportAlertAgent.getId();
			redisTemplate.opsForValue().set(Constant.REDIS_KEY_SUPPORT_ALERT_AGENT_ID,
					supportAlertPromId.toString());
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
		List<MonitorModule> snmpTypeModule = monitorModuleService
				.list(new LambdaQueryWrapper<MonitorModule>().isNotNull(MonitorModule::getSnmpYml));
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
	 * @param currentAgent
	 * @return
	 */
	public Map<String, String> generatePrometheusCmdLine(Agent currentAgent) {
		List<SysConfigEntity> sysConfigList = sysConfigService.list();
		Map<String, String> sysConfiMap = sysConfigList.stream().collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));

		Map params = Tool.MapUtil.builder()
				.put("currentAgent", currentAgent)
				.put("systemConfig", sysConfiMap)
				.map();
		String content = "";
		try {
			Template template = TemplateUtil.stringToTemplate(
					sysConfigService.getValue(Constant.SYSCONFIG_KEY_PROM_CMD_TMPL),
					Constant.SYSCONFIG_KEY_PROM_CMD_TMPL);
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
	 * @param logsEndpointConfigListForAsset
	 * @return
	 */
	@SuppressWarnings("all")
	public Map generatePromtailYml(Agent currentAgent, List<Agent> globalAgentList,
			List<MonitorEndpoint> logsEndpointConfigListForAsset) {
		log.debug("promtail.yml 开始生成");
		Map<String, Object> params = new LinkedHashMap();
		List<MonitorEndpoint> endpointList = new ArrayList<>();

		for (MonitorEndpoint logEndpointConfigInfoEntity : logsEndpointConfigListForAsset) {
			String configStr = logEndpointConfigInfoEntity.getEndpointConfig().getConfig();
			List<Map> logConfigs = JSONObject.parseArray(configStr, Map.class);
			for (Map configMap : logConfigs) {
				configMap = new TreeMap<>(configMap);
				MonitorEndpoint clone = ObjectUtil.cloneByStream(logEndpointConfigInfoEntity);
				MonitorEndpointConfig endpointConfig = clone.getEndpointConfig();
				endpointConfig.setConfig(JSONObject.toJSONString(configMap, SerializerFeature.MapSortField));
				endpointList.add(clone);
			}
		}

		// 避免重复 配置项目 下发 / 根据 endpoint config 配置字段去重
		endpointList = endpointList.stream()
				.collect(Collectors.collectingAndThen(
						Collectors.toCollection(
								() -> new TreeSet<>(Comparator.comparing(o -> o.getEndpointConfig().getConfig()))),
						ArrayList::new));
		endpointList = endpointList.stream().sorted(Comparator.comparing(MonitorEndpoint::getId))
				.collect(Collectors.toList());
		// 处理 endpoint 配置项 为了适应模板
		this.handleDataConfigForTmpl(currentAgent, endpointList, Collections.emptyList());

		params.put("endpointList", endpointList);

		List<Map> clientsMapList = new ArrayList<>();
		// 日志代理推送模式
		String logsProxyPushType = sysConfigService.getValue(Constant.SYSCONFIG_KEY_LOGS_PROXY_PUSH_TYPE);
		switch (StrUtil.emptyToDefault(logsProxyPushType, "1")) {
		case "1": // 全部使用 agent proxy 方式
		case "2": // 联邦开启模式下，promtail 先推送至 per-datacenter agent 代理接口，再 push global loki
			Map clientEntity = new HashMap(4);
			clientEntity.put("url", String.format("%s://%s:%s%s%s", currentAgent.getProtocol(), currentAgent.getHost(), currentAgent.getPort(),
					sysConfigService.getValue(Constant.CONFAGENT_PATH), "/loki/proxy/loki/api/v1/push"));
			clientEntity.put("token", currentAgent.getToken());
			clientsMapList.add(clientEntity);
			break;
		case "3"://直接推送到 loki
			// 1、本地存储，2、s3
			String logsStorageType = sysConfigService.getValueOrDefault(Constant.SYSCONFIG_KEY_LOGS_STORAGE_TYPE, "1");

			if (Tool.StrUtil.equals("1", logsStorageType)) {
				for (Agent globalAgent : globalAgentList) {
					Map client = new HashMap(4);
					client.put("url", String.format("http://%s:%s%s", globalAgent.getHost(), lokiDefaultPort,
							"/loki/api/v1/push"));
					client.put("token", globalAgent.getToken());
					clientsMapList.add(client);
				}
			} else {
				// s3存储，只需要推送一个
				int randomInt = Tool.RandomUtil.randomInt(globalAgentList.size());
				Agent agent = globalAgentList.get(randomInt);
				Map client = new HashMap(4);
				client.put("url",String.format("http://%s:%s%s", agent.getHost(), lokiDefaultPort, "/loki/api/v1/push"));
				client.put("token", agent.getToken());
				clientsMapList.add(client);
			}
			break;

		}

		params.put("clientsMapList", clientsMapList);

		Map map = null;
		String content = "";
		try {
			Template template = TemplateUtil.stringToTemplate(
					sysConfigService.getValue(Constant.SYSCONFIG_KEY_PROMTAIL_YML_TMPL),
					Constant.SYSCONFIG_KEY_PROMTAIL_YML_TMPL);
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
	 * @param agent
	 * @param endpointList
	 * @param assetList
	 * @param globalAgents
	 * @return
	 */
	public Map generatePerdatacenterYml(Agent agent, List<MonitorEndpoint> endpointList,
			List<AssetAsset> assetList, List<Agent> globalAgents) {
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
		List<Map> jobList = new ArrayList<>();

		// prometheus_metric
		Map promMetricsMap = new LinkedHashMap();
		promMetricsMap.put("job_name", "prometheus_metric");
		promMetricsMap.put("metrics_path",sysConfigService.getValue(Constant.CONFAGENT_PATH) + "/prometheus/proxy/metrics");
		promMetricsMap.put("scheme",agent.getProtocol());
		
		List promMetricsStaticConfigList = new ArrayList();
		Map promMetricsStaticConfig = new LinkedHashMap(2);
		List<String> promMetricsTargets = new ArrayList();
		promMetricsTargets.add(agent.getHost() + ":" + agent.getPort());
		promMetricsStaticConfig.put("targets", promMetricsTargets);

		Map labels = new LinkedHashMap(4);
		Dc dc = dcService.getById(agent.getDcId());
		labels.put("datacenter", ToolUtil.string2Json(dc.getName()));
		labels.put("type", Constant.AgentType.getAgentType(agent.getType()).getName());
		labels.put("application", "prometheus");
		labels.put("agent_name",agent.getName());
		labels.put("nz_agent_id",agent.getId());
		promMetricsStaticConfig.put("labels", labels);
		promMetricsStaticConfigList.add(promMetricsStaticConfig);
		promMetricsMap.put("staticConfigList", promMetricsStaticConfigList);
		promMetricsMap.put("instanceRelabel", true);

		// agent metrics
		Map agentMetricsMap = new LinkedHashMap();
		agentMetricsMap.put("job_name", "agent_metrics");
		agentMetricsMap.put("metrics_path",sysConfigService.getValue(Constant.CONFAGENT_PATH) + "/actuator/prometheus");
		agentMetricsMap.put("scheme",agent.getProtocol());
		
		List agentMetricsStaticConfigList = new ArrayList();
		Map agentMetricsStaticConfig = new LinkedHashMap(2);
		List agentMetricsTargets = new ArrayList();
		agentMetricsTargets.add(String.format("%s:%s", agent.getHost(), agent.getPort()));
		agentMetricsStaticConfig.put("targets", agentMetricsTargets);

		Map agentLabels = new LinkedHashMap(4);
		agentLabels.put("datacenter", ToolUtil.string2Json(dc.getName()));
		agentLabels.put("type", Constant.AgentType.getAgentType(agent.getType()).getName());
		agentLabels.put("application", "nz-agent");
		agentLabels.put("agent_name", agent.getName());
		agentLabels.put("nz_agent_id",agent.getId());
		agentMetricsStaticConfig.put("labels", agentLabels);

		agentMetricsStaticConfigList.add(agentMetricsStaticConfig);
		agentMetricsMap.put("staticConfigList", agentMetricsStaticConfigList);
		agentMetricsMap.put("instanceRelabel", false);
		
		// snmp exporter metrics
		Map snmpExporterMetricsMap = new LinkedHashMap();
		snmpExporterMetricsMap.put("job_name", "snmp_exporter_metrics");
		snmpExporterMetricsMap.put("metrics_path",sysConfigService.getValue(Constant.SNMP_EXPORTER_MONITOR_PATH));
		snmpExporterMetricsMap.put("scheme",sysConfigService.getValue(Constant.SNMP_EXPORTER_MONITOR_SCHEME));
		
		List snmpExporterMetricsStaticConfigList = new ArrayList();
		Map snmpExporterMetricsStaticConfig = new LinkedHashMap(2);
		List snmpExporterMetricsTargets = new ArrayList();
		snmpExporterMetricsTargets.add(String.format("%s:%s", StrUtil.isBlank(sysConfigService.getValue(Constant.SNMP_EXPORTER_MONITOR_HOST))?agent.getHost():sysConfigService.getValue(Constant.SNMP_EXPORTER_MONITOR_HOST), sysConfigService.getValue(Constant.SNMP_EXPORTER_MONITOR_PORT)));
		snmpExporterMetricsStaticConfig.put("targets", snmpExporterMetricsTargets);

		Map snmpExporterLabels = new LinkedHashMap(4);
		snmpExporterLabels.put("datacenter", ToolUtil.string2Json(dc.getName()));
		snmpExporterLabels.put("type", Constant.AgentType.getAgentType(agent.getType()).getName());
		snmpExporterLabels.put("application", "snmp-exporter");
		snmpExporterLabels.put("agent_name",agent.getName());
		snmpExporterLabels.put("nz_agent_id",agent.getId());
		snmpExporterMetricsStaticConfig.put("labels", snmpExporterLabels);

		snmpExporterMetricsStaticConfigList.add(snmpExporterMetricsStaticConfig);
		snmpExporterMetricsMap.put("staticConfigList", snmpExporterMetricsStaticConfigList);
		snmpExporterMetricsMap.put("instanceRelabel", false);
		
		// blackbox exporter metrics
		Map blackboxExporterMetricsMap = new LinkedHashMap();
		blackboxExporterMetricsMap.put("job_name", "blackbox_exporter_metrics");
		blackboxExporterMetricsMap.put("metrics_path",sysConfigService.getValue(Constant.BLACKBOX_EXPORTER_MONITOR_PATH));
		blackboxExporterMetricsMap.put("scheme",sysConfigService.getValue(Constant.BLACKBOX_EXPORTER_MONITOR_SCHEME));
		
		List blackboxExporterMetricsStaticConfigList = new ArrayList();
		Map blackboxExporterMetricsStaticConfig = new LinkedHashMap(2);
		List blackboxExporterMetricsTargets = new ArrayList();
		blackboxExporterMetricsTargets.add(String.format("%s:%s", StrUtil.isBlank(sysConfigService.getValue(Constant.BLACKBOX_EXPORTER_MONITOR_HOST))?agent.getHost():sysConfigService.getValue(Constant.BLACKBOX_EXPORTER_MONITOR_HOST), sysConfigService.getValue(Constant.BLACKBOX_EXPORTER_MONITOR_PORT)));
		blackboxExporterMetricsStaticConfig.put("targets", blackboxExporterMetricsTargets);

		Map blackboxExporterLabels = new LinkedHashMap(4);
		blackboxExporterLabels.put("datacenter", ToolUtil.string2Json(dc.getName()));
		blackboxExporterLabels.put("type", Constant.AgentType.getAgentType(agent.getType()).getName());
		blackboxExporterLabels.put("application", "blackbox-exporter");
		blackboxExporterLabels.put("agent_name",agent.getName());
		blackboxExporterLabels.put("nz_agent_id",agent.getId());
		blackboxExporterMetricsStaticConfig.put("labels", blackboxExporterLabels);

		blackboxExporterMetricsStaticConfigList.add(blackboxExporterMetricsStaticConfig);
		blackboxExporterMetricsMap.put("staticConfigList", blackboxExporterMetricsStaticConfigList);
		blackboxExporterMetricsMap.put("instanceRelabel", false);
		
		jobList.add(agentMetricsMap);
		jobList.add(promMetricsMap);
		jobList.add(snmpExporterMetricsMap);
		jobList.add(blackboxExporterMetricsMap);
		params.put("jobList", jobList);

		// 处理 endpoint 配置项 为了适应模板
		this.handleDataConfigForTmpl(agent, endpointList, assetList);

		params.put("endpointList", endpointList);
		// remote_write url and token
		List<Map> remoteWriteUrlInfoMapList = new ArrayList<>();
		// object storage 标识 1: 本地文件存储，2：S3对象存储
		String objStroageEnable = sysConfigService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_TYPE);
		boolean isLocalStorage = StrUtil.equals("1", objStroageEnable);

		String agentPath = sysConfigService.getValue(Constant.CONFAGENT_PATH);

		// 指标代理模式 metrics_proxy_push_type 1: 经过 agent 代理推送 cortex/prometheus 2: 直接推送
		// cortex/prometheus
		String metricsProxyPushType = sysConfigService.getValue(Constant.SYSCONFIG_KEY_METRICS_PROXY_PUSH_TYPE);
		switch (StrUtil.emptyToDefault(metricsProxyPushType, "1")) {
		case "1": {
			if (isLocalStorage) {
				// metrics 本地存储，弃用 cortex ，直接采用 prometheus , 根据 sys_config prometheus_fed_mode
				// 确定 pull/push
				// 1: pull 2:push 默认 push
				String prometheusFedMode = sysConfigService.getValue(Constant.SYSCONFIG_KEY_PROM_FEDER_MODE);
				prometheusFedMode = StrUtil.emptyToDefault(prometheusFedMode, "2");
				if (StrUtil.equals("1", prometheusFedMode)) {
					// global 使用 federation job 集中收集各个 per-datacenter 数据，do nothing
				} else if (StrUtil.equals("2", prometheusFedMode)) {
					// global使用 remote_write 集中收集各个 per-datacenter数据
					for (Agent globalAgent : globalAgents) {
						Map map = new HashMap();
						map.put("url", String.format("%s://%s:%s%s/prometheus/proxy/api/v1/write",globalAgent.getProtocol(),
								globalAgent.getHost(), globalAgent.getPort(), agentPath));
						map.put("token", globalAgent.getToken());
						remoteWriteUrlInfoMapList.add(map);
					}
				}
			} else {
				// s3 存储 and agent 代理，任选一个 global 即可
				Map map = new HashMap();// http://127.0.0.1:${agentPort?c}${agentPath}/cortex/proxy/api/v1/push
				map.put("url", String.format("%s://%s:%s%s/cortex/proxy/api/v1/push",globalAgents.get(0).getProtocol(),
						globalAgents.get(0).getHost(), globalAgents.get(0).getPort(), agentPath));
				map.put("token", globalAgents.get(0).getToken());
				remoteWriteUrlInfoMapList.add(map);
			}
			break;
		}
		case "2": {
			if (isLocalStorage) {
				// metrics 本地存储，弃用 cortex ，直接采用 prometheus , 根据 sys_config prometheus_fed_mode
				// 确定 pull/push
				// 1: pull 2:push 默认 push
				String prometheusFedMode = sysConfigService.getValue(Constant.SYSCONFIG_KEY_PROM_FEDER_MODE);
				prometheusFedMode = StrUtil.emptyToDefault(prometheusFedMode, "2");
				if (StrUtil.equals("1", prometheusFedMode)) {
					// global 使用 federation job 集中收集各个 per-datacenter 数据，do nothing
				} else if (StrUtil.equals("2", prometheusFedMode)) {
					// global使用 remote_write 集中收集各个 per-datacenter数据
					for (Agent globalAgent : globalAgents) {
						Map map = new HashMap();
						map.put("url", String.format("http://%s:%s/api/v1/write", globalAgent.getHost(),
								prometheusDefaultPort));
						map.put("token", globalAgent.getToken());
						remoteWriteUrlInfoMapList.add(map);
					}
				}
			} else {
				// s3 存储 and cortex，任选一个 global cortex 即可
				Map map = new HashMap();// http://127.0.0.1:${agentPort?c}${agentPath}/cortex/proxy/api/v1/push
				map.put("url", String.format("http://%s:%s/api/v1/push", globalAgents.get(0).getHost(),
						cortexDefaultPort));
				map.put("token", globalAgents.get(0).getToken());
				remoteWriteUrlInfoMapList.add(map);
			}
			break;
		}
		}
		params.put("remoteWriteUrlInfos", remoteWriteUrlInfoMapList);

		List<SysConfigEntity> sysConfigList = sysConfigService.list();
		Map<String, String> sysConfiMap = sysConfigList.stream()
				.collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));
		params.put("systemConfig", sysConfiMap);
		params.put("currentAgent", agent);

		Map map = null;
		String content = "";
		try {
			Template template = TemplateUtil.stringToTemplate(
					sysConfigService.getValue(Constant.SYSCONFIG_KEY_PROM_YML_TMPL),
					Constant.SYSCONFIG_KEY_PROM_YML_TMPL);
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
	 * @param globalAgents
	 * @param perDataCenterAgents
	 * @param assetList
	 * @param endpointList
	 * @param isAlert
	 * @param isSupportSysteMetrics
	 * @return
	 */
	public Map generateGlobalYml(Agent currentAgent, List<Agent> globalAgents,
			List<Agent> perDataCenterAgents, List<AssetAsset> assetList, List<MonitorEndpoint> endpointList,
			Boolean isAlert, Boolean isSupportSysteMetrics) {
		log.debug("global prometheus.yml 开始生成");
		// sys_config 配置表信息
		List<SysConfigEntity> configEntityList = sysConfigService.list();
		Map<String, String> configMap = configEntityList.stream()
				.collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));

		boolean fedeEnabled = StrUtil.equals("1", configMap.get(Constant.SYSCONFIG_KEY_PROM_FEDER_ENABLED));
		// 关闭联邦 不启用 per-datecenter
		if (!fedeEnabled)
			perDataCenterAgents = new ArrayList<>();

		List<Agent> allAgentList = new ArrayList<>();
		allAgentList.addAll(globalAgents);
		allAgentList.addAll(perDataCenterAgents);

		// agent 代理接口前缀
		String proxyPath = configMap.get(Constant.CONFAGENT_PATH) + "/prometheus/proxy";
		// federate
		List<Map> federateMapList = new ArrayList<>();
		// blackbox job 1: global 2: per-datacenter
		String assetPingFrom = configMap.get(Constant.SYSCONFIG_KEY_ASSET_PING_FROM);
		for (Agent server : allAgentList) {
			// 联邦开启
			/*if (fedeEnabled) {
				// global 做 ping 检测，联邦排除自己
				if ("1".equals(assetPingFrom)) {
					if (server.getId().equals(currentAgent.getId()))
						continue;
				} else {
					// per-datacenter 做 ping 检测，global 角色排除掉，不需要加入到 federate 中
					if (Constant.PromserverType.GLOBAL.getValue().equals(server.getType()))
						continue;
				}
			} else {
				// 联邦关闭
				if (server.getId().equals(currentAgent.getId()))
					continue;
			}*/

			if (server.getId().equals(currentAgent.getId()))
				continue;

			Map federateMap = new LinkedHashMap();
			federateMap.put("job_name", "federate_" + server.getId());
			federateMap.put("metrics_path", proxyPath + "/federate");
			federateMap.put("scheme", server.getProtocol());
			
			federateMap.put("isFederate", true);
			// global 做 ping 检测，且 当前角色为 global
			if ("1".equals(assetPingFrom) && Constant.AgentType.GLOBAL.getValue().equals(server.getType())) {
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
		promMetricsMap.put("scheme", currentAgent.getProtocol());
		promMetricsMap.put("isFederate", false);
		promMetricsMap.put("instanceRelabel", true);

		List promMetricsStaticConfigList = new ArrayList();
		/*
		 * for (Agent entity : allPromserverList) { Map promMetricsStaticConfig =
		 * new LinkedHashMap(2); List<String> promMetricsTargets = new ArrayList();
		 * promMetricsTargets.add(entity.getHost() + ":" + entity.getPort());
		 * promMetricsStaticConfig.put("targets", promMetricsTargets);
		 * 
		 * Map labels = new LinkedHashMap(4); Dc dc =
		 * dcService.getById(entity.getDcId()); labels.put("datacenter",
		 * ToolUtil.string2Json(dc.getName())); labels.put("type",
		 * Constant.PromserverType.getPromserverType(entity.getType()).getName());
		 * labels.put("application", "prometheus");
		 * promMetricsStaticConfig.put("labels", labels);
		 * 
		 * promMetricsStaticConfigList.add(promMetricsStaticConfig); }
		 */
		Map promMetricsStaticConfig = new LinkedHashMap(2);
		List<String> promMetricsTargets = new ArrayList();
		promMetricsTargets.add(currentAgent.getHost() + ":" + currentAgent.getPort());
		promMetricsStaticConfig.put("targets", promMetricsTargets);

		Map labels = new LinkedHashMap(4);
		Dc dc = dcService.getById(currentAgent.getDcId());
		labels.put("datacenter", ToolUtil.string2Json(dc.getName()));
		labels.put("type", Constant.AgentType.getAgentType(currentAgent.getType()).getName());
		labels.put("application", "prometheus");
		labels.put("agent_name",currentAgent.getName());
		labels.put("nz_agent_id",currentAgent.getId());
		promMetricsStaticConfig.put("labels", labels);
		promMetricsStaticConfigList.add(promMetricsStaticConfig);
		promMetricsMap.put("staticConfigList", promMetricsStaticConfigList);

		// nezha_metrics
		String alertApi = configMap.get(Constant.SYSCONFIG_KEY_ALERT_API);
		String scheme = Tool.StrUtil.isBlank(alertApi) ? "http": alertApi.split("://")[0];
		String target = Tool.StrUtil.isBlank(alertApi) ? "" : alertApi.replace(scheme + "://", "");
		Map nezhaMetricsMap = new LinkedHashMap();
		String[] split = alertApi.split("://");
		nezhaMetricsMap.put("job_name", "nezha_metrics");
		nezhaMetricsMap.put("metrics_path",configMap.get(Constant.SYSCONFIG_KEY_ALERT_PATH_PREFIX) + "/actuator/prometheus");
		nezhaMetricsMap.put("isFederate", false);
		nezhaMetricsMap.put("scheme", scheme); // 协议
		nezhaMetricsMap.put("instanceRelabel", true);
		List nezhaMetricsStaticConfigList = new ArrayList();
		Map nezhaMetricsStaticConfig = new LinkedHashMap(2);
		List nezhaMetricsTargets = new ArrayList();
		nezhaMetricsTargets.add(target); // ip:port
		nezhaMetricsStaticConfig.put("targets", nezhaMetricsTargets);
		Map nezhaLabels = new LinkedHashMap(4);
		nezhaLabels.put("datacenter", ToolUtil.string2Json(dc.getName()));
		nezhaLabels.put("application", "nz-web");
		nezhaLabels.put("agent_name", currentAgent.getName());
		nezhaLabels.put("nz_agent_id", currentAgent.getId());
		nezhaMetricsStaticConfig.put("labels",nezhaLabels);
		nezhaMetricsStaticConfigList.add(nezhaMetricsStaticConfig);
		nezhaMetricsMap.put("staticConfigList", nezhaMetricsStaticConfigList);

		// agent metrics
		Map agentMetricsMap = new LinkedHashMap();
		agentMetricsMap.put("job_name", "agent_metrics");
		agentMetricsMap.put("metrics_path", configMap.get(Constant.CONFAGENT_PATH) + "/actuator/prometheus");
		agentMetricsMap.put("scheme", currentAgent.getProtocol());
		agentMetricsMap.put("isFederate", false);
		agentMetricsMap.put("instanceRelabel", true);

		List agentMetricsStaticConfigList = new ArrayList();
		Map agentMetricsStaticConfig = new LinkedHashMap(2);
		List agentMetricsTargets = new ArrayList();
		agentMetricsTargets.add(String.format("%s:%s", currentAgent.getHost(), currentAgent.getPort()));
		agentMetricsStaticConfig.put("targets", agentMetricsTargets);
		Map agentLabels = new LinkedHashMap(3);
		agentLabels.put("datacenter", ToolUtil.string2Json(dc.getName()));
		agentLabels.put("application", "nz-agent");
		agentLabels.put("type", Constant.AgentType.getAgentType(currentAgent.getType()).getName());
		agentLabels.put("agent_name", currentAgent.getName());
		agentLabels.put("nz_agent_id",currentAgent.getId());
		agentMetricsStaticConfig.put("labels", agentLabels);
		agentMetricsStaticConfigList.add(agentMetricsStaticConfig);
		agentMetricsMap.put("staticConfigList", agentMetricsStaticConfigList);

		// snmp exporter metrics
		Map snmpExporterMetricsMap = new LinkedHashMap();
		snmpExporterMetricsMap.put("job_name", "snmp_exporter_metrics");
		snmpExporterMetricsMap.put("metrics_path",sysConfigService.getValue(Constant.SNMP_EXPORTER_MONITOR_PATH));
		snmpExporterMetricsMap.put("scheme",sysConfigService.getValue(Constant.SNMP_EXPORTER_MONITOR_SCHEME));
		snmpExporterMetricsMap.put("isFederate", false);
		snmpExporterMetricsMap.put("instanceRelabel", true);
		
		List snmpExporterMetricsStaticConfigList = new ArrayList();
		Map snmpExporterMetricsStaticConfig = new LinkedHashMap(2);
		List snmpExporterMetricsTargets = new ArrayList();
		snmpExporterMetricsTargets.add(String.format("%s:%s", StrUtil.isBlank(sysConfigService.getValue(Constant.SNMP_EXPORTER_MONITOR_HOST))?currentAgent.getHost():sysConfigService.getValue(Constant.SNMP_EXPORTER_MONITOR_HOST), sysConfigService.getValue(Constant.SNMP_EXPORTER_MONITOR_PORT)));
		snmpExporterMetricsStaticConfig.put("targets", snmpExporterMetricsTargets);

		Map snmpExporterLabels = new LinkedHashMap(2);
		snmpExporterLabels.put("datacenter", ToolUtil.string2Json(dc.getName()));
		snmpExporterLabels.put("application", "snmp-exporter");
		snmpExporterLabels.put("agent_name", currentAgent.getName());
		snmpExporterLabels.put("nz_agent_id",currentAgent.getId());
		snmpExporterMetricsStaticConfig.put("labels", snmpExporterLabels);
		snmpExporterMetricsStaticConfigList.add(snmpExporterMetricsStaticConfig);
		snmpExporterMetricsMap.put("staticConfigList", snmpExporterMetricsStaticConfigList);
		
		// blackbox exporter metrics
		Map blackboxExporterMetricsMap = new LinkedHashMap();
		blackboxExporterMetricsMap.put("job_name", "blackbox_exporter_metrics");
		blackboxExporterMetricsMap.put("metrics_path",sysConfigService.getValue(Constant.BLACKBOX_EXPORTER_MONITOR_PATH));
		blackboxExporterMetricsMap.put("scheme",sysConfigService.getValue(Constant.BLACKBOX_EXPORTER_MONITOR_SCHEME));
		blackboxExporterMetricsMap.put("isFederate", false);
		blackboxExporterMetricsMap.put("instanceRelabel", true);
		
		List blackboxExporterMetricsStaticConfigList = new ArrayList();
		Map blackboxExporterMetricsStaticConfig = new LinkedHashMap(2);
		List blackboxExporterMetricsTargets = new ArrayList();
		blackboxExporterMetricsTargets.add(String.format("%s:%s", StrUtil.isBlank(sysConfigService.getValue(Constant.BLACKBOX_EXPORTER_MONITOR_HOST))?currentAgent.getHost():sysConfigService.getValue(Constant.BLACKBOX_EXPORTER_MONITOR_HOST), sysConfigService.getValue(Constant.BLACKBOX_EXPORTER_MONITOR_PORT)));
		blackboxExporterMetricsStaticConfig.put("targets", blackboxExporterMetricsTargets);

		Map blackboxExporterLabels = new LinkedHashMap(2);
		blackboxExporterLabels.put("datacenter", ToolUtil.string2Json(dc.getName()));
		blackboxExporterLabels.put("application", "blackbox-exporter");
		blackboxExporterLabels.put("agent_name",currentAgent.getName());
		blackboxExporterLabels.put("nz_agent_id",currentAgent.getId());
		blackboxExporterMetricsStaticConfig.put("labels", blackboxExporterLabels);
		blackboxExporterMetricsStaticConfigList.add(blackboxExporterMetricsStaticConfig);
		blackboxExporterMetricsMap.put("staticConfigList", blackboxExporterMetricsStaticConfigList);

		// loki metrics
		Map lokiMetricsMap = new LinkedHashMap();
		lokiMetricsMap.put("job_name", "loki_metrics");
		lokiMetricsMap.put("metrics_path",sysConfigService.getValue(Constant.LOKI_MONITOR_PATH));
		lokiMetricsMap.put("scheme",sysConfigService.getValue(Constant.LOKI_MONITOR_SCHEME));
		lokiMetricsMap.put("isFederate", false);
		lokiMetricsMap.put("instanceRelabel", true);
		List lokiMetricsStaticConfigList = new ArrayList();
		Map lokiMetricsStaticConfig = new LinkedHashMap(2);
		List lokiMetricsTargets = new ArrayList();
		lokiMetricsTargets.add(String.format("%s:%s", StrUtil.isBlank(sysConfigService.getValue(Constant.LOKI_MONITOR_HOST))?currentAgent.getHost():sysConfigService.getValue(Constant.LOKI_MONITOR_HOST), sysConfigService.getValue(Constant.LOKI_MONITOR_PORT)));
		lokiMetricsStaticConfig.put("targets", lokiMetricsTargets);
		Map lokiLabels = new LinkedHashMap(2);
		lokiLabels.put("datacenter", ToolUtil.string2Json(dc.getName()));
		lokiLabels.put("application", "loki");
		lokiLabels.put("agent_name",currentAgent.getName());
		lokiLabels.put("nz_agent_id",currentAgent.getId());
		lokiMetricsStaticConfig.put("labels", lokiLabels);
		lokiMetricsStaticConfigList.add(lokiMetricsStaticConfig);
		lokiMetricsMap.put("staticConfigList", lokiMetricsStaticConfigList);
		
		// cortex metrics
		Map cortexMetricsMap = new LinkedHashMap();
		cortexMetricsMap.put("job_name", "cortex_metrics");
		cortexMetricsMap.put("metrics_path",sysConfigService.getValue(Constant.CORTEX_MONITOR_PATH));
		cortexMetricsMap.put("scheme",sysConfigService.getValue(Constant.CORTEX_MONITOR_SCHEME));
		cortexMetricsMap.put("isFederate", false);
		cortexMetricsMap.put("instanceRelabel", true);
		List cortexMetricsStaticConfigList = new ArrayList();
		Map cortexMetricsStaticConfig = new LinkedHashMap(2);
		List cortexMetricsTargets = new ArrayList();
		cortexMetricsTargets.add(String.format("%s:%s", StrUtil.isBlank(sysConfigService.getValue(Constant.CORTEX_MONITOR_HOST))?currentAgent.getHost():sysConfigService.getValue(Constant.CORTEX_MONITOR_HOST), sysConfigService.getValue(Constant.CORTEX_MONITOR_PORT)));
		cortexMetricsStaticConfig.put("targets", cortexMetricsTargets);
		Map cortexLabels = new LinkedHashMap(2);
		cortexLabels.put("datacenter", ToolUtil.string2Json(dc.getName()));
		cortexLabels.put("application", "cortex");
		cortexLabels.put("agent_name", currentAgent.getName());
		cortexLabels.put("nz_agent_id", currentAgent.getId());
		cortexMetricsStaticConfig.put("labels", cortexLabels);
		cortexMetricsStaticConfigList.add(cortexMetricsStaticConfig);
		cortexMetricsMap.put("staticConfigList", cortexMetricsStaticConfigList);
		
		Map<String, Object> params = new LinkedHashMap();
		params.put("scrape_interval", configMap.get(Constant.SYSCONFIG_KEY_SCRAPEINTERVAL));
		params.put("scrape_timeout", configMap.get(Constant.SYSCONFIG_KEY_SCRAPETIMEOUT));
		params.put("path_prefix", configMap.get(Constant.SYSCONFIG_KEY_ALERT_PATH_PREFIX));
		params.put("alert_url", configMap.get(Constant.SYSCONFIG_KEY_ALERT_API));
		params.put("isGlobal", true);
		params.put("isAlert", isAlert);

		// object storage 标识 1: 本地文件存储，2：S3对象存储
		String objStroageEnable = configMap.get(Constant.SYSCONFIG_KEY_METRICS_STORAGE_TYPE);
		boolean isLocalStorage = StrUtil.equals("1", objStroageEnable);

		List<Map> jobList = new ArrayList<>();

		/**
		 * 指标代理模式 metrics_proxy_push_type 1: 经过 agent 代理推送 cortex 2: 直接推送 cortex
		 */
		String metricsProxyPushType = sysConfigService.getValue(Constant.SYSCONFIG_KEY_METRICS_PROXY_PUSH_TYPE);
		boolean isProxyPush = StrUtil.equalsIgnoreCase("1", StrUtil.emptyToDefault(metricsProxyPushType, "1"));

		// remote_write url and token
		List<Map> remoteWriteUrlInfoMapList = new ArrayList<>();
		if (fedeEnabled) {
			// 联邦开启
			// 本地存储 ， metrics 数据保存于 Prometheus , 弃用 cortex , global 接收 per-datacenter
			// 推过来得数据，每一个 global 支持 nzweb 系统指标
			if (isLocalStorage) {
				jobList.add(nezhaMetricsMap);
				// jobList.add(promMetricsMap);
				// metrics 本地存储，弃用 cortex ，直接采用 prometheus , 根据 sys_config prometheus_fed_mode
				// 确定 pull/push
				// 1: pull 2:push 默认 push
				String prometheusFedMode = sysConfigService.getValue(Constant.SYSCONFIG_KEY_PROM_FEDER_MODE);
				prometheusFedMode = StrUtil.emptyToDefault(prometheusFedMode, "2");
				if (StrUtil.equals("1", prometheusFedMode)) {
					// global 使用 federation job 集中收集各个 per-datacenter 数据
					// federate
					for (Map map : federateMapList) {
						jobList.add(map);
					}
				} else if (StrUtil.equals("2", prometheusFedMode)) {
					// global使用 remote_write 集中收集各个 per-datacenter数据 , 通过 联邦 job 收集其他 global 数据（global 负责任务采集，如 ping prometheus 等）
					// 只采集 其他 global 即可
					List<Integer> globalAgentIdList = globalAgents.stream().map(Agent::getId).collect(Collectors.toList());
					for (Map map : federateMapList) {
						String jobName = (String) map.get("job_name");
						Integer serverId = Integer.valueOf(jobName.replaceAll("federate_", ""));
						if (globalAgentIdList.contains(serverId) && !serverId.equals(currentAgent.getId())) {
							jobList.add(map);
						}
					}
				}
			} else {
				// s3 对象存储 global 接收 per-datacenter 推过来得数据,第一个支持 nzweb 系统模板即可
				Map map = new HashMap();
				if(isProxyPush) {
					map.put("url", String.format("%s://%s:%s%s",currentAgent.getProtocol(), 
							currentAgent.getHost(),currentAgent.getPort(),
							configMap.get(Constant.CONFAGENT_PATH) + "/cortex/proxy/api/v1/push"));
				}else {
					map.put("url", String.format("http://%s:%s%s", currentAgent.getHost(), cortexDefaultPort, "/api/v1/push"));
				}
				
				map.put("token", currentAgent.getToken());
				remoteWriteUrlInfoMapList.add(map);
				if (isSupportSysteMetrics) {
					jobList.add(nezhaMetricsMap);
					// jobList.add(promMetricsMap);
				}
			}
		} else {
			// 联邦关闭
			if (isLocalStorage) {
				// metrics 本地存储，弃用 cortex ，直接采用 prometheus , 根据 sys_config prometheus_fed_mode
				// 确定 pull/push
				// 1: pull 2:push 默认 push
				String prometheusFedMode = sysConfigService.getValue(Constant.SYSCONFIG_KEY_PROM_FEDER_MODE);
				prometheusFedMode = StrUtil.emptyToDefault(prometheusFedMode, "2");
				if (StrUtil.equals("1", prometheusFedMode)) {
					// global 使用 federation job 集中收集其他 global promethues
					for (Map map : federateMapList) {
						jobList.add(map);
					}
				} else if (StrUtil.equals("2", prometheusFedMode)) {
					// global使用 remote_write 集中收集其他 global promethues
					for (Agent globalAgent : globalAgents) {
						// 跳过自己
						if (globalAgent.getId().equals(currentAgent.getId()))
							continue;
						Map map = new HashMap();
						String remoteWriteUrl;
						if (isProxyPush) {
							remoteWriteUrl = String.format("%s://%s:%s%s/prometheus/proxy/api/v1/write",
									globalAgent.getProtocol(),
									globalAgent.getHost(), globalAgent.getPort(),
									configMap.get(Constant.CONFAGENT_PATH));
						} else {
							remoteWriteUrl = String.format("http://%s:%s/api/v1/write", globalAgent.getHost(),
									prometheusDefaultPort);
						}
						map.put("url", remoteWriteUrl);
						map.put("token", globalAgent.getToken());
						remoteWriteUrlInfoMapList.add(map);
					}
				}
			} else {
				// s3 配置 配置自己即可
				Map map = new HashMap();
				if(isProxyPush) {
					map.put("url",
							String.format("%s://%s:%s%s", currentAgent.getProtocol(), currentAgent.getHost(),currentAgent.getPort(),
									configMap.get(Constant.CONFAGENT_PATH) + "/cortex/proxy/api/v1/push"));
				}else {
					map.put("url",
							String.format("http://%s:%s%s", currentAgent.getHost(), cortexDefaultPort,"/api/v1/push"));
				}
				map.put("token", currentAgent.getToken());
				remoteWriteUrlInfoMapList.add(map);
			}
			// 不论 本地还是对象存储 ，有一个支持 nzweb 系统指标即可
			if (isSupportSysteMetrics) {
				jobList.add(nezhaMetricsMap);
				// jobList.add(promMetricsMap);
			}
		}

		jobList.add(promMetricsMap);
		jobList.add(agentMetricsMap);
		jobList.add(snmpExporterMetricsMap);
		jobList.add(blackboxExporterMetricsMap);
		jobList.add(lokiMetricsMap);
		jobList.add(cortexMetricsMap);
		
		params.put("remoteWriteUrlInfos", remoteWriteUrlInfoMapList);

		// 处理 endpoint 配置项 为了适应模板
		this.handleDataConfigForTmpl(currentAgent, endpointList, assetList);

		params.put("assetList", assetList);
		params.put("endpointList", endpointList);

		params.put("jobList", jobList);

		List<SysConfigEntity> sysConfigList = sysConfigService.list();
		Map<String, String> sysConfiMap = sysConfigList.stream()
				.collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));
		params.put("systemConfig", sysConfiMap);
		params.put("currentAgent", currentAgent);

		Map map = null;
		String content = "";
		try {
			Template template = TemplateUtil.stringToTemplate(configMap.get(Constant.SYSCONFIG_KEY_PROM_YML_TMPL),
					Constant.SYSCONFIG_KEY_PROM_YML_TMPL);
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
	 * @param agent
	 * @param endpointList
	 * @param assetList
	 */
	private void handleDataConfigForTmpl(Agent agent, List<MonitorEndpoint> endpointList,
			List<AssetAsset> assetList) {
		for (AssetAsset asset : assetList) {
			// dc name 处理
			Dc dc = asset.getDc();
			dc.setName(ToolUtil.string2Json(dc.getName()));
			asset.setDc(dc);

			// nz_agent_id，对应 agent id
			asset.setAgent(agent);
		}
		for (MonitorEndpoint endpoint : endpointList) {
			Map endpointConfigFieldMap = JSONObject.parseObject(endpoint.getEndpointConfig().getConfig(), Map.class);
			endpoint.setConfig(endpointConfigFieldMap);

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

			// nz_agent_id，对应 agent id
			endpoint.setAgent(agent);
		}
	}

	/**
	 * 获取 global lokipushurl config map
	 *
	 * @param agent
	 * @param globalAgents
	 * @return
	 */
	public Map<String, Object> getLokiPushConfig(Agent agent, List<Agent> globalAgents) {
		Map<String, Object> lokiPushUrlConfigBody = new LinkedHashMap(4);
		List<Map> lokiPushUrls = new ArrayList<>();
		lokiPushUrlConfigBody.put("lokiPushUrls", lokiPushUrls);
		/*
		 * 1、开启agent集群 + 本地存储 global 只推送到本地 loki per-datacenter 推送给所有 global 2、开启
		 * agent集群 + s3存储 global 只推送到本地 loki per-datacenter 推送给任意一个global
		 * 
		 * 3、关闭 agent集群 + 本地存储 global 推送到 本地 + 其它所有 global per-datacenter 不推送 talon推送给任意
		 * global 4、关闭 agent集群 + s3存储 global 推送到本地 per-datacenter 不推送 talon 推送给任意 global
		 */
		String logsStorageType = sysConfigService.getValue(Constant.SYSCONFIG_KEY_LOGS_STORAGE_TYPE);
		String logsProxyPushType = sysConfigService.getValueOrDefault(Constant.SYSCONFIG_KEY_LOGS_PROXY_PUSH_TYPE, "1");
		// 集群开关
		String federation = sysConfigService.getValueOrDefault(Constant.SYSCONFIG_KEY_PROM_FEDER_ENABLED, "1");
		String agentPath = sysConfigService.getValueOrDefault(Constant.CONFAGENT_PATH, "");

		/**
		 * 日志推送模式： 1：agent proxy 代理 2：联邦开启模式下，promtail先推送至 agent代理，再push global loki,
		 * 3：promtail 直接推送到 global loki
		 */
		if (Tool.StrUtil.equals("3", logsProxyPushType)) {
			return lokiPushUrlConfigBody;
		}

		// 开启agent集群
		if (Tool.StrUtil.equals("1", federation)) {
			Integer type = agent.getType();
			if (Tool.ObjectUtil.equal(type, 1)) {// global
				Map<String, String> push = Tool.MapUtil.builder("mode", "local").build();
				lokiPushUrls.add(push);
			}
			if (Tool.ObjectUtil.equal(type, 2)) {// per-datacenter
				if (Tool.StrUtil.equals("1", logsStorageType)) {// 本地存储
					globalAgents.forEach(prom -> {
						Map<String, String> push = Tool.MapUtil.builder("mode", "remote").build();
						push.put("token", prom.getToken());
						String target = Tool.StrUtil.equals("1", logsProxyPushType)
								? String.format("%s://%s:%s%s/loki/proxy",prom.getProtocol(), prom.getHost(), prom.getPort(), agentPath)
								: String.format("http://%s:%s", prom.getHost(), lokiDefaultPort);
						push.put("target", target);
						lokiPushUrls.add(push);
					});
				} else { // s3存储
					int randomInt = Tool.RandomUtil.randomInt(globalAgents.size());
					Agent ag = globalAgents.get(randomInt);
					Map<String, String> push = Tool.MapUtil.builder("mode", "remote").build();
					push.put("token", ag.getToken());
					String target = Tool.StrUtil.equals("1", logsProxyPushType)
							? String.format("%s://%s:%s%s/loki/proxy", ag.getProtocol(), ag.getHost(), ag.getPort(), agentPath)
							: String.format("http://%s:%s", ag.getHost(), lokiDefaultPort);
					push.put("target", target);
					lokiPushUrls.add(push);
				}
			}
		}

		// 关闭集群
		if (Tool.StrUtil.equals("0", federation)) {
			Integer type = agent.getType();
			if (Tool.ObjectUtil.equal(type, 1)) {// global
				Map<String, String> push = Tool.MapUtil.builder("mode", "local").build();
				lokiPushUrls.add(push);
				if (Tool.StrUtil.equals("1", logsStorageType)) {// 本地存储
					globalAgents.forEach(ag -> {
						if (Tool.ObjectUtil.notEqual(agent.getId(), ag.getId())) {
							Map<String, String> remote = Tool.MapUtil.builder("mode", "remote").build();
							remote.put("token", ag.getToken());
							remote.put("target", String.format("%s://%s:%s%s/loki/proxy",ag.getProtocol(), ag.getHost(),
									ag.getPort(), agentPath));
							lokiPushUrls.add(remote);
						}
					});
				}
			}
		}
		log.info("agent:{},type:{},federation:{},logsProxyPushType:{},logsStorageType:{}lokipushurl:{},",
				agent.getId(), agent.getType(), federation, logsProxyPushType, logsStorageType,
				Tool.JSONUtil.toJsonStr(lokiPushUrlConfigBody));
		return lokiPushUrlConfigBody;
	}

	/**
	 * 一致性 hash 分配 endpoint
	 *
	 * @param agentList
	 * @param endpointList
	 * @return
	 */
	public Map<Integer, Integer> consistentHashingAssignEndpoint(List<Agent> agentList,
			List<MonitorEndpoint> endpointList) {
		// 1. 真实节点
		Map<String, Integer> realNodeList = agentList.stream().collect(
				Collectors.toMap(pojo -> pojo.getHost() + ":" + pojo.getPort(), Agent::getId));
		// 2. 通过 真实节点 加上虚拟节点获取 hash 环
		SortedMap<Long, String> nodeMap = this.getHashRingByRealList(realNodeList);

		if (log.isDebugEnabled())
			log.debug("endpoint hash 环节点信息: " + JSONObject.toJSONString(nodeMap));

		// endpoint id 和 promerver id 对应 map
		Map<Integer, Integer> endpointIdAndPromIdMap = new LinkedHashMap(16);

		// endpoint hash 处理
		Map<Integer, String> endpointIdAndHashMap = endpointList.stream()
				.collect(Collectors.toMap(MonitorEndpoint::getId, endpoint -> endpoint.getEndpointConfig().getHash()));
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
					log.debug("endpoint: " + endpointId + "\t" + "node: " + nodeMap.get(nodeIndex) + "\t"
							+ "agent id: " + realNodeList.get(nodeMap.get(nodeIndex)));
			} else {
				Long nodeIndex = tailMap.firstKey();
				endpointIdAndPromIdMap.put(endpointId, realNodeList.get(nodeMap.get(nodeIndex)));
				if (log.isDebugEnabled())
					log.debug("endpoint: " + endpointId + "\t" + "node: " + nodeMap.get(nodeIndex) + "\t"
							+ "agent id: " + realNodeList.get(nodeMap.get(nodeIndex)));
			}
		}
		return endpointIdAndPromIdMap;
	}

	/**
	 * 一致性 hash 分配 asset
	 *
	 * @param agentList
	 * @param assetList
	 * @return
	 */
	public Map<Integer, Integer> consistentHashingAssignAsset(List<Agent> agentList,
			List<AssetAsset> assetList) {
		Map<String, Integer> realNodeList = agentList.stream().collect(
				Collectors.toMap(pojo -> pojo.getHost() + ":" + pojo.getPort(), Agent::getId));
		SortedMap<Long, String> nodeMap = this.getHashRingByRealList(realNodeList);

		if (log.isDebugEnabled())
			log.debug("asset hash 环节点信息:" + JSONObject.toJSONString(nodeMap));

		Map<Integer, Integer> assetIdAndPromIdMap = new LinkedHashMap(16);

		Map<Integer, String> assetIdAndHashMap = assetList.stream().collect(Collectors.toMap(AssetAsset::getId,
				asset -> String.valueOf(Math.abs(MurmurHash.hash64(asset.getManageIp() + ":" + asset.getDcId())))));
		for (Map.Entry<Integer, String> entry : assetIdAndHashMap.entrySet()) {
			Integer assetId = entry.getKey();
			String assetHash = entry.getValue();
			SortedMap<Long, String> tailMap = nodeMap.tailMap(Long.valueOf(assetHash));
			if (tailMap.isEmpty()) {
				Long nodeIndex = nodeMap.firstKey();
				assetIdAndPromIdMap.put(assetId, realNodeList.get(nodeMap.get(nodeIndex)));
				if (log.isDebugEnabled())
					log.debug("asset: " + assetId + "\t" + "node: " + nodeMap.get(nodeIndex) + "\t" + "agent id: "
							+ realNodeList.get(nodeMap.get(nodeIndex)));
			} else {
				Long nodeIndex = tailMap.firstKey();
				assetIdAndPromIdMap.put(assetId, realNodeList.get(nodeMap.get(nodeIndex)));
				if (log.isDebugEnabled())
					log.debug("asset: " + assetId + "\t" + "node: " + nodeMap.get(nodeIndex) + "\t" + "agent id: "
							+ realNodeList.get(nodeMap.get(nodeIndex)));
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
			// 节点信息 IP+PORT
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
	 * 更新 redis 中 endpointid 和 agent id对应关系 endpoint_prom
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
	 * @param agent
	 */
	public void afterRequestApi(String requestUrl, String result, Agent agent) {
		try {
			R parseObject = JSONObject.parseObject(result, R.class);
			if (!RCode.SUCCESS.getCode().equals((Integer) parseObject.get("code"))) {
				log.error(String.format("请求 Agent 接口异常，接口地址: %s 错误信息: %s, Agent: %s", requestUrl, result,
						JSONObject.toJSONString(agent)));
				this.updateAgentStatus(agent, 0);
			}
		} catch (Exception e) {
			log.error(String.format("请求 Agent 接口异常，接口地址: %s 错误信息: %s, Agent: %s", requestUrl, result,
					JSONObject.toJSONString(agent)), e);
		}
	}

	/**
	 * 修改 agents 状态
	 *
	 * @param agent
	 * @param status
	 */
	public void updateAgentStatus(Agent agent, Integer status) {
		agent.setStatus(status);
		agent.setStatusInfo(getPromStatusInfoErrorMsg());
		agent.setCheckTime(new Date());
		// 缓存和数据库需同时更新
		TimedCache<String, Object> cache = TimedCacheManager.getCache(Constant.SYS_STATE_CACHE_PREFIX, Constant.AGENT_STATE_CACHE_KEY);
		cache.put(agent.getId().toString(), JSON.toJSONString(agent));
		agentService.updateById(agent);
	}

	/**
	 * generate Init Cortex Yml
	 *
	 * @return
	 */
	public Map generateInitCortexYml() {
		List<SysConfigEntity> sysConfigList = sysConfigService.list();
		Map<String, String> sysConfiMap = sysConfigList.stream()
				.collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));

		// template variables
		Map<Object, Object> params = Tool.MapUtil.builder()
				.put("systemConfig", sysConfiMap)
				.build();

		log.info("[generateInitCortexYml] [begin]");
		String content = StrUtil.EMPTY;
		try {
			String cortexInitYmlTmpl = sysConfigService.getValue(Constant.SYSCONFIG_KEY_CORTEX_INIT_YML_TMPL);
			// 渲染
			Template template = TemplateUtil.stringToTemplate(cortexInitYmlTmpl, Constant.SYSCONFIG_KEY_CORTEX_INIT_YML_TMPL);
			content = FreeMarkerTemplateUtils.processTemplateIntoString(template, params);

			// JSON Str to JSON Obj
			Map ymlMap = JSONObject.parseObject(content, LinkedHashMap.class, Feature.OrderedField);
			log.info("[generateInitCortexYml] [finshed]");
			return ymlMap;
		} catch (Exception e) {
			log.error(e, "[generateInitCortexYml] [error] [content: {}]", content);
		}
		return Tool.MapUtil.empty();
	}

	/**
	 * generate Init Loki Yml
	 *
	 * @return
	 */
	public Map generateInitLokiYml() {
		List<SysConfigEntity> sysConfigList = sysConfigService.list();
		Map<String, String> sysConfiMap = sysConfigList.stream()
				.collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));

		// template variables
		Map<Object, Object> params = Tool.MapUtil.builder()
				.put("systemConfig", sysConfiMap)
				.build();

		log.info("[generateInitLokiYml] [begin]");
		String content = StrUtil.EMPTY;

		try {
			String cortexInitYmlTmpl = sysConfigService.getValue(Constant.SYSCONFIG_KEY_LOKI_INIT_YML_TMPL);
			// 渲染
			Template template = TemplateUtil.stringToTemplate(cortexInitYmlTmpl, Constant.SYSCONFIG_KEY_LOKI_INIT_YML_TMPL);
			content = FreeMarkerTemplateUtils.processTemplateIntoString(template, params);

			// JSON Str to JSON Obj
			Map ymlMap = JSONObject.parseObject(content, LinkedHashMap.class, Feature.OrderedField);
			log.info("[generateInitLokiYml] [finshed]");
			return ymlMap;
		} catch (Exception e) {
			log.error(e, "[generateInitLokiYml] [error] [content: {}]", content);
		}
		return Tool.MapUtil.empty();
	}
}
