package com.nis.modules.alert.backend;

import cn.hutool.core.date.StopWatch;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.service.MonitorEventLogService;
import com.nis.modules.alert.entity.RecordRuleEntity;
import com.nis.modules.alert.service.RecordRuleService;
import com.nis.modules.sys.service.SysConfigService;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * record rule 配置更新管理任务
 * 1、获取有效的 record rule
 * 2、获取当前 运行的 record eval job
 * 3、判断新增，删除的 job
 * 
 * @author ThinkPad
 *
 */
@Component
@DisallowConcurrentExecution
public class RecordManagerJob extends QuartzJobBean {
	private static final Log log = Log.get();
	@Autowired
	private MonitorEventLogService eventLogService;
	@Autowired
	private RecordRuleService recordService;
	@Autowired
	private SysConfigService configService;
	private Scheduler scheduler;

	private static final String RECORD_EVENT_LOG_KEY = "record";
	private static final String RECORD_EVAL_GROUP = "record_eval_group";
	public static final String JOB_DATA_KEY_RECORD_RULE_ENTITY = "record_rule_entity";

	// 计数器，每 forceSync 个周期 执行一次强制执行
	private volatile static Integer counter = 1;
	// 缓存当前 event_log 配置
	private volatile static Integer currentIndex = null;
	@Value("${alert.rule.force.sync:30}")
	private Integer forceSync;

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		Thread.currentThread().setName("RecordManagerJob");
		scheduler = context.getScheduler();
		// 获取当前 event_log.rule 值
		Integer event = eventLogService.getByEvent(RECORD_EVENT_LOG_KEY);
		if (event == null) {
			log.warn("monitor_event_log can not find record!");
			return;
		}
		counter++;

		if (counter % forceSync == 0) {
			counter = 1;
			log.info("Force synchronization");
		} else if (Tool.ObjectUtil.equal(currentIndex, event)) {
			// 比较当前缓存 rule 记录和 数据库获取的是否一致
			log.info("monitor_event_log record is not change,current value:{}", currentIndex);
			return;
		}

		/*
		 * 更新 record eval job
		 */

		StopWatch stopWatch = Tool.DateUtil.createStopWatch();
		stopWatch.start();
		this.updateRecordEvalJob();
		stopWatch.stop();
		// 更新currentIndex
		currentIndex = event;
		long timeMillis = stopWatch.getTotalTimeMillis();
		log.info("update current index success,currentIndex: {},time:{}ms", event, timeMillis);
	}

	/**
	 * 更新 record eval job <br/>
	 * 
	 * 1、获取最新 record rule <br/>
	 * 2、获取 正在执行 record eval <br/>
	 * 3、取消已删除 record eval <br/>
	 * 4、新增 record eval
	 * 
	 * @throws SchedulerException
	 */
	private void updateRecordEvalJob() {
		try {

			// 获取 全量 state == 1 record rule
			List<RecordRuleEntity> allRuleList = recordService
					.list(new LambdaQueryWrapper<RecordRuleEntity>().eq(RecordRuleEntity::getState, 1));
			/*
			 * 计算 RecordRule hash 值，整理成 map形式，便于后面查找 key: hash value: RecordRuleEntity
			 */
			Map<String, RecordRuleEntity> newRecordRuleMap = this.transRecordRuleListToMap(allRuleList);
			GroupMatcher<JobKey> groupMatcher = GroupMatcher.groupEquals(RECORD_EVAL_GROUP);
			// jobkey 规则：[hash]-[rule id],如 ：adakdfakdf-123
			Set<JobKey> jobKeys = scheduler.getJobKeys(groupMatcher);
			Map<String, Integer> hashIdMap = Tool.MapUtil.newHashMap();
			jobKeys.forEach(key -> {
				String[] keyArr = key.getName().split("-");
				String hash = keyArr[0];
				Integer recordRuleId = Tool.NumberUtil.parseInt(keyArr[1]);
				hashIdMap.put(hash, recordRuleId);
			});
			// 1、停掉已删除job
			for (Entry<String, Integer> en : hashIdMap.entrySet()) {
				if (!newRecordRuleMap.containsKey(en.getKey())) {
					try {
						log.info("delete recordEvalJob,record rule id: {}", en.getValue());
						// 删除 alert rule judge job
						scheduler.deleteJob(this.getRecordEvalJobKey(en.getKey(), en.getValue()));
					} catch (Exception e) {
						log.warn(e, "delete recordEvalJob error");
					}
				}
			}

			// 2、新建 record eval job
			for (Entry<String, RecordRuleEntity> en : newRecordRuleMap.entrySet()) {
				String newHash = en.getKey();
				if (!hashIdMap.containsKey(newHash)) {
					RecordRuleEntity rule = en.getValue();
					log.info("hash:{},rule:{}", newHash, rule.toString());
					JobKey jobKey = this.getRecordEvalJobKey(newHash, rule.getId());
					// 开启job
					boolean exists = scheduler.checkExists(jobKey);
					if (!exists) {
						JobDataMap dataMap = new JobDataMap();
						dataMap.put(JOB_DATA_KEY_RECORD_RULE_ENTITY, rule);
						JobDetail jobDetail = JobBuilder.newJob(RecordRuleEvalJob.class).withIdentity(jobKey)
								.storeDurably().setJobData(dataMap).build();
						// 设置执行周期
						SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder
								.repeatSecondlyForever(rule.getInr());
						// 任务开始时间
						Long jobStartTime = rule.getStartts();
						if (Tool.ObjectUtil.equal(0L, jobStartTime)) {
							// startts = 0 时，分散执行时间，以当前时间加 60s 随机数
							jobStartTime = System.currentTimeMillis() + Tool.RandomUtil.randomInt(10, 60) * 1000;
						}
						SimpleTrigger simpleTrigger = TriggerBuilder.newTrigger().forJob(jobDetail)
								.withIdentity(jobKey.getName(), jobKey.getGroup()).withSchedule(scheduleBuilder)
								.startAt(Tool.DateUtil.date(jobStartTime))
								.build();
						// 创建任务
						scheduler.scheduleJob(jobDetail, simpleTrigger);
					}
					log.info("create recordEvalJob, ruleId: {},exists: {}", rule.getId(), exists);
				}
			}
		} catch (Exception e) {
			log.error(e, "update recordEvalJob error");
		}
	}

	/**
	 * 将 alertrule 转换为 map
	 * 
	 * @param allRuleList
	 * @return
	 */
	private Map<String, RecordRuleEntity> transRecordRuleListToMap(List<RecordRuleEntity> allRuleList) {
		Map<String, RecordRuleEntity> newRecordRuleMap = Tool.MapUtil.newHashMap();
		if (allRuleList != null) {
			int defaultInterval = Tool.NumberUtil
					.parseInt(configService.getValueOrDefault("default_scrape_interval", "30"));
			for (RecordRuleEntity entity : allRuleList) {
				// 判断是否需要设置默认执行周期
				Integer interval = entity.getInr();
				if (interval == null || interval == 0) {
					entity.setInr(defaultInterval);
				}
				newRecordRuleMap.put(entity.getRecordRuleHashId(), entity);
			}
		}
		return newRecordRuleMap;
	}

	private JobKey getRecordEvalJobKey(String hash, Integer id) {
		String name = String.format("%s-%s", hash, id);
		JobKey jobkey = new JobKey(name, RECORD_EVAL_GROUP);
		return jobkey;
	}
}
