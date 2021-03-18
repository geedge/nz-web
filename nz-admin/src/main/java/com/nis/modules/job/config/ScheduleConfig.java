package com.nis.modules.job.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.modules.sys.dao.SysConfigDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * 定时任务配置
 *

 */
@Configuration
@Slf4j
public class ScheduleConfig{
    @Autowired
    private DataSource dataSource;

    @Autowired
    private AutowiringSpringBeanJobFactory jobFactory;

    @Autowired
    private SysConfigDao sysConfigDao;

    @Bean
    public Properties quartzProperties() {
        // quartz参数
        Properties prop = new Properties();
        prop.put("org.quartz.scheduler.instanceName", "NeZhaScheduler");
        prop.put("org.quartz.scheduler.instanceId", "AUTO");
        // 线程池配置
        prop.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        prop.put("org.quartz.threadPool.threadCount", "20");
        prop.put("org.quartz.threadPool.threadPriority", "5");
        // JobStore配置
        prop.put("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
        // 集群配置
        prop.put("org.quartz.jobStore.isClustered", "true");
        prop.put("org.quartz.jobStore.clusterCheckinInterval", "15000");
        prop.put("org.quartz.jobStore.maxMisfiresToHandleAtATime", "1");

        prop.put("org.quartz.jobStore.misfireThreshold", "12000");
        prop.put("org.quartz.jobStore.tablePrefix", "QRTZ_");
        prop.put("org.quartz.jobStore.selectWithLockSQL", "SELECT * FROM {0}LOCKS UPDLOCK WHERE LOCK_NAME = ?");
        return prop;
    }

    @Bean("costomSchedulerFactoryBean")
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();

        // 设置自动启动
        factory.setAutoStartup(true);
        // 添加job工厂类
        factory.setJobFactory(jobFactory);

        SysConfigEntity sysConfigEntity = sysConfigDao.selectOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, "ha_mode"));
        String mode = sysConfigEntity.getParamValue();
        if (!"2".equals(mode)) {
            // 设置数据源
            factory.setDataSource(dataSource);
            // 配置文件
            factory.setQuartzProperties(quartzProperties());
        }
        return factory;
    }
}
