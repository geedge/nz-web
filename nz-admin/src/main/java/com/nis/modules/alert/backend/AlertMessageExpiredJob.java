package com.nis.modules.alert.backend;

import java.util.Date;
import java.util.List;

import com.alibaba.fastjson.JSONObject;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertHandlerService;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;

/**
 * 告警消息 超时判断线程，主要用于处理因为alert message 未能正常改为 expired 状态的消息
 * 1、查询 alert_message_active表 endAt < now 的告警消息
 * 2、将 查出来的告警消息 写入到  expired 表&删除redis key
 * 3、查询 alert_message_silence 表 endAt < now  的告警消息
 * 4、将 查出来的告警消息 写入到  expired 表&删除redis key
 * @author ThinkPad
 *
 */
@DisallowConcurrentExecution
public class AlertMessageExpiredJob extends QuartzJobBean{
	private static final Log log = Log.get();
	
	@Autowired
	private RedisTemplate<String,Object> redisTemplate;
	@Autowired
	private AlertHandlerService messageHandlerService;
	
	@Override
	protected void executeInternal(JobExecutionContext arg0) throws JobExecutionException {
		// 检查 haMode
    	String serverId = (String) redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
        if(!StrUtil.equals(serverId, Constant.SERVER_ID)) return;
        log.debug("AlertMessageExpiredJob 定时任务开始");
        StopWatch sw = StopWatch.create("messageExpired");
        sw.start("message active to expired");
        // 1、查询 alert_message_active表 endAt < now-resolveTime 的告警消息
        List<AlertMessageEntity> messageList = messageHandlerService.selectMessagegtNow(Constant.ALERT_MESSAGE_ACTIVE_TABLE);
        log.debug("expired 告警 {}", JSONObject.toJSONString(messageList));
        // 2、将 查出来的告警消息 写入到  expired 表&删除redis key
        if(Tool.CollUtil.isNotEmpty(messageList)) {
        	messageHandlerService.handleMessageExpiredBatch(messageList, Constant.ALERT_MESSAGE_ACTIVE_STATE);
        	log.info("处理已经expired未正常清除的active数据,总条数:{}", messageList.size());
        }
        sw.stop();
        sw.start("message silence to expired");
        // 3、查询 alert_message_silence 表 endAt < now  的告警消息
        messageList = messageHandlerService.selectMessagegtNow(Constant.ALERT_MESSAGE_SILENCE_TABLE);
        // 4、将 查出来的告警消息 写入到  expired 表&删除redis key
        if(Tool.CollUtil.isNotEmpty(messageList)) {
        	messageHandlerService.handleMessageExpiredBatch(messageList, Constant.ALERT_MESSAGE_SILENCE_STATE);
        	log.info("处理已经expired未正常清除的silence数据,总条数:{}", messageList.size());
        }
        sw.stop();
        sw.start("message close expired update endat");
        // 5、 更新 auto expired = 0 的告警消息endat ，否则前端展示为 duration 显示有歧义
        Date endAt = Tool.DateUtil.date();
        messageHandlerService.updateCloseExpiredMessageEndAt(Constant.ALERT_MESSAGE_ACTIVE_TABLE,endAt);
        messageHandlerService.updateCloseExpiredMessageEndAt(Constant.ALERT_MESSAGE_SILENCE_TABLE,endAt);
        sw.stop();
        if(log.isDebugEnabled()) {
        	log.debug("耗时统计:{}", sw.prettyPrint());
        }
	}
}
