package com.nis.modules.alert.job;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.AlertSilenceConf;
import com.nis.modules.alert.service.AlertSilenceService;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;

@DisallowConcurrentExecution
public class AlertSilenceStatusJob extends QuartzJobBean{

	private static final Log log = Log.get();
	
	@Autowired
	private AlertSilenceService alertSilenceService;
	
	@Autowired
	private RedisTemplate<String,Object> redisTemplate;
	
	@Override
	protected void executeInternal(JobExecutionContext arg0) throws JobExecutionException {
		
		// 检查 haMode
    	String serverId = (String) redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
        if(!StrUtil.equals(serverId, Constant.SERVER_ID)) return;
		
		log.debug("静默状态定时任务开始");
		
		Integer[] states= {1,2};
		// 将静默配置状态为active和pending的判断是否变更状态
		List<AlertSilenceConf> alertSilenceConfs = alertSilenceService.list(new QueryWrapper<AlertSilenceConf>().lambda().in(AlertSilenceConf::getState, Arrays.asList(states)));
		
		if(ObjectUtil.isEmpty(alertSilenceConfs)) {
			log.debug("没有任何静默配置信息");
			redisTemplate.delete(Constant.ALERT_MESSAGE_SILENCE);
			log.debug("清空缓存里的数据信息");
			return;
		}else {
			//修改silence状态 
			alertSilenceService.modifySilenceState();
			//清除缓存中废弃的数据
			Map<Object, Object> silences = redisTemplate.opsForHash().entries(Constant.ALERT_MESSAGE_SILENCE);
			for (Map.Entry<Object, Object> entry : silences.entrySet()) {
				AlertMessageEntity alertMessage = JSONObject.parseObject((String)entry.getValue(),AlertMessageEntity.class);
				boolean checkMatchSilence = alertSilenceService.checkMatchSilence(alertMessage);
				if(checkMatchSilence) {
					continue;
				}else {
					redisTemplate.opsForHash().delete(Constant.ALERT_MESSAGE_SILENCE,entry.getKey());
				}
	        }
		}		
	}
}
