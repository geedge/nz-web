package com.nis.common.job;

import cn.hutool.log.Log;
import com.nis.modules.agent.job.AgentJob;
import com.nis.modules.alert.job.AlertSilenceStatusJob;
import com.nis.modules.asset.job.AssetFeatureJob;
import com.nis.modules.asset.job.AssetPingStatusJob;
import com.nis.modules.endpoint.job.EndpointStateJob;
import com.nis.modules.job.utils.ScheduleUtils;
import org.apache.commons.lang.StringUtils;
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

/**
 * JobConfig 统一管理 job config
 *
 * @author shizhendong
 * @date 2021-04-23
 */
@Configuration
public class JobConfig {

    private static Log log = Log.get();

    @Value("${nezha.agentJobCron}")
    private String agentJobCron;

    @Value("${nezha.alertSilenceJobCron}")
    private String alertSilenceJobCron;

    @Value("${nezha.assetPingJobCron}")
    private String assetPingJobCron;

    @Value("${nezha.assetFeatureJobCron}")
    private String assetFeatureJobCron;

    @Value("${nezha.endpointStateJobCron}")
    private String endpointStateJobCron;

    @Autowired
    @Qualifier("costomSchedulerFactoryBean")
    private SchedulerFactoryBean schedulerFactoryBean;

    private String agentJobName = "AGENT_JOB";
    private String alertSilenceStatusJobName = "ALERTSILENCESTATUS_JOB";
    private String assetFeatureJobName = "ASSETFEATURE_JOB";
    private String assetPingStatusJobName = "ASSETPINGSTATUS_JOB";
    private String endpointStateJobName = "ENDPOINTSTATE_JOB";

    @Bean
    public JobDetail agentCronJobDetail() {
        return JobBuilder.newJob(AgentJob.class).withIdentity(ScheduleUtils.JOB_NAME + agentJobName).storeDurably().build();
    }

    @Bean
    public JobDetail alertSilenceStatusJobDetail() {
        return JobBuilder.newJob(AlertSilenceStatusJob.class).withIdentity(ScheduleUtils.JOB_NAME + alertSilenceStatusJobName).storeDurably().build();
    }

    @Bean
    public JobDetail assetFeatureCronJobDetail() {
        return JobBuilder.newJob(AssetFeatureJob.class).withIdentity(ScheduleUtils.JOB_NAME + assetFeatureJobName).storeDurably().build();
    }

    @Bean
    public JobDetail assetPingStatusCronJobDetail() {
        return JobBuilder.newJob(AssetPingStatusJob.class).withIdentity(ScheduleUtils.JOB_NAME + assetPingStatusJobName).storeDurably().build();
    }

    @Bean
    public JobDetail endpointStateJobDetail() {
        return JobBuilder.newJob(EndpointStateJob.class).withIdentity(ScheduleUtils.JOB_NAME + endpointStateJobName).storeDurably().build();
    }


    @PostConstruct
    public void init() throws SchedulerException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();

        // agent  如果配置文件没有配置定时周期  默认为 30s 执行一次
        ScheduleUtils.createCronScheduleJob(agentJobName, agentCronJobDetail(), StringUtils.isEmpty(agentJobCron) ? "0/30 * * * * ?" : agentJobCron, scheduler);

        // alertSilenceStatusJob 配置    如果配置文件没有配置定时周期 每5秒检测一次静默配置
        ScheduleUtils.createCronScheduleJob(alertSilenceStatusJobName, alertSilenceStatusJobDetail(), StringUtils.isEmpty(alertSilenceJobCron) ? "0/5 * * * * ?" : alertSilenceJobCron, scheduler);

        // assetFeatureJob 如果配置文件没有配置定时周期 默认为5分钟执行一次
        ScheduleUtils.createCronScheduleJob(assetFeatureJobName, assetFeatureCronJobDetail(), StringUtils.isEmpty(assetFeatureJobCron) ? "0 0/5 * * * ? *" : assetFeatureJobCron, scheduler);

        // asset pingJob 如果配置文件没有配置定时周期  默认为1分钟执行一次
        ScheduleUtils.createCronScheduleJob(assetPingStatusJobName, assetPingStatusCronJobDetail(), StringUtils.isEmpty(assetPingJobCron) ? "0 0/1 * * * ? *" : assetPingJobCron, scheduler);

        // endpoint state job 如果配置文件没有配置定时周期  默认为1分钟执行一次
        ScheduleUtils.createCronScheduleJob(endpointStateJobName, endpointStateJobDetail(), StringUtils.isEmpty(endpointStateJobCron) ? "0 0/1 * * * ? *" : endpointStateJobCron, scheduler);

    }
}
