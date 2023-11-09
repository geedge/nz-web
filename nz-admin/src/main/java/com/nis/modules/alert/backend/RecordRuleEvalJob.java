package com.nis.modules.alert.backend;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.exception.NZException;
import com.nis.common.protobuf.Types.Label;
import com.nis.common.protobuf.Types.Sample;
import com.nis.common.protobuf.Types.TimeSeries;
import com.nis.common.protobuf.Types.TimeSeries.Builder;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.LokiApiService;
import com.nis.modules.agent.service.PromApiService;
import com.nis.modules.alert.entity.RecordRuleEntity;
import com.nis.modules.alert.entity.RecordRuleEvalLog;
import com.nis.modules.alert.service.RecordRuleEvalLogService;
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

import java.util.List;
import java.util.Map;

@Component
@DisallowConcurrentExecution
public class RecordRuleEvalJob extends QuartzJobBean {
	private static final Log log = Log.get();

	@Autowired
	private PromApiService promApiService;

	@Autowired
	private LokiApiService lokiApiService;

	@Autowired
	private RecordRuleEvalLogService evalLogService;

	@Autowired
	private RecordRuleService recordRuleService;

	/**
	 * record rule 查询告警数据 向前偏移
	 */
	@Value("${record.rule.query.offset:0}")
	private Long queryOffsetSec;
	// 有效的 resultType
	private List<String> validResultTypeList = Tool.ListUtil.of("matrix", "vector", "scalar");

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
		RecordRuleEntity recordRuleEntity = (RecordRuleEntity) jobDataMap
				.get(RecordManagerJob.JOB_DATA_KEY_RECORD_RULE_ENTITY);
		Integer recordRuleId = recordRuleEntity.getId();
		Thread.currentThread().setName(String.format("RecordRuleEvalJob-%s", recordRuleId));
		// 检查 HA_LOCK
		LeaderElectionDao dao = LeaderElectionDao.getInstance();
		if (BooleanUtil.negate(dao.isLeader())) {
			log.info("skip execute, SERVER_ID: {}, ruleId: {}", Constant.SERVER_ID, recordRuleId);
			return;
		}

		StopWatch stopWatch = Tool.DateUtil.createStopWatch();
		stopWatch.start();
		// type 类型，1：metrics，2：logs
		Integer type = recordRuleEntity.getType();
		log.info("RecordRuleEvalJob start,ruleId:{},interval:{}s,type:{}", recordRuleId, recordRuleEntity.getInr(),
				type);

