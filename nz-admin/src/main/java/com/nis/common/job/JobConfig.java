package com.nis.common.job;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.job.AgentHealthCheckJob;
import com.nis.modules.agent.job.AutoConfigAgentJob;
import com.nis.modules.agent.job.CortexHistoryDataDelJob;
import com.nis.modules.agent.job.LokiHistoryDataDelJob;
import com.nis.modules.alert.backend.AlertMessageExpiredJob;
import com.nis.modules.alert.backend.AlertRuleManagerJob;
import com.nis.modules.alert.backend.AlertSilenceStatusJob;
import com.nis.modules.alert.backend.RecordManagerJob;
import com.nis.modules.alert.job.AlertMessageHistoryPartitionManagementJob;
import com.nis.modules.asset.job.AssetFeatureJob;
import com.nis.modules.asset.job.AssetPingStatusJob;
import com.nis.modules.asset.job.AssetTalonStatusJob;
import com.nis.modules.endpoint.job.MetricTypeEndpointStatusJob;
import com.nis.modules.sys.job.CleanJob;
import com.nis.modules.sys.job.SessionTimeOutJob;
import com.nis.modules.sys.job.SysBackFileSyncJob;
import com.nis.modules.sys.job.SysComponentJob;
import com.nis.modules.terminal.job.CleanExpiredTerminalRecordDataJob;
import com.nis.modules.terminal.job.CleanNoRealUseTerminalConnJob;
import org.apache.commons.lang.StringUtils;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
	
	@Value("${nezha.sysComponentJobCron:0/30 * * * * ?}")
	private String sysComponentJobCron;

    @Value("${nezha.cortexHistoryDataDelJobCron:0 0 0/2 * * ?}")
    private String cortexHistoryDataDelJobCron;

    @Value("${nezha.lokiHistoryDataDelJobCron:0 0 0/2 * * ?}")
    private String lokiHistoryDataDelJobCron;

    @Value("${nezha.alertMessageHistoryPartManJobCron:0 0 0 1/1 * ?}")
    private String alertMessageHistoryPartManJobCron;

    @Value("${nezha.recordtRuleManagerJobCron:0/10 * * * * ?}")
    private String recordtRuleManagerJobCron;

    @Value("${nezha.cleanNoRealUseTerminalConnJobCron:0 0/5 * * * ? *}")
    private String cleanNoRealUseTerminalConnJobCron;

    @Value("${nezha.cleanExpiredTerminalRecordDataJobCron:0 0 1 * * ?}")
    private String cleanExpiredTerminalRecordDataJobCron;

    @Value("${nezha.sysBackFileSyncJobCron:0 0 3 * * ?}")
    private String sysBackFileSyncJobCron;

    @Autowired
    private Scheduler scheduler;
