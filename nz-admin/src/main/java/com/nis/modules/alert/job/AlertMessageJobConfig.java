package com.nis.modules.alert.job;

import com.nis.common.utils.ToolUtil;
import com.nis.modules.job.utils.ScheduleUtils;
import com.nis.modules.sys.service.SysConfigService;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.annotation.PostConstruct;

@Configuration
public class AlertMessageJobConfig {

    @Value("${nezha.alertMessageJobCron}")
    private String alertMessageJobCron;

    @Value("${nezha.alertSilenceJobCron}")
    private String alertSilenceJobCron;
    
    @Autowired
    @Qualifier("costomSchedulerFactoryBean")
    private SchedulerFactoryBean schedulerFactoryBean;

    @Autowired
    private SysConfigService configService;

//    private String alertMessageEmailNotificationJobName = "ALERTMESSAGEEMAILNOTIFICATION_JOB";
//    private String alertMessageExpirStatusJobName = "ALERTMESSAGEEXPIRSTATUS_JOB";
    private String alertSilenceStatusJobName = "ALERTSILENCESTATUS_JOB";

	/*   @Bean
	public JobDetail alertMessageEmailNotifiJobDetail() {
	    return JobBuilder.newJob(AlertMessageEmailNotifiJob.class).withIdentity(ScheduleUtils.JOB_NAME + alertMessageEmailNotificationJobName).storeDurably().build();
	}

    @Bean
    public JobDetail alertMessageExpirStatusJobDetail() {
        return JobBuilder.newJob(AlertMessageExpirStatusJob.class).withIdentity(ScheduleUtils.JOB_NAME + alertMessageExpirStatusJobName).storeDurably().build();
    }*/
    
    @Bean
    public JobDetail alertSilenceStatusJobDetail() {
        return JobBuilder.newJob(AlertSilenceStatusJob.class).withIdentity(ScheduleUtils.JOB_NAME + alertSilenceStatusJobName).storeDurably().build();
    }

    @PostConstruct
    public void init() throws SchedulerException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();

        // alertemailJob 配置
        String alertMessageInterval = configService.getValue("alert_notify_interval");

        // 如果配置文件没有配置定时周期  默认为3分钟执行一次
        if (ToolUtil.isEmpty(alertMessageInterval)) {
            alertMessageInterval = "300";
        }

//        ScheduleUtils.createSimpleScheduleJob(alertMessageEmailNotificationJobName, alertMessageEmailNotifiJobDetail(), Integer.parseInt(alertMessageInterval), scheduler);

        // alertmessageJob 配置
//        ScheduleUtils.createCronScheduleJob(alertMessageExpirStatusJobName, alertMessageExpirStatusJobDetail(), alertMessageJobCron, scheduler);
    
        // alertSilenceStatusJob 配置
        ScheduleUtils.createCronScheduleJob(alertSilenceStatusJobName, alertSilenceStatusJobDetail(), alertSilenceJobCron, scheduler);
    }
}
