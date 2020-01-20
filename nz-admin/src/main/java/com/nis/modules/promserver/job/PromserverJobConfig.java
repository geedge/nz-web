package com.nis.modules.promserver.job;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class PromserverJobConfig {
	
	@Value("${nezha.promserverJobCron}")
    private String promserverJobCron;
	
	@Bean
    public JobDetail promserverCronJobDetail() {
        return JobBuilder.newJob(PromserverJob.class).withIdentity("promserverJob").storeDurably().build();
    }

    @Bean
    public Trigger promserverCronJobTrigger() {
    	
    	if(promserverJobCron.isEmpty()) {
    		//如果配置文件没有配置定时周期 默认为2分钟执行一次
    		promserverJobCron="0 0/2 * * * ? *";
    	}
    	
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(promserverJobCron);

        return TriggerBuilder.newTrigger()
                .forJob(promserverCronJobDetail())
                .withSchedule(cronScheduleBuilder)
                .build();
    }
    
}
