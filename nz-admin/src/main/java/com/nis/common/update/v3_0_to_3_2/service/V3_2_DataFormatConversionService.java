package com.nis.common.update.v3_0_to_3_2.service;

import cn.hutool.db.Entity;
import cn.hutool.db.SqlConnRunner;
import cn.hutool.db.handler.EntityListHandler;
import cn.hutool.db.handler.StringHandler;
import cn.hutool.db.sql.SqlExecutor;
import cn.hutool.log.Log;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.exception.NZException;
import com.nis.common.update.DataFormatConversionInterface;
import com.nis.common.utils.Constant;
import com.nis.common.utils.JSONUtil;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * V3_2_DataFormatConversionServiceImpl
 *
 * @author shizhendong
 * @date 2023/03/15
 */
@Service
public class V3_2_DataFormatConversionService implements DataFormatConversionInterface {

    private static final Log log = Log.get();

    /**
     * back up table name
     */
    private static final String BACK_UP_TABLE_NAME = "visual_chart_v3_2";

    private Connection connection;

    private SqlConnRunner sqlRunner;

    private static final String[] LETTERS = new String[]{
            "A", "B", "C", "D", "E", "F", "G",
            "H", "I", "J", "K", "L", "M", "N",
            "O", "P", "Q", "R", "S", "T",
            "U", "V", "W", "X", "Y", "Z"};

    /**
     * before Data Format Conversion
     * check backup table is exists
     *
     * @return
     */
    private boolean beforeDataFormatConversion() throws SQLException {
        String tableName = SqlExecutor.query(this.connection, "SHOW TABLES LIKE ?", new StringHandler(), BACK_UP_TABLE_NAME);
        log.info("[beforeDataFormatConversion] [check the backup table exists] [query result: {}]", tableName);

        if (Tool.StrUtil.isEmpty(tableName)) {
            log.warn("[beforeDataFormatConversion] [The backup table does not exist. Do not perform data conversion] [table: {}]", BACK_UP_TABLE_NAME);
            return true;
        }

        log.info("[beforeDataFormatConversion] [The backup table exists, execute data conversion] [table: {}]", BACK_UP_TABLE_NAME);
        return true;
    }


    @Override
    public void dataFormatConversion(Connection connection) throws Exception {
        // init sql conn runner
        this.connection = connection;
        sqlRunner = this.initSqlRunner(connection);

        boolean isConversion = this.beforeDataFormatConversion();
        if (!isConversion) {
            return;
        }

        log.info("[dataFormatConversion] [conversion V3_0 to V3_2 Chart Data]");

        // handle visual_chart_element name
        this.handleElementName();

        // conversion Visual Chart Data
        this.conversionVisualChartData();

        // remove back up table
        log.info("[dataFormatConversion] [remove back up table] [table_name: {}]", BACK_UP_TABLE_NAME);
        SqlExecutor.execute(this.connection, Tool.StrUtil.concat(true, "DROP TABLE ", BACK_UP_TABLE_NAME));
    }

    /**
     * chart element name 处理
     * 初始化规则： A-Z AA-AZ BA-BZ AAA-AAZ 顺序添加
     */
    private void handleElementName() throws SQLException {
        log.info("[handleElementName] [insert chart element name auto] [begin]");
        log.info("[handleElementName] [insert chart element name auto] [rule: {}]", "A-Z AA-AZ BA-BZ AAA-AAZ...");

        List<Entity> chartElementList = sqlRunner.findAll(this.connection, Entity.create("visual_chart_element"));
        Map<Object, List<Entity>> elementGroupingByChartIdMap = chartElementList.stream().collect(Collectors.groupingBy(entity -> entity.get("chart_id")));
        for (Map.Entry<Object, List<Entity>> entry : elementGroupingByChartIdMap.entrySet()) {
            List<Entity> elementListForChart = entry.getValue();
            for (int i = 0; i < elementListForChart.size(); i++) {
                String letter = this.transformNumToLetter(i);
                Entity chartElement = elementListForChart.get(i);
                chartElement.set("name", letter);
            }
        }
        List<Entity> allElementList = elementGroupingByChartIdMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        log.info("[handleElementName] [update element size: {}]", allElementList.size());
        if (Tool.CollUtil.isNotEmpty(allElementList)) {
            // 防止 sql 过长，分割后拼接 sql 修改
            Integer batchSize = 1000;
            Integer limit = (allElementList.size() + batchSize - 1) / batchSize;
            List<List<Entity>> splitVisualChartElementList = Stream.iterate(0, n -> n + 1).limit(limit).parallel().map(a -> allElementList.stream().skip(a * batchSize).limit(batchSize).parallel().collect(Collectors.toList())).collect(Collectors.toList());

            SQLUtils.FormatOption formatOption = new SQLUtils.FormatOption(false, false);
            for (List<Entity> batchUpdateElementList : splitVisualChartElementList) {
                List<String> list = Tool.ListUtil.list(true);
                for (Entity entity : batchUpdateElementList) {
                    String sql = SQLUtils.format("update visual_chart_element set name = ? where id = ?", DbType.mariadb, Tool.ListUtil.of(entity.getStr("name"), entity.get("id")), formatOption);
                    list.add(sql);
                }
                // execute Batch
                SqlExecutor.executeBatch(this.connection, list);
            }
        }
        log.info("[handleElementName] [insert chart element name auto] [finshed]");
    }

