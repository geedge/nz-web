package com.nis.modules.metric.config;

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

    /**
     * 使用jobDetail包装job
     *
     * @return
     */
    @Bean
    public JobDetail myCronJobDetail() {
        return JobBuilder.newJob(MetricJob.class).withIdentity("metricJob").storeDurably().build();
    }

    /**
     * 把jobDetail注册到Cron表达式的trigger上去
     *
     * @return
     */
    @Bean
    public Trigger CronJobTrigger() {
        // 定义 cron 表达式
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(metricJobCron);// 0 0 0/1 * * ? * 一小时执行一次

        return TriggerBuilder.newTrigger()
                .forJob(myCronJobDetail())
                .withIdentity("metricJobTrigger", "group1")
                .withSchedule(cronScheduleBuilder)
                .build();
    }
}