		RecordRuleEvalLog evalLog = new RecordRuleEvalLog();
		evalLog.setRuleId(recordRuleId);
		evalLog.setStts(Tool.DateUtil.current());
		evalLog.setState(200);
		evalLog.setMsg("success");
		try {
			/*
			 * 1、获取 当前可用 agent 2、query
			 * 
			 */
			// 获取可用 globa agent
			Agent agent = promApiService.getOneAvaliableGlobalAgent();
			if (Tool.ObjectUtil.isNull(agent)) {
				throw new NZException("No health agent available", 999);
			}

			// 查询结果 1、结果不能为空 2、结果状态 必须为 success 3、结果数据类型必须为 vector,matrix,scalar
			JSONObject result = this.queryVetor(agent, recordRuleEntity);
			ValidateUtils.is(result).notNull(RCode.ALERT_JUDGE_RESULT_ISNULL).and(result.getString("status"))
					.equalsIgnoreCase("success", RCode.ALERT_JUDGE_RESULT_FAILE);
			String resultType = result.getJSONObject("data").getString("resultType");
			if (!Tool.CollUtil.contains(validResultTypeList, resultType)) {
				throw new NZException(RCode.ALERT_JUDGE_RESULT_TYPE_ERROR);
			}

			// 获取 data 结果
			JSONArray dataArr = result.getJSONObject("data").getJSONArray("result");
			if (dataArr.size() > 0) {
				// 处理结果，修改__name__ value, 格式改为 matrix
				List<TimeSeries> timeSeries = this.coverToTimeSeries(result, recordRuleEntity);
				if (timeSeries.size() > 0) {
					// 存放到 redis
					recordRuleService.rightPushRecordToRedis(timeSeries);
				}
			}
			log.info("RecordRuleEvalJob end, ruleId:{}, interval:{}s, type:{}, result size: {}", recordRuleId,
					recordRuleEntity.getInr(), type, dataArr.size());
		} catch (Exception e) {
			log.error(e, "record rule eval error");
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
				Label recordLabel = Label.newBuilder().setName("id").setValue(String.valueOf(recordRuleId)).build();
				Label durationMetric = Label.newBuilder().setName("__name__").setValue("record_rule_eval_duration").build();
				Label stateMetric = Label.newBuilder().setName("__name__").setValue("record_rule_eval_state").build();
				Sample durationSample = Sample.newBuilder().setTimestamp(evalLog.getStts()).setValue(evalLog.getEtts() - evalLog.getStts()).build();
				Sample stateSample = Sample.newBuilder().setTimestamp(evalLog.getStts()).setValue(evalLog.getState()).build();
				TimeSeries durationTs = TimeSeries.newBuilder().addLabels(recordLabel).addLabels(durationMetric).addSamples(durationSample).build();
				TimeSeries stateTs = TimeSeries.newBuilder().addLabels(recordLabel).addLabels(stateMetric).addSamples(stateSample).build();
				recordRuleService.rightPushRecordToRedis(Tool.ListUtil.of(durationTs,stateTs));
			} catch (Exception e) {
				log.warn(e, "save record rule eval log error");
			}
		}
		stopWatch.stop();
		long timeMillis = stopWatch.getTotalTimeMillis();
		log.info("RecordRuleEvalJob ruleId:{},time: {}ms", recordRuleId, timeMillis);
	}

	/**
	 * 查询结果
	 * 
	 * @param agent
	 * @param rule
	 * @return
	 */
	private JSONObject queryVetor(Agent agent, RecordRuleEntity rule) {
		// 组合表达式
		String expression = rule.getExpr();
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
	 * 将 query 结果转为 matrix 格式
	 * 
	 * @param result
	 * @param recordRule
	 * @return
	 */
	private List<TimeSeries> coverToTimeSeries(JSONObject queryResult, RecordRuleEntity recordRule) {
		List<TimeSeries> result = Tool.ListUtil.list(false);
		// 获取 resultType
		String resultType = queryResult.getJSONObject("data").getString("resultType");
		// 获取 data 结果
		JSONArray dataArr = queryResult.getJSONObject("data").getJSONArray("result");
		switch (resultType) {
		case "matrix":
			for (int i = 0, j = dataArr.size(); i < j; i++) {
				JSONObject jsonObject = dataArr.getJSONObject(i);
				JSONArray valuesJsonArr = jsonObject.getJSONArray("values");
				if (valuesJsonArr.size() < 1) {
					continue;
				}
				JSONObject metricJson = jsonObject.getJSONObject("metric");
				Builder tsBuilder = TimeSeries.newBuilder();
				// 修改 labels
				List<Label> labels = this.getLabels(metricJson, recordRule);
				tsBuilder.addAllLabels(labels);
				for (Object objValue : valuesJsonArr) {
					JSONArray value = (JSONArray) objValue;
					Sample sample = Sample.newBuilder().setTimestamp((long) (value.getDoubleValue(0) * 1000))
							.setValue(value.getDoubleValue(1)).build();
					tsBuilder.addSamples(sample);
				}
				result.add(tsBuilder.build());
			}
			break;
		case "vector":
			for (int i = 0, j = dataArr.size(); i < j; i++) {
				JSONObject jsonObject = dataArr.getJSONObject(i);
				JSONArray value = jsonObject.getJSONArray("value");
				if (value.size() < 1) {
					continue;
				}
				JSONObject metricJson = jsonObject.getJSONObject("metric");
				Builder tsBuilder = TimeSeries.newBuilder();
				// 修改 labels
				List<Label> labels = this.getLabels(metricJson, recordRule);
				tsBuilder.addAllLabels(labels);
				Sample sample = Sample.newBuilder().setTimestamp((long) (value.getDoubleValue(0) * 1000))
						.setValue(value.getDoubleValue(1)).build();
				tsBuilder.addSamples(sample);
				result.add(tsBuilder.build());
			}
			break;
		case "scalar":
			for (int i = 0, j = dataArr.size(); i < j; i++) {
				JSONObject jsonObject = dataArr.getJSONObject(i);
				JSONArray value = jsonObject.getJSONArray("value");
				if (value.size() < 1) {
					continue;
				}

				Builder tsBuilder = TimeSeries.newBuilder();
				// 修改 labels
				List<Label> labels = this.getLabels(null, recordRule);
				tsBuilder.addAllLabels(labels);
				Sample sample = Sample.newBuilder().setTimestamp((long) (value.getDoubleValue(0) * 1000))
						.setValue(value.getDoubleValue(1)).build();
				tsBuilder.addSamples(sample);
				result.add(tsBuilder.build());
			}
			break;
		}
		return result;
	}

	/**
	 * 修改 timeseries labels 和 指标名称
	 * 
	 * @param metric
	 * @param recordRule
	 * @return
	 */
	private List<Label> getLabels(Map<String, Object> metric, RecordRuleEntity recordRule) {
		List<Label> result = Tool.ListUtil.list(false);
		if (metric == null) {
			metric = Tool.MapUtil.newHashMap();
		}
		// 添加 用户自定义 标签
		String labels = recordRule.getLabels();
		if (Tool.StrUtil.isNotBlank(labels)) {
			Map<String, String> labelMap = Tool.JSONUtil.toBean(labels, Map.class);
			metric.putAll(labelMap);
		}
		metric.put("__name__", recordRule.getName());
		metric.forEach((k, v) -> {
			Label label = Label.newBuilder().setName(k).setValue((String) v).build();
			result.add(label);
		});
		// 修改 指标名称
		return result;
	}



}