    /**
     * conversion Visual Chart Data
     *
     * @throws SQLException
     */
    private void conversionVisualChartData() throws SQLException {
        log.info("[conversionVisualChartData] [begin]");

        List<Entity> backUpChartList = SqlExecutor.query(this.connection, "SELECT * FROM visual_chart_v3_2 ORDER BY panel_id, group_id, weight, id", new EntityListHandler());
        log.info("[conversionVisualChartData] [conversion size: {}]", backUpChartList.size());

        // query element list.  name has been processed
        List<Entity> chartElementList = sqlRunner.findAll(this.connection, Entity.create("visual_chart_element"));
        Map<String, List<Entity>> elementGroupingByChartIdMap = chartElementList.stream().collect(Collectors.groupingBy(entity -> entity.getStr("chart_id")));

        // drop alertList type chart
        // grouping by panel_id
        Map<String, List<Entity>> chartGaroupingByPanelIdMap = backUpChartList.stream()
                .filter(entity -> Tool.ObjectUtil.notEqual("alertList", entity.getStr("type")))
                .collect(Collectors.groupingBy(entity -> entity.getStr("panel_id")));

        for (Map.Entry<String, List<Entity>> entry : chartGaroupingByPanelIdMap.entrySet()) {
            String panelId = entry.getKey();
            List<Entity> chartListForPanel = entry.getValue();

            // handle chart param/datasource
            this.handleChartParamFormat(chartListForPanel, elementGroupingByChartIdMap);

            // handle Position chart.x y
            this.handlePosition(panelId, chartListForPanel);

            // insert batch
            if (Tool.CollUtil.isNotEmpty(chartListForPanel)) {
                for (Entity entity : chartListForPanel) {
                    // reset table name
                    entity.setTableName("visual_chart");
                }
                sqlRunner.insert(this.connection,chartListForPanel);
            }
        }
        log.info("[conversionVisualChartData] [finshed]");
    }

    /**
     * 根据下标 获取对应 26 字母，如果超过 26 字母，按照  A-Z AA-AZ BA -BZ AAA-AAZ 顺序排
     *
     * @param num
     * @return
     */
    private String transformNumToLetter(Integer num) {
        String letter = "";
        Integer loopNum = num / 26;
        if (loopNum > 0) {
            letter += transformNumToLetter(loopNum - 1);
        }
        letter += LETTERS[num % 26];
        return letter;
    }

