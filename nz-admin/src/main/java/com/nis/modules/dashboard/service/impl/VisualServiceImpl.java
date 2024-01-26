package com.nis.modules.dashboard.service.impl;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.agent.service.LokiApiService;
import com.nis.modules.agent.service.PromApiService;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.dashboard.entity.VisualChart;
import com.nis.modules.dashboard.entity.VisualDashboard;
import com.nis.modules.dashboard.entity.VisualNotebook;
import com.nis.modules.dashboard.service.VisualChartService;
import com.nis.modules.dashboard.service.VisualDashboardService;
import com.nis.modules.dashboard.service.VisualNotebookService;
import com.nis.modules.dashboard.service.VisualService;
import com.nis.modules.dashboard.utils.DashboardConstant;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.stat.service.impl.StatServiceImpl;
import com.nis.modules.sys.service.SysConfService;
import com.nis.modules.sys.service.SysI18nService;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.nis.setup.controller.TimezoneController;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.time.StopWatch;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.print.PageSize;
import org.openqa.selenium.print.PrintOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Service("visualService")
public class VisualServiceImpl implements VisualService {

    private final static Log log = Log.get();

    @Value("${connect.timeout:30000}")
    private Integer connectTimeout;

    /**
     * dashboard snapshot 查询超时时间，单位：ms
     */
    @Value("${dashboard.snapshot.timeout:300000}")
    private Integer timeout;
    
    @Value("${dashboard.snapshot.chrome.option:--headless,--disable-cache,--disable-dev-shm-usage,--no-sandbox,--window-size=1920x1080,--lang=zh-CN.UTF-8}")
    private String[] chromeOption;
    
    @Value("${dashboard.snapshot.chrome.path:./lib/chrome-linux64/chrome}")
    private String chromePath;
    @Autowired
    private SysI18nService sysI18nService;

    @Autowired
    private SysConfService sysConfService;

    @Autowired
    private PromApiService promApiService;

    @Autowired
    private LokiApiService lokiApiService;

    @Autowired
    private StatServiceImpl statServiceImpl;

    @Autowired
    private VisualDashboardService dashboardService;

    @Autowired
    private VisualNotebookService notebookService;

    @Autowired
    private VisualChartService visualChartService;

    @Autowired
    private AssetAssetService assetAssetService;

    @Autowired
    private MonitorEndpointService monitorEndpointService;

    private static volatile TimedCache<String, Object> SNAPSHOT_CACHE = LocalCacheManager.getNeverExpireTimedCache(Constant.SYS_CACHE_PREFIX, "SNAPSHOT");

