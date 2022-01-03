package com.nis.common.update.v3_0_to_3_2.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.update.v3_0_to_3_2.dao.V3_0VisualChartDao;
import com.nis.common.update.v3_0_to_3_2.service.DataFormatConversionService;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.panel.dao.VisualChartDao;
import com.nis.modules.panel.dao.VisualChartElementDao;
import com.nis.modules.panel.entity.VisualChart;
import com.nis.modules.panel.entity.VisualChartElement;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DataFormatConversionServiceImpl implements DataFormatConversionService {

    private static cn.hutool.log.Log logger = Log.get();

    @Autowired
    private V3_0VisualChartDao v3_0VisualChartDao;

    @Autowired
    private VisualChartDao visualChartDao;

    @Autowired
    private VisualChartElementDao visualChartElementDao;

    static String[] LETTERS = new String[]{
            "A", "B", "C", "D", "E", "F", "G",
            "H", "I", "J", "K", "L", "M", "N",
            "O", "P", "Q", "R", "S", "T",
            "U", "V", "W", "X", "Y", "Z"};

    @Override
    public void visualChartDataFormatConversion() {
        // 1. 检查 visual_chart_V3_2 是否存在
        Map checkTableExists = v3_0VisualChartDao.checkTableExists("visual_chart_v3_2");
        if (Tool.CollectionUtil.isEmpty(checkTableExists)) {
            logger.info("Table visual_chart_v3_2 does not exist. The chart data format processing will not be executed");
            return;
        }
        logger.info("Table visual_chart_v3_2 exists, execute chart data format processing");

        StopWatch sw = new StopWatch();
        sw.start();
        try {
            logger.info("Start processing visual_chart_element data and supplement the element name value.");
            // 2. handle visual_chart_element name value (先处理 element name ,table chart handle 需要使用)
            this.handleElementName();
            logger.info("The visual_chart_element name processing done.");

            // 3. 处理数据变化
            List<VisualChart> oldChartList = v3_0VisualChartDao.queryList();

            // 再查询 element 内容，此时 name 已处理完成
            List<VisualChartElement> visualChartElements = visualChartElementDao.selectList(null);
            Map<Integer, List<VisualChartElement>> chartIdAndElementListMap = visualChartElements.stream().collect(Collectors.groupingBy(VisualChartElement::getChartId));

            logger.info("Start processing visual_chart data format changes.");
            // 排除 alertList ,alertList drop
            Map<Integer, List<VisualChart>> oldChartListGroupByPanelId = oldChartList.stream().filter(visualChart -> !Tool.StrUtil.equals(visualChart.getType(), "alertList")).collect(Collectors.groupingBy(VisualChart::getPanelId));
            for (Map.Entry<Integer, List<VisualChart>> entry : oldChartListGroupByPanelId.entrySet()) {
                Integer panelId = entry.getKey();
                List<VisualChart> oldChartListByPanelId = entry.getValue();

                // 处理 chart param/datasource
                this.handleChartParamFormat(oldChartListByPanelId, chartIdAndElementListMap);

                // 处理 xy 值
                this.handlePosition(panelId, oldChartListByPanelId);

                // 入库 visual_chart (新表)
                visualChartDao.insertBatchChart(oldChartListByPanelId);
            }
            logger.info("The visual_chart data format change processing done.");
            // 4. 删除 visual_chart_v3_2 表
            v3_0VisualChartDao.dropTable("visual_chart_v3_2");
        } catch (Exception e) {
            logger.error("visual_chart 数据转换有误", e);
        } finally {
            sw.stop();
        }
        logger.info("visual_chart data format conversion done Run Time: {}", sw.toString());
    }

    /**
     * chart element name 处理 初始化按照 每个chart数据  A-Z AA-AZ BA-BZ AAA-AAZ 顺序添加
     */
    public void handleElementName() {
        List<VisualChartElement> visualChartElements = visualChartElementDao.selectList(null);
        Map<Integer, List<VisualChartElement>> collect = visualChartElements.stream().collect(Collectors.groupingBy(VisualChartElement::getChartId));
        for (Map.Entry<Integer, List<VisualChartElement>> entry : collect.entrySet()) {
            List<VisualChartElement> elementList = entry.getValue();
            for (int i = 0; i < elementList.size(); i++) {
                String letter = this.transformNumToLetter(i);
                VisualChartElement visualChartElement = elementList.get(i);
                visualChartElement.setName(letter);
            }
        }
        List<VisualChartElement> allElementList = collect.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        // visualChartElementService.saveOrUpdateBatch(allElementList);
        if (Tool.CollectionUtil.isNotEmpty(allElementList)) {
            // visualChartElementDao xml sql 为 updaet xx set name case when ...  where id in xxx. 组合，防止 sql 过长，分割后拼接 sql 修改
            Integer batchSize = 1000;
            Integer limit = (allElementList.size() + batchSize - 1) / batchSize;
            List<List<VisualChartElement>> splitVisualChartElementList = Stream.iterate(0, n -> n + 1).limit(limit).parallel().map(a -> allElementList.stream().skip(a * batchSize).limit(batchSize).parallel().collect(Collectors.toList())).collect(Collectors.toList());
            for (List<VisualChartElement> elements : splitVisualChartElementList) {
                visualChartElementDao.updateNameBatch(elements);
            }
        }
    }

    /**
     * 根据下标 获取对应 26 字母，如果超过 26 字母，按照  A-Z AA-AZ BA-BZ AAA-AAZ 顺序排
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
    private void handlePosition(Integer panelId, List<VisualChart> chartList) {
        chartList = chartList.stream().sorted(Comparator.comparing(VisualChart::getWeight)).collect(Collectors.toList());

        if (panelId == 0) {
            // chart template 列表数据不需要处理 只需处理 group 内的 chart ,其他 chart 都设置为 0，0
            Map<Integer, List<VisualChart>> chartListGroupByGroupId = chartList.stream().collect(Collectors.groupingBy(VisualChart::getGroupId));
            for (Map.Entry<Integer, List<VisualChart>> entry : chartListGroupByGroupId.entrySet()) {
                Integer groupId = entry.getKey();
                List<VisualChart> visualChartList = entry.getValue();
                if (groupId == 0) {
                    for (VisualChart visualChart : visualChartList) {
                        if (Tool.StrUtil.equals(visualChart.getType(), "group")) {
                            visualChart.setHeight(3d);
                        }
                        visualChart.setX(0d);
                        visualChart.setY(0d);
                    }
                } else {
                    // 根据 span and height 计算 XY
                    this.calculateXYAccordingToSpanAndHeight(visualChartList);
                }
            }
        } else {
            // 排序 ，按照 chart - group  - chart 顺序排 （group inner chart） 排除在外
            List<VisualChart> list = new ArrayList<>(chartList.size());
            for (VisualChart visualChart : chartList) {
                if (Tool.StrUtil.equals(visualChart.getType(), "group")) {
                    // 添加 group
                    list.add(visualChart);
                } else if (!Tool.StrUtil.equals(visualChart.getType(), "group") && visualChart.getGroupId() == 0) {
                    // 非 group & 非 group inner chart
                    list.add(visualChart);
                }
            }
            // 根据 span and height 计算 XY
            this.calculateXYAccordingToSpanAndHeight(list);

            Map<Integer, List<VisualChart>> chartListGroupByGroupId = chartList.stream().filter(visualChart -> visualChart.getGroupId() != 0).collect(Collectors.groupingBy(VisualChart::getGroupId));
            for (Map.Entry<Integer, List<VisualChart>> entry : chartListGroupByGroupId.entrySet()) {
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
    private void calculateXYAccordingToSpanAndHeight(List<VisualChart> list) {
        // 每行 x 计数
        Integer colX = 0;
        // 每行 y 计数
        Integer colY = 0;
        // 行内最高值
        Integer highestValueInLine = 0;
        for (int i = 0; i < list.size(); i++) {
            VisualChart chart = list.get(i);
            Integer span = chart.getSpan().intValue();
            // group 默认给 3 就 ok ，前端会根据 组内 chart 自动调整高度
            Integer height = Tool.StrUtil.equals(chart.getType(), "group") ? 3 : chart.getHeight().intValue();
            chart.setHeight(height.doubleValue());
            colX += span;
            // 一行结束
            if (colX > 12) {
                colX = span;
                // 下一行第一个 Y 就是上一行 height 最大值
                colY += highestValueInLine;
                Integer colXValue = colX - span.intValue();
                chart.setX(colXValue.doubleValue());
                chart.setY(colY.doubleValue());
                highestValueInLine = 0;
            } else {
                // group
                if (Tool.StrUtil.equals(chart.getType(), "group")) {
                    chart.setX(0d);
                } else {
                    Integer colXValue = colX - span.intValue();
                    chart.setX(colXValue.doubleValue());
                }
                chart.setY(colY.doubleValue());
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
     * @param chartList
     * @param chartIdAndElementListMap
     */
    private void handleChartParamFormat(List<VisualChart> chartList, Map<Integer, List<VisualChartElement>> chartIdAndElementListMap) {
        Map<String, Object> enableMap = new HashMap();
        enableMap.put("thresholds", true);
        enableMap.put("valueMapping", true);
        enableMap.put("legend", true);

        for (VisualChart visualChart : chartList) {
            try {
                String type = visualChart.getType();
                if (visualChart.getParam() == null) {
                    visualChart.setParam(JSONObject.toJSONString(MapUtil.empty()));
                }
                if (Tool.StrUtil.isEmpty(visualChart.getParam().toString())) {
                    visualChart.setParam(JSONObject.toJSONString(MapUtil.empty()));
                }
                Map oldParamMap = JSONObject.parseObject(visualChart.getParam().toString(), Map.class);

                String thresholdStrVal = Tool.ObjectUtil.defaultIfNull(oldParamMap.get("threshold"), "").toString();
                String statisticsStrVal = Tool.ObjectUtil.defaultIfNull(oldParamMap.get("statistics"), "").toString();

                Map<String, Object> params = new HashMap<>();
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
                            visualChart.setType("area");
                        }
                        List<String> legendOnValueList;
                        if (oldParamMap.get("legendValue") == null) {
                            legendOnValueList = Collections.emptyList();
                        } else {
                            Map<String, String> legendValueMap = JSONObject.parseObject(oldParamMap.get("legendValue").toString(), Map.class);
                            legendOnValueList = legendValueMap.entrySet().stream().filter(entry -> Tool.StrUtil.equals(entry.getValue(), "on")).map(entry -> entry.getKey()).collect(Collectors.toList());
                        }

                        params.put("style", visualChart.getType());
                        params.put("stack", "true");
                        // legend
                        Map legend = new HashMap<>();
                        legend.put("placement", "bottom");
                        legend.put("values", legendOnValueList);
                        params.put("legend", legend);

                        if (Tool.StrUtil.isNotBlank(thresholdStrVal)) {
                            Map tempThresholdFirstMap = new HashMap();
                            tempThresholdFirstMap.put("color", "#d64f40");
                            tempThresholdFirstMap.put("val", thresholdStrVal);
                            thresholds.add(tempThresholdFirstMap);
                        }
                        params.put("thresholds", thresholds);
                        params.put("nullType", oldParamMap.get("nullType"));
                        params.put("enable", enableMap);
                        visualChart.setDatasource(Constant.ChartDatasourceType.METRICS.getValue());
                        break;
                    }
                    // chart-bar bar
                    case "bar": {
                        // none/null 赋默认值 last
                        params.put("statistics", Tool.StrUtil.equals(statisticsStrVal, "null") ? "last" : statisticsStrVal);
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
                        Map legend = new HashMap<>();
                        legend.put("placement", "bottom");
                        legend.put("values", legendOnValueList);
                        params.put("legend", legend);
                        params.put("enable", enableMap);
                        visualChart.setDatasource(Constant.ChartDatasourceType.METRICS.getValue());
                        break;
                    }
                    // chart-value stat
                    case "singleStat": {
                        visualChart.setType("stat");
                        // none/null 赋默认值 last
                        params.put("statistics", Tool.StrUtil.equals(statisticsStrVal, "null") ? "last" : statisticsStrVal);
                        params.put("nullType", oldParamMap.get("nullType"));
                        if (oldParamMap.get("valueMapping") != null) {
                            Map valueMappingMap = JSONObject.parseObject(oldParamMap.get("valueMapping").toString(), Map.class);
                            if (Tool.CollectionUtil.isNotEmpty(valueMappingMap)) {
                                String valueMappingMapType = Tool.ObjectUtil.defaultIfNull(valueMappingMap.get("type"), "").toString();
                                List<Map> mappingList = JSONObject.parseArray(valueMappingMap.get("mapping").toString(), Map.class);
                                for (Map map : mappingList) {
                                    Map tempValueMappingMap = new HashMap();
                                    tempValueMappingMap.put("type", valueMappingMapType);
                                    tempValueMappingMap.putAll(map);
                                    valueMappingList.add(tempValueMappingMap);
                                }
                            }
                        }
                        params.put("valueMapping", valueMappingList);
                        params.put("enable", enableMap);
                        visualChart.setDatasource(Constant.ChartDatasourceType.METRICS.getValue());
                        break;
                    }
                    // chart-pie pie
                    case "pie": {
                        // none/null 赋默认值 last
                        params.put("statistics", Tool.StrUtil.equals(statisticsStrVal, "null") ? "last" : statisticsStrVal);
                        params.put("nullType", oldParamMap.get("nullType"));
                        params.put("display", "value");
                        params.put("valueMapping", valueMappingList);
                        params.put("enable", enableMap);
                        visualChart.setDatasource(Constant.ChartDatasourceType.METRICS.getValue());
                        break;
                    }
                    // chart-table table  TODO
                    case "table": {
                        // none/null 赋默认值 last
                        params.put("statistics", Tool.StrUtil.equals(statisticsStrVal, "null") ? "last" : statisticsStrVal);
                        params.put("nullType", oldParamMap.get("nullType"));
                        params.put("enable", enableMap);
                        params.put("indexs", "");

                        List<VisualChartElement> visualChartElements = chartIdAndElementListMap.get(visualChart.getId());
                        String elementDisplay = "", valueDisplay = "";
                        for (VisualChartElement element : visualChartElements) {
                            elementDisplay += "{{" + element.getName() + ".$legend}}";
                            valueDisplay += "{{" + element.getName() + ".$value}}";
                        }

                        // columns
                        List<Map> columns = new ArrayList<>();
                        Map elementMap = new HashMap();
                        elementMap.put("title", "Element");
                        elementMap.put("show", true);
                        elementMap.put("unit", 2);
                        elementMap.put("display", elementDisplay);
                        columns.add(elementMap);
                        Map valueMap = new HashMap();
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
                                    Map tempValueMappingMap = new HashMap();
                                    tempValueMappingMap.put("type", valueMappingMapType);
                                    tempValueMappingMap.put("columns", "Value");
                                    tempValueMappingMap.putAll(map);
                                    valueMappingList.add(tempValueMappingMap);
                                }
                            }
                        }
                        params.put("valueMapping", valueMappingList);
                        visualChart.setDatasource(Constant.ChartDatasourceType.METRICS.getValue());
                        break;
                    }
                    case "group": {
                        String collapseStr = Tool.ObjectUtil.defaultIfNull(oldParamMap.get("collapse"), "false").toString();
                        params.put("collapse", Tool.StrUtil.equals(collapseStr, "true") ? true : false);
                        visualChart.setDatasource(Constant.ChartDatasourceType.MISC.getValue());
                        break;
                    }
                    case "text":
                    case "url":
                    case "diagram": {
                        visualChart.setDatasource(Constant.ChartDatasourceType.MISC.getValue());
                        break;
                    }
                    case "logs": {
                        visualChart.setType("log");
                        visualChart.setDatasource(Constant.ChartDatasourceType.LOGS.getValue());
                        break;
                    }
                    case "assetInfo":
                    case "endpointInfo": {
                        visualChart.setDatasource(Constant.ChartDatasourceType.SYSTEM.getValue());
                        break;
                    }
                    default: {
                        break;
                    }
                }
                // params 处理过 重新赋值
                if (Tool.CollectionUtil.isNotEmpty(params)) {
                    visualChart.setParam(JSONObject.toJSONString(params));
                }
            } catch (Exception e) {
                logger.error("Chart数据格式处理有误，原始数据：" + JSONObject.toJSONString(visualChart));
                logger.error(e);
            }
        }
    }
}
