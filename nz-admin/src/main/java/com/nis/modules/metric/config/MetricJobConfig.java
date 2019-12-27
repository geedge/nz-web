package com.nis.modules.metric.config;

import com.nis.modules.metric.controller.AlertMessageJob;
import com.nis.modules.metric.controller.MetricJob;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricJobConfig {

    // metric 同步频率 配置文件获取
    @Value("${nezha.metricJobCron}")
    private String metricJobCron;

    // alert message 状态同步频率 配置文件获取
    @Value("${nezha.alertMessageJobCron}")
    private String alertMessageJobCron;


    /**
     * metric 同步
     *
     * @return
     */
    @Bean
    public JobDetail metricCronJobDetail() {
        return JobBuilder.newJob(MetricJob.class).storeDurably().build();
    }

    @Bean
    public Trigger metricCronJobTrigger() {
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(metricJobCron);// 0 0 0/1 * * ? * 一小时执行一次

        return TriggerBuilder.newTrigger()
                .forJob(metricCronJobDetail())
                .withSchedule(cronScheduleBuilder)
                .build();
    }


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
