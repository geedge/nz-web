package com.nis.modules.alert.backend;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.entity.AlertRuleSchedLog;
import com.nis.modules.alert.service.AlertRuleSchedLogService;
import com.nis.modules.alert.service.AlertRuleService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * alert rule 定时器job
 * 
 * @author fang
 *
 */
@DisallowConcurrentExecution
public class AlertRuleScheduleJob extends QuartzJobBean {

	private static final Log log = Log.get();
	@Autowired
	private AlertRuleService alertRuleService;
	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	@Autowired
	private AlertRuleSchedLogService alertRuleSchedLogService;
	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		Thread.currentThread().setName("AlertRuleScheduleJob");
		// 检查 haMode
		String serverId = (String) redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
		if (!StrUtil.equals(serverId, Constant.SERVER_ID))
			return;
		JobKey key = context.getJobDetail().getKey();
		log.info("alert rule schedule job start,key:{}", key);
		// key格式：hash-id-state，例：adcdaed-12-1
		String[] split = key.getName().split("-");
		AlertRuleSchedLog schedLog = new AlertRuleSchedLog();
		schedLog.setRuleId(Integer.valueOf(split[1]));
		schedLog.setRuleState(Integer.valueOf(split[2]));
		schedLog.setTs(Tool.DateUtil.current());
		schedLog.setState(1);
		schedLog.setMsg("success");
		try {
			StopWatch sw = new StopWatch("AlertRuleScheduleJob");
			sw.start();
			//update alert_rule set state = 1 where id = 1;
			LambdaUpdateWrapper<AlertRuleEntity> wrapper = new UpdateWrapper<AlertRuleEntity>().lambda()
					.set(AlertRuleEntity::getState, split[2]).eq(AlertRuleEntity::getId, split[1]);
			alertRuleService.update(wrapper);
			sw.stop();
			if (log.isDebugEnabled()) {
				log.debug("耗时统计:{}", sw.prettyPrint());
			}
		} catch (Exception e) {
			schedLog.setState(0);
			schedLog.setMsg(Tool.StrUtil.sub(e.getMessage(), 0, 128));
			log.error(e, "alert rule schedule manager job error");
		}
		//save schedule log
		alertRuleSchedLogService.save(schedLog);
		log.info("alert rule schedule manager job end,key:{}", key);
	}
}
