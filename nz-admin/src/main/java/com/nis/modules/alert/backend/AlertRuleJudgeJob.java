package com.nis.modules.alert.backend;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.entity.AlertRuleEvalLog;
import com.nis.modules.alert.entity.receive.AlertMessageReceiveDTO;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.alert.service.AlertRuleEvalLogService;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.sys.service.SysConfigService;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;

@Component
@DisallowConcurrentExecution
public class AlertRuleJudgeJob extends QuartzJobBean {
	private static final Log log = Log.get();
	@Autowired
	private PromserverService promService;
	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	@Autowired
	private AlertRuleEvalLogService evalLogService;
	@Autowired
	private AlertMessageService alertMessageService;
	@Autowired
	private SysConfigService sysConfigService;

	/**
	 * alert rule 查询告警数据 向前偏移
	 */
	@Value("${alert.rule.query.offset:0}")
	private Long queryOffsetSec;


	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
		AlertRuleEntity alertRuleEntity = (AlertRuleEntity) jobDataMap.get(AlertContext.JOB_DATA_KEY_ALERT_RULE_ENTITY);
		Integer alertRuleId = alertRuleEntity.getId();
		Thread.currentThread().setName(String.format("alertRuleJuage-%s", alertRuleId));
		// 检查 haMode
		String serverId = (String) redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
		if (!StrUtil.equals(serverId, Constant.SERVER_ID)) {
			log.info("skip execute, SERVER_ID: {}, ruleId: {}",Constant.SERVER_ID, alertRuleId);
			return;
		}

