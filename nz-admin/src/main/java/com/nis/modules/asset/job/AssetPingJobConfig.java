package com.nis.modules.asset.job;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssetPingJobConfig {

    @Value("${nezha.assetPingJobCron}")
    private String assetPingJobCron;

    @Bean
    public JobDetail assetPingCronJobDetail() {
        return JobBuilder.newJob(AssetPingJob.class).withIdentity("assetPingJob").storeDurably().build();
    }

    @Bean
    public Trigger assetPingCronJobTrigger() {
        if (assetPingJobCron.isEmpty()) {
            // 如果配置文件没有配置定时周期  默认为3分钟执行一次
            assetPingJobCron = "0 0/3 * * * ? *";
        }

        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(assetPingJobCron);

        return TriggerBuilder.newTrigger()
                .forJob(assetPingCronJobDetail())
                .withSchedule(cronScheduleBuilder)
                .build();
    }

}
