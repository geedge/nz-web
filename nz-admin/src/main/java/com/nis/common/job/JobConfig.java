package com.nis.common.job;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import com.nis.modules.agent.job.AgentJob;
import com.nis.modules.alert.job.AlertSilenceStatusJob;
import com.nis.modules.asset.job.AssetFeatureJob;
import com.nis.modules.asset.job.AssetPingStatusJob;
import com.nis.modules.endpoint.job.EndpointStateJob;
import com.nis.modules.sys.job.SessionTimeOutJob;

import cn.hutool.log.Log;

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

    @Value("${nezha.haJobCron}")
    private String haJobCron;

    @Value("${nezha.endpointStateJobCron}")
    private String endpointStateJobCron;
    
	@Value("${nezha.sessionTimeOutJobCron}")
	private String sessionTimeOutJobCron;

    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;
    private static final String JOB_NAME = "NZJOB";
    private String agentJobName = "AGENT_JOB";
    private String alertSilenceStatusJobName = "ALERTSILENCESTATUS_JOB";
    private String assetFeatureJobName = "ASSETFEATURE_JOB";
    private String assetPingStatusJobName = "ASSETPINGSTATUS_JOB";
    private String endpointStateJobName = "ENDPOINTSTATE_JOB";
    private String haJobName = "HA_JOB";
    private String sessionTimeOutJobName = "SESSIONTIMEOUT_JOB";
    
    @Bean
    public JobDetail agentCronJobDetail() {
        return JobBuilder.newJob(AgentJob.class).withIdentity(JOB_NAME + agentJobName).storeDurably().build();
    }

    @Bean
    public JobDetail alertSilenceStatusJobDetail() {
        return JobBuilder.newJob(AlertSilenceStatusJob.class).withIdentity(JOB_NAME + alertSilenceStatusJobName).storeDurably().build();
    }

    @Bean
    public JobDetail assetFeatureCronJobDetail() {
        return JobBuilder.newJob(AssetFeatureJob.class).withIdentity(JOB_NAME + assetFeatureJobName).storeDurably().build();
    }

    @Bean
    public JobDetail assetPingStatusCronJobDetail() {
        return JobBuilder.newJob(AssetPingStatusJob.class).withIdentity(JOB_NAME + assetPingStatusJobName).storeDurably().build();
    }

    @Bean
    public JobDetail endpointStateJobDetail() {
        return JobBuilder.newJob(EndpointStateJob.class).withIdentity(JOB_NAME + endpointStateJobName).storeDurably().build();
    }
    
    @Bean
    public JobDetail haJobDetail() {
        return JobBuilder.newJob(HaJob.class).withIdentity(JOB_NAME + haJobName).storeDurably().build();
    }

	@Bean
	public JobDetail sessionTimeOutJobDetail() {
	    return JobBuilder.newJob(SessionTimeOutJob.class).withIdentity(JOB_NAME + sessionTimeOutJobName).storeDurably().build();
	}
    
    @PostConstruct
    public void init() throws SchedulerException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();

        // agent  如果配置文件没有配置定时周期  默认为 30s 执行一次
        createCronScheduleJob(agentJobName, agentCronJobDetail(), StringUtils.isEmpty(agentJobCron) ? "0/30 * * * * ?" : agentJobCron, scheduler);

        // alertSilenceStatusJob 配置    如果配置文件没有配置定时周期 每5秒检测一次静默配置
        createCronScheduleJob(alertSilenceStatusJobName, alertSilenceStatusJobDetail(), StringUtils.isEmpty(alertSilenceJobCron) ? "0/5 * * * * ?" : alertSilenceJobCron, scheduler);

        // assetFeatureJob 如果配置文件没有配置定时周期 默认为5分钟执行一次
        createCronScheduleJob(assetFeatureJobName, assetFeatureCronJobDetail(), StringUtils.isEmpty(assetFeatureJobCron) ? "0 0/5 * * * ? *" : assetFeatureJobCron, scheduler);

        // asset pingJob 如果配置文件没有配置定时周期  默认为1分钟执行一次
        createCronScheduleJob(assetPingStatusJobName, assetPingStatusCronJobDetail(), StringUtils.isEmpty(assetPingJobCron) ? "0 0/1 * * * ? *" : assetPingJobCron, scheduler);

        // HA Job 如果配置文件没有配置定时周期  默认为5s执行一次
        createCronScheduleJob(haJobName, haJobDetail(), StringUtils.isEmpty(haJobCron) ? "0/5 * * * * ?" : haJobCron, scheduler);
        
        // endpoint state job 如果配置文件没有配置定时周期  默认为1分钟执行一次
        createCronScheduleJob(endpointStateJobName, endpointStateJobDetail(), StringUtils.isEmpty(endpointStateJobCron) ? "0 0/1 * * * ? *" : endpointStateJobCron, scheduler);

        // session time out job 如果配置文件没有配置定时周期  默认为5分钟执行一次
        createCronScheduleJob(sessionTimeOutJobName, sessionTimeOutJobDetail(), StringUtils.isEmpty(sessionTimeOutJobCron) ? "0 0/5 * * * ? *" : sessionTimeOutJobCron, scheduler);
    }
    
    /**
     * 根据 jobName jobDetail jobCronExpression  创建任务，如果存在则删除再创建
     * @param jobName
     * @param jobDetail
     * @param jobCronExpression
     * @param scheduler
     * @throws Exception
     */
    public static void createCronScheduleJob(String jobName, JobDetail jobDetail, String jobCronExpression, Scheduler scheduler) throws SchedulerException {
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(jobCronExpression);

        CronTrigger cronTrigger = TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withSchedule(cronScheduleBuilder)
                .build();

        // job如果存在则删除
        JobKey jobKey = new JobKey(JOB_NAME + jobName);
        boolean b = scheduler.checkExists(jobKey);
        if (b) {
            scheduler.deleteJob(jobKey);
        }
        // 创建任务
        scheduler.scheduleJob(jobDetail, cronTrigger);
    }

}
