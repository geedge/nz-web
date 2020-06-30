package com.nis.modules.asset.job;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssetFeatureJobConfig {
	@Value("${nezha.assetFeatureJobCron}")
    private String assetFeatureJobCron;
	
	@Bean
    public JobDetail assetFeatureCronJobDetail() {
        return JobBuilder.newJob(AssetFeatureJob.class).withIdentity("assetFeatureJob").storeDurably().build();
    }

    @Bean
    public Trigger assetFeatureCronJobTrigger() {
    	
    	if(assetFeatureJobCron.isEmpty()) {
    		//如果配置文件没有配置定时周期 默认为5分钟执行一次
    		assetFeatureJobCron="0 0/5 * * * ? *";
    	}
    	
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(assetFeatureJobCron);

        return TriggerBuilder.newTrigger()
                .forJob(assetFeatureCronJobDetail())
                .withSchedule(cronScheduleBuilder)
                .build();
    }
}
