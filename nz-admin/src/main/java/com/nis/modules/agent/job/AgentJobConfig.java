package com.nis.modules.agent.job;

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
public class AgentJobConfig {

    @Value("${nezha.agentJobCron}")
    private String agentJobCron;

    @Autowired
    @Qualifier("costomSchedulerFactoryBean")
    private SchedulerFactoryBean schedulerFactoryBean;

    private String agentJobName = "AGENT_JOB";

    @Bean
    public JobDetail agentCronJobDetail() {
        return JobBuilder.newJob(AgentJob.class).withIdentity(ScheduleUtils.JOB_NAME + agentJobName).storeDurably().build();
    }

    @PostConstruct
    public void init() throws SchedulerException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();

        // 如果配置文件没有配置定时周期  默认为 30s 执行一次
        if (agentJobCron.isEmpty()) {
            agentJobCron = "0/30 * * * * ?";
        }

        ScheduleUtils.createCronScheduleJob(agentJobName, agentCronJobDetail(), agentJobCron, scheduler);
    }
}
