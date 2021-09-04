package com.nis.modules.alert.backend;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
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

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.StopWatch;
import cn.hutool.log.Log;
import lombok.Data;

@Data
@Component
@Scope("prototype")
public class AlertRuleJudgeRunnable implements Runnable {
	private static final Log log = Log.get();
	private AlertRuleEntity alertRuleEntity;
	private volatile Future<?> future;
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
	// 运行标识
	private volatile boolean runFlag = true;
	// 是否告警规则删除
	private volatile boolean cleanCache = false;
	/**
	 * alert 状态 key
	 */
	private static final String ALERT_FOR_STATE_KEY_FORMAT = "alert_for_state:%s";

	/**
	 * alert rule 查询告警数据 向前偏移
	 */
	@Value("${alert.rule.query.offset:0}")
	private Long queryOffsetSec;
	/*
	 * query path 模板
	 */
	private final static Map<Integer, String> QUERY_PATH_FORMAT = Tool.MapUtil
			.builder(1, "%s/cortex/proxy/prometheus/api/v1/query?query=%s&time=%s") // 指标查询 path 模板
			.put(2, "%s/loki/proxy/loki/api/v1/query?query=%s&limit=1&time=%s").build(); // 日志查询path 模板

	public AlertRuleJudgeRunnable() {}

	@Override
	public void run() {
		Integer alertRuleId = alertRuleEntity.getId();
		Thread.currentThread().setName(String.format("alertRuleJuage-%s", alertRuleId));
		StopWatch stopWatch = Tool.DateUtil.createStopWatch();
		stopWatch.start();
		// type 类型，1：metrics，2：logs
		Integer type = alertRuleEntity.getType();
		log.info("judge start,ruleId:{},interval:{}s,type:{}", alertRuleId, alertRuleEntity.getInr(), type);
		// 获取当前所有缓存的keys
		String ruleCacheKey = this.getRedisCacheKey();
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
			if (dataArr != null && dataArr.size() > 0) {
				Iterator<Object> ite = dataArr.iterator();
				while (runFlag && ite.hasNext()) {
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
						continue;
					}
					/*
					 * 以下为不是第一次达到阈值的处理 1、判断是否达到 for 的时间 2、超过 for 时间 发送 告警消息
					 */
					Long firstTime = Tool.NumberUtil.parseLong(hashOperations.get(ruleCacheKey, hash));
					if (timestamp - firstTime > alertRuleEntity.getLast() * 1000) {
						// 直接将 告警消息 放到 redis 缓存 endTime + 5min
						AlertMessageReceiveDTO message = this.genAlertMessageFromVector(metric, values.getString(1),
								Tool.AlertUtil.formatMessageDate(firstTime), null);
						messages.add(message);
						if (log.isDebugEnabled()) {
							log.debug("add new alert message,state: active,content : {}",
									Tool.JSONUtil.toJsonStr(message));
						}
						continue;
					}
				}
				// 已经停止的rule 清空新生成的告警消息
				if (!runFlag) {
					messages.clear();
					newAlertHash.clear();
				}
			}

			// 遍历已有 告警消息，判断是否已经过期
			long nowTs = Tool.DateUtil.current();
			Iterator<String> iterator = entries.keySet().iterator();
			while (runFlag && iterator.hasNext()) {
				String activeHash = iterator.next();
				if (!newAlertHash.contains(activeHash)) {
					Long firstTime = Tool.NumberUtil.parseLong(entries.get(activeHash));
					if (nowTs - firstTime > alertRuleEntity.getLast() * 1000) {
						// 说明之前发送过 告警消息，需要发送已经失效的告警消息，endAt < now
						AlertMessageReceiveDTO message = this.genAlertMessageFromVector(
								Tool.JSONUtil.parseObj(activeHash), Tool.StrUtil.EMPTY,
								Tool.AlertUtil.formatMessageDate(firstTime), Tool.AlertUtil.formatMessageDate(nowTs));
						messages.add(message);
						if (log.isDebugEnabled()) {
							log.debug("add new alert message,state: expired,content : {}",
									Tool.JSONUtil.toJsonStr(message));
						}
						continue;
					}
					// 没有 到达 for 时长，还没有发送过告警消息，可以直接删除
					hashOperations.delete(ruleCacheKey, activeHash);
				}
			}
			if (runFlag && messages.size() > 0) {
				alertMessageService.leftPushAlertMessageToRedis(Tool.JSONUtil.toJsonStr(messages));
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
			if (!runFlag && cleanCache) {
				// 删除 redis hash
				try {
					redisTemplate.delete(ruleCacheKey);
				} catch (Exception e) {
					log.warn(e, "delete alert for state key error");
				}
				log.info("judge thread stop ,ruleId: {}", alertRuleId);
			} else {
				if (redisTemplate.hasKey(ruleCacheKey)) {
					// hash 缓存设置超时时间，防止 意外不能删除, interval + 5min
					redisTemplate.expire(ruleCacheKey, alertRuleEntity.getInr() + 5 * 60, TimeUnit.SECONDS);
				}
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
		log.debug("judge thread ruleId:{},time: {}ms,runFlag:{},cleanCache:{}", alertRuleId, timeMillis, runFlag,
				cleanCache);
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

		String agentPathPrex = sysConfigService.getValueOrDefault(Constant.CONFAGENT_PATH, "");
		String path = String.format(QUERY_PATH_FORMAT.get(type), agentPathPrex, expression,
				queryTime == null ? "" : queryTime);
		JSONObject jsonObject = promService.requestAgent(agent, path);
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
	private AlertMessageReceiveDTO genAlertMessageFromVector(Map labels, String value, String startAt, String endAt) {
		AlertMessageReceiveDTO dto = new AlertMessageReceiveDTO();
		// 添加label
		String ruleType = this.alertRuleEntity.getType() == 1?"metrics":"logs";
		labels.put("rule_type", ruleType);
		labels.put("alertname", this.alertRuleEntity.getId().toString());
		if (alertRuleEntity.getSeverity() != null) {
			labels.put("severity", this.alertRuleEntity.getSeverity().getName());
		}
		labels.put("severity_id", this.alertRuleEntity.getSeverityId().toString());
		dto.setLabels(new LinkedHashMap<String, String>(labels));
		dto.setStartsAt(startAt);
		dto.setEndsAt(endAt);
		// 渲染 annotation
		String summary = alertRuleEntity.getSummary();
		String description = alertRuleEntity.getDescription();
		HashMap<String, String> anno = Tool.MapUtil.newHashMap(2);
		Map<String, Object> context = Tool.MapUtil.newHashMap();
		context.put("label", labels);
		context.put("alertRule", this.alertRuleEntity);
		context.put("value", value);
		summary = Tool.AlertUtil.renderAlertRuleAnnotations(summary, context);
		description = Tool.AlertUtil.renderAlertRuleAnnotations(description, context);
		anno.put("summary", summary);
		anno.put("description", description);
		dto.setAnnotations(anno);
		return dto;
	}

	/**
	 * 将ms 转化为 utc 时间格式 yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
	 * 
	 * @param time
	 * @return
	 */


	public String getRedisCacheKey() {
		return String.format(ALERT_FOR_STATE_KEY_FORMAT, this.alertRuleEntity.getId());
	}

	public void stop(boolean cleanCache) {
		this.runFlag = false;
		future.cancel(true);
		if (cleanCache) {
			try {
				redisTemplate.delete(this.getRedisCacheKey());
			} catch (Exception e) {
				log.warn(e, "delete alert for state key error");
			}
		}

	}

}
