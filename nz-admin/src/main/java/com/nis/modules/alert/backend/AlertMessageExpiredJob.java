package com.nis.modules.alert.backend;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.log.Log;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertHandlerService;
import com.nis.modules.election.dao.LeaderElectionDao;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.Date;
import java.util.List;

/**
 * 告警消息 超时判断线程，主要用于处理因为alert message 未能正常改为 expired 状态的消息
 * 1、查询 alert_message_active表 endAt < now 的告警消息
 * 2、将 查出来的告警消息 写入到  expired 表&删除本地 cache key
 * 3、查询 alert_message_silence 表 endAt < now  的告警消息
 * 4、将 查出来的告警消息 写入到  expired 表&删除本地 cache key
 * @author ThinkPad
 *
 */
@DisallowConcurrentExecution
public class AlertMessageExpiredJob extends QuartzJobBean{
	private static final Log log = Log.get();
	
	@Autowired
	private AlertHandlerService messageHandlerService;
	
	@Value("${alert.message.selectLimit:1000}")
	private Integer limit;
	
	@Override
	protected void executeInternal(JobExecutionContext arg0) throws JobExecutionException {
		Thread.currentThread().setName("AlertMessageExpiredJob");
        // 检查 HA_LOCK
        LeaderElectionDao dao = LeaderElectionDao.getInstance();
        if (BooleanUtil.negate(dao.isLeader(Constant.SYS_HA_LOCK, Constant.SERVER_ID))) return;
        log.info("AlertMessageExpiredJob start");
        try {
        	StopWatch sw = StopWatch.create("messageExpired");
            sw.start("message active to expired");
            // 1、查询 alert_message_active表 endAt < now-resolveTime 的告警消息
            List<AlertMessageEntity> messageList = messageHandlerService.selectMessagegtNow(Constant.ALERT_MESSAGE_ACTIVE_TABLE,limit);
            // 2、将 查出来的告警消息 写入到  expired 表&删除本地 cache key
            if(Tool.CollUtil.isNotEmpty(messageList)) {
            	messageHandlerService.handleMessageExpiredBatch(messageList, Constant.ALERT_MESSAGE_ACTIVE_STATE);
            }
            log.info("处理已经expired未正常清除的active数据,总条数:{}", messageList.size());
            sw.stop();
            sw.start("message silence to expired");
            // 3、查询 alert_message_silence 表 endAt < now  的告警消息
            messageList = messageHandlerService.selectMessagegtNow(Constant.ALERT_MESSAGE_SILENCE_TABLE,limit);
            // 4、将 查出来的告警消息 写入到  expired 表&删除本地 cache key
            if(Tool.CollUtil.isNotEmpty(messageList)) {
            	messageHandlerService.handleMessageExpiredBatch(messageList, Constant.ALERT_MESSAGE_SILENCE_STATE);
            }
            log.info("处理已经expired未正常清除的silence数据,总条数:{}", messageList.size());
            sw.stop();
            sw.start("message close expired update endat");
            // 5、 更新 auto expired = 0 的告警消息endat ，否则前端展示为 duration 显示有歧义
            Date endAt = Tool.DateUtil.date();
            messageHandlerService.updateCloseExpiredMessageEndAt(Constant.ALERT_MESSAGE_ACTIVE_TABLE,endAt);
            log.info("update close Expire message end time,table: {}", Constant.ALERT_MESSAGE_ACTIVE_TABLE);
            messageHandlerService.updateCloseExpiredMessageEndAt(Constant.ALERT_MESSAGE_SILENCE_TABLE,endAt);
            log.info("update close Expire message end time,table: {}", Constant.ALERT_MESSAGE_SILENCE_TABLE);
            sw.stop();
            if(log.isDebugEnabled()) {
            	log.debug("耗时统计:{}", sw.prettyPrint());
            }
		} catch (Exception e) {
			log.error(e, "AlertMessageExpiredJob error");
		}
        log.info("AlertMessageExpiredJob end");
	}
}
