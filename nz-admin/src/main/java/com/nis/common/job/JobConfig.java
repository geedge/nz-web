package com.nis.common.job;

import cn.hutool.log.Log;
import com.nis.modules.agent.job.AgentHealthCheckJob;
import com.nis.modules.agent.job.AutoConfigAgentJob;
import com.nis.modules.alert.backend.AlertMessageExpiredJob;
import com.nis.modules.alert.backend.AlertRuleManagerJob;
import com.nis.modules.alert.backend.AlertSilenceStatusJob;
import com.nis.modules.asset.job.AssetFeatureJob;
import com.nis.modules.asset.job.AssetPingStatusJob;
import com.nis.modules.asset.job.AssetTalonStatusJob;
import com.nis.modules.endpoint.job.EndpointStateJob;
import com.nis.modules.sys.job.SessionTimeOutJob;
import com.nis.modules.terminal.job.TerminalCleanRecordJob;
import org.apache.commons.lang.StringUtils;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final Log log = Log.get();

    @Value("${nezha.agentJobCron:0/30 * * * * ?}")
    private String agentJobCron;

    @Value("${nezha.agentHealthyJobCron:0/30 * * * * ?}")
    private String agentHealthyJobCron;

    @Value("${nezha.alertSilenceJobCron:0/10 * * * * ?}")
    private String alertSilenceJobCron;
    
    @Value("${nezha.alertMessageExpiredJobCron:0/10 * * * * ?}")
    private String alertMessageExpiredJobCron;
    
    @Value("${nezha.alertRuleManagerJobCron:0/10 * * * * ?}")
    private String alertRuleManagerJobCron;
    
    @Value("${nezha.assetPingJobCron:0 0/1 * * * ? *}")
    private String assetPingJobCron;

    @Value("${nezha.assetTalonJobCron:0 0/1 * * * ? *}")
    private String assetTalonJobCron;

    @Value("${nezha.assetFeatureJobCron:0 0/5 * 1 * ? *}")
    private String assetFeatureJobCron;

    @Value("${nezha.haJobCron:0/5 * * * * ?}")
    private String haJobCron;

    @Value("${nezha.endpointStateJobCron:0 0/1 * * * ? *}")
    private String endpointStateJobCron;
    
	@Value("${nezha.sessionTimeOutJobCron:0 0/5 * * * ? *}")
	private String sessionTimeOutJobCron;

	@Value("${nezha.terminalCleanJobCron:0 0 1 * * ?}")
	private String terminalCleanJobCron;
	
    @Autowired
