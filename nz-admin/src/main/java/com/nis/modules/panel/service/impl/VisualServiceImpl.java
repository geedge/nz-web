package com.nis.modules.panel.service.impl;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.thread.NamedThreadFactory;
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
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.panel.entity.VisualChart;
import com.nis.modules.panel.entity.VisualChartElement;
import com.nis.modules.panel.entity.VisualPanel;
import com.nis.modules.panel.service.VisualChartElementService;
import com.nis.modules.panel.service.VisualChartService;
import com.nis.modules.panel.service.VisualPanelService;
import com.nis.modules.panel.service.VisualService;
import com.nis.modules.panel.utils.DashboardConstant;
import com.nis.modules.stat.service.impl.StatServiceImpl;
import com.nis.modules.sys.entity.Timezone;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.TimezoneService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
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

    @Value("${snapshot.corePoolSize:5}")
    private Integer snapshotCorePoolSize;

    @Value("${snapshot.concurrency:10}")
    private Integer snapshotConcurrency;

    @Value("${snapshot.keepAliveTime:30}")
    private Long snapshotKeepAliveTime;

    @Value("${snapshot.capacity:10000}")
    private Integer snapshotCapacity;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private PromApiService promApiService;

    @Autowired
    private LokiApiService lokiApiService;

    @Autowired
    private TimezoneService timezoneService;

    @Autowired
    private StatServiceImpl statServiceImpl;

    @Autowired
    private VisualPanelService visualPanelService;

    @Autowired
    private VisualChartService visualChartService;

    @Autowired
    private VisualChartElementService visualChartElementService;

    @Autowired
    private AssetAssetService assetAssetService;

    @Autowired
    private MonitorEndpointService monitorEndpointService;

    private static volatile TimedCache<String, Object> SNAPSHOT_CACHE;

    private ExecutorService snapshotThreadPool;

    @PostConstruct
    public void init() {
        log.info("Setup dashboard snapshot cache begin...");
        // 创建缓存，默认 snapshotTimeout * 5
        SNAPSHOT_CACHE = Tool.CacheUtil.newTimedCache(timeout * 5);

        // snapshot 执行线程池，避免大量请求访问 prometheus 服务
        snapshotThreadPool = new ThreadPoolExecutor(
                snapshotCorePoolSize,
                snapshotConcurrency,
                snapshotKeepAliveTime,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(snapshotCapacity),
                new NamedThreadFactory("SNAPSHOT-", true));

                /*new ThreadPoolExecutor(
                10,
                Integer.MAX_VALUE,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new ThreadFactoryBuilder().setNamePrefix("dashboard-snapshot-pool-").build());*/
        log.info("Setup dashboard snapshot cache finished");
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
                    queryData = promApiService.query_range(expression, StrUtil.toString(start), StrUtil.toString(end), step, null, null);
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
        String timezone = sysConfigService.getValue("timezone");
        Timezone timezoneEntity = timezoneService.getOne(new LambdaUpdateWrapper<Timezone>().eq(Timezone::getName, timezone));
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
            String path = sysConfigService.getValue("snapshot_template_path");
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
    public String createSnapshotTask(String format, Integer panelId, Long start, Long end, String varsJsonStr) {
        VisualPanel panel = visualPanelService.getById(panelId);
        ValidateUtils.is(panel).notNull(RCode.CHART_PANEL_ISNULL);

        // 查询 charts
        Map param = Tool.MapUtil.builder()
                .put("pageSize", "-1")
                .put("panelId", panel.getId())
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
                .put("panel", R.ok(panel))
                .put("charts", R.ok(chartPageUtils))
                .map();

        log.info("dashboard snapshot start. taskId: {}, panelId:{} total: {}", taskId, panelId, allChartList.size());
        String stat = StrUtil.concat(true, "snapshot_stat_", taskId);
        SNAPSHOT_CACHE.put(stat, taskStat);

        ThreadUtil.execAsync(() -> {
            // 计算步长
            String step = this.getTimeStep(start, end);
            Map<String, String> basicQueryParam = Tool.MapUtil.newHashMap();
            basicQueryParam.put("start", StrUtil.toString(start));
            basicQueryParam.put("end", StrUtil.toString(end));
            basicQueryParam.put("step", StrUtil.toString(step));

            // run Snapshot Runnable
            ThreadGroup threadGroup = new ThreadGroup(taskId);
            SNAPSHOT_CACHE.put(StrUtil.concat(true, "snapshot_threadGroup_", taskId), threadGroup);

            ReentrantReadWriteLock readWriteLock = Tool.LockUtil.createReadWriteLock(false);
            ReentrantReadWriteLock.WriteLock lock = readWriteLock.writeLock();

            allChartList.stream().forEach(chart -> {
                SnapshotRunnable snapshotRunnable = new SnapshotRunnable(threadGroup, lock, chart, basicQueryParam);

                // add thredGroup & submit
                ReflectUtil.invoke(threadGroup, "add", snapshotRunnable);
                snapshotThreadPool.submit(snapshotRunnable);
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
            List<VisualChart> allChartList = visualChartService.getAllChartList(chartList);
            return allChartList;
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
            Integer startTempId = -10;
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

                    List<VisualChartElement> elements = chart.getElements();
                    elements = Tool.CollUtil.defaultIfEmpty(elements, Tool.ListUtil.list(true));
                    for (VisualChartElement element : elements) {
                        // temp chart id
                        element.setChartId(tempChartId);
                        String expression = element.getExpression();
                        expression = expression.replaceAll(StrUtil.concat(true, "\\$", repeatVariable), varValue);
                        element.setExpression(expression);
                    }
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
            List<VisualChartElement> elements = chart.getElements();
            elements = Tool.CollUtil.defaultIfEmpty(elements, Tool.ListUtil.list(true));
            for (VisualChartElement element : elements) {
                String expression = element.getExpression();
                for (Map.Entry<String, String> entry : singleVariableEntrySet) {
                    expression = expression.replaceAll(StrUtil.concat(true, "\\$", entry.getKey()), entry.getValue());
                }
                element.setExpression(expression);
            }
        }
        log.info("[renderSingleChartVariables] [finshed]");
        return allChartList;
    }


    @Override
    public Map snapshotResultProcess(String taskId) {
        String snapshotStatKey = StrUtil.concat(true, "snapshot_stat_", taskId);
        Object obj = SNAPSHOT_CACHE.get(snapshotStatKey);
        if (obj == null) {
            log.warn("dashboard snapshot result process error, the task does not exist or has expired. task id: {}", taskId);
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
    public void cancelSnapshotTask(String taskId) {
        log.info("dashboard snapshot cancel task id: {}", taskId);
        String snapshotThreadGroupKey = StrUtil.concat(true, "snapshot_threadGroup_", taskId);
        Object obj = SNAPSHOT_CACHE.get(snapshotThreadGroupKey);

        if (obj == null) {
            log.warn("dashboard snapshot task cancel error, the task does not exist or has expired. task id: {}", taskId);
            throw new NZException(RCode.DASHBOARD_SNAPSHOT_TASK_NOT_EXIST);
        } else {
            try {
                ThreadGroup threadGroup = (ThreadGroup) obj;
                log.info("dashboard snapshot task cancel, task id: {}, canceled task thread num: {}", taskId, threadGroup.activeCount());
                // thread interrupt
                threadGroup.interrupt();

                // remove cache data
                String snapshotStatKey = StrUtil.concat(true, "snapshot_stat_", taskId);
                String snapshotResultKey = StrUtil.concat(true, "snapshot_result_", taskId);

                SNAPSHOT_CACHE.remove(snapshotThreadGroupKey);
                SNAPSHOT_CACHE.remove(snapshotStatKey);
                SNAPSHOT_CACHE.remove(snapshotResultKey);
                log.info("dashboard snapshot task is canceled, the cache data is cleared,task id {}", taskId);
            } catch (Exception e) {
                log.error("dashboard snapshot task cancel error", e);
            }
        }
    }

    @Override
    public void downloadSnapshotFile(HttpServletResponse response, String language, String taskId) {
        String snapshotStatKey = StrUtil.concat(true, "snapshot_stat_", taskId);
        String snapshotResultKey = StrUtil.concat(true, "snapshot_result_", taskId);

        Object obj = SNAPSHOT_CACHE.get(snapshotStatKey);
        if (obj == null) {
            throw new NZException(RCode.DASHBOARD_SNAPSHOT_TASK_NOT_EXIST);
        }

        try {
            Map snapshotInfoMap = (Map) obj;
            Object start = snapshotInfoMap.get("start");
            Object end = snapshotInfoMap.get("end");
            Object panel = snapshotInfoMap.get("panel");
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
            resultMap.put("panel", panel);
            resultMap.put("charts", charts);
            String snapshotTemplate = this.replaceSnapshotTemplate(resultMap);

            // download file
            ResponseUtil.downloadFile(response, "exploreSnapshot.html", snapshotTemplate.getBytes());
        } catch (Exception e) {
            log.error(e);
        } finally {
            String snapshotThreadGroupKey = StrUtil.concat(true, "snapshot_threadGroup_", taskId);

            Object tg = SNAPSHOT_CACHE.get(snapshotThreadGroupKey);
            ThreadGroup threadGroup = (ThreadGroup) tg;
            log.info("dashboard snapshot task cancel, task id: {}, canceled task thread num: {}", taskId, threadGroup.activeCount());
            // thread interrupt
            threadGroup.interrupt();

            // remove cache data
            SNAPSHOT_CACHE.remove(snapshotThreadGroupKey);
            SNAPSHOT_CACHE.remove(snapshotStatKey);
            SNAPSHOT_CACHE.remove(snapshotResultKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncTemplateDashboard(Integer id, Integer tmplId, Integer modelId, Integer moduleId) {
        // need Sync Dashboard lsit
        List<VisualPanel> needSyncDashboardList = visualPanelService.getTemplateSyncDashboard(id, tmplId, modelId, moduleId);
        log.info("[syncTemplateDashboard] [sync Dashboard List] [size: {}]", needSyncDashboardList.size());

        if (Tool.CollUtil.isEmpty(needSyncDashboardList)) {
            log.warn("[syncTemplateDashboard] [sync Dashboard not found] [panelId: {}] [tmplId: {}] [modelId: {}] [moduleId: {}]", id, tmplId, modelId, moduleId);
            return;
        }

        log.info("[syncTemplateDashboard] [sync Dashboard begin]");
        StopWatch sw = new StopWatch();
        sw.start();
        // sync
        Map<Integer, List<VisualPanel>> needSyncDashboardMap = needSyncDashboardList.stream().collect(Collectors.groupingBy(VisualPanel::getTmplId));
        for (Map.Entry<Integer, List<VisualPanel>> entry : needSyncDashboardMap.entrySet()) {
            Integer templateId = entry.getKey();
            VisualPanel templateDashboard = visualPanelService.queryPanelIncludeChartInfo(templateId);

            List<VisualPanel> syncDashboardList = entry.getValue();
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

        VisualPanel tPanel = visualPanelService.getOne(new LambdaQueryWrapper<VisualPanel>()
                .eq(VisualPanel::getName, name)
                .eq(VisualPanel::getType, type));
        if (ObjectUtil.isNotNull(tPanel)) {
            throw new NZException(RCode.PANEL_NAME_DUPLICATE);
        }

        // normal dashboard
        VisualPanel panel = new VisualPanel();
        panel.setName(name);
        panel.setCreateBy(ShiroUtils.getUserId().intValue());
        panel.setType(type);
        panel.setRemark(description);
        panel.setUts(System.currentTimeMillis());
        visualPanelService.save(panel);

        if (ObjectUtil.isAllNotEmpty(tmplId, link)) {
            // template Dashboard
            VisualPanel templateDashboard = visualPanelService.queryPanelIncludeChartInfo(tmplId);

            Integer varType = templateDashboard.getVarType();
            if (ObjectUtil.equal(DashboardConstant.VariableType.ASSET.getValue(), varType)) {
                AssetAsset asset = assetAssetService.getById(link);
                ValidateUtils.is(asset).notNull(RCode.ASSET_NOT_EXIST);
            } else if (ObjectUtil.equal(DashboardConstant.VariableType.ENDPOINT.getValue(), varType)) {
                MonitorEndpoint endpoint = monitorEndpointService.getById(link);
                ValidateUtils.is(endpoint).notNull(RCode.ENDPOINT_NOTFOUND);
            }

            panel.setVarType(varType);
            panel.setTmplId(tmplId);
            panel.setLink(link);

            // sync by template
            this.performSync(templateDashboard, Tool.ListUtil.of(panel));
        }
        return panel.getId();
    }

    /**
     * perform Sync dashboard
     *
     * @param templateDashboard
     * @param syncDashboardList
     */
    private void performSync(VisualPanel templateDashboard, List<VisualPanel> syncDashboardList) {
        Integer templateDashboardId = templateDashboard.getId();
        Object templateDashboardParam = templateDashboard.getParam();
        List<VisualChart> templateDashboardCharts = templateDashboard.getCharts();
        templateDashboardCharts = templateDashboardCharts.stream().sorted(Comparator.comparing(VisualChart::getGroupId)).collect(Collectors.toList());

        for (VisualPanel syncPanel : syncDashboardList) {
            // get link object
            Integer varType = syncPanel.getVarType();
            Integer link = syncPanel.getLink();
            Object linkObject = null;
            Integer chartVarType = DashboardConstant.VariableType.NONE.getValue();
            if (ObjectUtil.equal(DashboardConstant.VariableType.ASSET.getValue(), varType)) {
                linkObject = assetAssetService.queryAssetInfo(link);
                chartVarType = DashboardConstant.VariableType.ASSET.getValue();
            } else if (ObjectUtil.equal(DashboardConstant.VariableType.ENDPOINT.getValue(), varType)) {
                linkObject = monitorEndpointService.queryEndpointEntity(link);
                chartVarType = DashboardConstant.VariableType.ENDPOINT.getValue();
            }

            log.info("[syncTemplateDashboard] [performSync] [link id: {}] [link Object exist: {}]", link, ObjectUtil.isNotNull(linkObject));
            if (ObjectUtil.isNull(linkObject)) {
                log.warn("[syncTemplateDashboard] [performSync] [link Object not exist] [link id: {}]", link);
                continue;
            }

            Integer syncPanelId = syncPanel.getId();

            // remove chart. no remove buildin assetInfo endpointInfo chart
            List<VisualChart> removeChartList = visualChartService.list(new LambdaUpdateWrapper<VisualChart>()
                    .eq(VisualChart::getPanelId, syncPanelId)
                    .eq(VisualChart::getBuildIn, 0));
                    //.ne(VisualChart::getType, Constant.CHART_ASSETINFO_TYPE)
                    //.ne(VisualChart::getType, Constant.CHART_ENDPOINTINFO_TYPE));

            List<Integer> removeChartIdList = removeChartList.stream().map(VisualChart::getId).collect(Collectors.toList());
            if (Tool.CollUtil.isNotEmpty(removeChartIdList)) {
                visualChartService.removeByIds(removeChartIdList);
                visualChartElementService.remove(new LambdaQueryWrapper<VisualChartElement>().in(VisualChartElement::getChartId, removeChartIdList));
            }

            // update dashboard param & tmplid
            VisualPanel updatePanelObject = new VisualPanel();
            BeanUtil.copyProperties(syncPanel, updatePanelObject);

            updatePanelObject.setParam(Tool.NzExpressionUtil.renderExpression(templateDashboardParam.toString(), linkObject));
            updatePanelObject.setTmplId(templateDashboardId);
            updatePanelObject.setUts(System.currentTimeMillis());
            visualPanelService.updateById(updatePanelObject);

            // update Report Config In Redis
            visualPanelService.updateReportConfigInRedis(syncPanel, updatePanelObject);

            // insert chart & chart element
            List<VisualChartElement> saveChartElementList = Tool.ListUtil.list(true);
            Map<Integer, Integer> groupIdMapping = new HashMap<>();
            for (VisualChart tempChart : templateDashboardCharts) {
                VisualChart saveChart = new VisualChart();
                BeanUtil.copyProperties(tempChart, saveChart);

                saveChart.setId(null);
                saveChart.setPanelId(syncPanelId);
                saveChart.setPid(tempChart.getId());
                saveChart.setVarId(link);
                saveChart.setVarType(chartVarType);
                saveChart.setUpdateAt(new Date());
                saveChart.setUpdateBy(ShiroUtils.getUserId().intValue());

                saveChart.setGroupId(ObjectUtil.defaultIfNull(groupIdMapping.get(saveChart.getGroupId()), 0));

                saveChart.setName(Tool.NzExpressionUtil.renderExpression(saveChart.getName(), linkObject));
                saveChart.setParam(ToolUtil.isNotEmpty(saveChart.getParam()) ? Tool.NzExpressionUtil.renderExpression(saveChart.getParam().toString(), linkObject) : null);

                // save
                visualChartService.save(saveChart);
                if (Tool.CollUtil.isNotEmpty(saveChart.getElements())) {
                    for (VisualChartElement element : saveChart.getElements()) {
                        element.setId(null);
                        element.setExpression(Tool.NzExpressionUtil.renderExpression(element.getExpression(), linkObject));
                        element.setChartId(saveChart.getId());
                    }
                    saveChartElementList.addAll(saveChart.getElements());
                }

                // record group id mapping
                if (StrUtil.equalsIgnoreCase(Constant.CHART_GROUP_TYPE, saveChart.getType())) {
                    groupIdMapping.put(tempChart.getId(), saveChart.getId());
                }
            }

            // save element
            if (Tool.CollUtil.isNotEmpty(saveChartElementList))
                visualChartElementService.saveBatch(saveChartElementList);
        }
    }

    private class SnapshotRunnable extends Thread {

        private VisualChart chart;
        private ThreadGroup threadGroup;
        private Map<String, String> basicQueryParam;
        private Lock lock;

        private boolean isInterrupted = false;

        public SnapshotRunnable(ThreadGroup threadGroup, Lock lock, VisualChart chart, Map<String, String> basicQueryParam) {
            // group & name
            super(threadGroup, String.format("Snapshot-%s-%s", threadGroup.getName(), chart.getId()));

            this.threadGroup = threadGroup;
            this.lock = lock;
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
                if (!this.isInterrupted())
                    this.updateSnapshotResultAndProcess(resultData);
            } catch (Exception e) {
                log.error(e);
            } finally {
                lock.unlock();
                ReflectUtil.invoke(threadGroup, "remove", this);
            }
        }

        @Override
        public void interrupt() {
            super.interrupt();
            isInterrupted = true;
            log.warn("dashboard snapshot thread is interrupted. name: {}", this.getName());
        }

        @Override
        public boolean isInterrupted() {
            return isInterrupted;
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
                Map<String, Object> chartParam = (HashMap) chart.getParam();

                // system
                if (StrUtil.equals(Constant.ChartDatasourceType.SYSTEM.getValue(), datasource)) {
                    VisualPanel visualPanel = chart.getPanel();
                    R r = R.ok();
                    if (StrUtil.equals(Constant.CHART_ASSETINFO_TYPE, chart.getType())) {
                        r.put("data", assetAssetService.queryAssetInfo(visualPanel.getLink()));
                    } else if (StrUtil.equals(Constant.CHART_ENDPOINTINFO_TYPE, chart.getType())) {
                        r.put("data", monitorEndpointService.queryEndpointEntity(visualPanel.getLink()));
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
                        for (VisualChartElement element : chart.getElements()) {
                            String indexStr = StrUtil.toString(eleIndex++);
                            JSONObject jsonObject = new JSONObject();
                            if (StrUtil.equals(Constant.ChartDatasourceType.METRICS.getValue(), datasource)) {
                                jsonObject = promApiService.query_range(element.getExpression(), start, end, step, nullType, null);
                            } else if (StrUtil.equals(Constant.ChartDatasourceType.LOGS.getValue(), datasource)) {
                                jsonObject = lokiApiService.query_range(element.getExpression(), start, end, limit, step, null, logsDirection, "1", null);
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
                log.error("dashboard snapshot data get failed. taskId: {}, chartInfo: {}", e, JSONUtil.toJsonStr(chart));
            }
            return resultData;
        }

        /**
         * update snapshot resule & process
         *
         * @param resultData
         */
        private void updateSnapshotResultAndProcess(Object resultData) {
            try {
                // task id
                String taskId = threadGroup.getName();

                // snapshot result cache
                String snapshotResultKey = StrUtil.concat(true, "snapshot_result_", taskId);
                List<Object> snapshotResultList = (List<Object>) SNAPSHOT_CACHE.get(snapshotResultKey);
                snapshotResultList = Tool.CollUtil.defaultIfEmpty(snapshotResultList, Tool.ListUtil.list(true));

                Tool.CollUtil.addAll(snapshotResultList, resultData);
                SNAPSHOT_CACHE.put(snapshotResultKey, snapshotResultList);

                // snapshot task process
                String snapshotStatKey = StrUtil.concat(true, "snapshot_stat_", taskId);
                Map snapshotStatMap = (Map) SNAPSHOT_CACHE.get(snapshotStatKey);
                Integer total = (Integer) snapshotStatMap.get("total");
                Integer done = (Integer) snapshotStatMap.get("done");
                done = done + 1;

                // cal process
                String percent = NumberUtil.formatPercent((double) done / total, 2);
                snapshotStatMap.put("process", percent.replace("%", ""));
                snapshotStatMap.put("done", done);
                snapshotStatMap.put("total", total);
                SNAPSHOT_CACHE.put(snapshotStatKey, snapshotStatMap);

                log.info("dashboard snapshot result updated, taskId: {}, total: {}, done: {}, process: {}", taskId, total, done, percent);
            } catch (Exception e) {
                log.error("dashboard snapshot result error,taskId: {}", e, threadGroup.getName());
            }
        }
    }

}