		StopWatch stopWatch = Tool.DateUtil.createStopWatch();
		stopWatch.start();
		// type 类型，1：metrics，2：logs
		Integer type = alertRuleEntity.getType();
		log.info("judge start,ruleId:{},interval:{}s,type:{}", alertRuleId, alertRuleEntity.getInr(), type);
		// 获取当前所有缓存的keys
		String ruleCacheKey = AlertContext.getRedisCacheKey(alertRuleId);
		AlertRuleEvalLog evalLog = new AlertRuleEvalLog();
		evalLog.setRuleId(alertRuleId);
		evalLog.setStts(Tool.DateUtil.current());
		evalLog.setState(200);
		evalLog.setMsg("success");
		try {
			// redis hash
			HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
			/*
			 * 1、获取 当前可用 agent 2、query 3、根据返回结果处理
			 * 
			 */
			// 获取可用 globa agent
			Promserver agent = promService.getOneAvaliableGlobalAgent();
			if (Tool.ObjectUtil.isNull(agent)) {
				throw new NZException("No health agent available", 999);
			}

			// 查询结果 1、结果不能为空 2、结果状态 必须为 success 3、结果数据类型必须为 vector
			JSONObject result = this.queryVetor(agent, alertRuleEntity);
			ValidateUtils.is(result).notNull(RCode.ALERT_JUDGE_RESULT_ISNULL).and(result.getString("status"))
					.equalsIgnoreCase("success", RCode.ALERT_JUDGE_RESULT_FAILE)
					.and(result.getJSONObject("data").getString("resultType"))
					.equalsIgnoreCase("vector", RCode.ALERT_JUDGE_RESULT_TYPE_ERROR);

			// 获取 data 结果
			JSONArray dataArr = result.getJSONObject("data").getJSONArray("result");
			// 当前所有触发过的 告警消息，包括 ：pending，active
			Map<String, String> entries = hashOperations.entries(ruleCacheKey);

			Set<String> newAlertHash = Tool.CollUtil.newHashSet();
			List<AlertMessageReceiveDTO> messages = Tool.ListUtil.list(true);
			List<AlertMessageReceiveDTO> evalMessages = Tool.ListUtil.list(true);
			int timeout = alertRuleEntity.getExpiredTimeout();
			if (dataArr != null && dataArr.size() > 0) {
				Iterator<Object> ite = dataArr.iterator();
				while (ite.hasNext()) {
					JSONObject r = (JSONObject) ite.next();
					JSONObject metric = r.getJSONObject("metric");
					JSONArray values = r.getJSONArray("value");
					// 时间戳，单位 ms
					Long timestamp = (long) (values.getDoubleValue(0) * 1000L);
					String hash = metric.toJSONString();
					newAlertHash.add(hash);// 下一步 判断 expired 的告警消息
					if (!entries.containsKey(hash)) {// 第一次达到告警阈值
						// 将第一次 触发的时间 作为 value 保存，后续根据此计算 达到 for 的条件
						hashOperations.put(ruleCacheKey, hash, timestamp.toString());
						AlertMessageReceiveDTO message = this.genAlertMessageFromVector(alertRuleEntity, metric, values.getString(1),
								null, null);
						message.setActs((long) (values.getDoubleValue(0) * 1000L));
						message.setState(Constant.AlertMessageStatus.PENDING.getValue());
						message.setValue(values.getString(1));
						evalMessages.add(message);
						continue;
					}
					/*
					 * 以下为不是第一次达到阈值的处理 1、判断是否达到 for 的时间 2、超过 for 时间 发送 告警消息
					 */
					Long firstTime = Tool.NumberUtil.parseLong(hashOperations.get(ruleCacheKey, hash));
					if (timestamp - firstTime > alertRuleEntity.getLast() * 1000) {
						// 用户设置了 开启了自动失效，且 设置了 timeout
						String et = (timeout > 0) ? Tool.AlertUtil.formatMessageDate(timestamp + timeout * 1000) : null;
						AlertMessageReceiveDTO message = this.genAlertMessageFromVector(alertRuleEntity, metric,
								values.getString(1), Tool.AlertUtil.formatMessageDate(firstTime), et);
						message.setActs((long) (values.getDoubleValue(0) * 1000L));
						message.setState(Constant.AlertMessageStatus.FIRING.getValue());
						message.setValue(values.getString(1));
						messages.add(message);
						evalMessages.add(message);
						if (log.isDebugEnabled()) {
							log.debug("add new alert message,state: active,content : {}",
									Tool.JSONUtil.toJsonStr(message));
						}
						continue;
					}
				}

				log.info("alert message active size: {}", messages.size());
			}

			// 遍历已有 告警消息，判断是否已经过期
			long nowTs = Tool.DateUtil.current() - (alertRuleEntity.getInr() * 500);
			Iterator<String> iterator = entries.keySet().iterator();
			while (iterator.hasNext()) {
				String activeHash = iterator.next();
				if (!newAlertHash.contains(activeHash)) {
					Long firstTime = Tool.NumberUtil.parseLong(entries.get(activeHash));
					if (nowTs - firstTime > alertRuleEntity.getLast() * 1000) {
						// 说明之前发送过 告警消息，需要发送已经失效的告警消息，endAt < now
						AlertMessageReceiveDTO message = this.genAlertMessageFromVector(alertRuleEntity,
								Tool.JSONUtil.parseObj(activeHash), Tool.StrUtil.EMPTY,
								Tool.AlertUtil.formatMessageDate(firstTime), Tool.AlertUtil.formatMessageDate(nowTs));
						message.setActs(nowTs);
						message.setState(Constant.AlertMessageStatus.INACTIVE.getValue());
						message.setValue(Tool.StrUtil.EMPTY);
						messages.add(message);
						evalMessages.add(message);
						if (log.isDebugEnabled()) {
							log.debug("add new alert message,state: expired,content : {}",
									Tool.JSONUtil.toJsonStr(message));
						}
					}
					// 没有 到达 for 时长，还没有发送过告警消息，可以直接删除
					hashOperations.delete(ruleCacheKey, activeHash);
				}
			}
			if (messages.size() > 0) {
				alertMessageService.leftPushAlertMessageToRedis(Tool.JSONUtil.toJsonStr(messages));
			}
			if (evalMessages.size() > 0) {
				evalLog.setMessages(evalMessages);
			}
			log.info("judge end,ruleId:{},interval:{}s,type:{},aletmessagesize:{}", alertRuleId,
					alertRuleEntity.getInr(), type, messages.size());
		} catch (Exception e) {
			log.error(e, "judge error");
			if (e instanceof NZException) {
				evalLog.setState(((NZException) e).getCode());
				evalLog.setMsg(((NZException) e).getMsg());
			} else {
				evalLog.setState(999);
				evalLog.setMsg(Tool.StrUtil.subPre(e.getMessage(), 128));
			}
		} finally {
			if (redisTemplate.hasKey(ruleCacheKey)) {
				// hash 缓存设置超时时间，防止 意外不能删除, interval + 5min
				redisTemplate.expire(ruleCacheKey, alertRuleEntity.getInr() + 5 * 60, TimeUnit.SECONDS);
			}
			try {
				// 保存执行日志
				evalLog.setEtts(Tool.DateUtil.current());
				evalLogService.save(evalLog);
			} catch (Exception e) {
				log.warn(e, "save alert rule eval log error");
			}
		}
		stopWatch.stop();
		long timeMillis = stopWatch.getTotalTimeMillis();
		log.info("judge thread ruleId:{},time: {}ms", alertRuleId, timeMillis);

	}

	/**
	 * 查询结果
	 * 
	 * @param agent
	 * @param rule
	 * @return
	 */
	private JSONObject queryVetor(Promserver agent, AlertRuleEntity rule) {
		// 组合表达式
		String expression = rule.getExpression();
		// type 类型，1：metrics，2：logs
		Integer type = rule.getType();

		Long queryTime = null;
		if (this.queryOffsetSec != 0) {
			queryTime = Tool.DateUtil.currentSeconds() + this.queryOffsetSec;
			if (type == 2) {
				// loki 查询单位 ns
				queryTime = queryTime * 1000 * 1000;
			}
		}

		String path = null, queryString = null;
		if (type == 1) {
			String promPathPrefix = promService.getPromPathPrefix();
			path = String.format("%s/api/v1/query", promPathPrefix);
			queryString = String.format("query=%s&time=%s", expression, queryTime == null ? "" : queryTime);
		} else if (type == 2) {
			String agentPathPrex = sysConfigService.getValueOrDefault(Constant.CONFAGENT_PATH, "");
			path = String.format("%s/loki/proxy/loki/api/v1/query", agentPathPrex);
			queryString = String.format("query=%s&limit=1&time=%s", expression, queryTime == null ? "" : queryTime);
		}
		JSONObject jsonObject = promService.requestAgent(agent, path, queryString);
		return jsonObject;
	}

	/**
	 * 根据告警规则 和 查询结果 改造 alert message
	 * 
	 * @param labels
	 * @param value
	 * @param rule
	 * @return
	 */
	@SuppressWarnings("all")
	private AlertMessageReceiveDTO genAlertMessageFromVector(AlertRuleEntity alertRuleEntity, Map labels, String value,
			String startAt, String endAt) {
		AlertMessageReceiveDTO dto = new AlertMessageReceiveDTO();
		// 添加label
		String ruleType = alertRuleEntity.getType() == 1 ? "metrics" : "logs";
		labels.put("rule_type", ruleType);
		labels.put("alertname", alertRuleEntity.getId().toString());
		if (alertRuleEntity.getSeverity() != null) {
			labels.put("severity", alertRuleEntity.getSeverity().getName());
		}
		labels.put("severity_id", alertRuleEntity.getSeverityId().toString());
		dto.setLabels(new LinkedHashMap<String, String>(labels));
		dto.setStartsAt(startAt);
		dto.setEndsAt(endAt);
		// 渲染 annotation
		String summary = alertRuleEntity.getSummary();
		String description = alertRuleEntity.getDescription();
		HashMap<String, String> anno = Tool.MapUtil.newHashMap(2);
		Map<String, Object> context = Tool.MapUtil.newHashMap();
		context.put("labels", labels);
		context.put("alertRule", alertRuleEntity);
		context.put("value", value);
		summary = Tool.AlertUtil.renderAlertRuleAnnotations(summary, context);
		description = Tool.AlertUtil.renderAlertRuleAnnotations(description, context);
		anno.put("summary", summary);
		anno.put("description", description);
		dto.setAnnotations(anno);
		return dto;
	}

	

}
