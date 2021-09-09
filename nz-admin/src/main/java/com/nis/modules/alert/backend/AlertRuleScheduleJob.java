package com.nis.modules.alert.backend;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.nis.common.utils.Constant;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.service.AlertRuleService;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;

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

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		// 检查 haMode
		String serverId = (String) redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
		if (!StrUtil.equals(serverId, Constant.SERVER_ID))
			return;
		JobKey key = context.getJobDetail().getKey();
		log.debug("alert rule schedule job start,key:{}", key);
		// key格式：hash-id-state，例：adcdaed-12-1
		String[] split = key.getName().split("-");
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
		} catch (RuntimeException e) {
			log.error(e, "alert rule schedule manager job error");
		}
		log.debug("alert rule schedule manager job end,key:{}", key);
	}
}