    /**
     * 存储 thread Executor ，用以中断线程
     */
    private static ConcurrentHashMap<String, ExecutorService> TASK_EXECUTOR_MAP = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // ChromeDriver
        // System.setProperty("webdriver.chrome.driver", "./lib/chromedriver.exe");
        // System.setProperty("webdriver.http.factory", "jdk-http-client");
        System.setProperty("webdriver.chrome.driver", "./lib/chromedriver");
    }

    private synchronized ExecutorService getSnapshotThreadPoolByTid(String tid) {
        ExecutorService executorService = TASK_EXECUTOR_MAP.get(tid);
        if (executorService == null) {
            executorService = Executors.newThreadPerTaskExecutor(
                    Thread.ofVirtual().name(tid + "-", 0).factory()
            );
            // add
            TASK_EXECUTOR_MAP.put(tid, executorService);
        }
        return executorService;
    }

    @Override
    public void exploreSnapshot(HttpServletResponse response, String format, Integer type, Long start, Long end, String language, Integer unit, String limit, String direction, String[] expressions) throws IOException {
        // 计算步长
        String step = this.getTimeStep(start, end);
        if(!StrUtil.equals(direction, "forward")){
            direction = "backward";
        }

        List<CompletableFuture<JSONObject>> futureList = new ArrayList<>();
        for (int i = 0; i < expressions.length; i++) {
            String expression = URLUtil.decode(expressions[i]);
            String finalDirection = direction;
            futureList.add(CompletableFuture.supplyAsync(() -> {
                JSONObject queryData = null;
                if (type == 1) {
                    JSONObject queryApiResultData = promApiService.query(expression, null, null);
                    Object queryResultDataType = JSONPath.read(Tool.JSONUtil.toJsonStr(queryApiResultData), "data.resultType");
                    if (ObjectUtil.equal("matrix", queryResultDataType)) {
                        queryData = queryApiResultData;
                        JSONPath.set(queryData, "api", "query");
                    } else {
                        queryData = promApiService.query_range(expression, StrUtil.toString(start), StrUtil.toString(end), step, "null", null);
                        JSONPath.set(queryData, "api", "query_range");
                    }
                } else if (type == 2) {
                    queryData = lokiApiService.query_range(expression, StrUtil.toString(start), StrUtil.toString(end), StrUtil.emptyToDefault(limit, "100"), null, null, finalDirection, "1", null);
                }
                queryData.put("expression", expression);
                return queryData;
            }));
        }
        List<JSONObject> dataList = new ArrayList();
        try {
            CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).get(connectTimeout, TimeUnit.MILLISECONDS);
            futureList.forEach(future -> {
                JSONObject result = future.getNow(null);
                if (Tool.MapUtil.isNotEmpty(result)) {
                    dataList.add(result);
                }
            });
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error(e);
        }
        // 封装返回结果
        Map<String, Object> resultMap = this.snapshotResultMap(start, end, language);
        resultMap.put("type", type);
        resultMap.put("unit", unit);
        resultMap.put("direction", direction);
        resultMap.put("limit", limit);
        resultMap.put("data", dataList);

        String snapshotTemplate = this.replaceSnapshotTemplate(resultMap);
        // 目前下载名称由前端控制
        // String downFileName = String.format("%s-%s.%s", "explore", System.currentTimeMillis(), StrUtil.emptyToDefault(format, "html"));
        ResponseUtil.downloadFile(response, "exploreSnapshot.html", snapshotTemplate.getBytes());
    }

    /**
     * 模板基本返回参数
     */
    @Override
    public Map<String, Object> snapshotResultMap(Long start, Long end, String language) {
        Map<String, Object> resultMap = Tool.MapUtil.newHashMap();
        resultMap.put("language", language);
        resultMap.put("start", start);
        resultMap.put("end", end);
        // 查询系统时区信息
        String timezone = sysConfService.getValue("timezone");
        TimezoneController.Timezone timezoneEntity = TimezoneController.queryTimezoneByName(timezone);
        resultMap.put("offset", timezoneEntity.getOffset());
        resultMap.put("timezone", timezone);
        return resultMap;
    }

    /**
     * 读取快照模板，替换数据
     */
    @Override
    public String replaceSnapshotTemplate(Map<String, Object> resultMap) {
        BufferedReader reader = null;
        StringBuffer sbf = new StringBuffer();
        try {
            String path = sysConfService.getValue("snapshot_template_path");
            reader = new BufferedReader(new FileReader(Tool.StrUtil.concat(true, path, File.separator, "snapshot_template.html")));
            String line;
            while ((line = reader.readLine()) != null) {
                if (Tool.StrUtil.startWith(line, "<script>window.dataJson"))
                    line = "<script>window.dataJson=" + JSONUtil.toJsonStr(resultMap) + "</script>";
                sbf.append(line + "\n");
            }
        } catch (IOException e) {
            log.error(e);
        } finally {
            if (reader != null) IoUtil.close(reader);
        }
        return sbf.toString();
    }

    /**
     * 计算Dashboards step
     */
    @Override
    public String getTimeStep(Long start, Long end) {
        Long timeDiff = (end - start) / (24 * 60 * 60);
        if (timeDiff < 1) {
            return "15s";
        } else if (timeDiff < 7) {
            return "5m";
        } else if (timeDiff < 30) {
            return "10m";
        } else {
            return "30m";
        }
    }

    @Override
    public String createDashboardSnapshotTask(String format, Integer dashboardId, Long start, Long end, String varsJsonStr) {
        VisualDashboard dashboard = dashboardService.getById(dashboardId);
        ValidateUtils.is(dashboard).notNull(RCode.DASHBOARD_NOT_EXIST);

        // 查询 charts
        Map param = Tool.MapUtil.builder()
                .put("pageSize", "-1")
                .put("dashboardId", dashboard.getId())
                .put("groupId", 0)
                .map();

        PageUtils chartPageUtils = visualChartService.queryPage(param);
        List<VisualChart> chartsList = (List<VisualChart>) chartPageUtils.getList();
        ValidateUtils.is(chartsList).notNull(RCode.CHART_NOTEXSITS);

        // render repeat chart
        List<VisualChart> randeredRepeatChartList = this.renderRepeatChartVariables(chartsList, varsJsonStr);
        chartPageUtils.setList(randeredRepeatChartList);

        // render single chart
        List<VisualChart> allChartList = this.renderSingleChartVariables(randeredRepeatChartList, varsJsonStr);

        String taskId = StrUtil.uuid();
        Map taskStat = Tool.MapUtil.builder()
                .put("total", allChartList.size())
                .put("done", 0)
                .put("process", 0)
                // repaired for snapshot template
                .put("start", start)
                .put("end", end)
                .put("dashboard", R.ok(dashboard))
                .put("charts", R.ok(chartPageUtils))
                .map();

        log.info("dashboard snapshot start. taskId: {}, dashboardId:{} total: {}", taskId, dashboardId, allChartList.size());
        String stat = StrUtil.concat(true, DashboardConstant.DASHBOARD_SNAPSHOT_TYPE, "_snapshot_stat_", taskId);
        SNAPSHOT_CACHE.put(stat, taskStat, timeout * 5);

        ThreadUtil.execAsync(() -> {
            // 计算步长
            String step = this.getTimeStep(start, end);
            Map<String, String> basicQueryParam = Tool.MapUtil.newHashMap();
            basicQueryParam.put("start", StrUtil.toString(start));
            basicQueryParam.put("end", StrUtil.toString(end));
            basicQueryParam.put("step", StrUtil.toString(step));

            // run Snapshot Runnable
            ReentrantReadWriteLock readWriteLock = Tool.LockUtil.createReadWriteLock(false);
            ReentrantReadWriteLock.WriteLock lock = readWriteLock.writeLock();

            ExecutorService taskExecutor = getSnapshotThreadPoolByTid(taskId);
            allChartList.stream().forEach(chart -> {
                SnapshotRunnable snapshotRunnable = new SnapshotRunnable(taskId, lock, DashboardConstant.DASHBOARD_SNAPSHOT_TYPE, chart, basicQueryParam);
                // submit
                taskExecutor.submit(snapshotRunnable);
            });
        });
        log.info("dashboard snapshot task create finished. taskId: {}", taskId);
        return taskId;
    }


    /**
     * render Repeat Chart Variables
     *
     * @param chartList
     * @param varsJsonStr
     * @return
     */
    private List<VisualChart> renderRepeatChartVariables(List<VisualChart> chartList, String varsJsonStr) {
        log.info("[renderRepeatChartVariables] [begin] [variable config: {}]", varsJsonStr);
        List<Map> varConfigList = StrUtil.isNotEmpty(varsJsonStr) ? JSONObject.parseArray(varsJsonStr, Map.class) : Tool.ListUtil.empty();
        if (Tool.CollUtil.isEmpty(varConfigList)) {
            log.warn("[renderRepeatChartVariables] [no chart variable config] [vars size: {}]", varConfigList.size());
            return chartList;
        }

        // multi variable
        Map<String, List<String>> multiVariableMap = varConfigList.stream()
                .filter(map -> {
                    List values = (List) map.get("values");
                    if (Tool.CollUtil.isEmpty(values)) return false;
                    return values.size() > 1;
                })
                .collect(Collectors.toMap(map -> (String) map.get("name"), map -> (List<String>) map.get("values")));
        log.info("[renderRepeatChartVariables] [multi variable] [info: {}]", JSONUtil.toJsonStr(multiVariableMap));

        List<VisualChart> repeatConfGroupChartList = chartList.stream()
                .filter(chart -> StrUtil.equals(Constant.CHART_GROUP_TYPE, chart.getType()))
                .filter(chart -> {
                    String chartParamJsonStr = JSONUtil.toJsonStr(ObjectUtil.defaultIfNull(chart.getParam(), Tool.MapUtil.empty()));
                    // repeat enable
                    Object repeatEnable = JSONPath.read(chartParamJsonStr, "enable.repeat");
                    String repeatEnableStr = StrUtil.toString(ObjectUtil.defaultIfNull(repeatEnable, false));
                    // repeat var
                    Object repeatVariable = JSONPath.read(chartParamJsonStr, "repeat.variable");
                    return BooleanUtil.and(
                            StrUtil.equals("true", repeatEnableStr),
                            ObjectUtil.isNotNull(repeatVariable)
                    );
                })
                .collect(Collectors.toList());

        // copy group charts and render vars
        Integer startTempId = -10;
        for (VisualChart groupChart : repeatConfGroupChartList) {
            String chartParamJsonStr = JSONUtil.toJsonStr(ObjectUtil.defaultIfNull(groupChart.getParam(), Tool.MapUtil.empty()));
            String repeatVariable = JSONPath.read(chartParamJsonStr, "repeat.variable").toString();

            List<String> varValues = multiVariableMap.get(repeatVariable);
            if (Tool.CollUtil.isEmpty(varValues)) {
                log.warn("[renderRepeatChartVariables] [group chart config variable not found, not repeat] [group chart variable: {}]", repeatVariable);
                continue;
            }
            log.info("[renderRepeatChartVariables] [repeat group chart] [id: {}] [name: {}]", groupChart.getId(), groupChart.getName());
            // remove old chart
            chartList.remove(groupChart);
            List<VisualChart> copyGroupChartList = Tool.ListUtil.list(true);
            for (String varValue : varValues) {
                // 这里重新对 group_id 进行赋值
                Integer tempGroupId = startTempId--;
                VisualChart tempGroupChart = new VisualChart();
                tempGroupChart = ObjectUtil.clone(groupChart);
                // new temp group id
                tempGroupChart.setId(tempGroupId);
                // render group name
                String name = tempGroupChart.getName();
                tempGroupChart.setName(name.replaceAll(StrUtil.concat(true, "\\$", repeatVariable), varValue));

                // render repeatVariable
                List<VisualChart> children = tempGroupChart.getChildren();
                children = Tool.CollUtil.defaultIfEmpty(children, Tool.ListUtil.list(true));
                for (VisualChart chart : children) {
                    Integer tempChartId = startTempId--;
                    // temp id
                    chart.setId(tempChartId);
                    chart.setGroupId(tempGroupId);
                    // chart name
                    String chartName = chart.getName();
                    chart.setName(chartName.replaceAll(StrUtil.concat(true, "\\$", repeatVariable), varValue));

                    List<Map> elements = chart.getElements();
                    elements = Tool.CollUtil.defaultIfEmpty(elements, Tool.ListUtil.list(true));
                    for (Map element : elements) {
                        String expression = Tool.MapUtil.getStr(element, "expression", "");
                        expression = expression.replaceAll(StrUtil.concat(true, "\\$", repeatVariable), varValue);
                        element.put("expression", expression);
                    }
                    chart.setElements(elements);
                }
                copyGroupChartList.add(tempGroupChart);
            }
            // add new chart
            chartList.addAll(copyGroupChartList);
        }
        log.info("[renderRepeatChartVariables] [finshed]");
        return chartList;
    }

    /**
     * render Single Chart Variables
     *
     * @param chartList
     * @param varsJsonStr
     * @return
     */
    private List<VisualChart> renderSingleChartVariables(List<VisualChart> chartList, String varsJsonStr) {
        log.info("[renderSingleChartVariables] [begin] [variable config: {}]", varsJsonStr);
        List<Map> varConfigList = StrUtil.isNotEmpty(varsJsonStr) ? JSONObject.parseArray(varsJsonStr, Map.class) : Tool.ListUtil.empty();
        if (Tool.CollUtil.isEmpty(varConfigList)) {
            log.warn("[renderSingleChartVariables] [no chart variable config] [vars size: {}]", varConfigList.size());
            List<VisualChart> allChartList = visualChartService.getAllChartList(chartList);
            return allChartList;
        }

        // all Chart List & render vars
        List<VisualChart> allChartList = visualChartService.getAllChartList(chartList);

        // single Variables map
        Map<String, String> singleVariableMap = Tool.MapUtil.newHashMap();
        for (Map map : varConfigList) {
            String name = StrUtil.toString(map.get("name"));
            List values = (List) map.get("values");
            values = Tool.CollUtil.defaultIfEmpty(values, Tool.ListUtil.list(true));
            if (Tool.CollUtil.isEmpty(values)) {
                continue;
            }

            singleVariableMap.put(name, Tool.CollUtil.join(values, "|"));
        }
        log.info("[renderSingleChartVariables] [single variable] [info: {}]", JSONUtil.toJsonStr(singleVariableMap));
        Set<Map.Entry<String, String>> singleVariableEntrySet = singleVariableMap.entrySet();

        for (VisualChart chart : allChartList) {
            for (Map.Entry<String, String> entry : singleVariableEntrySet) {
                // chart name
                String chartName = chart.getName();
                chart.setName(chartName.replaceAll(StrUtil.concat(true, "\\$", entry.getKey()), entry.getValue()));
            }

            List<Map> elements = chart.getElements();
            elements = Tool.CollUtil.defaultIfEmpty(elements, Tool.ListUtil.list(true));
            for (Map element : elements) {
                String expression = Tool.MapUtil.getStr(element, "expression", "");
                for (Map.Entry<String, String> entry : singleVariableEntrySet) {
                    expression = expression.replaceAll(StrUtil.concat(true, "\\$", entry.getKey()), entry.getValue());
                }
                element.put("expression", expression);
            }
            chart.setElements(elements);
        }
        log.info("[renderSingleChartVariables] [finshed]");
        return allChartList;
    }

    @Override
    public String createNotebookSnapshotTask(String format, Integer notebookId, Long start, Long end) {
        VisualNotebook notebook = notebookService.getById(notebookId);
        ValidateUtils.is(notebook).notNull(RCode.NOTEBOOK_NOT_EXIST);

        List<VisualChart> chartsList = JSONUtil.toList(StrUtil.utf8Str(notebook.getCharts()), VisualChart.class);
        ValidateUtils.is(chartsList).notNull(RCode.CHART_NOTEXSITS);

        PageUtils chartPageUtils = new PageUtils(new Query(VisualChart.class).getPage(Tool.MapUtil.newHashMap()));
        chartPageUtils.setList(chartsList);

        List<VisualChart> allChartList = visualChartService.getAllChartList(chartsList);

        String taskId = StrUtil.uuid();
        Map taskStat = Tool.MapUtil.builder()
                .put("total", allChartList.size())
                .put("done", 0)
                .put("process", 0)
                .put("start", start)
                .put("end", end)
                .put("notebook", R.ok(notebook))
                .put("format", format)
                .put("charts", R.ok(chartPageUtils))
                .map();

        log.info("[createNotebookSnapshotTask] [notebook snapshot] [taskId: {}] [notebookId: {}] [total: {}]", taskId, notebookId, allChartList.size());
        String stat = StrUtil.concat(true, DashboardConstant.NOTEBOOK_SNAPSHOT_TYPE, "_snapshot_stat_", taskId);
        SNAPSHOT_CACHE.put(stat, taskStat, timeout * 5);

        ThreadUtil.execAsync(() -> {
            // 计算步长
            String step = this.getTimeStep(start, end);
            Map<String, String> basicQueryParam = Tool.MapUtil.newHashMap();
            basicQueryParam.put("start", StrUtil.toString(start));
            basicQueryParam.put("end", StrUtil.toString(end));
            basicQueryParam.put("step", StrUtil.toString(step));

            ReentrantReadWriteLock readWriteLock = Tool.LockUtil.createReadWriteLock(false);
            ReentrantReadWriteLock.WriteLock lock = readWriteLock.writeLock();

            ExecutorService taskExecutor = getSnapshotThreadPoolByTid(taskId);
            allChartList.stream().forEach(chart -> {
                SnapshotRunnable snapshotRunnable = new SnapshotRunnable(taskId, lock, DashboardConstant.NOTEBOOK_SNAPSHOT_TYPE, chart, basicQueryParam);
                // submit
                taskExecutor.submit(snapshotRunnable);
            });
        });
        log.info("[createNotebookSnapshotTask] [task create finished] [taskId: {}]", taskId);
        return taskId;
    }

    @Override
    public Map querySnapshotResultProcess(String type, String taskId) {
        String snapshotStatKey = StrUtil.concat(true, type, "_snapshot_stat_", taskId);
        Object obj = SNAPSHOT_CACHE.get(snapshotStatKey);
        if (obj == null) {
            log.warn("[querySnapshotResultProcess] [snapshot result process error, the task does not exist or has expired] [type: {}] [taskId: {}]", type, taskId);
            throw new NZException(RCode.DASHBOARD_SNAPSHOT_TASK_NOT_EXIST);
        }

        Map<Object, Object> processMap = Tool.MapUtil.builder()
                .put("total", ((Map) obj).get("total"))
                .put("done", ((Map) obj).get("done"))
                .put("process", ((Map) obj).get("process"))
                .map();
        return processMap;
    }

    @Override
    public void cancelSnapshotTask(String type, String taskId) {
        log.info("[cancelSnapshotTask] [snapshot cancel] [type: {}] [taskId: {}]", type, taskId);
        ExecutorService taskExecutor = TASK_EXECUTOR_MAP.get(taskId);
        if (taskExecutor == null) {
            log.warn("[cancelSnapshotTask] [snapshot task cancel error, the task does not exist or has expired] [type: {}] [taskId: {}]", type, taskId);
            throw new NZException(RCode.DASHBOARD_SNAPSHOT_TASK_NOT_EXIST);
        } else {
            try {
                // shutdownNow
                taskExecutor.shutdownNow();
                // remove
                TASK_EXECUTOR_MAP.remove(taskId);

                log.info("[cancelSnapshotTask] [snapshot task cancel] [type: {}] [taskId: {}]", type, taskId);
                // remove cache data
                String snapshotStatKey = StrUtil.concat(true, type, "_snapshot_stat_", taskId);
                String snapshotResultKey = StrUtil.concat(true, type, "_snapshot_result_", taskId);

                SNAPSHOT_CACHE.remove(snapshotStatKey);
                SNAPSHOT_CACHE.remove(snapshotResultKey);
                log.info("[cancelSnapshotTask] [snapshot task is canceled,the cache data is cleared] [type: {}] [taskId: {}]", type, taskId);
            } catch (Exception e) {
                log.error(e, "[cancelSnapshotTask] [error] [type: {}] [taskId: {}]", type, taskId);
            }
        }
    }

    @Override
    public void downloadSnapshotFile(HttpServletResponse response, String type, String language, String taskId) {
        String snapshotStatKey = StrUtil.concat(true, type, "_snapshot_stat_", taskId);
        String snapshotResultKey = StrUtil.concat(true, type, "_snapshot_result_", taskId);

        Object obj = SNAPSHOT_CACHE.get(snapshotStatKey);
        if (obj == null) {
            throw new NZException(RCode.DASHBOARD_SNAPSHOT_TASK_NOT_EXIST);
        }

        try {
            Map snapshotInfoMap = (Map) obj;
            Object start = snapshotInfoMap.get("start");
            Object end = snapshotInfoMap.get("end");
            Object dashboard = snapshotInfoMap.get("dashboard");
            Object notebook = snapshotInfoMap.get("notebook");
            Object format = snapshotInfoMap.get("format");
            Object charts = snapshotInfoMap.get("charts");

            List<Object> snapshotResultList = (List<Object>) SNAPSHOT_CACHE.get(snapshotResultKey);

            Map<Object, Object> chartsDataMap = Tool.MapUtil.newHashMap();
            for (Object result : snapshotResultList) {
                if (ObjectUtil.isEmpty(result)) continue;

                String chartId = Tool.MapUtil.getStr((Map) result, "chartId");
                String eleIndex = Tool.MapUtil.getStr((Map) result, "eleIndex");

                String key = StrUtil.concat(true, chartId, StrUtil.UNDERLINE, eleIndex);
                chartsDataMap.put(key, result);
            }

            // handle snapshot file data
            Map<String, Object> resultMap = this.snapshotResultMap(Long.valueOf(start.toString()), Long.valueOf(end.toString()), language);
            resultMap.put("chartsData", chartsDataMap);
            resultMap.put("dashboard", dashboard);
            resultMap.put("notebook", notebook);
            resultMap.put("charts", charts);
            resultMap.put("type", type);

            if (StrUtil.equals(DashboardConstant.DASHBOARD_SNAPSHOT_TYPE, type)) {
                String snapshotHtmlContent = this.replaceSnapshotTemplate(resultMap);

                // download file
                ResponseUtil.downloadFile(response, "dashboardSnapshot.html", snapshotHtmlContent.getBytes());
            } else if (StrUtil.equals(DashboardConstant.NOTEBOOK_SNAPSHOT_TYPE, type)) {
                if (ObjectUtil.equal(format, "html")) {
                    String snapshotHtmlContent = this.replaceSnapshotTemplate(resultMap);
                    ResponseUtil.downloadFile(response, "notebookSnapshot.html", snapshotHtmlContent.getBytes());
                } else if (ObjectUtil.equal(format, "pdf")) {
                    resultMap.put("isPdfExport", true);

                    // html
                    String snapshotHtmlContent = this.replaceSnapshotTemplate(resultMap);
                    // pdf
                    String pdfContent = this.htmlToPdf(taskId, snapshotHtmlContent);

                    ResponseUtil.downloadFile(response, "notebookSnapshot.pdf", OutputType.BYTES.convertFromBase64Png(pdfContent));
                } else if (ObjectUtil.equal(format, "markdown")) {
                    // html
                    String snapshotHtmlContent = this.replaceSnapshotTemplate(resultMap);

                    String timeRangeStr = this.getMdHeaderTimeRangeString((long) start, (long) end);
                    String generateInfoStr = this.getMdHeaderGenerateInfoString(language);
                    // markDown
                    String markDownContent = this.htmlToMarkdown(taskId, notebook, timeRangeStr, generateInfoStr, snapshotHtmlContent);

                    ResponseUtil.downloadFile(response, "notebookSnapshot.md", markDownContent.getBytes());
                }
            }
        } catch (Exception e) {
            log.error(e, "[downloadSnapshotFile] [error] [type: {}] [taskId: {}]", type, taskId);
            throw new NZException(RCode.DASHBOARD_SNAPSHOT_ERROR);
        } finally {
            ExecutorService taskExecutor = TASK_EXECUTOR_MAP.get(taskId);
            log.info("[downloadSnapshotFile] [snapshot task cancel] [type: {}] [taskId: {}]", type, taskId);
            if (taskExecutor != null && !taskExecutor.isShutdown()) {
                taskExecutor.shutdownNow();
            }

            // remove cache data
            TASK_EXECUTOR_MAP.remove(taskId);
            SNAPSHOT_CACHE.remove(snapshotStatKey);
            SNAPSHOT_CACHE.remove(snapshotResultKey);
        }
    }

    /**
     * create chrome driver
     *
     * @return
     */
    private ChromeDriver createChromeDriver() {
        final var chromeOptions = new ChromeOptions();
        chromeOptions.addArguments(chromeOption);
        chromeOptions.setBinary(chromePath);
        ChromeDriver chromeDriver = new ChromeDriver(chromeOptions);
        log.info("chrome options: {}", chromeOptions.toString());
        return chromeDriver;
    }

    /**
     * html to pdf
     *
     * @param taskId
     * @param htmlContent
     * @return
     */
    private String htmlToPdf(String taskId, String htmlContent) {
        log.info("[htmlToPdf] [begin] [taskId: {}] [htmlContent: {}]", taskId, StrUtil.length(htmlContent));
        StopWatch sw = new StopWatch();
        sw.start();

        ChromeDriver driver = this.createChromeDriver();
        File tempFile = null;
        try {
            tempFile = Tool.FileUtil.file(Tool.StrUtil.concat(true, Constant.TEMP_PATH, File.separator, taskId, ".html"));
            FileUtil.writeUtf8String(htmlContent, tempFile);

            driver.resetInputState();
            driver.get("file://" + tempFile.getAbsolutePath());

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60 * 5));
            wait.until(webDriver -> {
                Boolean isLoaded = (Boolean) ((JavascriptExecutor) webDriver).executeScript("return window.dataJson.AllChartDataLoad == true");
                return Tool.ObjectUtil.defaultIfNull(isLoaded, false);
            });

            PrintOptions printOptions = new PrintOptions();
            printOptions.setPageSize(new PageSize(21, 29.7)); // A4
            printOptions.setOrientation(PrintOptions.Orientation.LANDSCAPE); // 纵向
            printOptions.setScale(0.5);
            printOptions.setBackground(true);
