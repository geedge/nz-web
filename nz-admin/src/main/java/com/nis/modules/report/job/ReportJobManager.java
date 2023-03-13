package com.nis.modules.report.job;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONPath;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ReportJobManager {

    private static Log log = Log.get();

    @Autowired
    @Lazy
    private Scheduler scheduler;

    public JobKey getJobKey(String jobName, String jobGroup) {
        return JobKey.jobKey(jobName, jobGroup);
    }

    public void createReportJob(String reportScheduleJsonStr, String jobName) throws SchedulerException {
        log.info("ReportJobManager create job,job name: {}", jobName);

        String scheduleType = JSONPath.read(reportScheduleJsonStr, "report.schedule.type").toString();
        String stime = JSONPath.read(reportScheduleJsonStr, "report.schedule.stime").toString();
        DateTime startAt = Tool.DateUtil.parse(stime, "yyyy-MM-dd HH:mm:ss");

        Object etimeObj = JSONPath.read(reportScheduleJsonStr, "report.schedule.etime");
        String etime = etimeObj != null ? etimeObj.toString() : "";
        DateTime endAt = StrUtil.isNotEmpty(etime) ? Tool.DateUtil.parse(etime, "yyyy-MM-dd HH:mm:ss") : null;

        // job 已结束，不再创建
        Date now = new Date();
        if (endAt != null && !now.before(endAt)) {
            log.info("Report endtime is before current time,job is over. dashboardId:{}", jobName);
            return;
        }

        JobKey jobKey = this.getJobKey(jobName, Constant.REPORT_JOB_GROUP);
        JobDetail jobDetail = JobBuilder.newJob(ReportJob.class).withIdentity(jobKey).storeDurably().build();

        switch (scheduleType) {
            case "1": {// one time
                SimpleTrigger simpleTrigger = TriggerBuilder.newTrigger().forJob(jobKey).withIdentity(jobKey.getName(), jobKey.getGroup())
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule())
                        .startNow().startAt(startAt).endAt(endAt).build();
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
                        .startNow().startAt(startAt).endAt(endAt).build();
                scheduler.scheduleJob(jobDetail, simpleTrigger);
                break;
            }
            default:
                break;
        }
    }

    /**
     * 根据 jobkey 查询 JOB 是否存在，存在则删除
     * @param dashboardId
     * @throws SchedulerException
     */
    public void delJobByName(String dashboardId) throws SchedulerException {
        JobKey jobKey = this.getJobKey(dashboardId, Constant.REPORT_JOB_GROUP);
        boolean b = scheduler.checkExists(jobKey);
        if (b) scheduler.deleteJob(jobKey);
    }

    /**
     * 根据 startAt 和 report 规则 判断本次是否执行
     *
     * @param scheduleType
     * @param stime
     * @param repeatStr
     * @param nums
     * @return
     */
    public static Boolean checkJobIsRun(String scheduleType, String stime, String repeatStr, String nums) {
        Date now = new Date();
        Date startAt = Tool.DateUtil.parse(stime, "yyyy-MM-dd HH:mm:ss");
        Integer repeat = Integer.valueOf(StrUtil.emptyToDefault(repeatStr, "1"));

        switch (scheduleType) {
            // one time
            case "1":
                return true;
            case "2": {// daily
                if (repeat == 1) return true;
                // 判断间隔时间
                long betweenDay = Tool.DateUtil.between(startAt, now, DateUnit.DAY);
                if (betweenDay % repeat == 0) return true;
                break;
            }
            case "3": {// weekly
                List<Integer> execWeekArray = Arrays.asList(nums.split(",")).stream().mapToInt(Integer::parseInt).sorted().boxed().collect(Collectors.toList());

                // 获取今天周几
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(now);
                int todayWeek = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1;
                if (todayWeek == 0) todayWeek = 7;

                // startAt 和当前时间在同一周，  符合 周几、周几 执行即可
                if (Tool.DateUtil.weekOfYear(startAt) == Tool.DateUtil.weekOfYear(now)) {
                    if (execWeekArray.contains(todayWeek)) return true;
                } else {
                    // startAt 和当前时间 不在同一周，根据 repeat + weekly 判断是否执行
                    if (repeat == 1) {
                        // 每周都执行 符合 周几、周几 执行即可
                        if (execWeekArray.contains(todayWeek)) return true;
                    } else {
                        // 根据今天是周几，找到 startAt 所在周 周几的日期，差值取模
                        Date date = getSpecifiedWeekByDate(startAt, todayWeek);
                        long betweenDay = Tool.DateUtil.between(date, now, DateUnit.DAY);
                        if (betweenDay % (repeat * 7) == 0 && execWeekArray.contains(todayWeek)) return true;
                    }
                }
                break;
            }
            case "4": {// monthly
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(now);
                int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
                List<String> execDayList = Arrays.asList(nums.split(","));

                if (StrUtil.contains(nums, "-1")) {
                    LocalDateTime lastDayOfMonth = now.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().with(TemporalAdjusters.lastDayOfMonth());
                    if (dayOfMonth == lastDayOfMonth.getDayOfMonth()) return true;
                }

                if (execDayList.contains(dayOfMonth + "")) return true;
                break;
            }
            default:
                break;
        }
        return false;
    }

    /**
     * 根据传入日期，获取所在周指定周几的 时间
     *
     * @param date
     * @param week
     * @return
     */
    public static Date getSpecifiedWeekByDate(Date date, Integer week) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        // 判断要计算的日期是否是周日，如果是则减一天计算周六的，否则会出问题，计算到下一周去了
        int dayWeek = cal.get(Calendar.DAY_OF_WEEK);
        if (1 == dayWeek) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
        // 设置一个星期的第一天
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        int day = cal.get(Calendar.DAY_OF_WEEK);// 获得当前日期是一个星期的第几天
        // 根据日历的规则，给当前日期减去星期几与一个星期第一天的差值
        cal.add(Calendar.DATE, cal.getFirstDayOfWeek() - day);
        // 获取指定星期的日期
        cal.add(Calendar.DATE, week - 1);
        return cal.getTime();
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
    /*public void makeDailyJobAndStart(String jobGroup, JobDataMap jobDataMap, DateTime startAt, DateTime endAt, Integer repeat) throws SchedulerException {
        JobKey jobKey = getJobKey(ReportConstant.REPORT_JOB_NAME, jobGroup);

        SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInHours(24 * repeat)
                .repeatForever();
        SimpleTrigger simpleTrigger = TriggerBuilder.newTrigger().forJob(jobKey).withIdentity(jobKey.getName(), jobKey.getGroup())
                .withSchedule(simpleScheduleBuilder)
                .startNow().startAt(startAt).endAt(endAt).build();

        JobDetail jobDetail = JobBuilder.newJob(ReportJob.class).withIdentity(jobKey).setJobData(jobDataMap).storeDurably().build();
        scheduler.scheduleJob(jobDetail, simpleTrigger);
    }*/

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
   /* public void makeWeeklyJobAndStart(String jobGroup, JobDataMap jobDataMap, DateTime startAt, DateTime endAt, int[] execWeekArray, Integer repeat) throws SchedulerException {
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
    }*/

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
    /*public void makeMonthlyJobAndStart(String jobGroup, JobDataMap jobDataMap, DateTime startAt, DateTime endAt, String execDayArray) throws SchedulerException {
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
    }*/
}
