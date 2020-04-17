package com.nis.modules.alert.job;

import com.nis.common.utils.SpringContextUtils;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.sys.service.SysConfigService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlertMessageJobConfig {

    @Autowired
    SysConfigService configService;
    //SysConfigService configService = (SysConfigService) SpringContextUtils.getBean("SysConfigService");

    @Bean
    public JobDetail alertMessageJobDetail() {
        return JobBuilder.newJob(AlertMessageJob.class).withIdentity("alertMessageJob").storeDurably().build();
    }

    @Bean
    public Trigger alertMessageJobTrigger() {
        String alertMessageInterval= configService.getValue("alert_notify_interval");

        if (ToolUtil.isEmpty(alertMessageInterval)) {
            //如果配置文件没有配置定时周期  默认为3分钟执行一次
            alertMessageInterval = "300";
        }
        //CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(alertMessageJobCron);
        SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule();
        return TriggerBuilder.newTrigger()
                .forJob(alertMessageJobDetail())
                .withSchedule(simpleScheduleBuilder
                        .repeatForever()
                        .withIntervalInSeconds(Integer.parseInt(alertMessageInterval)))
                .build();
    }
}
