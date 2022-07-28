package com.nis.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class SchedulerConfig implements SchedulerFactoryBeanCustomizer {

    @Value("${quartz.enabled:true}")
    private boolean runJob;

    @Override
    public void customize(SchedulerFactoryBean schedulerFactoryBean) {

        //单元测试时不执行定时任务
        if (!runJob){
            schedulerFactoryBean.setAutoStartup(runJob);
        }

    }
}