    /**
     * 处理位置信息 x/y
     *
     * @param panelId
     * @param chartList
     */
    private void handlePosition(String panelId, List<Entity> chartList) {
        chartList = chartList.stream().sorted(
                Comparator.comparing(entity -> {
                    String weightStr = Tool.StrUtil.emptyToDefault(entity.getStr("weight"), "0");
                    return Integer.valueOf(weightStr);
                })
        ).collect(Collectors.toList());

        if (Tool.StrUtil.equals("0", panelId)) {
            // chart template 列表数据不需要处理 只需处理 group 内的 chart ,其他 chart 都设置为 0，0
            Map<String, List<Entity>> chartListGroupByGroupId = chartList.stream().collect(Collectors.groupingBy(entity -> entity.getStr("group_id")));
            for (Map.Entry<String, List<Entity>> entry : chartListGroupByGroupId.entrySet()) {
                String groupId = entry.getKey();
                List<Entity> visualChartList = entry.getValue();
                if (Tool.StrUtil.equals("0", groupId)) {
                    for (Entity visualChart : visualChartList) {
                        if (Tool.StrUtil.equals(visualChart.getStr("type"), "group")) {
                            visualChart.set("height", 3);
                        }
                        visualChart.set("x", 0);
                        visualChart.set("y", 0);
                    }
                } else {
                    // 根据 span and height 计算 XY
                    this.calculateXYAccordingToSpanAndHeight(visualChartList);
                }
            }
        } else {
            // 排序 ，按照 chart - group  - chart 顺序排 （group inner chart） 排除在外
            List<Entity> list = new ArrayList<>(chartList.size());
            for (Entity visualChart : chartList) {
                if (Tool.StrUtil.equals(visualChart.getStr("type"), "group")) {
                    // 添加 group
                    list.add(visualChart);
                } else if (!Tool.StrUtil.equals(visualChart.getStr("type"), "group")
                        && Tool.StrUtil.equals(visualChart.getStr("group_id"), "0")) {
                    // 非 group & 非 group inner chart
                    list.add(visualChart);
                }
            }
            // 根据 span and height 计算 XY
            this.calculateXYAccordingToSpanAndHeight(list);

            Map<String, List<Entity>> chartListGroupByGroupId = chartList.stream()
                    .filter(entity -> Tool.ObjectUtil.notEqual("0", entity.getStr("group_id")))
                    .collect(Collectors.groupingBy(entity -> entity.getStr("group_id")));

            for (Map.Entry<String, List<Entity>> entry : chartListGroupByGroupId.entrySet()) {
                // 根据 span and height 计算 XY
                this.calculateXYAccordingToSpanAndHeight(entry.getValue());
            }
        }
    }

    /**
     * 根据 span and height 计算 XY
     *
     * @param list
     */
    private void calculateXYAccordingToSpanAndHeight(List<Entity> list) {
        // 每行 x 计数
        Integer colX = 0;
        // 每行 y 计数
        Integer colY = 0;
        // 行内最高值
        Integer highestValueInLine = 0;
        for (int i = 0; i < list.size(); i++) {
            Entity chart = list.get(i);
            Integer span = Integer.valueOf(chart.getStr("span"));
            // group 默认给 3 就 ok ，前端会根据 组内 chart 自动调整高度
            Integer height = Tool.StrUtil.equals(chart.getStr("type"), "group") ? 3 : Integer.valueOf(chart.getStr("height"));
            chart.set("height", height);
            colX += span;
            // 一行结束
            if (colX > 12) {
                colX = span;
                // 下一行第一个 Y 就是上一行 height 最大值
                colY += highestValueInLine;
                Integer colXValue = colX - span.intValue();
                chart.set("x", colXValue);
                chart.set("y", colY);
                highestValueInLine = 0;
            } else {
                // group
                if (Tool.StrUtil.equals(chart.getStr("type"), "group")) {
                    chart.set("x", 0);
                } else {
                    Integer colXValue = colX - span.intValue();
                    chart.set("x", colXValue);
                }
                chart.set("y", colY);
            }
            // 行内最高值
            highestValueInLine = highestValueInLine < height ? height : highestValueInLine;
        }
    }

