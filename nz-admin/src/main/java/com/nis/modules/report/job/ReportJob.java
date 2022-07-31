package com.nis.modules.report.job;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.MailUtil;
import com.nis.common.utils.TemplateUtil;
import com.nis.common.utils.Tool;
import com.nis.modules.panel.entity.VisualPanel;
import com.nis.modules.panel.service.VisualPanelService;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysConfService;
import com.nis.modules.sys.service.SysUserService;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Component
@DisallowConcurrentExecution
public class ReportJob extends QuartzJobBean {

    private static final Log log = Log.get();

    private static Template template;

    @Autowired
    private MailUtil mailUtil;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysConfService sysConfService;

    @Autowired
    private VisualPanelService visualPanelService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        // 检查 haMode
        String serverId = (String) redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
        if (!StrUtil.equals(serverId, Constant.SERVER_ID)) return;

        JobKey key = context.getJobDetail().getKey();
        log.info(String.format("Job name: %s, Job group: %s", key.getName(), key.getGroup()));

        String panelId = key.getName();
        VisualPanel visualPanel = visualPanelService.getById(panelId);
        if (visualPanel == null) {
            log.error("dashboard report panel not found, panelId:{}", panelId);
            return;
        }

        // JOB 每天都执行，根据匹配规则，检查本次是否执行
        String configJsonStr = visualPanel.getParam().toString();
        Boolean isRun = this.checkJobIsRun(configJsonStr);
        if (isRun) {
            log.info("dashboard report notific begin ...");
            String receiversStr = JSONPath.read(configJsonStr, "report.receivers").toString();
            if (StrUtil.isEmpty(receiversStr)) {
                log.error("dashboard report receiver user is empty, panelId:{}", panelId);
                return;
            }

            List<String> receiverIds = JSONObject.parseArray(receiversStr, String.class);
            List<SysUserEntity> userList = sysUserService.list(new LambdaQueryWrapper<SysUserEntity>().in(SysUserEntity::getId, receiverIds));
            if (receiverIds.size() != userList.size()) {
                log.error("dashboard report receiver user not found, panelId:{}", panelId);
                return;
            }

            // 计算 query_range start&end
            String timezone = sysConfService.getValue("timezone");
            timezone = StrUtil.emptyToDefault(timezone, TimeZone.getDefault().getID());
            Map<String, Long> queryRangeStartAndEndTime = getQueryRangeStartAndEndTime(configJsonStr, timezone.trim());
            log.info("dashboard report range data, panelId:{}, query_range start&end {}", panelId, JSONObject.toJSONString(queryRangeStartAndEndTime));

            try {
                Template template = getEmailTemplate();
                String content = this.genMailContent(visualPanel, template);

                String format = "html";
                String fileName = String.format("%s.%s", visualPanel.getName(), format);
                for (SysUserEntity user : userList) {
                    // 以用户默认语言生成 快照文件
                    String lang = user.getLang();
                    String snapshotStr = visualPanelService.snapshot(format, Integer.valueOf(panelId), queryRangeStartAndEndTime.get("start"), queryRangeStartAndEndTime.get("end"), StrUtil.emptyToDefault(lang, "zh"));

                    if (StrUtil.isEmpty(snapshotStr)) {
                        log.error("dashboard snapshot generate error");
                        continue;
                    }

                    File file = null;
                    try {
                        // 写到临时目录下，通知用户之后，此文件删除
                        file = Tool.FileUtil.newFile(Constant.TEMP_PATH + File.separator + fileName);
                        Tool.FileUtil.writeUtf8String(snapshotStr, file);

                        // 发送邮件通知
                        mailUtil.sendHTMLAndAppendFile(Tool.ListUtil.toList(user.getEmail()), "Dashboard report", content, file.getAbsolutePath());
                        log.info("dashboard report notific success ,user {}", user.getUsername());
                    } finally {
                        if (file != null && file.exists()) file.delete();
                    }
                }
            } catch (IOException | TemplateException | MessagingException e) {
                log.error("dashboard report notific error.", e);
            }
            log.info("dashboard report notific finished.");
        } else {
            log.info("dashboard report is not executed, this time does't match the report schedule");
        }
    }


    /**
     * 根据 startAt 和 report 规则 判断本次是否执行
     * @param reportScheduleJsonStr
     * @return
     */
    public Boolean checkJobIsRun(String reportScheduleJsonStr) {
        String scheduleType = JSONPath.read(reportScheduleJsonStr, "report.schedule.type").toString();

        String stime = JSONPath.read(reportScheduleJsonStr, "report.schedule.stime").toString();
        Date startAt = Tool.DateUtil.parse(stime, "yyyy-MM-dd HH:mm:ss");
        Date now = new Date();

        String repeatStr = JSONPath.read(reportScheduleJsonStr, "report.schedule.repeat").toString();
        Integer repeat = Integer.valueOf(StrUtil.emptyToDefault(repeatStr, "1"));

        switch (scheduleType) {
            // one time
            case "1": return true;
            case "2": {// daily
                if (repeat == 1) return true;
                // 判断间隔时间
                long betweenDay = Tool.DateUtil.between(startAt, now, DateUnit.DAY);
                if (betweenDay % repeat == 0) return true;
                break;
            }
            case "3": {// weekly
                String nums = JSONPath.read(reportScheduleJsonStr, "report.schedule.nums").toString().replaceAll("\\[|\\]", "");
                List<Integer> execWeekArray = Arrays.asList(nums.split(",")).stream().mapToInt(Integer::parseInt).sorted().boxed().collect(Collectors.toList());

                // 获取今天周几
                Calendar cal = Calendar.getInstance();
                cal.setTime(now);
                int todayWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
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
                        Date date = this.getSpecifiedWeekByDate(startAt, todayWeek);
                        long betweenDay = Tool.DateUtil.between(date, now, DateUnit.DAY);
                        if (betweenDay % (repeat * 7) == 0 && execWeekArray.contains(todayWeek)) return true;
                    }
                }
                break;
            }
            case "4": {// monthly
                Calendar cal = Calendar.getInstance();
                cal.setTime(now);
                int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
                String execDayArray = JSONPath.read(reportScheduleJsonStr, "report.schedule.nums").toString();
                List<String> execDayList = Arrays.asList(execDayArray.split(","));

                if (StrUtil.contains(execDayArray, "-1")) {
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
     * mail content
     *
     * @param visualPanel
     * @param template
     * @return
     * @throws IOException
     * @throws TemplateException
     */
    private String genMailContent(VisualPanel visualPanel, Template template) throws IOException, TemplateException {
        Map<String, Object> modelMap = new HashMap<>();
        modelMap.put("panel", visualPanel);
        String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, modelMap);
        return content;
    }

    /**
     * 获取email 模板
     *
     * @return
     * @throws IOException
     */
    private Template getEmailTemplate() throws IOException {
        if (template == null) {
            String templateString = sysConfService.getValue(Constant.REPORT_EMAIL_TEMPLATE);
            template = TemplateUtil.stringToTemplate(templateString, "reportEmailTemplate");
        }
        return template;
    }


    /**
     * 根据传入日期，获取所在周指定周几的 时间
     * @param date
     * @param week
     * @return
     */
    public Date getSpecifiedWeekByDate(Date date, Integer week) {
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
     * 根据 report range param 得到 startTime & endTime
     *
     * @param rangeConfigJsonStr
     * @param timezone
     * @return
     */
    private Map<String, Long> getQueryRangeStartAndEndTime(String rangeConfigJsonStr, String timezone) {
        // previous last
        String rangeType = JSONPath.read(rangeConfigJsonStr, "report.range.type").toString();
        rangeType = StrUtil.emptyToDefault(rangeType, "last");
        // hour day week month
        String rangeUnit = JSONPath.read(rangeConfigJsonStr, "report.range.unit").toString().toLowerCase();
        rangeUnit = StrUtil.emptyToDefault(rangeUnit, "hour");
        // 范围值
        String rangeIntervalStr = JSONPath.read(rangeConfigJsonStr, "report.range.interval").toString();
        Integer interval = Integer.valueOf(StrUtil.emptyToDefault(rangeIntervalStr, "1"));

        Map map = new HashMap(4);
        if (StrUtil.equalsIgnoreCase("last", rangeType)) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeZone(TimeZone.getTimeZone(timezone));
            cal.setTimeInMillis(System.currentTimeMillis());
            // 当前时间为 endTime 单位秒
            Long end = cal.getTimeInMillis() / 1000;
            switch (rangeUnit) {
                case "hour":
                    cal.add(Calendar.HOUR_OF_DAY, -interval);
                    break;
                case "day":
                    cal.add(Calendar.DATE, -interval);
                    break;
                case "week":
                    cal.add(Calendar.WEEK_OF_YEAR, -interval);
                    break;
                case "month":
                    cal.add(Calendar.MONTH, -interval);
                    break;
                default:
                    break;
            }
            map.put("start", cal.getTimeInMillis() / 1000);
            map.put("end", end);
        } else if (StrUtil.equalsIgnoreCase("previous", rangeType)) {
            Long start = null, end = null;
            switch (rangeUnit) {
                case "hour": // 前几小时的开始时间 xx:00:00 - 前一小时的最后时间 xx:59:59
                    start = this.getPreHourStartTime(interval, timezone);
                    end = this.getPreHourEndTime(timezone);
                    break;
                case "day":// 前几天的开始时间 00:00:00 - 前一天的最后时间 23:59:59
                    start = this.getPreDayStartTime(interval, timezone);
                    end = this.getPreDayEndTime(timezone);
                    break;
                case "week":// 前几周的开始时间 00:00:00 - 前一天的最后时间 23:59:59
                    start = this.getPreWeekStartTime(interval, timezone);
                    end = this.getPreDayEndTime(timezone);
                    break;
                case "month":// 前几月的开始时间 00:00:00 - 前一天的最后时间 23:59:59
                    start = this.getPreMonthStartTime(interval, timezone);
                    end = this.getPreDayEndTime(timezone);
                    break;
                default:
                    break;
            }
            map.put("start", start);
            map.put("end", end);
        }
        return map;
    }

    /**
     * 获取前几小时的开始时间 xx:00:00
     *
     * @param interval
     * @param timezone
     * @return
     */
    private Long getPreHourStartTime(Integer interval, String timezone) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone(timezone));
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.HOUR_OF_DAY, -interval);
        cal.set(Calendar.HOUR_OF_DAY, cal.getTime().getHours());
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTimeInMillis() / 1000;
    }

    /**
     * 获取前一小时的最后时间 xx:59:59
     *
     * @param timezone
     * @return
     */
    private Long getPreHourEndTime(String timezone) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone(timezone));
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.HOUR_OF_DAY, -1);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        return cal.getTimeInMillis() / 1000;
    }

    /**
     * 获取前几天的开始时间 00:00:00
     *
     * @param interval
     * @param timezone
     * @return
     */
    private Long getPreDayStartTime(Integer interval, String timezone) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone(timezone));
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.DATE, -interval);
        cal.set(Calendar.DATE, cal.getTime().getDate());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTimeInMillis() / 1000;
    }

    /**
     * 获取前一天的最后时间 23:59:59
     *
     * @param timezone
     * @return
     */
    private Long getPreDayEndTime(String timezone) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone(timezone));
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.DATE, -1);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        return cal.getTimeInMillis() / 1000;
    }

    /**
     * 前一周的开始时间 00:00:00  (一周=7天)
     *
     * @param interval
     * @param timezone
     * @return
     */
    private Long getPreWeekStartTime(Integer interval, String timezone) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone(timezone));
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.DATE, -interval * 7);
        cal.set(Calendar.DATE, cal.getTime().getDate());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTimeInMillis() / 1000;
    }

    /**
     * 前一月的开始时间 00:00:00
     *
     * @param interval
     * @param timezone
     * @return
     */
    private Long getPreMonthStartTime(Integer interval, String timezone) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone(timezone));
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.MONTH, -interval);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTimeInMillis() / 1000;
    }

}
