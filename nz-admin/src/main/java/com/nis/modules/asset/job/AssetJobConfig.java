package com.nis.modules.asset.job;

import com.nis.modules.job.utils.ScheduleUtils;
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
public class AssetJobConfig {

    @Value("${nezha.assetPingJobCron}")
    private String assetPingJobCron;

    @Value("${nezha.assetFeatureJobCron}")
    private String assetFeatureJobCron;

    @Autowired
    @Qualifier("costomSchedulerFactoryBean")
    private SchedulerFactoryBean schedulerFactoryBean;

    private String assetPingJobName = "ASSETPING_JOB";
    private String assetFeatureJobName = "ASSETFEATURE_JOB";
    private String assetPingStatusJobName = "ASSETPINGSTATUS_JOB";

    @Bean
    public JobDetail assetPingCronJobDetail() {
        return JobBuilder.newJob(AssetPingJob.class).withIdentity(ScheduleUtils.JOB_NAME + assetPingJobName).storeDurably().build();
    }

    @Bean
    public JobDetail assetFeatureCronJobDetail() {
        return JobBuilder.newJob(AssetFeatureJob.class).withIdentity(ScheduleUtils.JOB_NAME + assetFeatureJobName).storeDurably().build();
    }

    @Bean
    public JobDetail assetPingStatusCronJobDetail() {
        return JobBuilder.newJob(AssetPingStatusJob.class).withIdentity(ScheduleUtils.JOB_NAME + assetPingStatusJobName).storeDurably().build();
    }

    @PostConstruct
    public void init() throws SchedulerException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();

        // 如果配置文件没有配置定时周期  默认为3分钟执行一次
        if (assetPingJobCron.isEmpty()) {
            assetPingJobCron = "0 0/3 * * * ? *";
        }
        ScheduleUtils.createCronScheduleJob(assetPingJobName, assetPingCronJobDetail(), assetPingJobCron, scheduler);
        ScheduleUtils.createCronScheduleJob(assetPingStatusJobName, assetPingStatusCronJobDetail(), assetPingJobCron, scheduler);


        // 如果配置文件没有配置定时周期 默认为5分钟执行一次
        if (assetFeatureJobCron.isEmpty()) {
            assetFeatureJobCron = "0 0/5 * * * ? *";
        }
        ScheduleUtils.createCronScheduleJob(assetFeatureJobName, assetFeatureCronJobDetail(), assetFeatureJobCron, scheduler);
    }
}