//            printOptions.setPageSize(new PageSize(80D, 100D));

            Pdf pdf = ((PrintsPage) driver).print(printOptions);
            String pdfContent = pdf.getContent();
            sw.stop();
            log.info("[htmlToPdf] [finshed] [taskId: {}] [pdfContent: {}] [Run time: {}]", taskId, StrUtil.length(pdfContent), sw.toString());
            return pdfContent;
        } finally {
            if (driver != null) {
                driver.quit();
            }
            FileUtil.del(tempFile);
        }
    }

    /**
     * html to markdown
     *
     * @param taskId
     * @param notebookMap
     * @param timeRangeStr
     * @param generateInfoStr
     * @param htmlContent
     * @return
     * @throws IOException
     * @throws TemplateException
     */
    private String htmlToMarkdown(String taskId, Object notebookMap, String timeRangeStr, String generateInfoStr, String htmlContent) throws IOException, TemplateException {
        log.info("[htmlToMarkdown] [begin] [taskId: {}] [htmlContent: {}]", taskId, StrUtil.length(htmlContent));
        StopWatch sw = new StopWatch();
        sw.start();

        ChromeDriver driver = this.createChromeDriver();
        File tempFile = null;
        try {
            tempFile = Tool.FileUtil.file(Tool.StrUtil.concat(true, Constant.TEMP_PATH, File.separator, taskId, ".html"));
            FileUtil.writeUtf8String(htmlContent, tempFile);

            driver.resetInputState();
            driver.get("file://" + tempFile.getAbsolutePath());

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60 * 5));
            wait.until(webDriver -> {
                Boolean isLoaded = (Boolean) ((JavascriptExecutor) webDriver).executeScript("return window.dataJson.AllChartDataLoad == true");
                return Tool.ObjectUtil.defaultIfNull(isLoaded, false);
            });

            VisualNotebook notebook = MapUtil.get((Map) notebookMap, "data", VisualNotebook.class);
            List<VisualChart> chartsList = JSONUtil.toList(StrUtil.utf8Str(notebook.getCharts()), VisualChart.class);

            List<Map<Object, Object>> paragraphList = Tool.ListUtil.list(true);

            List<WebElement> divElementList = driver.findElements(By.className("panel-chart"));
            for (WebElement divElement : divElementList) {
                String id = divElement.getAttribute("id");
                if (Tool.StrUtil.isEmpty(id)) {
                    continue;
                }
                String chartId = id.replaceAll("chart-local-", "");
                VisualChart visualChart = chartsList.stream().filter(chart -> ObjectUtil.equal(chart.getId().toString(), chartId)).findFirst().orElse(null);
                if (ObjectUtil.isNull(visualChart)) {
                    continue;
                }
                if (ObjectUtil.isNotEmpty(visualChart.getParam())) {
                    Map paramObject = JSONUtil.toBean(StrUtil.utf8Str(visualChart.getParam()), Map.class);
                    visualChart.setParam(paramObject);
                }

                String editorType = MapUtil.getStr(((Map) visualChart.getParam()), "editorType");
                if (StrUtil.equals("text", visualChart.getType()) && StrUtil.equals("markdown", editorType)) {
                    Map<Object, Object> tempNodeMap = MapUtil.builder()
                            .put("chart", visualChart)
                            .put("imageDataStr", "")
                            .map();
                    paragraphList.add(tempNodeMap);
                    continue;
                }
                // nz-chart 包含图表&legend
                WebElement chartElement = divElement.findElement(By.className("nz-chart"));
                int height = chartElement.getSize().getHeight();
                if (ObjectUtil.equal(0, height)) {
                    log.warn("[htmlToMarkdown] [element height is 0] [divElement id: {}]", id);
                    continue;
                }
                String fileBase64 = chartElement.getScreenshotAs(OutputType.BASE64);
                Map<Object, Object> tempNodeMap = MapUtil.builder()
                        .put("chart", visualChart)
                        .put("imageDataStr", StrUtil.concat(true, "data:image/png;base64,", fileBase64))
                        .map();
                paragraphList.add(tempNodeMap);
            }

            Map<Object, Object> templateDataParam = MapUtil.builder()
                    .put("paragraphList", paragraphList)
                    .put("notebook", notebook)
                    .put("timeRangeStr", timeRangeStr)
                    .put("generateInfoStr", generateInfoStr)
                    .map();

            Template template = TemplateUtil.stringToTemplate(
                    sysConfService.getValue("notebook_snapshot_markdown_template"),
                    "notebook_snapshot_markdown_template");
            String markDownContent = FreeMarkerTemplateUtils.processTemplateIntoString(template, templateDataParam);

            sw.stop();
            log.info("[htmlToMarkdown] [finshed] [taskId: {}] [htmlContent: {}] [Run time: {}]", taskId, StrUtil.length(htmlContent), sw.toString());
            return markDownContent;
        } finally {
            if (driver != null) {
                driver.quit();
            }
            FileUtil.del(tempFile);
        }
    }

    private String getMdHeaderTimeRangeString(long startTimeSec, long endTimeSec) {
        // timezone
        String timezoneConfName = sysConfService.getValue("timezone");
        TimezoneController.Timezone timezone = TimezoneController.queryTimezoneByName(timezoneConfName);

        // dateformat
        DateFormat dateformat = CommonUtils.getDateFormatByOffset(timezone.getOffset(), "yyyy-MM-dd'T'HH:mm:ssZ");

        String startDateStr = dateformat.format(new Date(startTimeSec * 1000));
        startDateStr = Tool.StrUtil.builder(startDateStr).insert(startDateStr.length() - 2, ":").toString();

        String endDateStr = dateformat.format(new Date(endTimeSec * 1000));
        endDateStr = Tool.StrUtil.builder(endDateStr).insert(endDateStr.length() - 2, ":").toString();

        // start - end (timezone.name)
        Object timeRangeStr = Tool.StrUtil.concat(true, startDateStr, " - ", endDateStr, " (", timezone.getName(), ")");

        // i18n
        String timeRangeStrByI18n = sysI18nService.queryValue("notebook.markdown.timeRange.info", timeRangeStr);
        return timeRangeStrByI18n;
    }

    private String getMdHeaderGenerateInfoString(String language) {
        // user name
        String userName = ShiroUtils.getUserEntity().getName();
        String timezoneConfName = sysConfService.getValue("timezone");
        TimezoneController.Timezone timezone = TimezoneController.queryTimezoneByName(timezoneConfName);

        String dateFromatPattern = "";
        if (StrUtil.equalsIgnoreCase("en", language)) {
            dateFromatPattern = "MMM dd yyyy";
        } else if (StrUtil.equalsIgnoreCase("zh", language)) {
            dateFromatPattern = "yyyy年MM月dd日";
        }

        // dateformat
        DateFormat dateformat = CommonUtils.getDateFormatByOffset(timezone.getOffset(), dateFromatPattern);
        String formatDateStr = dateformat.format(new Date());

        // i18n
        String generateInfoStrByI18n = sysI18nService.queryValue("notebook.markdown.generate.info", userName, formatDateStr);
        return generateInfoStrByI18n;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncTemplateDashboard(Integer id, Integer tmplId, Integer modelId, Integer moduleId) {
        // need Sync Dashboard lsit
        List<VisualDashboard> needSyncDashboardList = dashboardService.getTemplateSyncDashboard(id, tmplId, modelId, moduleId);
        log.info("[syncTemplateDashboard] [sync Dashboard List] [size: {}]", needSyncDashboardList.size());

        if (Tool.CollUtil.isEmpty(needSyncDashboardList)) {
            log.warn("[syncTemplateDashboard] [sync Dashboard not found] [dashboardId: {}] [tmplId: {}] [modelId: {}] [moduleId: {}]", id, tmplId, modelId, moduleId);
            return;
        }

        log.info("[syncTemplateDashboard] [sync Dashboard begin]");
        StopWatch sw = new StopWatch();
        sw.start();
        // sync
        Map<Integer, List<VisualDashboard>> needSyncDashboardMap = needSyncDashboardList.stream().collect(Collectors.groupingBy(VisualDashboard::getTmplId));
        for (Map.Entry<Integer, List<VisualDashboard>> entry : needSyncDashboardMap.entrySet()) {
            Integer templateId = entry.getKey();
            VisualDashboard templateDashboard = dashboardService.queryDashboardIncludeChartInfo(templateId);

            List<VisualDashboard> syncDashboardList = entry.getValue();
            log.info("[syncTemplateDashboard] [sync Dashboard] [template: {}] [dashboard size: {}]", templateDashboard.getId(), syncDashboardList.size());

            if (ObjectUtil.hasEmpty(templateDashboard, syncDashboardList)) {
                log.warn("[syncTemplateDashboard] [do not perform template dashboard sync. sync object is empty]");
                continue;
            }

            // perform synchronization
            this.performSync(templateDashboard, syncDashboardList);
        }
        sw.stop();
        log.info("[syncTemplateDashboard] [sync Dashboard finshed] [Run time: {}]", sw.toString());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer createDashboardUsingTemplate(String name, String type, String description, Integer tmplId, Integer link) {
        log.info("[createDashboardUsingTemplate] [name: {}] [tmplId: {}] [link: {}]", name, tmplId, link);

        VisualDashboard tDashboard = dashboardService.getOne(new LambdaQueryWrapper<VisualDashboard>()
                .eq(VisualDashboard::getName, name)
                .eq(VisualDashboard::getType, type));
        if (ObjectUtil.isNotNull(tDashboard)) {
            throw new NZException(RCode.DASHBOARD_NAME_DUPLICATE);
        }

        // normal dashboard
        VisualDashboard dashboard = new VisualDashboard();
        dashboard.setName(name);
        dashboard.setCreateBy(ShiroUtils.getUserId().intValue());
        dashboard.setType(type);
        dashboard.setRemark(description);
        dashboard.setUts(System.currentTimeMillis());

        // add new dashboard to the end
        List<VisualDashboard> leafNodeDashboardList = dashboardService.list(new LambdaUpdateWrapper<VisualDashboard>().eq(VisualDashboard::getType, type).eq(VisualDashboard::getPid, 0));
        Integer maxWeight = leafNodeDashboardList.stream().sorted(Comparator.comparing(VisualDashboard::getWeight).reversed()).map(VisualDashboard::getWeight).findFirst().orElse(0);
        dashboard.setWeight(++maxWeight);

        dashboardService.save(dashboard);

        if (ObjectUtil.isNotEmpty(tmplId)) {
            // template Dashboard
            VisualDashboard templateDashboard = dashboardService.queryDashboardIncludeChartInfo(tmplId);

            Integer varType = templateDashboard.getVarType();
            if (ObjectUtil.equal(DashboardConstant.VariableType.ASSET.getValue(), varType)) {
                AssetAsset asset = assetAssetService.getById(link);
                ValidateUtils.is(asset).notNull(RCode.ASSET_NOT_EXIST);
            } else if (ObjectUtil.equal(DashboardConstant.VariableType.ENDPOINT.getValue(), varType)) {
                MonitorEndpoint endpoint = monitorEndpointService.getById(link);
                ValidateUtils.is(endpoint).notNull(RCode.ENDPOINT_NOTFOUND);
            } else if (ObjectUtil.equal(DashboardConstant.VariableType.NONE.getValue(), varType)) {
                // no validate link value, not using link
                link = null;
            }

            dashboard.setVarType(varType);
            dashboard.setTmplId(tmplId);
            dashboard.setLink(link);

            // sync by template
            this.performSync(templateDashboard, Tool.ListUtil.of(dashboard));
        }
        return dashboard.getId();
    }

    /**
     * perform Sync dashboard
     *
     * @param templateDashboard
     * @param syncDashboardList
     */
    private void performSync(VisualDashboard templateDashboard, List<VisualDashboard> syncDashboardList) {
        Integer templateDashboardId = templateDashboard.getId();
        Object templateDashboardParam = templateDashboard.getParam();
        List<VisualChart> templateDashboardCharts = templateDashboard.getCharts();
        templateDashboardCharts = templateDashboardCharts.stream().sorted(Comparator.comparing(VisualChart::getGroupId)).collect(Collectors.toList());

        for (VisualDashboard syncDashboard : syncDashboardList) {
            // get link object
            Integer varType = syncDashboard.getVarType();
            Integer link = syncDashboard.getLink();
            Object linkObject = null;
            Integer chartVarType = DashboardConstant.VariableType.NONE.getValue();
            if (ObjectUtil.equal(DashboardConstant.VariableType.ASSET.getValue(), varType)) {
                linkObject = assetAssetService.queryAssetInfo(link);
                chartVarType = DashboardConstant.VariableType.ASSET.getValue();
            } else if (ObjectUtil.equal(DashboardConstant.VariableType.ENDPOINT.getValue(), varType)) {
                linkObject = monitorEndpointService.queryEndpointEntity(link);
                chartVarType = DashboardConstant.VariableType.ENDPOINT.getValue();
            }

            log.info("[syncTemplateDashboard] [performSync] [dashboard type: {}] [link id: {}] [link Object exist: {}]", DashboardConstant.VariableType.getVariableType(varType), link, ObjectUtil.isNotNull(linkObject));
            if (BooleanUtil.and(
                    ObjectUtil.notEqual(DashboardConstant.VariableType.NONE.getValue(), varType),
                    ObjectUtil.isNull(linkObject))) {
                log.warn("[syncTemplateDashboard] [performSync] [link Object not exist] [link id: {}]", link);
                continue;
            }

            Integer syncDashboardId = syncDashboard.getId();

            // remove chart. no remove buildin chart
            List<VisualChart> removeChartList = visualChartService.list(new LambdaUpdateWrapper<VisualChart>()
                    .eq(VisualChart::getDashboardId, syncDashboardId)
                    .eq(VisualChart::getBuildIn, 0));
                    //.ne(VisualChart::getType, Constant.CHART_ASSETINFO_TYPE)
                    //.ne(VisualChart::getType, Constant.CHART_ENDPOINTINFO_TYPE));

            List<Integer> removeChartIdList = removeChartList.stream().map(VisualChart::getId).collect(Collectors.toList());
            if (Tool.CollUtil.isNotEmpty(removeChartIdList)) {
                visualChartService.removeByIds(removeChartIdList);
            }

            // update dashboard param & tmplid
            VisualDashboard updateDashboardObject = JSONUtil.toBean(JSONUtil.toJsonStr(syncDashboard), VisualDashboard.class);

            updateDashboardObject.setParam(Tool.NzExpressionUtil.renderExpression(templateDashboardParam.toString(), linkObject));
            updateDashboardObject.setTmplId(templateDashboardId);
            updateDashboardObject.setUts(System.currentTimeMillis());
            dashboardService.updateById(updateDashboardObject);

            // update Report Config In Cache
            dashboardService.updateReportConfigInCache(syncDashboard, updateDashboardObject);

            // insert chart
            Map<Integer, Integer> groupIdMapping = new HashMap<>();
            for (VisualChart tempChart : templateDashboardCharts) {
                VisualChart saveChart = JSONUtil.toBean(JSONUtil.toJsonStr(tempChart), VisualChart.class);

                saveChart.setId(null);
                saveChart.setDashboardId(syncDashboardId);
                saveChart.setPid(tempChart.getId());
                saveChart.setVarId(link);
                saveChart.setVarType(chartVarType);
                saveChart.setUpdateAt(new Date());
                saveChart.setUpdateBy(ShiroUtils.getUserId().intValue());

                saveChart.setGroupId(ObjectUtil.defaultIfNull(groupIdMapping.get(saveChart.getGroupId()), 0));

                saveChart.setName(Tool.NzExpressionUtil.renderExpression(saveChart.getName(), linkObject));
                saveChart.setParam(ToolUtil.isNotEmpty(saveChart.getParam()) ? Tool.NzExpressionUtil.renderExpression(saveChart.getParam().toString(), linkObject) : null);

                // element_arr
                if (Tool.CollUtil.isNotEmpty(saveChart.getElements())) {
                    for (Map element : saveChart.getElements()) {
                        element.put("expression", Tool.NzExpressionUtil.renderExpression(Tool.MapUtil.getStr(element, "expression", ""), linkObject));
                    }
                    saveChart.setElementArr(JSONUtil.toJsonStr(saveChart.getElements()));
                }

                // save
                visualChartService.save(saveChart);

                // record group id mapping
                if (StrUtil.equalsIgnoreCase(Constant.CHART_GROUP_TYPE, saveChart.getType())) {
                    groupIdMapping.put(tempChart.getId(), saveChart.getId());
                }
            }
        }
    }

    private class SnapshotRunnable extends Thread {

        private VisualChart chart;
        private Map<String, String> basicQueryParam;
        private String tid;
        private Lock lock;

        // dashboard or notebook
        private String type;

        public SnapshotRunnable(String tid, Lock lock, String type, VisualChart chart, Map<String, String> basicQueryParam) {
            this.tid = tid;
            this.lock = lock;
            this.type = type;
            this.chart = chart;
            this.basicQueryParam = basicQueryParam;
        }

        @Override
        public void run() {
            // get chart snapshot data according to datasource (system、metrics\logs)
            Object resultData = this.getChartSnapshotData();
            lock.lock();
            try {
                // update snapshot resule & process
                if (!Thread.currentThread().isInterrupted())
                    this.updateSnapshotResultAndProcess(type, resultData);
            } catch (Exception e) {
                log.error(e);
            } finally {
                lock.unlock();
            }
        }

        /**
         * get chart snapshot data according to datasource (system、metrics\logs)
         *
         * @return Object or List<Object></>
         */
        private Object getChartSnapshotData() {
            Object resultData = null;
            try {
                String datasource = chart.getDatasource();
                Map<String, Object> chartParam = JSONUtil.toBean(JSONUtil.toJsonStr(chart.getParam()), Map.class);

                // system
                if (StrUtil.equals(Constant.ChartDatasourceType.SYSTEM.getValue(), datasource)) {
                    VisualDashboard visualDashboard = chart.getDashboard();
                    R r = R.ok();
                    Map<String, Object> queryParams = MapUtil.newHashMap();
                    queryParams.put("type", "total");
                    queryParams.put("dimension", "priority");
                    queryParams.put("step", "d");
                    if (StrUtil.equals(Constant.CHART_ASSETINFO_TYPE, chart.getType())) {
                        queryParams.put("assetId", visualDashboard.getLink());
                        Map<String, Object> alertMessageTrend = statServiceImpl.queryAlertMessageTrend(queryParams);
                        AssetAsset asset = assetAssetService.queryAssetInfo(visualDashboard.getLink());
                        Map<String, Object> assetDataMap = BeanUtil.beanToMap(asset);
                        assetDataMap.put("alertMessageTrend", alertMessageTrend);
                        r.put("data", assetDataMap);
                    } else if (StrUtil.equals(Constant.CHART_ENDPOINTINFO_TYPE, chart.getType())) {
                        queryParams.put("endpointId", visualDashboard.getLink());
                        Map<String, Object> alertMessageTrend = statServiceImpl.queryAlertMessageTrend(queryParams);
                        MonitorEndpoint endpoint = monitorEndpointService.queryEndpointEntity(visualDashboard.getLink());
                        Map<String, Object> endpointDataMap = BeanUtil.beanToMap(endpoint);
                        endpointDataMap.put("alertMessageTrend", alertMessageTrend);
                        r.put("data", endpointDataMap);
                    } else {
                        cn.hutool.json.JSONObject dataSource = JSONUtil.parseArray(chartParam.get("datasource")).getJSONObject(0);
                        dataSource.set("select", JSONUtil.createArray().put(dataSource.get("select")));
                        r.putAllData(
                                statServiceImpl.queryAnalyse(
                                        JSONUtil.toBean(JSONUtil.createObj().set("q", dataSource.toString()), Map.class)
                                )
                        );
                    }
                    r.put("eleIndex", "0");
                    r.put("chartId", chart.getId());

                    resultData = ObjectUtil.clone(r);
                } else {
                    // metrics\logs
                    if (Tool.CollUtil.isNotEmpty(chart.getElements())) {
                        String start = basicQueryParam.get("start");
                        String end = basicQueryParam.get("end");
                        String step = basicQueryParam.get("step");
                        String limit = ToolUtil.toStr(chartParam.get("limit"), "100");
                        String nullType = ToolUtil.toStr(chartParam.get("nullType"), "null");
                        String logsDirection = "backward";

                        List<Object> rList = Tool.ListUtil.list(true);
                        Integer eleIndex = 0;
                        for (Map element : chart.getElements()) {
                            String indexStr = StrUtil.toString(eleIndex++);
                            JSONObject jsonObject = new JSONObject();
                            if (StrUtil.equals(Constant.ChartDatasourceType.METRICS.getValue(), datasource)) {
                                jsonObject = promApiService.query_range(Tool.MapUtil.getStr(element, "expression", ""), start, end, step, nullType, null);
                            } else if (StrUtil.equals(Constant.ChartDatasourceType.LOGS.getValue(), datasource)) {
                                jsonObject = lokiApiService.query_range(Tool.MapUtil.getStr(element, "expression", ""), start, end, limit, step, null, logsDirection, "1", null);
                            }
                            jsonObject.put("eleIndex", indexStr);
                            jsonObject.put("chartId", chart.getId());
                            rList.add(jsonObject);
                        }

                        resultData = ObjectUtil.clone(rList);
                    } else {
                        // no element chart data
                        resultData = JSONUtil.toBean(
                                JSONUtil.createObj()
                                        .set("eleIndex", 0)
                                        .set("chartId", chart.getId())
                                , Map.class);
                    }
                }
            } catch (Exception e) {
                log.error(e, "snapshot data get failed. type: {}, taskId: {}, chartInfo: {}", type, tid, JSONUtil.toJsonStr(chart));
            }
            return resultData;
        }

        /**
         * update snapshot resule & process
         *
         * @param type
         * @param resultData
         */
        private void updateSnapshotResultAndProcess(String type, Object resultData) {
            try {
                // snapshot result cache
                String snapshotResultKey = StrUtil.concat(true, type, "_snapshot_result_", tid);
                List<Object> snapshotResultList = (List<Object>) SNAPSHOT_CACHE.get(snapshotResultKey);
                snapshotResultList = Tool.CollUtil.defaultIfEmpty(snapshotResultList, Tool.ListUtil.list(true));

                Tool.CollUtil.addAll(snapshotResultList, resultData);
                SNAPSHOT_CACHE.put(snapshotResultKey, snapshotResultList, timeout * 5);

                // snapshot task process
                String snapshotStatKey = StrUtil.concat(true, type, "_snapshot_stat_", tid);
                Map snapshotStatMap = (Map) SNAPSHOT_CACHE.get(snapshotStatKey);
                Integer total = (Integer) snapshotStatMap.get("total");
                Integer done = (Integer) snapshotStatMap.get("done");
                done = done + 1;

                // cal process
                String percent = NumberUtil.formatPercent((double) done / total, 2);
                snapshotStatMap.put("process", percent.replace("%", ""));
                snapshotStatMap.put("done", done);
                snapshotStatMap.put("total", total);
                SNAPSHOT_CACHE.put(snapshotStatKey, snapshotStatMap, timeout * 5);

                log.info("snapshot result updated, type: {}, taskId: {}, total: {}, done: {}, process: {}", type, tid, total, done, percent);
            } catch (Exception e) {
                log.error(e, "snapshot result error. type: {}, taskId: {}", type, tid);
            }
        }
    }

}
