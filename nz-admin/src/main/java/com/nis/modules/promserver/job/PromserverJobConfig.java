package com.nis.modules.promserver.job;

import com.nis.modules.job.utils.ScheduleUtils;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.annotation.PostConstruct;

@Configuration
public class PromserverJobConfig {

    @Value("${nezha.promserverJobCron}")
    private String promserverJobCron;

    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;

    private String promserverJobName = "PROMSERVER_JOB";

    @Bean
    public JobDetail promserverCronJobDetail() {
        return JobBuilder.newJob(PromserverJob.class).withIdentity(ScheduleUtils.JOB_NAME + promserverJobName).storeDurably().build();
    }

    @PostConstruct
    public void init() throws Exception {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();

        // 如果配置文件没有配置定时周期 默认为2分钟执行一次
        if (promserverJobCron.isEmpty()) {
            promserverJobCron = "0 0/2 * * * ? *";
        }

        ScheduleUtils.createCronScheduleJob(promserverJobName, promserverCronJobDetail(), promserverJobCron, scheduler);
    }
}
