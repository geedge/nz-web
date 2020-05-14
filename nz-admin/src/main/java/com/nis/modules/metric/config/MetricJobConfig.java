package com.nis.modules.metric.config;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.nis.modules.metric.controller.AlertMessageJob;

@Configuration
public class MetricJobConfig {


    // alert message 状态同步频率 配置文件获取
    @Value("${nezha.alertMessageJobCron}")
    private String alertMessageJobCron;


    /**
     * alert message 同步
     *
     * @return
     */
    @Bean
    public JobDetail alertMessageCronJobDetail() {
        return JobBuilder.newJob(AlertMessageJob.class).withIdentity("alertJob").storeDurably().build();
    }

    @Bean
    public Trigger AlertCronJobTrigger() {
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(alertMessageJobCron);
        return TriggerBuilder.newTrigger()
                .forJob(alertMessageCronJobDetail())
                .withSchedule(cronScheduleBuilder)
                .build();
    }
}
