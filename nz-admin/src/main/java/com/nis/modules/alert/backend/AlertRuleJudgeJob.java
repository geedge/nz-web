package com.nis.modules.alert.backend;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.date.StopWatch;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.exception.NZException;
import com.nis.common.protobuf.Types.Label;
import com.nis.common.protobuf.Types.Sample;
import com.nis.common.protobuf.Types.TimeSeries;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.common.utils.LocalCacheManager;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.LokiApiService;
import com.nis.modules.agent.service.PromApiService;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.entity.AlertRuleEntity.ConditionItem;
import com.nis.modules.alert.entity.AlertRuleEvalLog;
import com.nis.modules.alert.entity.receive.AlertMessageReceiveDTO;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.alert.service.AlertRuleEvalLogService;
import com.nis.modules.alert.service.RecordRuleService;
import com.nis.modules.election.dao.LeaderElectionDao;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@DisallowConcurrentExecution
public class AlertRuleJudgeJob extends QuartzJobBean {
	private static final Log log = Log.get();

	@Autowired
	private PromApiService promApiService;

	@Autowired
	private LokiApiService lokiApiService;

	@Autowired
	private AlertRuleEvalLogService evalLogService;

	@Autowired
	private AlertMessageService alertMessageService;
	
	@Autowired
	private RecordRuleService recordRuleService;

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
		// 检查 HA_LOCK
		LeaderElectionDao dao = LeaderElectionDao.getInstance();
		if (Tool.BooleanUtil.negate(dao.isLeader(Constant.SYS_HA_LOCK, Constant.SERVER_ID))) {
			log.info("skip execute, SERVER_ID: {}, ruleId: {}",Constant.SERVER_ID, alertRuleId);
			return;
		}

