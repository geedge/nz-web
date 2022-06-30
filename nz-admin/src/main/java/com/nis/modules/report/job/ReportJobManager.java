package com.nis.modules.report.job;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONPath;
import com.nis.common.utils.Tool;
import com.nis.modules.report.config.ReportConstant;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Calendar;
import java.util.*;

@Component
public class ReportJobManager {

    private static Log log = Log.get();

    @Autowired
    @Lazy
    private Scheduler scheduler;

    public static final JobKey getJobKey(String jobName, String jobGroup) {
        return JobKey.jobKey(jobName, jobGroup);
    }

    /**
     * 创建调度
     *
     * @param reportScheduleJsonStr report schedule config json str
     * @param jobGroup              jobGroup panelId
     * @throws SchedulerException
     */
    public void createReportJob(String reportScheduleJsonStr, String jobGroup) throws SchedulerException {
        log.info("ReportJobManager create job,job group: {}", jobGroup);

        String scheduleType = JSONPath.read(reportScheduleJsonStr, "report.schedule.type").toString();
        String stime = JSONPath.read(reportScheduleJsonStr, "report.schedule.stime").toString();
        DateTime startAt = Tool.DateUtil.parse(stime, "yyyy-MM-dd HH:mm:ss");

        Object etimeObj = JSONPath.read(reportScheduleJsonStr, "report.schedule.etime");
        String etime = etimeObj != null ? etimeObj.toString() : "";
        DateTime endAt = StrUtil.isNotEmpty(etime) ? Tool.DateUtil.parse(etime, "yyyy-MM-dd HH:mm:ss") : null;

        // job 已结束，不再创建
        Date now = new Date();
        if (endAt != null && !now.before(endAt)) {
            log.info("Report endtime is before current time,job is over.panelId:{}", jobGroup);
            return;
        }

        String repeatStr = JSONPath.read(reportScheduleJsonStr, "report.schedule.repeat").toString();
        Integer repeat = Integer.valueOf(StrUtil.emptyToDefault(repeatStr, "1"));

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(ReportConstant.REPORT_JOB_DATAMAP, reportScheduleJsonStr);

        switch (scheduleType) {
            case "1": {// one time
                JobKey jobKey = getJobKey(ReportConstant.REPORT_JOB_NAME, jobGroup);
                SimpleTrigger simpleTrigger = TriggerBuilder.newTrigger().forJob(jobKey).withIdentity(jobKey.getName(), jobKey.getGroup())
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule())
                        .startNow().startAt(startAt).endAt(endAt).build();
                JobDetail jobDetail = JobBuilder.newJob(ReportJob.class).withIdentity(jobKey).setJobData(jobDataMap).storeDurably().build();
                scheduler.scheduleJob(jobDetail, simpleTrigger);
                break;
            }
            case "2": {// daily
                makeDailyJobAndStart(jobGroup, jobDataMap, startAt, endAt, repeat);
                break;
            }
            case "3": {// weekly.
                String nums = JSONPath.read(reportScheduleJsonStr, "report.schedule.nums").toString().replaceAll("\\[|\\]", "");
                int[] execWeekArray = Arrays.asList(nums.split(",")).stream().mapToInt(Integer::parseInt).sorted().toArray();

                makeWeeklyJobAndStart(jobGroup, jobDataMap, startAt, endAt, execWeekArray, repeat);
                break;
            }
            case "4": {// monthly
                String execDayArray = JSONPath.read(reportScheduleJsonStr, "report.schedule.nums").toString();
                makeMonthlyJobAndStart(jobGroup, jobDataMap, startAt, endAt, execDayArray);
            }
            default:
                break;
        }
    }

    /**
     * 每隔几天执行一次
     *
     * @param jobGroup
     * @param jobDataMap
     * @param startAt
     * @param endAt
     * @param repeat
     * @throws SchedulerException
     */
    public void makeDailyJobAndStart(String jobGroup, JobDataMap jobDataMap, DateTime startAt, DateTime endAt, Integer repeat) throws SchedulerException {
        JobKey jobKey = getJobKey(ReportConstant.REPORT_JOB_NAME, jobGroup);

        SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInHours(24 * repeat)
                .repeatForever();
        SimpleTrigger simpleTrigger = TriggerBuilder.newTrigger().forJob(jobKey).withIdentity(jobKey.getName(), jobKey.getGroup())
                .withSchedule(simpleScheduleBuilder)
                .startNow().startAt(startAt).endAt(endAt).build();

        JobDetail jobDetail = JobBuilder.newJob(ReportJob.class).withIdentity(jobKey).setJobData(jobDataMap).storeDurably().build();
        scheduler.scheduleJob(jobDetail, simpleTrigger);
    }


    /**
     * 每隔几周的 周一，周三、、、 执行
     *
     * @param jobGroup
     * @param jobDataMap
     * @param startAt
     * @param endAt
     * @param execWeekArray
     * @param repeat
     * @throws SchedulerException
     */
    public void makeWeeklyJobAndStart(String jobGroup, JobDataMap jobDataMap, DateTime startAt, DateTime endAt, int[] execWeekArray, Integer repeat) throws SchedulerException {
        SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInHours(24 * 7 * repeat)// 每搁几周执行一次
                .repeatForever();

        // 获取今天周几
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        int todayWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (todayWeek == 0) todayWeek = 7;

        Date now = new Date();
        for (int i = 0; i < execWeekArray.length; i++) {
            int execWeek = execWeekArray[i];
            JobKey jobKey = getJobKey(ReportConstant.REPORT_JOB_NAME + "-" + execWeek, jobGroup);
            SimpleTrigger simpleTrigger = null;
            // 如果配置星期为当天 ，判断开始时间是否大于当前时间，大于则设置为JOB startAt（当天执行）
            if (todayWeek == execWeek) {
                if (!now.before(startAt)) {
                    simpleTrigger = TriggerBuilder.newTrigger().forJob(jobKey).withIdentity(jobKey.getName(), jobKey.getGroup())
                            .withSchedule(simpleScheduleBuilder)
                            .startNow().startAt(startAt).endAt(endAt).build();
                } else {
                    // 使用 startAt 找到最近的星期 更新 startAt 例：获取下一次周二的日期。如果当前日期是周一则获取的是本周二日期，如果是周二到周日，获取的则是下周二日期
                    LocalDateTime ldt = startAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().with(TemporalAdjusters.next(DayOfWeek.of(execWeek)));
                    Date nextWeekDate = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
                    simpleTrigger = TriggerBuilder.newTrigger().forJob(jobKey).withIdentity(jobKey.getName(), jobKey.getGroup())
                            .withSchedule(simpleScheduleBuilder)
                            .startNow().startAt(nextWeekDate).endAt(endAt).build();
                }
            } else {
                // 使用 startAt 找到最近的星期 更新 startAt
                LocalDateTime ldt = startAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().with(TemporalAdjusters.next(DayOfWeek.of(execWeek)));
                Date nextWeekDate = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
                simpleTrigger = TriggerBuilder.newTrigger().forJob(jobKey).withIdentity(jobKey.getName(), jobKey.getGroup())
                        .withSchedule(simpleScheduleBuilder)
                        .startNow().startAt(nextWeekDate).endAt(endAt).build();
            }
            JobDetail jobDetail = JobBuilder.newJob(ReportJob.class).withIdentity(jobKey).setJobData(jobDataMap).storeDurably().build();
            scheduler.scheduleJob(jobDetail, simpleTrigger);
        }
    }

    /**
     * 每月指定日期执行 -1 表示最后一天 'L'
     *
     * @param jobGroup
     * @param jobDataMap
     * @param startAt
     * @param endAt
     * @param execDayArray
     * @throws SchedulerException
     */
    public void makeMonthlyJobAndStart(String jobGroup, JobDataMap jobDataMap, DateTime startAt, DateTime endAt, String execDayArray) throws SchedulerException {
        // 每月几号执行
        List<String> cronExpreLiost = new ArrayList<>();
        if (StrUtil.contains(execDayArray, "-1")) {
            // throw new ParseException("Support for specifying 'L' and 'LW' with other days of the month is not implemented", -1);
            execDayArray = execDayArray.replaceAll("\\[|\\]", "").replaceAll("-1", "");
            cronExpreLiost.add(String.format("0 0 0 %s * ?", execDayArray));// 其他日期
            cronExpreLiost.add(String.format("0 0 0 %s * ?", "L")); // 最后一天 Last
        }

        // String timezone = sysConfService.getValue("timezone");
        // timezone = StrUtil.emptyToDefault(timezone, TimeZone.getDefault().getID());

        for (int i = 0; i < cronExpreLiost.size(); i++) {
            CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpreLiost.get(i));
            JobKey jobKey = getJobKey(ReportConstant.REPORT_JOB_NAME + "-" + i, jobGroup);

            // 不触发立即执行，等待下次 Cron 触发频率到达时刻开始按照 Cron 频率依次执行
            CronTrigger cronTrigger = TriggerBuilder.newTrigger().forJob(jobKey).withIdentity(jobKey.getName(), jobKey.getGroup())
                    .withSchedule(cronScheduleBuilder.withMisfireHandlingInstructionDoNothing())// .inTimeZone(TimeZone.getTimeZone(timezone))
                    .startAt(startAt).endAt(endAt).build();
            JobDetail jobDetail = JobBuilder.newJob(ReportJob.class).withIdentity(jobKey).setJobData(jobDataMap).storeDurably().build();
            scheduler.scheduleJob(jobDetail, cronTrigger);
        }
    }


    /**
     * 根据 group 查找 Job,找到关闭 Job
     *
     * @param panelId
     * @throws SchedulerException
     */
    public void findJobAndDelByGroup(String panelId) throws SchedulerException {
        // 新增|修改 JOB  先删后增
        Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.groupEquals(panelId));
        if (Tool.CollectionUtil.isNotEmpty(jobKeys)) {
            for (JobKey key : jobKeys) {
                scheduler.deleteJob(key);
            }
        }
    }
}
