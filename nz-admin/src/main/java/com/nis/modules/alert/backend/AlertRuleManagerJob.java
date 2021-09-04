package com.nis.modules.alert.backend;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.SpringContextUtils;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.service.MonitorEventLogService;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.sys.service.SysConfigService;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;

@Component
@DisallowConcurrentExecution
public class AlertRuleManagerJob extends QuartzJobBean {
	private static final Log log = Log.get();
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
	@Autowired
	private MonitorEventLogService eventLogService;
	@Autowired
	private AlertRuleService ruleService;
	@Autowired
	private SysConfigService configService;

	private static final String RULE_EVENT_LOG_KEY = "rule";
	/**
	 * 1：metrics type 2：logs type
	 */
	private static final List<Integer> METRIC_LOG_TYPE = Tool.ListUtil.of(1, 2);

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		// 检查 haMode
		String serverId = (String) redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
		// 获取当前 event_log.rule 值
		Integer event = eventLogService.getByEvent(RULE_EVENT_LOG_KEY);
		if (event == null) {
			log.warn("monitor_event_log can not find rule!");
			return;
		}
		// 比较当前缓存 rule 记录和 数据库获取的是否一致
		if (Tool.ObjectUtil.equal(AlertContext.getCurrentIndex(), event)) {
			log.info("monitor_event_log rule is not change,current value:{}", event);
			return;
		}
		//只要 event_log.rule 变化就更新 首先更新 snmp trap rule,目前所有设备都参与 snmp rule 规则判断
		this.updateSnmptrapRule();
		// nezha alert rule 规则判断是否启用
		String enable = configService.getValueOrDefault("nz_alert_rule_eval_enable", "0");
		log.info("nz alert rule eval enable : {}", enable);
		if (!StrUtil.equals(serverId, Constant.SERVER_ID) || !Tool.StrUtil.equals(enable, "1")) {
			log.debug("serverId is not me");
			// 检查 AlertContext 注册表是否有 运行线程
			Map<String, AlertRuleJudgeRunnable> ruleMap = AlertContext.getAlertRuleMap();
			if (ruleMap.size() > 0) {
				Set<String> keySet = ruleMap.keySet();
				log.debug("delete run alert judge thread start");
				for (String hashId : keySet) {
					// 不需要删除 redis key
					AlertContext.unRegisterJudgeRunnable(hashId, false);
				}
				log.debug("delete run alert judge thread end");
			}
			return;
		}

		
		StopWatch stopWatch = Tool.DateUtil.createStopWatch();
		stopWatch.start();
		/**
		 * 以下为alert rule 变化了的逻辑
		 */
		// 获取 全量 state == 1 alert rule，只查找 metrics，logs 类型rule type
		List<AlertRuleEntity> allRuleList = ruleService
				.list(new LambdaQueryWrapper<AlertRuleEntity>().eq(AlertRuleEntity::getState, 1).in(AlertRuleEntity::getType, METRIC_LOG_TYPE));
		// 计算 alertRule hash 值，整理成 map形式，便于后面查找
		Map<String, AlertRuleEntity> newAlertRuleMap = this.transAlertRuleListToMap(allRuleList);
		//当前生效的alert rule judge
		Map<String, AlertRuleJudgeRunnable> alertRuleMap = AlertContext.getAlertRuleMap();
		/*
		 * 更新 alert rule judge 线程 1、停掉已经不存在的 alert rule 2、启动 新增的alert rule judge线程
		 */
		// 停掉已经不存在的 alert rule judge 线程
		for (Entry<String, AlertRuleJudgeRunnable> en : alertRuleMap.entrySet()) {
			String curHash = en.getKey();
			if (!newAlertRuleMap.containsKey(curHash)) {
				AlertRuleJudgeRunnable judgeRunnable = en.getValue();
				AlertContext.unRegisterJudgeRunnable(curHash, true);
				log.info("unRegisterJudgeRunnable success: ruleId : {}", judgeRunnable.getAlertRuleEntity().getId());
			}
		}
		// 添加并启动 新增 alert rule judage
		for (Entry<String, AlertRuleEntity> en : newAlertRuleMap.entrySet()) {
			String newHash = en.getKey();
			if (!alertRuleMap.containsKey(newHash)) {
				AlertRuleEntity rule = en.getValue();
				Integer type = rule.getType();
				// 只有状态为 1 的才需要 做告警判断
				if (METRIC_LOG_TYPE.contains(type)) {
					AlertRuleJudgeRunnable judgeRunnable = SpringContextUtils.getBean(AlertRuleJudgeRunnable.class);
					judgeRunnable.setAlertRuleEntity(rule);
					AlertContext.registerJudgeRunnable(newHash, judgeRunnable);
					log.info("registerJudgeRunnable success: ruleId : {}", rule.getId());
				}
			}
		}
		// 更新currentIndex
		AlertContext.setCurrentIndex(event);
		stopWatch.stop();
		long timeMillis = stopWatch.getTotalTimeMillis();
		log.info("update current index success,currentIndex: {},time:{}ms", event, timeMillis);
	}

	/**
	 * 将 alertrule 转换为 map
	 * @param allRuleList
	 * @return
	 */
	private Map<String, AlertRuleEntity> transAlertRuleListToMap(List<AlertRuleEntity> allRuleList) {
		Map<String,AlertRuleEntity> newAlertRuleMap = Tool.MapUtil.newHashMap();
		if (allRuleList != null) {
			int defaultInterval = Tool.NumberUtil.parseInt(configService.getValueOrDefault("default_scrape_interval", "30"));
			int defaultTimeout = Tool.NumberUtil.parseInt(configService.getValueOrDefault("default_scrape_timeout", "30"));
			for (AlertRuleEntity entity : allRuleList) {
				// 计算 alertrule hash值
				String hashId = entity.getAlertRuleHashId();
				// 判断是否需要设置默认执行周期
				Integer interval = entity.getInr();
				if (interval == null || interval == 0) {
					entity.setInr(defaultInterval);
				}
				// 判断是否需要设置默认执行超时时间
				Integer timeout = entity.getTimeout();
				if (timeout == null || timeout == 0) {
					entity.setTimeout(defaultTimeout);
				}
				newAlertRuleMap.put(hashId, entity);
			}
		}
		return newAlertRuleMap;
	}

	/**
	 * 更新 snmptrap 告警规则
	 */
	public void updateSnmptrapRule() {
		// 获取 全量 state == 1 type = 3 的 alert rule
		List<AlertRuleEntity> allRuleList = ruleService.list(new LambdaQueryWrapper<AlertRuleEntity>()
				.eq(AlertRuleEntity::getState, 1).eq(AlertRuleEntity::getType, 3));
		// 首先清空所有 snmp trap 规则
		AlertContext.ALL_SNMP_ALERT_RULE.clear();
		if (allRuleList != null && !allRuleList.isEmpty()) {
			Map<String, AlertRuleEntity> snmptrapRule = Tool.MapUtil.newHashMap();
			allRuleList.forEach((rule) -> {
				String expr = rule.getExpr().trim();
				expr = expr.startsWith(".") ? expr.substring(1) : expr;
				if (Tool.StrUtil.isNotBlank(expr)) {
					snmptrapRule.put(rule.getExpr(), rule);
				}
			});
			// 重新添加
			AlertContext.ALL_SNMP_ALERT_RULE.putAll(snmptrapRule);
			if (log.isDebugEnabled()) {
				log.debug("update snmp trap rules: {}", Tool.JSONUtil.toJsonStr(snmptrapRule));
			}
			log.info("update snmp trap rule success,size:{}", snmptrapRule.size());
		}
	}

}