		StopWatch stopWatch = Tool.DateUtil.createStopWatch();
		stopWatch.start();
		// type 类型，1：metrics，2：logs
		Integer type = alertRuleEntity.getType();
		log.info("judge start,ruleId:{},interval:{}s,type:{}", alertRuleId, alertRuleEntity.getInr(), type);
		// 获取当前所有缓存的keys
		String ruleCacheKey = AlertContext.getCacheKey(alertRuleId);
		AlertRuleEvalLog evalLog = new AlertRuleEvalLog();
		evalLog.setRuleId(alertRuleId);
		evalLog.setStts(Tool.DateUtil.current());
		evalLog.setState(200);
		evalLog.setMsg("success");
		try {
			/*
			 * 1、获取 当前可用 agent 2、query 3、根据返回结果处理
			 * 
			 */
			// 获取可用 globa agent
			Agent agent = promApiService.getOneAvaliableGlobalAgent();
			if (Tool.ObjectUtil.isNull(agent)) {
				throw new NZException("No health agent available", 999);
			}

			// 查询结果 1、结果不能为空 2、结果状态 必须为 success 3、结果数据类型必须为 vector
			JSONObject result = this.queryVetor(alertRuleEntity);
			ValidateUtils.is(result).notNull(RCode.ALERT_JUDGE_RESULT_ISNULL).and(result.getString("status"))
					.equalsIgnoreCase("success", RCode.ALERT_JUDGE_RESULT_FAILE)
					.and(result.getJSONObject("data").getString("resultType"))
					.equalsIgnoreCase("vector", RCode.ALERT_JUDGE_RESULT_TYPE_ERROR);

			// 获取 data 结果
			JSONArray dataArr = result.getJSONObject("data").getJSONArray("result");
			// 当前所有触发过的 告警消息，包括 ：pending，active
			TimedCache<String, Object> cache = LocalCacheManager.getTimedCache(Constant.ALERT_CACHE_PREFIX, ruleCacheKey);

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
					// 当前值，用于告警级别判断
					String curVal = values.getString(1);
					/*1、首先判断是否符合告警条件*/
					List<ConditionItem> condition = alertRuleEntity.getConditionItems();
					Integer severityId = null; // 记录最高告警级别
					for(ConditionItem item : condition) {
						if(item.validate(curVal)) {
							severityId = item.getId();
							break;
						}
					}
					
					String hash = metric.toJSONString();
					if (severityId != null) {
						newAlertHash.add(hash);// 下一步 判断 expired 的告警消息
					}
					// 根据 firstTime 是否为null 判断是否第一次符合条件
					Long firstTime = Optional.ofNullable(cache.get(hash)).map(String::valueOf).map(Long::valueOf).orElse(null);
					if (severityId != null && firstTime == null) {// 符合告警条件，且第一次达到告警阈值
						// 将第一次 触发的时间 作为 value 保存，后续根据此计算 达到 for 的条件
						// hash 缓存设置超时时间，防止 意外不能删除, interval + 5min
						cache.put(hash, timestamp.toString(), TimeUnit.SECONDS.toMillis(alertRuleEntity.getInr() + 5 * 60));
						AlertMessageReceiveDTO message = this.genAlertMessageFromVector(alertRuleEntity, severityId, metric, values.getString(1),
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
					
					if (severityId != null && firstTime != null && (timestamp - firstTime > alertRuleEntity.getLast() * 1000)) {
						// 用户设置了 开启了自动失效，且 设置了 timeout
						String et = (timeout > 0) ? Tool.AlertUtil.formatMessageDate(timestamp + timeout * 1000) : null;
						AlertMessageReceiveDTO message = this.genAlertMessageFromVector(alertRuleEntity, severityId, metric,
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
			Iterator<String> iterator = cache.keySet().iterator();
			while (iterator.hasNext()) {
				String activeHash = iterator.next();
				if (!newAlertHash.contains(activeHash)) {
					Long firstTime = Optional.ofNullable(cache.get(activeHash)).map(String::valueOf).map(Long::valueOf).orElse(null);
					if (nowTs - firstTime > alertRuleEntity.getLast() * 1000) {
						// 说明之前发送过 告警消息，需要发送已经失效的告警消息，endAt < now
						AlertMessageReceiveDTO message = this.genAlertMessageFromVector(alertRuleEntity, null,
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
					cache.remove(activeHash);
				}
			}
			if (messages.size() > 0) {
				alertMessageService.leftPushAlertMessageToCache(Tool.JSONUtil.toJsonStr(messages));
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
			try {
				// 保存执行日志
				evalLog.setEtts(Tool.DateUtil.current());
				evalLogService.save(evalLog);
				//存一份数据到 prometheus
				Label recordLabel = Label.newBuilder().setName("id").setValue(String.valueOf(alertRuleId)).build();
				Label durationMetric = Label.newBuilder().setName("__name__").setValue("alert_rule_eval_duration").build();
				Label stateMetric = Label.newBuilder().setName("__name__").setValue("alert_rule_eval_state").build();
				Sample durationSample = Sample.newBuilder().setTimestamp(evalLog.getStts()).setValue(evalLog.getEtts() - evalLog.getStts()).build();
				Sample stateSample = Sample.newBuilder().setTimestamp(evalLog.getStts()).setValue(evalLog.getState()).build();
				TimeSeries durationTs = TimeSeries.newBuilder().addLabels(recordLabel).addLabels(durationMetric).addSamples(durationSample).build();
				TimeSeries stateTs = TimeSeries.newBuilder().addLabels(recordLabel).addLabels(stateMetric).addSamples(stateSample).build();
				recordRuleService.rightPushRecordToCache(Tool.ListUtil.of(durationTs,stateTs));
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
	 * @param rule
	 * @return
	 */
	private JSONObject queryVetor(AlertRuleEntity rule) {
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

		JSONObject jsonObject = new JSONObject();
		String time = queryTime == null ? "" : queryTime.toString();
		if (type == 1) {
			jsonObject = promApiService.query(expression, time, null);
		} else if (type == 2) {
			jsonObject = lokiApiService.query(expression, "1", time, null, null, null);
		}
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
	private AlertMessageReceiveDTO genAlertMessageFromVector(AlertRuleEntity alertRuleEntity,Integer severityId, Map labels, String value,
			String startAt, String endAt) {
		AlertMessageReceiveDTO dto = new AlertMessageReceiveDTO();
		LinkedHashMap alertLabels = new LinkedHashMap(labels);
		alertLabels.put("alertname", alertRuleEntity.getId().toString());
		alertLabels.put("rule_id", alertRuleEntity.getId().toString());
		alertLabels.put("rule_name", alertRuleEntity.getName());
		// 添加label
		String ruleType = alertRuleEntity.getType() == 1 ? "metrics" : "logs";
		alertLabels.put("rule_type", ruleType);
		if (severityId != null) {
			alertLabels.put("severity_id", severityId.toString());
		}
		alertLabels.remove("__name__");
		// 添加自定义标签
		List<Map<String, String>> extLabelList = alertRuleEntity.getExtLabelList();
		extLabelList.forEach(map -> {
			alertLabels.put(map.get("name"), map.get("value"));
		});
		dto.setLabels(alertLabels);
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
