package com.nis.modules.endpoint.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.StringUtils;
import com.nis.modules.job.utils.ScheduleUtils;
import com.nis.modules.sys.dao.SysConfigDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.annotation.PostConstruct;

@Configuration
public class EndpointStateJobConfig {

    @Autowired
    @Qualifier("costomSchedulerFactoryBean")
    private SchedulerFactoryBean schedulerFactoryBean;

    @Autowired
    private SysConfigDao sysConfigDao;

    private String endpointStateJobName = "ENDPOINTSTATE_JOB";

    @Bean
    public JobDetail endpointStateJobDetail() {
        return JobBuilder.newJob(EndpointStateJob.class).withIdentity(ScheduleUtils.JOB_NAME + endpointStateJobName).storeDurably().build();
    }

    @PostConstruct
    public void init(){
        Scheduler scheduler = schedulerFactoryBean.getScheduler();

        // 查询job执行周期
        SysConfigEntity scrapeIntervalConfig = sysConfigDao.selectOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ENDPOINT_QUERYINTERVAL));

        String scrapeInterval;
        // 为空设置默认周期为 prometheus 刮取周期
        if (scrapeIntervalConfig == null || StringUtils.isEmpty(scrapeIntervalConfig.getParamValue())) {
            SysConfigEntity prometheusScrapeIntervalConfig = sysConfigDao.selectOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_SCRAPEINTERVAL));
            scrapeInterval = prometheusScrapeIntervalConfig.getParamValue();
        }else {
            scrapeInterval = scrapeIntervalConfig.getParamValue();
        }

        try {
			ScheduleUtils.createSimpleScheduleJob(endpointStateJobName, endpointStateJobDetail(), Integer.parseInt(scrapeInterval), scheduler);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
    }
}