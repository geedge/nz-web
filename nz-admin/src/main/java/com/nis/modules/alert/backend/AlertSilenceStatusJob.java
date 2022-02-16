package com.nis.modules.alert.backend;

import java.util.List;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.entity.AlertSilenceConf;
import com.nis.modules.alert.service.AlertHandlerService;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;

/**
 * sliece 静默规则处理线程
 * 1、静默状态由 pending 变更为 active
 *     查询state = pending & 生效时间 > now
 *     查询当前active 的message，判断是否符合静默规则
 *     符合静默规则的alert message 改为保存到 silencebiao
 *     保存 message 与 silence conf 关系
 * 2、静默状态由 active 变更为 expired
 *     查询 state = active & 失效时间 < now
 *     根据 silence conf id 查询已经被静默的 message 且 message 只匹配了当前 silence conf
 *     删除 silence message 与 silence conf id 关系
 * @author ThinkPad
 *
 */
@DisallowConcurrentExecution
public class AlertSilenceStatusJob extends QuartzJobBean{

	private static final Log log = Log.get();
	
	@Autowired
	private RedisTemplate<String,Object> redisTemplate;
	
	@Autowired
	private AlertHandlerService handlerService;
	
	@Override
	protected void executeInternal(JobExecutionContext arg0) throws JobExecutionException {
		Thread.currentThread().setName("AlertSilenceStatusJob");
		// 检查 haMode
    	String serverId = (String) redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
        if(!StrUtil.equals(serverId, Constant.SERVER_ID)) return;
		log.info("AlertSilenceStatusJob start");
		try {
			/**
			 * active 变更为 expired
			 */
			List<AlertSilenceConf> silenceList = handlerService.selectActiveToExpiredSilence();
			StopWatch sw = new StopWatch("silence");
	        sw.start("silence conf active to expired");
			if(Tool.CollUtil.isNotEmpty(silenceList)) {
				log.info("silence conf active to expired, size:{}",Tool.CollUtil.size(silenceList));
				//有状态变化 silence 
				//1、alert message 判断是否需要移动到 history 表
				//2 修改 silence conf 状态
				handlerService.handleSilenceConfActiveToExired(silenceList);
			}
			sw.stop();
			/**
			 * pending 变更为 active
			 */
			sw.start("silence conf pending to active");
			silenceList = handlerService.selectPendingToActiveSilence();
			if(Tool.CollUtil.isNotEmpty(silenceList)) {
				log.info("silence conf pending to active, size:{}",Tool.CollUtil.size(silenceList));
				//1、判断当前 alert_message_active表中数据是否有符合 静默规则的
				//2、修改 silence conf 状态
				handlerService.handleSilenceConfPendingToActive(silenceList);
			}
			sw.stop();
			if(log.isDebugEnabled()) {
	        	log.debug("耗时统计:{}", sw.prettyPrint());
	        }
		} catch (Exception e) {
			log.error(e, "AlertSilenceStatusJob error");
		}
		log.info("AlertSilenceStatusJob end");
	}
}
