package com.nis.modules.sys.job;

import cn.hutool.core.date.DateTime;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONPath;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class SysBackJobManager {

    private static Log log = Log.get();

    public static final String SYS_BACKUP_JOBNAME = "NZ_SYS_BACKUP_JOB";

    @Lazy
    @Autowired
    private Scheduler scheduler;

    public JobKey getJobKey(String jobName, String jobGroup) {
        return JobKey.jobKey(jobName, jobGroup);
    }

    /**
     * 创建任务
     *
     * @param scheduleJsonStr
     * @throws SchedulerException
     */
    public void createSysBackJob(String scheduleJsonStr) throws SchedulerException {
        log.info("SysBackJobManager create job,job name: {}", SysBackJobManager.SYS_BACKUP_JOBNAME);
        String scheduleType = JSONPath.read(scheduleJsonStr, "schedule.type").toString();
        String stime = JSONPath.read(scheduleJsonStr, "schedule.stime").toString();
        DateTime startAt = Tool.DateUtil.parse(stime, "yyyy-MM-dd HH:mm:ss");

        JobKey jobKey = this.getJobKey(SysBackJobManager.SYS_BACKUP_JOBNAME, Constant.BACKUP_JOB_GROUP);
        JobDetail jobDetail = JobBuilder.newJob(SysBackJob.class).withIdentity(jobKey).storeDurably().build();

        switch (scheduleType) {
            case "1": {// one time
                SimpleTrigger simpleTrigger = TriggerBuilder.newTrigger().forJob(jobKey).withIdentity(jobKey.getName(), jobKey.getGroup())
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule())
                        .startNow().startAt(startAt).build();
                scheduler.scheduleJob(jobDetail, simpleTrigger);
                break;
            }
            case "2": // daily
            case "3": // weekly
            case "4": {// monthly
                // 创建每天执行的定时任务，在定时任务中判定是否执行
                SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInHours(24)
                        .repeatForever();
                SimpleTrigger simpleTrigger = TriggerBuilder.newTrigger().forJob(jobKey).withIdentity(jobKey.getName(), jobKey.getGroup())
                        .withSchedule(simpleScheduleBuilder)
                        .startNow().startAt(startAt).build();
                scheduler.scheduleJob(jobDetail, simpleTrigger);
                break;
            }
            default:
                break;
        }
        log.info("SysBackJobManager create job success.");
    }

    /**
     * 删除任务
     *
     * @throws SchedulerException
     */
    public void delJob() throws SchedulerException {
        log.info("SysBackJobManager delete job,job name: {}", SysBackJobManager.SYS_BACKUP_JOBNAME);
        JobKey jobKey = this.getJobKey(SysBackJobManager.SYS_BACKUP_JOBNAME, Constant.BACKUP_JOB_GROUP);
        boolean b = scheduler.checkExists(jobKey);
        if (b) {
            scheduler.deleteJob(jobKey);
            log.info("SysBackJobManager delete job success");
        } else {
            log.info("SysBackJobManager not found job");
        }
    }
}
