package com.nis.common.job;

import java.util.concurrent.TimeUnit;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.nis.common.utils.Constant;
import com.nis.common.utils.ToolUtil;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;

@DisallowConcurrentExecution
public class HaJob extends QuartzJobBean {
	
	private Log log = Log.get();
	
	@Autowired
	private RedisTemplate redisTemplate;
	
	@SuppressWarnings("unchecked")
	@Override
	protected void executeInternal(JobExecutionContext arg0) throws JobExecutionException {
		String serverId = (String) redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
		if(StrUtil.isBlank(serverId)) {
			Boolean lock = redisTemplate.opsForValue().setIfAbsent(Constant.SYS_HA_LOCK, Constant.SERVER_ID);
			if(lock) {
				redisTemplate.expire(Constant.SYS_HA_LOCK, 10, TimeUnit.SECONDS);
				log.debug("HA job set lock");
			}
		}else if(StrUtil.equals(serverId, Constant.SERVER_ID)){
			redisTemplate.expire(Constant.SYS_HA_LOCK, 10, TimeUnit.SECONDS);
			log.debug("HA job get lock , Timeout update");
		}else {
			log.debug("HA job can not get lock , job in another program");
		}
	}

}