//    @Qualifier("costomSchedulerFactoryBean")
    private SchedulerFactoryBean schedulerFactoryBean;
    private static final String JOB_NAME = "NZJOB";
    private String agentJobName = "AGENT_JOB";
    private String agentHealthCheckJobName = "AGENTHEALTHY_JOB";
    private String alertSilenceStatusJobName = "ALERTSILENCESTATUS_JOB";
    private String alertMessageExpiredJobName = "ALERTMESSAGEEXPIRED_JOB";
    private String alertRuleManagerJobName = "ALERTRULEMANAGER_JOB";
    private String assetFeatureJobName = "ASSETFEATURE_JOB";
    private String assetPingStatusJobName = "ASSETPINGSTATUS_JOB";
    private String assetTalonStatusJobName = "ASSETTALONSTATUS_JOB";
    private String endpointStateJobName = "ENDPOINTSTATE_JOB";
    private String haJobName = "HA_JOB";
    private String sessionTimeOutJobName = "SESSIONTIMEOUT_JOB";
    private String terminalCleanJobCronName = "TERMINALCLEAN_JOB";
   
    @Bean
    public JobDetail agentCronJobDetail() {
        return JobBuilder.newJob(AutoConfigAgentJob.class).withIdentity(JOB_NAME + agentJobName).storeDurably().build();
    }

    @Bean
    public JobDetail agentHealthyCronJobDetail() {
        return JobBuilder.newJob(AgentHealthCheckJob.class).withIdentity(JOB_NAME + agentHealthCheckJobName).storeDurably().build();
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
    public JobDetail assetTalonStatusCronJobDetail() {
        return JobBuilder.newJob(AssetTalonStatusJob.class).withIdentity(JOB_NAME + assetTalonStatusJobName).storeDurably().build();
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
    public JobDetail alertMessageExpiredDetail() {
        return JobBuilder.newJob(AlertMessageExpiredJob.class).withIdentity(JOB_NAME + alertMessageExpiredJobName).storeDurably().build();
    }

	@Bean
	public JobDetail sessionTimeOutJobDetail() {
	    return JobBuilder.newJob(SessionTimeOutJob.class).withIdentity(JOB_NAME + sessionTimeOutJobName).storeDurably().build();
	}
    
	@Bean
	public JobDetail terminalCleanJobDetail() {
	    return JobBuilder.newJob(TerminalCleanRecordJob.class).withIdentity(JOB_NAME + terminalCleanJobCronName).storeDurably().build();
	}
	
    @Bean
    public JobDetail alertRuleManagerJobDetail() {
        return JobBuilder.newJob(AlertRuleManagerJob.class).withIdentity(JOB_NAME + alertRuleManagerJobName).storeDurably().build();
    }
	
    @PostConstruct
    public void init() throws SchedulerException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();

        createCronScheduleJob(agentHealthCheckJobName, agentHealthyCronJobDetail(), StringUtils.isEmpty(agentHealthyJobCron) ? "0 0/1 * * * ? *" : agentHealthyJobCron, scheduler);

        // asset talon state 如果配置文件没有配置定时周期  默认为1分钟执行一次
        createCronScheduleJob(assetTalonStatusJobName, assetTalonStatusCronJobDetail(), StringUtils.isEmpty(assetTalonJobCron) ? "0 0/1 * * * ? *" : assetTalonJobCron, scheduler);

        // agent  如果配置文件没有配置定时周期  默认为 30s 执行一次
        createCronScheduleJob(agentJobName, agentCronJobDetail(), StringUtils.isEmpty(agentJobCron) ? "0/30 * * * * ?" : agentJobCron, scheduler);

        // assetFeatureJob 如果配置文件没有配置定时周期 默认为5分钟执行一次
        createCronScheduleJob(assetFeatureJobName, assetFeatureCronJobDetail(), StringUtils.isEmpty(assetFeatureJobCron) ? "0 0/5 * 1 * ? *" : assetFeatureJobCron, scheduler);

        // asset pingJob 如果配置文件没有配置定时周期  默认为1分钟执行一次
        createCronScheduleJob(assetPingStatusJobName, assetPingStatusCronJobDetail(), StringUtils.isEmpty(assetPingJobCron) ? "0 0/1 * * * ? *" : assetPingJobCron, scheduler);

        // HA Job 如果配置文件没有配置定时周期  默认为5s执行一次
        createCronScheduleJob(haJobName, haJobDetail(), StringUtils.isEmpty(haJobCron) ? "0/5 * * * * ?" : haJobCron, scheduler);
        
        // endpoint state job 如果配置文件没有配置定时周期  默认为1分钟执行一次
        createCronScheduleJob(endpointStateJobName, endpointStateJobDetail(), StringUtils.isEmpty(endpointStateJobCron) ? "0 0/1 * * * ? *" : endpointStateJobCron, scheduler);

        // 告警静默状态变更线程
        //alertSilenceStatusJob 配置    如果配置文件没有配置定时周期 每5秒检测一次静默配置
        createCronScheduleJob(alertSilenceStatusJobName, alertSilenceStatusJobDetail(), StringUtils.isEmpty(alertSilenceJobCron) ? "0/10 * * * * ?" : alertSilenceJobCron, scheduler);
        
        // 告警消息超时判断线程
        createCronScheduleJob(alertMessageExpiredJobName, alertMessageExpiredDetail(), StringUtils.isEmpty(alertMessageExpiredJobCron) ? "0/10 * * * * ?" : alertMessageExpiredJobCron, scheduler);
        
        //告警规则管理线程
        createCronScheduleJob(alertRuleManagerJobName, alertRuleManagerJobDetail(), StringUtils.isEmpty(alertRuleManagerJobCron) ? "0/10 * * * * ?" : alertRuleManagerJobCron, scheduler);
        
        // session time out job 如果配置文件没有配置定时周期  默认为5分钟执行一次
        createCronScheduleJob(sessionTimeOutJobName, sessionTimeOutJobDetail(), StringUtils.isEmpty(sessionTimeOutJobCron) ? "0 0/5 * * * ? *" : sessionTimeOutJobCron, scheduler);
        
        // terminal clean job 如果配置文件没有配置定时周期  每天凌晨1点执行一次
        createCronScheduleJob(terminalCleanJobCronName, terminalCleanJobDetail(), StringUtils.isEmpty(terminalCleanJobCron) ? "0 0 1 * * ?" : terminalCleanJobCron, scheduler);
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