//    @Qualifier("costomSchedulerFactoryBean")
    //private SchedulerFactoryBean schedulerFactoryBean;
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
    private String cleanJobCronName = "CLEAN_JOB";
    private String sysComponentJobName = "SYSCOMPONENT_JOB";
    private String cortexHistoryDataDelJobName = "CORTEXHISTORYDATADEL_JOB";
    private String lokiHistoryDataDelJobName = "LOKIHISTORYDATADELJOB_JOB";
    private String alertMessageHistoryPartManJobName = "ALERTMESSAGEHISTORYPARTMAN_JOB";
    private String recordtRuleManagerJobName = "RECORDTRULEMANAGER_JOB";
    private String cleanNoRealUseTerminalConnJobName = "CLEANNOREALUSETERMINALCONN_JOB";
    private String cleanExpiredTerminalRecordDataJob = "CLEAN_EXPIRED_TERMINAL_RECORD_DATA_JOB";
    private String sysBackFileSyncJob = "SYS_BACKUP_SYNC_JOB";

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
        return JobBuilder.newJob(MetricTypeEndpointStatusJob.class).withIdentity(JOB_NAME + endpointStateJobName).storeDurably().build();
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
    public JobDetail cleanJobDetail() {
        return JobBuilder.newJob(CleanJob.class).withIdentity(JOB_NAME + cleanJobCronName).storeDurably().build();
    }
	
    @Bean
    public JobDetail alertRuleManagerJobDetail() {
        return JobBuilder.newJob(AlertRuleManagerJob.class).withIdentity(JOB_NAME + alertRuleManagerJobName).storeDurably().build();
    }
	
    @Bean
    public JobDetail sysComponentJobDetail() {
        return JobBuilder.newJob(SysComponentJob.class).withIdentity(JOB_NAME + sysComponentJobName).storeDurably().build();
    }

    @Bean
    public JobDetail cortexHistoryDataDelJobDetail() {
        return JobBuilder.newJob(CortexHistoryDataDelJob.class).withIdentity(JOB_NAME + cortexHistoryDataDelJobName).storeDurably().build();
    }

    @Bean
    public JobDetail lokiHistoryDataDelJobDetail() {
        return JobBuilder.newJob(LokiHistoryDataDelJob.class).withIdentity(JOB_NAME + lokiHistoryDataDelJobName).storeDurably().build();
    }

    @Bean
    public JobDetail alertMessageHistoryPartManJobDetail() {
        return JobBuilder.newJob(AlertMessageHistoryPartitionManagementJob.class).withIdentity(JOB_NAME + alertMessageHistoryPartManJobName).storeDurably().build();
    }
    
    @Bean
    public JobDetail recordtRuleManagerJobDetail() {
        return JobBuilder.newJob(RecordManagerJob.class).withIdentity(JOB_NAME + recordtRuleManagerJobName).storeDurably().build();
    }

    @Bean
    public JobDetail cleanNoRealUseTerminalConnJobDetail() {
        return JobBuilder.newJob(CleanNoRealUseTerminalConnJob.class).withIdentity(JOB_NAME + cleanNoRealUseTerminalConnJobName).storeDurably().build();
    }

    @Bean
    public JobDetail cleanExpiredTerminalRecordDataJobDetail() {
        return JobBuilder.newJob(CleanExpiredTerminalRecordDataJob.class).withIdentity(JOB_NAME + cleanExpiredTerminalRecordDataJob).storeDurably().build();
    }

    @Bean
    public JobDetail sysBackFileSyncJobDetail() {
        return JobBuilder.newJob(SysBackFileSyncJob.class).withIdentity(JOB_NAME + sysBackFileSyncJob).storeDurably().build();
    }

    @PostConstruct
    public void init() throws SchedulerException {
        // agent 健康检查定时任务
        createCronScheduleJob(agentHealthCheckJobName, agentHealthyCronJobDetail(), StringUtils.isEmpty(agentHealthyJobCron) ? "0 0/1 * * * ? *" : agentHealthyJobCron);

        // asset talon state 如果配置文件没有配置定时周期  默认为1分钟执行一次
        createCronScheduleJob(assetTalonStatusJobName, assetTalonStatusCronJobDetail(), StringUtils.isEmpty(assetTalonJobCron) ? "0 0/1 * * * ? *" : assetTalonJobCron);

        // agent  如果配置文件没有配置定时周期  默认为 30s 执行一次
        createCronScheduleJob(agentJobName, agentCronJobDetail(), StringUtils.isEmpty(agentJobCron) ? "0/30 * * * * ?" : agentJobCron);

        // assetFeatureJob 如果配置文件没有配置定时周期 默认为5分钟执行一次
        createCronScheduleJob(assetFeatureJobName, assetFeatureCronJobDetail(), StringUtils.isEmpty(assetFeatureJobCron) ? "0 0/5 * 1 * ? *" : assetFeatureJobCron);

        // asset pingJob 如果配置文件没有配置定时周期  默认为1分钟执行一次
        createCronScheduleJob(assetPingStatusJobName, assetPingStatusCronJobDetail(), StringUtils.isEmpty(assetPingJobCron) ? "0 0/1 * * * ? *" : assetPingJobCron);

        // HA Job 如果配置文件没有配置定时周期  默认为5s执行一次
        createCronScheduleJob(haJobName, haJobDetail(), StringUtils.isEmpty(haJobCron) ? "0/5 * * * * ?" : haJobCron);

        // endpoint state job 如果配置文件没有配置定时周期  默认为1分钟执行一次
        createCronScheduleJob(endpointStateJobName, endpointStateJobDetail(), StringUtils.isEmpty(endpointStateJobCron) ? "0 0/1 * * * ? *" : endpointStateJobCron);

        // 告警静默状态变更线程
        //alertSilenceStatusJob 配置    如果配置文件没有配置定时周期 每5秒检测一次静默配置
        createCronScheduleJob(alertSilenceStatusJobName, alertSilenceStatusJobDetail(), StringUtils.isEmpty(alertSilenceJobCron) ? "0/10 * * * * ?" : alertSilenceJobCron);

        // 告警消息超时判断线程
        createCronScheduleJob(alertMessageExpiredJobName, alertMessageExpiredDetail(), StringUtils.isEmpty(alertMessageExpiredJobCron) ? "0/10 * * * * ?" : alertMessageExpiredJobCron);

        //告警规则管理线程
        createCronScheduleJob(alertRuleManagerJobName, alertRuleManagerJobDetail(), StringUtils.isEmpty(alertRuleManagerJobCron) ? "0/10 * * * * ?" : alertRuleManagerJobCron);

        // session time out job 如果配置文件没有配置定时周期  默认为5分钟执行一次
        createCronScheduleJob(sessionTimeOutJobName, sessionTimeOutJobDetail(), StringUtils.isEmpty(sessionTimeOutJobCron) ? "0 0/5 * * * ? *" : sessionTimeOutJobCron);

        // terminal clean job 如果配置文件没有配置定时周期  每天凌晨1点执行一次
        createCronScheduleJob(cleanJobCronName, cleanJobDetail(), StringUtils.isEmpty(terminalCleanJobCron) ? "0 0 1 * * ?" : terminalCleanJobCron);
        
        //sys component 状态更新线程
        createCronScheduleJob(sysComponentJobName, sysComponentJobDetail(), StringUtils.isEmpty(sysComponentJobCron) ? "0/30 * * * * ?" : sysComponentJobCron);

        // cortex metrics 历史数据删除
        createCronScheduleJob(cortexHistoryDataDelJobName, cortexHistoryDataDelJobDetail(), StringUtils.isEmpty(cortexHistoryDataDelJobCron) ? "0 0 0/2 * * ?" : cortexHistoryDataDelJobCron);

        // loki logs 历史数据删除
        createCronScheduleJob(lokiHistoryDataDelJobName, lokiHistoryDataDelJobDetail(), StrUtil.emptyToDefault(lokiHistoryDataDelJobCron, "0 0 0/2 * * ?"));

        // alert message History Partition Management 分区管理
        createCronScheduleJob(alertMessageHistoryPartManJobName, alertMessageHistoryPartManJobDetail(), StringUtils.isEmpty(alertMessageHistoryPartManJobCron) ? "0 0 0 1/1 * ?" : alertMessageHistoryPartManJobCron);
        
        // record rule 配置管理任务
        createCronScheduleJob(recordtRuleManagerJobName, recordtRuleManagerJobDetail(), StringUtils.isEmpty(recordtRuleManagerJobCron) ? "0/10 * * * * ?" : recordtRuleManagerJobCron);

        // 清理没有真实使用的 terminal ssh/telnet 连接，以超过最后数据交互时间 30 分钟为准 （sys_config.session_timeout）
        createCronScheduleJob(cleanNoRealUseTerminalConnJobName, cleanNoRealUseTerminalConnJobDetail(), StringUtils.isEmpty(cleanNoRealUseTerminalConnJobCron) ? "0 0/5 * * * ? *" : cleanNoRealUseTerminalConnJobCron);

        // terminal record expired data clean job
        createCronScheduleJob(cleanExpiredTerminalRecordDataJob, cleanExpiredTerminalRecordDataJobDetail(), Tool.StrUtil.emptyToDefault(cleanExpiredTerminalRecordDataJobCron, "0 0 1 * * ?"));

        // SysBackFileSyncJob
        createCronScheduleJob(sysBackFileSyncJob, sysBackFileSyncJobDetail(), Tool.StrUtil.emptyToDefault(sysBackFileSyncJobCron, "0 0 3 * * ?"));
    }
    
    /**
     * 根据 jobName jobDetail jobCronExpression  创建任务，如果存在则删除再创建
     * @param jobName
     * @param jobDetail
     * @param jobCronExpression
     * @throws Exception
     */
    public void createCronScheduleJob(String jobName, JobDetail jobDetail, String jobCronExpression) throws SchedulerException {
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
