/*
package com.nis.modules.alert.job;

import com.nis.modules.alert.dao.AlertMessageDao;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.sys.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;

*/
/**
 * alert message
 * 根据redis中的alertMessage 数据endtime和当前时间与alertrule表中持续时间比对
 * 失效则修改alert_message state为2 并删除redis中对应内容*//*



@Slf4j
@DisallowConcurrentExecution
public class AlertMessageExpirStatusJob extends QuartzJobBean {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private AlertRuleService alertRuleService;

    @Autowired
    private AlertMessageDao alertMessageDao;

    @Autowired
    private SysConfigService sysConfigService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        */
/*log.debug("告警消息过期状态任务开始");
        long start, end;
        start = System.currentTimeMillis();

        this.handleAlertStatus();

        end = System.currentTimeMillis();
        log.debug("告警消息过期状态任务 start time:" + start + "; end time:" + end + "; Run Time:" + (end - start) + "(ms)");
        log.debug("告警消息过期状态任务结束");*//*

    }


}
*/
