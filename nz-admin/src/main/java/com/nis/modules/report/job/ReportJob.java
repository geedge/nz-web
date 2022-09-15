package com.nis.modules.report.job;

import cn.hutool.core.util.ReUtil;
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
import com.nis.modules.agent.service.PromApiService;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@DisallowConcurrentExecution
public class ReportJob extends QuartzJobBean {

    private static final Log log = Log.get();

    private static Template template;

    static Map<String, String> SPECIAL_KEY_MAP;

    static {
        SPECIAL_KEY_MAP = new HashMap<>(8);

        SPECIAL_KEY_MAP.put("label_names", "^label_names\\(\\)\\s*$");
        SPECIAL_KEY_MAP.put("label_values", "^label_values\\((?:(.+),\\s*)?([a-zA-Z_][a-zA-Z0-9_]*)\\)\\s*$");
        SPECIAL_KEY_MAP.put("metrics", "^metrics\\((.+)\\)\\s*$");
        SPECIAL_KEY_MAP.put("query_result", "^query_result\\((.+)\\)\\s*$");
    }

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

    @Autowired
    private PromApiService promApiService;

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
        String scheduleType = JSONPath.read(configJsonStr, "report.schedule.type").toString();
        String stime = JSONPath.read(configJsonStr, "report.schedule.stime").toString();
        String repeatStr = JSONPath.read(configJsonStr, "report.schedule.repeat").toString();
        String nums = JSONPath.read(configJsonStr, "report.schedule.nums").toString().replaceAll("\\[|\\]", "");

        Boolean isRun = ReportJobManager.checkJobIsRun(scheduleType, stime, repeatStr, nums);
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

                Long start = queryRangeStartAndEndTime.get("start");
                Long end = queryRangeStartAndEndTime.get("end");

                // 模拟前端实现 组织快照接口 vars 参数
                List<Map<String, String>> snapshotVars = this.getSnapshotVars(configJsonStr, start, end);
                log.info("dashboard report snapshot vars: {}", JSONObject.toJSONString(snapshotVars));

