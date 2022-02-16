package com.nis.modules.alert.backend;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.service.MonitorEventLogService;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.sys.service.SysConfigService;

import cn.hutool.core.date.StopWatch;
import cn.hutool.log.Log;

/**
 * 所有 nz-web 实例都执行此定时任务
 * 
 * @author ThinkPad
 *
 */
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
	private Scheduler scheduler;

	private static final String RULE_EVENT_LOG_KEY = "rule";
	private static final String ALERT_RULE_SCHEDULE_GROUP = "alert_rule_schedule_group";
	private static final String ALERT_RULE_JUDGE_GROUP = "alert_rule_judge_group";

	/**
	 * 1：metrics type 2：logs type
	 */
	private static final List<Integer> METRIC_LOG_TYPE = Tool.ListUtil.of(1, 2);

	// 计数器，每 forceSync 个周期 执行一次强制执行
	private volatile static Integer counter = 1;

	@Value("${alert.rule.force.sync:30}")
	private Integer forceSync;
	
	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		Thread.currentThread().setName("AlertRuleManagerJob");
		scheduler = context.getScheduler();
		// 获取当前 event_log.rule 值
		Integer event = eventLogService.getByEvent(RULE_EVENT_LOG_KEY);
		if (event == null) {
			log.warn("monitor_event_log can not find rule!");
			return;
		}
		counter++;

		if (counter % forceSync == 0) {
			counter = 1;
			log.info("Force synchronization");
		} else if (Tool.ObjectUtil.equal(AlertContext.getCurrentIndex(), event)) {
			// 比较当前缓存 rule 记录和 数据库获取的是否一致
			log.info("monitor_event_log rule is not change,current value:{}", event);
			return;
		}

		/*
		 * 1、更新 snmp trap alert rule 只要 event_log.rule 变化就更新 首先更新 snmp trap
		 * rule,目前所有设备都参与 snmp rule 规则判断
		 */
		this.updateSnmptrapRule();

		/*
		 * 2、 更新 alert rule schedule
		 */
		this.updateAlertRuleSchedule();

		/*
		 * 3、更新 alert rule judge nezha alert rule 规则判断是否启用 a.未启用 nezha alert rule
		 * 时，取消所有alertrulejudgejob b.更新 alertrulejudgejob
		 */
		String enable = configService.getValueOrDefault("nz_alert_rule_eval_enable", "1");
		log.info("nz alert rule eval enable : {}", enable);
		StopWatch stopWatch = Tool.DateUtil.createStopWatch();
		stopWatch.start();
		if (!Tool.StrUtil.equals(enable, "1")) {
			try {
				GroupMatcher<JobKey> groupMatcher = GroupMatcher.groupEquals(ALERT_RULE_JUDGE_GROUP);
				// jobkey 规则：[hash]-[rule id],如 ：adakdfakdf-123
				Set<JobKey> jobKeys = scheduler.getJobKeys(groupMatcher);
				scheduler.deleteJobs(Tool.ListUtil.toList(jobKeys));
				log.info("delete alert rule judge job : {}", Tool.CollUtil.size(jobKeys));
			} catch (Exception e) {
				log.error(e, "delete alert rule judge job error");
			}
		} else {
			// 更新 alert rule judge
			this.updateAlertRuleJudgeJob();
		}
		stopWatch.stop();
		// 更新currentIndex
		AlertContext.setCurrentIndex(event);
		long timeMillis = stopWatch.getTotalTimeMillis();
		log.info("update current index success,currentIndex: {},time:{}ms", event, timeMillis);
	}

	/**
	 * 更新 alert rule judge job 1、获取最新 alert rule 2、获取 正在执行 alert rule judge 3、取消已删除
	 * alert rule judge 4、新增 alert rule judge
	 * 
	 * @throws SchedulerException
	 */
	private void updateAlertRuleJudgeJob() {
		try {
			/**
			 * 以下为alert rule 变化了的逻辑
			 */
			// 获取 全量 state == 1 alert rule，只查找 metrics，logs 类型rule type
			List<AlertRuleEntity> allRuleList = ruleService.list(new LambdaQueryWrapper<AlertRuleEntity>()
					.eq(AlertRuleEntity::getState, 1).in(AlertRuleEntity::getType, METRIC_LOG_TYPE));
			/*
			 * 计算 alertRule hash 值，整理成 map形式，便于后面查找 key: hash value: alertRuleEntity
			 */
			Map<String, AlertRuleEntity> newAlertRuleMap = this.transAlertRuleListToMap(allRuleList);
			GroupMatcher<JobKey> groupMatcher = GroupMatcher.groupEquals(ALERT_RULE_JUDGE_GROUP);
			// jobkey 规则：[hash]-[rule id],如 ：adakdfakdf-123
			Set<JobKey> jobKeys = scheduler.getJobKeys(groupMatcher);
			Map<String, Integer> hashIdMap = Tool.MapUtil.newHashMap();
			jobKeys.forEach(key -> {
				String[] keyArr = key.getName().split("-");
				String hash = keyArr[0];
				Integer alertRuleId = Tool.NumberUtil.parseInt(keyArr[1]);
				hashIdMap.put(hash, alertRuleId);
			});
			log.info(Tool.JSONUtil.toJsonStr(hashIdMap));
			log.info(Tool.JSONUtil.toJsonStr(newAlertRuleMap));
			// 1、停掉已删除job
			for (Entry<String, Integer> en : hashIdMap.entrySet()) {
				if (!newAlertRuleMap.containsKey(en.getKey())) {
					try {
						log.info("delete alertRuleJudgeJob,rule id: {}", en.getValue());
						// 删除 alert rule judge job
						scheduler.deleteJob(this.getAlertRuleJudgeJobKey(en.getKey(), en.getValue()));
						// 清除缓存
						redisTemplate.delete(AlertContext.getRedisCacheKey(en.getValue()));
					} catch (Exception e) {
						log.warn(e, "delete alert for state key error");
					}
				}
			}

			// 2、新建 alert rule judage job
			for (Entry<String, AlertRuleEntity> en : newAlertRuleMap.entrySet()) {
				String newHash = en.getKey();
				if (!hashIdMap.containsKey(newHash)) {
					AlertRuleEntity rule = en.getValue();
					log.info("hash:{},rule:{}",newHash,rule.toString());
					Integer type = rule.getType();
					// 只有状态为 1 的才需要 做告警判断
					if (METRIC_LOG_TYPE.contains(type)) {
						JobKey jobKey = this.getAlertRuleJudgeJobKey(newHash, rule.getId());
						// 开启job
						boolean exists = scheduler.checkExists(jobKey);
						if (!exists) {
							JobDataMap dataMap = new JobDataMap();
							dataMap.put(AlertContext.JOB_DATA_KEY_ALERT_RULE_ENTITY, rule);
							JobDetail jobDetail = JobBuilder.newJob(AlertRuleJudgeJob.class).withIdentity(jobKey)
									.storeDurably().setJobData(dataMap).build();
							// 设置执行周期
							SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.repeatSecondlyForever(rule.getInr());
					        SimpleTrigger simpleTrigger = TriggerBuilder.newTrigger()
					                .forJob(jobDetail)
					                .withIdentity(jobKey.getName(), jobKey.getGroup())
					                .withSchedule(scheduleBuilder)
					                .startAt(Tool.DateUtil.date(System.currentTimeMillis() + Tool.RandomUtil.randomInt(10, 60)*1000)) //加入随机时间，分散alert rule 执行时间
					                .build();
							// 创建任务
							scheduler.scheduleJob(jobDetail, simpleTrigger);
						}
						log.info("create alert rule judge, ruleId: {},exists: {}", rule.getId(), exists);
					}
				}
			}
		} catch (Exception e) {
			log.error(e, "update alert rule judge error");
		}
	}

	/**
	 * 将 alertrule 转换为 map
	 * 
	 * @param allRuleList
	 * @return
	 */
	private Map<String, AlertRuleEntity> transAlertRuleListToMap(List<AlertRuleEntity> allRuleList) {
		Map<String, AlertRuleEntity> newAlertRuleMap = Tool.MapUtil.newHashMap();
		if (allRuleList != null) {
			int defaultInterval = Tool.NumberUtil
					.parseInt(configService.getValueOrDefault("default_scrape_interval", "30"));
			for (AlertRuleEntity entity : allRuleList) {
				// 判断是否需要设置默认执行周期
				Integer interval = entity.getInr();
				if (interval == null || interval == 0) {
					entity.setInr(defaultInterval);
				}
				// 判断是否需要设置默认执行超时时间
				if (entity.getAutoExpired() == 1 && entity.getTimeout() < entity.getInr()) {
					entity.setTimeout(entity.getInr() + entity.getTimeout());
				}
				newAlertRuleMap.put(entity.getAlertRuleHashId(), entity);
			}
		}
		return newAlertRuleMap;
	}

	/**
	 * 更新 snmptrap 告警规则
	 */
	public void updateSnmptrapRule() {
		try {
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
		} catch (Exception e) {
			log.error(e, "update snmp trap rule error");
		}
	}

	/**
	 * 更新 alert rule scheduler
	 * 
	 */
	public void updateAlertRuleSchedule() {
		GroupMatcher<JobKey> groupMatcher = GroupMatcher.groupEquals(ALERT_RULE_SCHEDULE_GROUP);
		try {
			// 查询 所有 schedule enable 的alert rule
			List<AlertRuleEntity> allRuleList = ruleService
					.list(new LambdaQueryWrapper<AlertRuleEntity>().eq(AlertRuleEntity::getSchedEnable, 1));
			Map<String, AlertRuleEntity> alertRuleMap = this.transAlertRuleListToMap(allRuleList);
			Set<JobKey> jobKeys = scheduler.getJobKeys(groupMatcher);
			// 先删除已经变化的 alert rule
			for (JobKey key : jobKeys) {
				// key格式：hash-id-state，例：adcdaed-12-1
				String[] split = key.getName().split("-");
				if (alertRuleMap.containsKey(split[0])) {
					continue;
				}
				try {
					scheduler.deleteJob(key);
					log.info("alert rule schedule stop,id:{}", split[1]);
				} catch (Exception e) {
					log.error(e, "alert rule schedule stop error");
				}
			}

			// 发生更新 的alert rule，判断是否添加到定时器
			Set<Entry<String, AlertRuleEntity>> entrySet = alertRuleMap.entrySet();
			for (Entry<String, AlertRuleEntity> en : entrySet) {
				String hash = en.getKey();
				AlertRuleEntity rule = en.getValue();
				if (rule.getSchedEnable() == 0) {// schedule 关闭
					continue;
				}
				this.checkAndUpdateAlertRuleSchedule(hash, rule);
			}

		} catch (Exception e1) {
			log.error(e1, "alert rule schedule error");
		}
	}

	public void checkAndUpdateAlertRuleSchedule(String hash, AlertRuleEntity rule) throws SchedulerException {
		// 判断是否已经存在
		String on = String.format("%s-%s-%s", hash, rule.getId(), 1);
		String off = String.format("%s-%s-%s", hash, rule.getId(), 0);
		JobKey onJobKey = new JobKey(on, ALERT_RULE_SCHEDULE_GROUP);
		JobKey offJobKey = new JobKey(off, ALERT_RULE_SCHEDULE_GROUP);
		// 开启job
		boolean exists = scheduler.checkExists(onJobKey);
		if (!exists) {
			JobDetail jobDetail = JobBuilder.newJob(AlertRuleScheduleJob.class).withIdentity(onJobKey).storeDurably()
					.build();
			String jobCronExpression = this.genCron(rule.getSchedStime(), rule.getSchedDays());
			CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(jobCronExpression);
			CronTrigger cronTrigger = TriggerBuilder.newTrigger().forJob(jobDetail).withSchedule(cronScheduleBuilder)
					.build();
			// 创建任务
			scheduler.scheduleJob(jobDetail, cronTrigger);
		}

		// 关闭job
		exists = scheduler.checkExists(offJobKey);
		if (!exists) {
			JobDetail jobDetail = JobBuilder.newJob(AlertRuleScheduleJob.class).withIdentity(offJobKey).storeDurably()
					.build();
			String jobCronExpression = this.genCron(rule.getSchedEtime(), rule.getSchedDays());
			CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(jobCronExpression);
			CronTrigger cronTrigger = TriggerBuilder.newTrigger().forJob(jobDetail).withSchedule(cronScheduleBuilder)
					.build();
			// 创建任务
			scheduler.scheduleJob(jobDetail, cronTrigger);
		}
	}

	/**
	 * 
	 * @param time 时间，00:00
	 * @param days 星期，多个逗号分隔
	 * @return
	 */
	public String genCron(String time, String days) {
		if (Tool.StrUtil.isAllNotBlank(time, days)) {
			String[] split = time.split(":");
			// 将定时时间 根据系统设置 转换为 utc 对应时间
			String value = configService.getValue("timezone");
			TimeZone timezone = TimeZone.getTimeZone(value);
			Calendar cal = Calendar.getInstance();
			int hourOfDay = Tool.NumberUtil.parseInt(split[0]);
			int minute = Tool.NumberUtil.parseInt(split[1]);
			cal.set(1997, 7, 1, hourOfDay, minute);
			cal.add(Calendar.MILLISECOND, 0 - timezone.getRawOffset());
			hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
			minute = cal.get(Calendar.MINUTE);
			// 0 1 2 0 0 1,2,4
			return String.format("0 %s %s ? * %s *", minute, hourOfDay, days);
		}
		return null;
	}

	private JobKey getAlertRuleJudgeJobKey(String hash, Integer id) {
		String name = String.format("%s-%s", hash, id);
		JobKey jobkey = new JobKey(name, ALERT_RULE_JUDGE_GROUP);
		return jobkey;
	}
}