    /**
     * line
     * stackArea
     * bar
     * singleStat
     * pie
     * table
     * alertList
     * text
     * url
     * diagram
     * logs
     * group
     *
     * @param chartListForPanel
     * @param elementGroupingByChartIdMap
     */
    private void handleChartParamFormat(List<Entity> chartListForPanel, Map<String, List<Entity>> elementGroupingByChartIdMap) {
        Map<String, Object> enableMap = Tool.MapUtil.newHashMap();
        enableMap.put("thresholds", true);
        enableMap.put("valueMapping", true);
        enableMap.put("legend", true);

        for (Entity visualChart : chartListForPanel) {
            try {
                // type
                String type = visualChart.getStr("type");

                String paramStr = visualChart.getStr("param");
                paramStr = Tool.StrUtil.emptyToDefault(paramStr, Tool.StrUtil.EMPTY_JSON);
                visualChart.set("param", paramStr);

                Map oldParamMap = JSONObject.parseObject(paramStr, Map.class);

                String thresholdStrVal = Tool.ObjectUtil.defaultIfNull(oldParamMap.get("threshold"), "").toString();
                String statisticsStrVal = Tool.ObjectUtil.defaultIfNull(oldParamMap.get("statistics"), "").toString();

                Map<String, Object> params = Tool.MapUtil.newHashMap();
                // thresholds
                List<Map> thresholds = new ArrayList<>();
                // valueMapping
                List<Map> valueMappingList = new ArrayList<>();
                switch (type) {
                    // chart-timeSeries  line/area
                    case "line":
                    case "stackArea": {
                        // type value 变更
                        if (Tool.StrUtil.equalsIgnoreCase(type, "stackArea")) {
                            visualChart.set("type", "area");
                            // area 类型图表 stack 默认值 为 true
                            params.put("stack", true);
                        } else {
                            // line 类型图表 stack 默认值 为 false
                            params.put("stack", false);
                        }
                        List<String> legendOnValueList;
                        if (oldParamMap.get("legendValue") == null) {
                            legendOnValueList = Collections.emptyList();
                        } else {
                            Map<String, String> legendValueMap = JSONObject.parseObject(oldParamMap.get("legendValue").toString(), Map.class);
                            legendOnValueList = legendValueMap.entrySet().stream().filter(entry -> Tool.StrUtil.equals(entry.getValue(), "on")).map(entry -> entry.getKey()).collect(Collectors.toList());
                        }

                        params.put("style", visualChart.getStr("type"));
                        // legend
                        Map legend = Tool.MapUtil.newHashMap();
                        legend.put("placement", "bottom");
                        legend.put("values", legendOnValueList);
                        params.put("legend", legend);

                        if (Tool.StrUtil.isNotBlank(thresholdStrVal)) {
                            Map tempThresholdFirstMap = Tool.MapUtil.newHashMap();
                            tempThresholdFirstMap.put("color", "#d64f40");
                            tempThresholdFirstMap.put("value", thresholdStrVal);
                            thresholds.add(tempThresholdFirstMap);
                        }
                        params.put("thresholds", thresholds);
                        params.put("nullType", oldParamMap.get("nullType"));
                        params.put("enable", enableMap);
                        visualChart.set("datasource", Constant.ChartDatasourceType.METRICS.getValue());
                        break;
                    }
                    // chart-bar bar
                    case "bar": {
                        // none/null 赋默认值 last
                        params.put("statistics", Tool.StrUtil.equals(statisticsStrVal, "null") || Tool.StrUtil.equals(statisticsStrVal, "") ? "last" : statisticsStrVal);
                        params.put("nullType", oldParamMap.get("nullType"));
                        params.put("display", "value");
                        params.put("valueMapping", valueMappingList);
                        List<String> legendOnValueList;
                        if (oldParamMap.get("legendValue") == null) {
                            legendOnValueList = Collections.emptyList();
                        } else {
                            Map<String, String> legendValueMap = JSONObject.parseObject(oldParamMap.get("legendValue").toString(), Map.class);
                            legendOnValueList = legendValueMap.entrySet().stream().filter(entry -> Tool.StrUtil.equals(entry.getValue(), "on")).map(entry -> entry.getKey()).collect(Collectors.toList());
                        }
                        // legend
                        Map legend = Tool.MapUtil.newHashMap();
                        legend.put("placement", "bottom");
                        legend.put("values", legendOnValueList);
                        params.put("legend", legend);
                        params.put("enable", enableMap);
                        visualChart.set("datasource", Constant.ChartDatasourceType.METRICS.getValue());
                        break;
                    }
                    // chart-value stat
                    case "singleStat": {
                        visualChart.set("type", "stat");
                        // none/null 赋默认值 last
                        params.put("statistics", Tool.StrUtil.equals(statisticsStrVal, "null") || Tool.StrUtil.equals(statisticsStrVal, "") ? "last" : statisticsStrVal);
                        params.put("nullType", oldParamMap.get("nullType"));
                        if (oldParamMap.get("valueMapping") != null) {
                            Map valueMappingMap = JSONObject.parseObject(oldParamMap.get("valueMapping").toString(), Map.class);
                            if (Tool.CollectionUtil.isNotEmpty(valueMappingMap)) {
                                String valueMappingMapType = Tool.ObjectUtil.defaultIfNull(valueMappingMap.get("type"), "").toString();
                                List<Map> mappingList = JSONObject.parseArray(valueMappingMap.get("mapping").toString(), Map.class);
                                for (Map map : mappingList) {
                                    Map tempValueMappingMap = Tool.MapUtil.newHashMap();
                                    tempValueMappingMap.put("type", valueMappingMapType);
                                    tempValueMappingMap.putAll(map);
                                    valueMappingList.add(tempValueMappingMap);
                                }
                            }
                        }
                        params.put("valueMapping", valueMappingList);
                        params.put("enable", enableMap);
                        visualChart.set("datasource", Constant.ChartDatasourceType.METRICS.getValue());
                        break;
                    }
                    // chart-pie pie
                    case "pie": {
                        // none/null 赋默认值 last
                        params.put("statistics", Tool.StrUtil.equals(statisticsStrVal, "null") || Tool.StrUtil.equals(statisticsStrVal, "") ? "last" : statisticsStrVal);
                        params.put("nullType", oldParamMap.get("nullType"));
                        params.put("display", "value");
                        params.put("valueMapping", valueMappingList);
                        params.put("enable", enableMap);
                        visualChart.set("datasource", Constant.ChartDatasourceType.METRICS.getValue());
                        break;
                    }
                    // chart-table table
                    case "table": {
                        // none/null 赋默认值 last
                        params.put("statistics", Tool.StrUtil.equals(statisticsStrVal, "null") || Tool.StrUtil.equals(statisticsStrVal, "") ? "last" : statisticsStrVal);
                        params.put("nullType", oldParamMap.get("nullType"));
                        params.put("enable", enableMap);
                        params.put("indexs", "");

                        List<Entity> visualChartElements = elementGroupingByChartIdMap.get(visualChart.getStr("id"));
                        String elementDisplay = "", valueDisplay = "";
                        for (Entity element : visualChartElements) {
                            elementDisplay += "{{" + element.getStr("name") + ".$legend}}";
                            valueDisplay += "{{" + element.getStr("name") + ".$value}}";
                        }

                        // columns
                        List<Map> columns = new ArrayList<>();
                        Map elementMap = Tool.MapUtil.newHashMap();
                        elementMap.put("title", "Element");
                        elementMap.put("show", true);
                        elementMap.put("unit", 2);
                        elementMap.put("display", elementDisplay);
                        columns.add(elementMap);
                        Map valueMap = Tool.MapUtil.newHashMap();
                        valueMap.put("title", "Value");
                        valueMap.put("show", true);
                        valueMap.put("unit", 2);
                        valueMap.put("display", valueDisplay);
                        columns.add(valueMap);

                        params.put("columns", columns);

                        // valueMapping
                        if (oldParamMap.get("valueMapping") != null) {
                            Map valueMappingMap = JSONObject.parseObject(oldParamMap.get("valueMapping").toString(), Map.class);
                            if (Tool.CollectionUtil.isNotEmpty(valueMappingMap)) {
                                String valueMappingMapType = Tool.ObjectUtil.defaultIfNull(valueMappingMap.get("type"), "").toString();
                                List<Map> mappingList = JSONObject.parseArray(valueMappingMap.get("mapping").toString(), Map.class);
                                for (Map map : mappingList) {
                                    Map tempValueMappingMap = Tool.MapUtil.newHashMap();
                                    tempValueMappingMap.put("type", valueMappingMapType);
                                    tempValueMappingMap.put("columns", "Value");
                                    tempValueMappingMap.putAll(map);
                                    valueMappingList.add(tempValueMappingMap);
                                }
                            }
                        }
                        params.put("valueMapping", valueMappingList);
                        visualChart.set("datasource", Constant.ChartDatasourceType.METRICS.getValue());
                        break;
                    }
                    case "group": {
                        String collapseStr = Tool.ObjectUtil.defaultIfNull(oldParamMap.get("collapse"), "false").toString();
                        params.put("collapse", Tool.StrUtil.equals(collapseStr, "true") ? true : false);
                        visualChart.set("datasource", Constant.ChartDatasourceType.MISC.getValue());
                        break;
                    }
                    case "text":
                    case "url":
                    case "diagram": {
                        visualChart.set("datasource", Constant.ChartDatasourceType.MISC.getValue());
                        break;
                    }
                    case "logs": {
                        visualChart.set("type", "log");
                        visualChart.set("datasource", Constant.ChartDatasourceType.LOGS.getValue());
                        break;
                    }
                    case "assetInfo":
                    case "endpointInfo": {
                        visualChart.set("datasource", Constant.ChartDatasourceType.SYSTEM.getValue());
                        break;
                    }
                    default: {
                        break;
                    }
                }
                // params 处理过 重新赋值
                if (Tool.CollectionUtil.isNotEmpty(params)) {
                    visualChart.set("param", JSONObject.toJSONString(params));
                }
            } catch (Exception e) {
                log.error(e, "[handleChartParamFormat] [Data format processing error] [backUp data: {}]", JSONUtil.toJsonStr(visualChart));
                throw new NZException(RCode.ERROR);
            }
        }
    }
}