                for (SysUserEntity user : userList) {
                    // 以用户默认语言生成 快照文件
                    String lang = user.getLang();
                    String snapshotStr = visualPanelService.genSnapshotByTemplate(Integer.valueOf(panelId), start, end, lang, JSONObject.toJSONString(snapshotVars));

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

    private List<Map<String, String>> getSnapshotVars(String paramStr, Long start, Long end) {
        Map map = JSONObject.parseObject(paramStr, Map.class);
        List<Map> variables = (List<Map>) map.get("variables");

        List<Map<String, String>> snapshotVars = new ArrayList<>();
        for (Map varMap : variables) {
            String varJsonStr = JSONObject.toJSONString(varMap);
            String showStr = JSONPath.read(varJsonStr, "show").toString();
            showStr = Tool.StrUtil.emptyToDefault(showStr, "true");

            // 与前端同步 不展示的不计算 vars
            if (!Tool.StrUtil.equals(showStr, "true")) continue;

            List<String> valueList = new ArrayList<>();
            String type = JSONPath.read(varJsonStr, "type").toString();
            if (Tool.StrUtil.equals(type, "custom")) {
                String customOptions = JSONPath.read(varJsonStr, "customOptions").toString();
                List<String> customOptionList = Arrays.asList(customOptions.split(","));
                for (String option : customOptionList) {
                    option = option.trim();
                    String[] split = option.split(":");
                    if (split.length == 1) {
                        valueList.add(option);
                    } else {
                        valueList.add(split[1]);
                    }
                }
            } else if (Tool.StrUtil.equals(type, "query")) {
                String expression = JSONPath.read(varJsonStr, "expression").toString();
                List<String> promiseListByExpr = this.getPromiseListByExpr(expression, start, end);
                Object regexObj = JSONPath.read(varJsonStr, "regex");
                String regex = regexObj == null ? StrUtil.EMPTY : regexObj.toString();
                String name = JSONPath.read(varJsonStr, "name").toString();
                if (!StrUtil.isEmpty(regex)) {
                    if (regex.charAt(0) == '/') {
                        regex = regex.substring(1, regex.length());
                    }
                    if (regex.charAt(regex.length() - 1) == '/') {
                        regex = regex.substring(0, regex.length() - 1);
                    }
                    // 和前端一致 regenx 后根据 value 去重
                    List<String> textValueList = Tool.ListUtil.list(false);
                    for (String str : promiseListByExpr) {
                        Matcher matcher = Pattern.compile(regex).matcher(str);
                        if (matcher.find()) {
                            // regex： asset="(?<text>[^"]+).*asset_id="(?<value>[^"]+)
                            // String text = matcher.group("text");
                            try {
                                // 无法直接获得 Pattern.namedGroups
                                String value = matcher.group("value");
                                String text = matcher.group("text");
                                textValueList.add(value);
                                // valueList.add(value);
                            } catch (Exception e) {
                                valueList.add(str);
                            }
                        }
                    }
                    if (Tool.CollUtil.isNotEmpty(textValueList)) {
                        textValueList = textValueList.stream().distinct().collect(Collectors.toList());
                        valueList.addAll(textValueList);
                    }
                } else {
                    valueList.addAll(promiseListByExpr);
                }
            }

            Map m = new HashMap(4);
            String name = JSONPath.read(varJsonStr, "name").toString();
            m.put("name", name);

            // 根据 allOption 确定最终值
            String allOptionStr = JSONPath.read(varJsonStr, "allOption").toString();
            allOptionStr = Tool.StrUtil.emptyToDefault(allOptionStr, "false");

            if (Tool.StrUtil.equals(allOptionStr, "true")) {
                // allOption = true  时，变量值 为 .*
                m.put("value", ".*");
            } else {
                // 否则 取第一个值
                m.put("value", Tool.CollectionUtil.isEmpty(valueList) ? "" : valueList.get(0));
            }
            snapshotVars.add(m);
        }
        return snapshotVars;
    }

    /**
     * 根据表达式查询api并简单处理返回，返回结果后续进行正则校验
     *
     * @param expression
     * @param start
     * @param end
     * @return
     */
    private List<String> getPromiseListByExpr(String expression, Long start, Long end) {
        List<String> promiseList = new ArrayList<>();
        boolean isLabelNames = Tool.ReUtil.isMatch(SPECIAL_KEY_MAP.get("label_names"), expression);
        if (isLabelNames) {
            JSONObject resultMap = promApiService.labels("/api/v1/labels", String.valueOf(start), String.valueOf(end), null);
            List<String> labelList = (List<String>) resultMap.get("data");

            promiseList = Tool.CollectionUtil.isEmpty(labelList) ? new ArrayList<>() : labelList;
            return promiseList;
        }

        boolean isLabelValues = Tool.ReUtil.isMatch(SPECIAL_KEY_MAP.get("label_values"), expression);
        if (isLabelValues) {
            List<String> allGroups = Tool.ReUtil.getAllGroups(Pattern.compile(SPECIAL_KEY_MAP.get("label_values")), expression, false);
            if (allGroups.size() == 1) {
                promiseList = this.labelValuesQuery(allGroups.get(0), StrUtil.EMPTY, start, end);
            } else if (allGroups.size() == 2) {
                promiseList = this.labelValuesQuery(allGroups.get(0), allGroups.get(1), start, end);
            }
            return promiseList;
        }

        boolean isMetrics = Tool.ReUtil.isMatch(SPECIAL_KEY_MAP.get("metrics"), expression);
        if (isMetrics) {
            String metricFilterPattern = Tool.ReUtil.getGroup1(SPECIAL_KEY_MAP.get("metrics"), expression);
            promiseList = this.metricNameQuery(metricFilterPattern, start, end);
            return promiseList;
        }

        boolean isQueryResult = Tool.ReUtil.isMatch(SPECIAL_KEY_MAP.get("query_result"), expression);
        if (isQueryResult) {
            String query = Tool.ReUtil.getGroup1(SPECIAL_KEY_MAP.get("query_result"), expression);
            promiseList = this.queryResultQuery(query);
            return promiseList;
        }

        // other
        promiseList = this.metricNameAndLabelsQuery(expression, start, end);
        return promiseList;
    }

    /**
     * other
     *
     * @param expression
     * @param start
     * @param end
     * @return
     */
    private List metricNameAndLabelsQuery(String expression, Long start, Long end) {
        List<String> matchList = Tool.ListUtil.of(expression);
        JSONObject resultMap = promApiService.series(String.valueOf(start), String.valueOf(end), matchList);
        List<Map> dataList = (List<Map>) resultMap.get("data");

        if (Tool.CollectionUtil.isEmpty(dataList)) return new ArrayList();

        List<String> list = new ArrayList<>();
        for (Map<String, String> metricMap : dataList) {
            String metricsName = metricMap.get("__name__");
            String text = StrUtil.emptyToDefault(metricsName, "");
            metricMap.remove("__name__");
            text += "{";
            List<String> stringList = new ArrayList<>();
            metricMap.entrySet().stream().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                stringList.add(key + '=' + '"' + value + '"');
            });
            text += stringList.stream().sorted().collect(Collectors.joining(","));
            text += "}";
            list.add(text);
        }
        return list;
    }

    /**
     * query_result(query)
     *
     * @param query
     * @return
     */
    private List queryResultQuery(String query) {
        JSONObject jsonObject = promApiService.query(query, null, null);
        Map dataMap = (Map) jsonObject.get("data");

        if (Tool.MapUtil.isEmpty(dataMap)) {
            return new ArrayList<>();
        }
        List<Map> resultObjList = (List<Map>) dataMap.get("result");
        List<String> queryResultList = new ArrayList();
        for (Map resultMap : resultObjList) {
            Map<String, String> metricMap = (Map) resultMap.get("metric");
            String metricsName = metricMap.get("__name__");
            String text = StrUtil.emptyToDefault(metricsName, "");
            metricMap.remove("__name__");
            text += "{";
            List<String> stringList = new ArrayList<>();
            metricMap.entrySet().stream().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                stringList.add(key + '=' + '"' + value + '"');
            });
            text += stringList.stream().sorted().collect(Collectors.joining(","));
            text += "}";

            List<Object> value = (List<Object>) resultMap.get("value");
            Double t = Double.valueOf(value.get(0).toString());
            text += " " + value.get(1) + " " + t * 1000;
            queryResultList.add(text);
        }
        return queryResultList;
    }


    /**
     * metrics(metrics_regex)
     *
     * @param metricFilterPattern
     * @param start
     * @param end
     * @return
     */
    private List metricNameQuery(String metricFilterPattern, Long start, Long end) {
        JSONObject jsonObject = promApiService.labels("/api/v1/label/__name__/values", String.valueOf(start), String.valueOf(end), Tool.ListUtil.empty());
        List<String> metricsList = (List<String>)  jsonObject.get("data");

        if (Tool.CollectionUtil.isEmpty(metricsList)) return new ArrayList();
        metricsList = metricsList.stream().filter(s -> ReUtil.isMatch(metricFilterPattern, s)).distinct().collect(Collectors.toList());
        return metricsList;
    }

    /**
     * label_values(label) | label_values(metric,label)
     *
     * @param metric
     * @param label
     * @param start
     * @param end
     * @return
     */
    private List labelValuesQuery(String metric, String label, Long start, Long end) {
        if (StrUtil.isEmpty(metric)) {
            String url = String.format("/api/v1/label/%s/values", label);

            JSONObject jsonObject = promApiService.labels(url, String.valueOf(start), String.valueOf(end), Tool.ListUtil.empty());
            List<String> labelValueList = (List<String>) jsonObject.get("data");

            labelValueList = Tool.CollectionUtil.isEmpty(labelValueList) ? new ArrayList<>() : labelValueList;
            return labelValueList;
        } else {
            List<String> matchList = Tool.ListUtil.of(metric);
            JSONObject series = promApiService.series(String.valueOf(start), String.valueOf(end), matchList);
            List<Map> metricList = (List<Map>) series.get("data");

            metricList = Tool.CollectionUtil.isEmpty(metricList) ? new ArrayList<>() : metricList;
            List<String> labelValueListOfmetric = metricList.stream().filter(m -> m.get(label) != null).map(m -> m.get(label).toString()).distinct().collect(Collectors.toList());
            return labelValueListOfmetric;
        }
    }

}
