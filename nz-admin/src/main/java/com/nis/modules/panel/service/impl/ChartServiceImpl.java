package com.nis.modules.panel.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.dao.AssetDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.panel.dao.ChartDao;
import com.nis.modules.panel.entity.Chart;
import com.nis.modules.panel.entity.ChartElement;
import com.nis.modules.panel.entity.Node;
import com.nis.modules.panel.entity.Panel;
import com.nis.modules.panel.service.ChartElementService;
import com.nis.modules.panel.service.ChartService;
import com.nis.modules.panel.service.NodeService;
import com.nis.modules.panel.service.PanelService;
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChartServiceImpl extends ServiceImpl<ChartDao, Chart> implements ChartService {
    @Autowired
    private ChartElementService chartElementService;

    @Autowired
    private SysDictService sysDictService;

    @Autowired
    private PanelService panelService;

    @Autowired
    private AssetDao assetDao;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private ChartDao chartDao;

    @Override
    public List<Chart> queryCharts(Integer panelId, Integer id, String title) {
        // 1. 根据条件查询 charts
        List<Chart> charts = chartDao.queryCharts(panelId, id, title);
        if (CollectionUtils.isEmpty(charts)) {
            return charts;
        }

        Map<Integer, List<ChartElement>> idAndElementsMap = charts.stream().collect(Collectors.toMap(Chart::getId, Chart::getElements));
        Set<Integer> idList = idAndElementsMap.keySet();

        // 2. 查询该 panelId 下所有的 charts
        List<Chart> allCharts = this.list(new QueryWrapper<Chart>().lambda().eq(Chart::getPanelId, panelId));
        // 排序
        allCharts = this.getSequentialChartList(allCharts);

        // 3. 以有序的集合为基准 将 查到的 charts 排序返回前端
        Iterator<Chart> iterator = allCharts.iterator();
        Chart chart;
        String param;
        while (iterator.hasNext()) {
            chart = iterator.next();
            if (!idList.contains(chart.getId())) {
                iterator.remove();
                continue;
            }
            // 4. param 格式处理
            param = (String) chart.getParam();
            if (StringUtils.isNotEmpty(param)) {
                chart.setParam(JSONObject.parseObject(param, Map.class));
            }
            chart.setElements(idAndElementsMap.get(chart.getId()));
        }
        return allCharts;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveChart(Integer panelId, Chart chart) {
        // 1. 新增 chart 自动追加至末尾
        chart.setNext(Constant.END_NEXT_VALUE);
        chart.setCreateAt(DateUtil.getUTCTimeByConfigTimeZone());
        chart.setPanelId(panelId);
        Object param = chart.getParam();
        if (param != null && param instanceof HashMap) {
            chart.setParam(JSONObject.toJSONString(param));
        }

        // 2. 判断当前 panel 下是否已存在 charts
        List<Chart> list = this.list(new QueryWrapper<Chart>().lambda().eq(Chart::getPanelId, chart.getPanelId()));
        if (CollectionUtils.isEmpty(list)) {
            // 不存在 charts
            chart.setPrev(Constant.START_PREV_VALUE);
            this.save(chart);
        } else {
            // 已存在 charts 再次新增时自动排到末尾
            Chart endChart = this.getOne(new QueryWrapper<Chart>().lambda().eq(Chart::getNext, Constant.END_NEXT_VALUE).eq(Chart::getPanelId, chart.getPanelId()));
            chart.setPrev(endChart.getId());
            this.save(chart);

            // 更新原来最后图表顺序
            endChart.setNext(chart.getId());
            this.updateById(endChart);
        }

        // 3. 保存 chart elements信息
        chartElementService.saveOrUpdateElements(chart.getId(), chart.getElements());

        // 4. 同步新增处理
        Integer sync = chart.getSync();
        if (sync != null) {
            Panel panel = panelService.getById(chart.getPanelId());
            // 当前 chart 所属 panel 为 model 类型并且 同步标识 为 1 时 ，对已通过对应 model panel 创建的 asset panel 追加新增的 chart
            if (Constant.PanelType.MODEL.getValue().equals(panel.getType()) && sync.equals(1)) {
                this.saveAssetChartsSimultaneously(panel, chart);
            }
        }
    }

    /**
     * 新增 model 时对已通过对应 model panel 创建的 asset panel 追加新增的 chart
     *
     * @param panel
     * @param chart
     */
    private void saveAssetChartsSimultaneously(Panel panel, Chart chart) {
        // 1. 通过 panel  linke 的 modelId 查找该 model 下是否关联 asset
        List<Asset> assets = assetDao.selectList(new QueryWrapper<Asset>().lambda().eq(Asset::getModelId, panel.getLink()));
        if (CollectionUtils.isEmpty(assets)) {
            return;
        }

        // 2. 通过 assets 找到关联的所有 asset panel
        List<Integer> assetsIds = assets.stream().map(Asset::getId).collect(Collectors.toList());
        List<Panel> panels = panelService.list(new QueryWrapper<Panel>().lambda().eq(Panel::getType, Constant.PanelType.ASSET.getValue()).in(Panel::getLink, assetsIds));

        Map<Integer, Asset> assetIdAndEntityMap = new HashMap<>();
        for (Asset asset : assets) {
            assetIdAndEntityMap.put(asset.getId(), asset);
        }

        // 3. 得到 panelId 和 关联的 asset 实体 ，准备做 变量替换所需
        Map<Integer, Asset> panelIdAndAsset = new HashMap<>();
        for (Panel entity : panels) {
            panelIdAndAsset.put(entity.getId(), assetIdAndEntityMap.get(entity.getLink()));
        }

        // 4. 处理属性
        Chart targetChart = new Chart();
        Asset asset;
        for (Map.Entry<Integer, Asset> entry : panelIdAndAsset.entrySet()) {
            asset = entry.getValue();
            targetChart = JSONObject.parseObject(JSON.toJSONString(chart), Chart.class);
            targetChart.setSync(null);
            targetChart.setId(null);
            targetChart.setPid(chart.getId());

            // chart ： title param   chart element : expression   属性需要做 asset 变量替换
            targetChart.setTitle(this.replacAssetInfos(targetChart.getTitle(), asset));
            targetChart.setParam(ToolUtil.isNotEmpty(targetChart.getParam()) ? this.replacAssetInfos(targetChart.getParam().toString(), asset) : null);

            if (CollectionUtils.isNotEmpty(targetChart.getElements())) {
                for (ChartElement element : targetChart.getElements()) {
                    element.setId(null);
                    element.setExpression(ToolUtil.isNotEmpty(element.getExpression()) ? this.replacAssetInfos(element.getExpression(), asset) : null);
                }
            }

            // 5. 调用保存方法进行 新增
            this.saveChart(entry.getKey(), targetChart);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChart(Integer panelId, Chart chart) {
        // 1. 校验
        this.validateChart(panelId, chart);

        chart.setPanelId(panelId);
        Object param = chart.getParam();
        if (param != null) {
            chart.setParam(JSONObject.toJSONString(param));
        }
        Integer prev = chart.getPrev();
        Integer next = chart.getNext();

        // 2. 修改位置 调用 移动位置接口 （原则上不会通过修改接口改变图表顺序）
        if (prev != null && next != null) {
            this.modify(chart.getPanelId(), chart);
        }
        this.updateById(chart);

        // 3. 修改 chart elements
        chartElementService.saveOrUpdateElements(chart.getId(), chart.getElements());

        // 4. 同步修改处理
        Integer sync = chart.getSync();
        if (sync != null) {
            Panel panel = panelService.getById(chart.getPanelId());
            // 当前 chart 所属 panel 为 model 类型并且 同步标识 为 1 时 ，对根据 chart pid 查找通过此 chart 创建的 chart 更新
            if (Constant.PanelType.MODEL.getValue().equals(panel.getType()) && sync.equals(1)) {
                this.updateAssetChartsSimultaneously(chart);
            }
        }
    }

    /**
     * 对根据 chart pid 查找通过此 chart 创建的 chart 进行更新
     *
     * @param chart
     */
    private void updateAssetChartsSimultaneously(Chart chart) {
        // 1. 查找引用的 charts
        List<Chart> charts = this.list(new QueryWrapper<Chart>().lambda().eq(Chart::getPid, chart.getId()));
        if (CollectionUtils.isEmpty(charts)) {
            return;
        }

        List<Integer> idList = charts.stream().map(Chart::getId).collect(Collectors.toList());

        // 2. 先删除之前的 elements  使用当前 chart element 进行更新
        chartElementService.delElementByChartIds(idList);

        // 3. 返回引用 chartId 和 对应的 asset 信息 ，准备做 变量替换所需
        List<Chart> chartIdWithAssetEntity = assetDao.getAssetsByChartIds(idList);
        Map<Integer, Asset> chartIdAndAssetEntityMap = chartIdWithAssetEntity.stream().collect(Collectors.toMap(Chart::getId, Chart::getAsset));

        // 4. 处理更新
        List<ChartElement> elements = chart.getElements();
        List<ChartElement> saveElements = new ArrayList<>();
        ChartElement tempElement;
        Asset asset;
        for (Chart entity : charts) {
            asset = chartIdAndAssetEntityMap.get(entity.getId());
            entity.setTitle(this.replacAssetInfos(chart.getTitle(), asset));
            entity.setSpan(chart.getSpan());
            entity.setHeight(chart.getHeight());
            entity.setType(chart.getType());
            entity.setParam(ToolUtil.isNotEmpty(chart.getParam()) ? this.replacAssetInfos(chart.getParam().toString(), asset) : null);
            if (CollectionUtils.isNotEmpty(elements)) {
                for (ChartElement chartElement : elements) {
                    tempElement = new ChartElement();
                    BeanUtils.copyProperties(chartElement, tempElement);
                    tempElement.setChartId(entity.getId());
                    tempElement.setId(null);
                    tempElement.setExpression(ToolUtil.isNotEmpty(chartElement.getExpression()) ? this.replacAssetInfos(chartElement.getExpression(), asset) : null);
                    saveElements.add(tempElement);
                }
            }
        }

        this.saveOrUpdateBatch(charts);
        chartElementService.saveBatch(saveElements);
    }

    /**
     * 校验 chart 信息
     * 1. panel 是否存在
     * 2. tyep 有效性校验
     * 3. param 校验
     *
     * @param panelId
     * @param chart
     */
    @Override
    public void validateChart(Integer panelId, Chart chart) {
        // 1. panel 是否存在
        this.validatePanel(panelId);

        String type = chart.getType();
        ValidateUtils.is(chart.getTitle()).notNull(RCode.CHART_TITLE_ISNULL)
                .and(chart.getSpan()).notNull(RCode.CHART_SPAN_ISNULL)
                .and(chart.getHeight()).notNull(RCode.CHART_HEIGHT_ISNULL)
                .and(type).notNull(RCode.CHART_TYPE_ISNULL);

        // 2. 验证type是否在枚举范围内
        sysDictService.checkSysDicValue(Constant.DICT_CHART_TYPE, type, RCode.CHART_TYPE_INVALIDE);

        // 3. 根据 type 校验 各项参数
        Map param = (Map) chart.getParam();
        List<ChartElement> elements = chart.getElements();
        switch (type) {
            // url类型
            case Constant.CHART_URL_TYPE: {
                ValidateUtils.is(param).notNull(RCode.CHART_URLPARAM_ISNULL);
                Object url = param.get(Constant.CHART_URL_TYPE);
                if (ToolUtil.isEmpty(url)) {
                    throw new NZException(RCode.CHART_PARAMURL_ISNULL);
                }

                String urlStr = url.toString();
                // 如果是model panel添加charts url类型支持{{}}替换
                Panel panel = panelService.getById(panelId);
                if (Constant.PanelType.MODEL.getValue().equals(panel.getType())) {
                    // 替换全部变量 替换为 a（任意字符） 不影响原来的数据格式 然后进行校验
                    urlStr = urlStr.replaceAll("\\{\\{asset\\..*?\\}\\}", "a");

                    boolean b = CommonUtils.checkUrlInModel(urlStr);
                    boolean b1 = CommonUtils.checkUrl(urlStr);

                    if (!b && !b1) {
                        throw new NZException(RCode.CHART_PARAMURL_FORMAT);
                    }
                } else {
                    if (!CommonUtils.checkUrl(urlStr)) {
                        throw new NZException(RCode.CHART_PARAMURL_FORMAT);
                    }
                }
                break;
            }
            // singleStat 类型
            case Constant.CHART_SINGLESTAT_TYPE: {
                ValidateUtils.is(param).notNull(RCode.CHART_SINGLESTATPARAM_ISNULL)
                        .and(elements).notNull(RCode.CHART_ELEMENT_ISNULL);

                Object statistics = param.get(Constant.CHART_SINGLESTAT_PARAMkEY);
                if (ToolUtil.isEmpty(statistics)) {
                    throw new NZException(RCode.CHART_PARAMSINGLESTAT_ISNULL);
                }
                if (!Constant.CHART_SINGLESTAT_STATISTICS.contains(statistics.toString())) {
                    throw new NZException(RCode.CHART_PARAMSINGLESTAT_INVALIDE);
                }

                chartElementService.validateElements(elements);
                break;
            }
            // alertList 类型不需要校验 elements
            case Constant.CHART_ALERTLIST_TYPE:{
                break;
            }
            default: {
                // 除 url alertList 类型外，其他类型都需校验 elements
                ValidateUtils.is(elements).notNull(RCode.CHART_ELEMENT_ISNULL);
                chartElementService.validateElements(elements);
            }
        }
    }

    /**
     * 校验 chart 是否存在该 panel 下
     *
     * @param panelId
     * @param chartId
     */
    private void validateChartExists(Integer panelId, Integer chartId) {
        Chart chart = this.getOne(new QueryWrapper<Chart>().lambda().eq(Chart::getPanelId, panelId).eq(Chart::getId, chartId));

        if (chart == null) {
            throw new NZException(RCode.CHART_NOTEXSITS);
        }
    }

    /**
     * 校验 panel 是否存在
     *
     * @param panelId
     */
    public void validatePanel(Integer panelId) {
        Panel panel = panelService.getById(panelId);

        if (panel == null) {
            throw new NZException(RCode.CHART_PANEL_ISNULL);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delCharts(Integer panelId, Integer[] ids, Integer sync) {
        if (ToolUtil.isEmpty(ids)) {
            return;
        }

        // 校验 panel 是否存在
        this.validatePanel(panelId);

        // 内置chart不能删除
        this.validateBuildInCharts(Arrays.asList(ids));

        List<Chart> chartList = this.list(new QueryWrapper<Chart>().lambda().eq(Chart::getPanelId, panelId));

        if (CollectionUtils.isEmpty(chartList)) {
            return;
        }

        // 排序
        chartList = this.getSequentialChartList(chartList);
        List<Integer> chartIdList = chartList.stream().map(Chart::getId).collect(Collectors.toList());

        // 只允许删除当前 panel 下的 charts
        List<Integer> deleteIds = new ArrayList<>();
        for (Integer id : ids) {
            if (chartIdList.contains(id)) {
                deleteIds.add(id);
            }
        }

        if (CollectionUtils.isNotEmpty(deleteIds)) {
            if (sync != null) {
                // 同步删除
                Panel panel = panelService.getById(panelId);
                if (Constant.PanelType.MODEL.getValue().equals(panel.getType()) && sync.equals(1)) {
                    this.syncDeleteAssetsCharts(deleteIds);
                }
            }

            // 删除自身
            this.baseMapper.deleteByIds(panelId, deleteIds);

            // 删除char element
            chartElementService.delElementByChartIds(deleteIds);

            // 处理删除之后的位置问题
            this.handlePositionAfterDelete(chartList, deleteIds);
        }
    }

    /**
     * 处理删除之后的位置信息
     *
     * @param sequentialChartList 有序集合列表
     * @param deleteIds           删除的charts ids
     */
    private void handlePositionAfterDelete(List<Chart> sequentialChartList, List<Integer> deleteIds) {
        // 全删除了
        if (sequentialChartList.size() == deleteIds.size()) {
            return;
        }

        // 保证原先次序 并且 删除之后剩余的集合
        List<Chart> deletedChartList = new ArrayList<>();
        for (Chart obj : sequentialChartList) {
            if (!deleteIds.contains(obj.getId())) {
                deletedChartList.add(obj);
            }
        }

        Chart firstChart = deletedChartList.get(0);
        firstChart.setPrev(Constant.START_PREV_VALUE);

        // 剩余一个
        if (deletedChartList.size() == 1) {
            firstChart.setNext(Constant.END_NEXT_VALUE);
            chartDao.updatePrevNextById(firstChart);
        } else {
            // 剩余数量大于 1
            List<Chart> updateCharts = new ArrayList<>();

            Chart tempChart = null;
            int size = deletedChartList.size();
            for (int i = 0; i < size; i++) {
                if (i == 0) {
                    firstChart.setNext(deletedChartList.get(i + 1).getId());
                    updateCharts.add(firstChart);
                } else {
                    tempChart = deletedChartList.get(i);
                    tempChart.setPrev(updateCharts.get(updateCharts.size() - 1).getId());
                    if (i < size - 1) {
                        tempChart.setNext(deletedChartList.get(i + 1).getId());
                    }
                    updateCharts.add(tempChart);
                }
            }
            updateCharts.get(updateCharts.size() - 1).setNext(Constant.END_NEXT_VALUE);

            // 批量修改前后值
            chartDao.updatePrevNextBatch(updateCharts);
        }
    }

    /**
     * 根据 chart.pid 查找引用自此chart，同步删除
     *
     * @param deleteIds
     */
    private void syncDeleteAssetsCharts(List<Integer> deleteIds) {
        // 关联 charts
        List<Chart> list = this.list(new QueryWrapper<Chart>().lambda().in(Chart::getPid, deleteIds));
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        Map<Integer, List<Chart>> listMap = list.stream().collect(Collectors.groupingBy(Chart::getPanelId));

        // 需要同步删除的 panel 关联的 charts
        List<Chart> chartsByPanel = this.list(new QueryWrapper<Chart>().lambda().in(Chart::getPanelId, listMap.keySet()));
        Map<Integer, List<Chart>> chartsByPanelMap = chartsByPanel.stream().collect(Collectors.groupingBy(Chart::getPanelId));

        List<Integer> allDeleteChartIds = listMap.values().stream().flatMap(Collection::stream).map(Chart::getId).collect(Collectors.toList());
        // 删除自身
        this.removeByIds(allDeleteChartIds);
        // 删除关联的 chart Elements
        chartElementService.delElementByChartIds(allDeleteChartIds);

        Integer panid;
        List<Chart> sequentialChartList;
        List<Integer> delIds;
        for (Map.Entry<Integer, List<Chart>> entry : chartsByPanelMap.entrySet()) {
            panid = entry.getKey();
            sequentialChartList = this.getSequentialChartList(entry.getValue());
            delIds = listMap.get(panid).stream().map(Chart::getId).collect(Collectors.toList());
            // 处理删除后的位置信息
            this.handlePositionAfterDelete(sequentialChartList, delIds);
        }
    }

    /**
     * charts 排序
     *
     * @param charts
     * @return
     */
    private List<Chart> getSequentialChartList(List<Chart> charts) {
        if (CollectionUtils.isEmpty(charts)) {
            return new ArrayList();
        }

        List<Chart> resultList = new ArrayList<>();

        // 按照prev升序排序
        charts = charts.stream().sorted(Comparator.comparing(Chart::getPrev)).collect(Collectors.toList());

        Map<Integer, Chart> idAndChartMap = new HashMap<>();
        for (Chart chart1 : charts) {
            idAndChartMap.put(chart1.getId(), chart1);
        }

        Chart firstChart = charts.get(0);

        Chart tempChar = firstChart;
        resultList.add(firstChart);
        Chart nextChart = null;

        while (true) {
            nextChart = idAndChartMap.get(tempChar.getNext());
            if (nextChart != null) {
                resultList.add(nextChart);
                tempChar = nextChart;
            } else {
                break;
            }
        }
        return resultList;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delChartsByPanelId(Integer... panelIds) {
        if (ToolUtil.isEmpty(panelIds)) {
            return;
        }

        // 1. 再次通过 panelid 查询 是为了只删除当前 panel 下的 charts
        List<Chart> charts = this.list(new QueryWrapper<Chart>().lambda().in(Chart::getPanelId, panelIds));
        List<Integer> chartIds = charts.stream().map(Chart::getId).collect(Collectors.toList());

        // 2. 内置chart不能删除
        this.validateBuildInCharts(chartIds);

        // 3. 不为空 则删除自身  同时删除关联的 element
        if (CollectionUtils.isNotEmpty(chartIds)) {
            this.removeByIds(chartIds);
            chartElementService.delElementByChartIds(chartIds);
        }
    }


    /**
     * cahrts 调整位置接口
     *
     * @param panelId
     * @param chart
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modify(Integer panelId, Chart chart) {
        // 校验 panel 是否存在
        this.validatePanel(panelId);

        // 取出改panel下所有的charts
        List<Chart> chartList = this.list(new QueryWrapper<Chart>().lambda().eq(Chart::getPanelId, panelId).orderByAsc(Chart::getPrev));

        Map<Integer, Node> idAndChartMap = new HashMap<>();
        for (Chart chart1 : chartList) {
            idAndChartMap.put(chart1.getId(), chart1);
        }

        Node oldEntity = idAndChartMap.get(chart.getId());
        // chart不存在于当前panel
        if (oldEntity == null) {
            throw new NZException(RCode.CHART_NOTEXSITS);
        }

        // 就只有一个图表 不让其调整位置
        if (chartList.size() == 1) {
            chart.setPrev(Constant.START_PREV_VALUE);
            chart.setNext(Constant.END_NEXT_VALUE);
            chartDao.updatePrevNextById(chart);
            return;
        }

        // 校验 prev next 参数是否合法
        if (nodeService.validatePrevAndNext(idAndChartMap, chart)) {
            // 没有改变位置 直接修改库中 span height属性
            chartDao.updatePrevNextById(chart);
        }

        List<Node> updateCharts = nodeService.modify(idAndChartMap, chart, oldEntity);
        // 更新库中数据
        if (CollectionUtils.isNotEmpty(updateCharts)) {
            Set<Chart> charts = new HashSet<>();
            for (Node updatChart : updateCharts) {
                charts.add((Chart) updatChart);
            }
            chartDao.updatePrevNextBatch(new ArrayList<>(charts));
        }
    }

    /**
     * 校验是否是内置chart
     *
     * @param chartIds
     */
    private void validateBuildInCharts(List<Integer> chartIds) {
        // 内置charts
        List<Chart> buildInCharts = this.list(new QueryWrapper<Chart>().lambda().eq(Chart::getBuildIn, 1));
        Map<Integer, String> buildInChartIdAndTitleMap = buildInCharts.stream().collect(Collectors.toMap(Chart::getId, Chart::getTitle));

        chartIds.forEach(id -> {
            if (ToolUtil.isNotEmpty(buildInChartIdAndTitleMap.get(id))) {
                throw new NZException(buildInChartIdAndTitleMap.get(id) + " chart is built-in and cannot be deleted", RCode.CHART_BUILDIN_CAN_NOT_REMOVE.getCode());
            }
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Chart duplicate(Integer panelId, Integer chartId) {
        // 校验 panel 是否存在
        this.validatePanel(panelId);

        // 图表复制
        Chart source = this.getById(chartId);
        Chart target = new Chart();
        BeanUtils.copyProperties(source, target);

        if (Constant.END_NEXT_VALUE.equals(source.getNext())) {
            //位于图表中最后一位时
            target.setPrev(source.getId());
            target.setNext(source.getNext());
            this.save(target);
            source.setNext(target.getId());
            this.updateById(source);
        } else {
            target.setPrev(source.getId());
            target.setNext(source.getNext());
            this.save(target);

            List<Chart> charts = new ArrayList<Chart>();
            Chart threeChart = this.getById(target.getNext());
            threeChart.setPrev(target.getId());
            source.setNext(target.getId());
            charts.add(threeChart);
            charts.add(source);
            this.updateBatchById(charts);
        }
        // 保存图表的element配置
        QueryWrapper<ChartElement> chartElementQueryWrapper = new QueryWrapper<>();
        chartElementQueryWrapper.lambda().eq(ChartElement::getChartId, chartId);
        List<ChartElement> list = chartElementService.list(chartElementQueryWrapper);
        list.forEach(chartElement -> {
            chartElement.setChartId(target.getId());
        });
        chartElementService.saveBatch(list);
        target.setElements(list);
        String param = (String) target.getParam();
        if (StringUtils.isNotEmpty(param)) {
            target.setParam(JSONObject.parseObject(param, Map.class));
        }
        return target;
    }

    @Override
    public void saveAssetPanelAndChart(Asset asset) {
        // 当前型号关联的panel对象
        Panel modelPanel = panelService.getOne(new QueryWrapper<Panel>().lambda().eq(Panel::getType, Constant.PanelType.MODEL.getValue()).eq(Panel::getLink, asset.getModelId()));

        Panel panel = new Panel();
        panel.setType(Constant.PanelType.ASSET.getValue());
        panel.setLink(asset.getId());
        panel.setCreateBy(ShiroUtils.getUserId().intValue());
        panel.setBuildIn(modelPanel.getBuildIn());
        // 固定格式 panel name format is asset_panel_link
        panel.setName("asset_panel_" + asset.getId());
        panel.setPrev(Constant.START_PREV_VALUE);
        panel.setNext(Constant.END_NEXT_VALUE);
        panelService.save(panel);

        // 复制一份charts
        this.copyChartsToAssetChartsByModelPanel(modelPanel, panel, asset);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAssetChartsByNewModel(Asset asset) {
        List<Panel> panels = panelService.list();

        // 修改的Model 关联的panel
        Panel modelPanel = panels.stream().filter(panel -> Constant.PanelType.MODEL.getValue().equals(panel.getType()) && panel.getLink().equals(asset.getModelId()) ? true : false).findAny().orElse(null);
        Panel assetPanel = panels.stream().filter(panel -> Constant.PanelType.ASSET.getValue().equals(panel.getType()) && panel.getLink().equals(asset.getId()) ? true : false).findAny().orElse(null);

        List<Chart> list = this.list(new QueryWrapper<Chart>().lambda().eq(Chart::getPanelId, assetPanel.getId()));
        List<Integer> delIds = list.stream().filter(chart -> chart.getPid() != null ? true : false).map(Chart::getId).collect(Collectors.toList());
        // 删除之前model关联的图表
        Integer[] ids = new Integer[delIds.size()];
        delIds.toArray(ids);

        // 删除旧model关联的charts
        this.delCharts(assetPanel.getId(), ids, null);
        // 复制一份charts
        this.copyChartsToAssetChartsByModelPanel(modelPanel, assetPanel, asset);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAssetChartsThroughRelatedModel(Asset asset) {
        List<Panel> panels = panelService.list();

        Panel modelPanel = panels.stream().filter(panel -> Constant.PanelType.MODEL.getValue().equals(panel.getType()) && panel.getLink().equals(asset.getModelId()) ? true : false).findAny().orElse(null);
        Panel assetPanel = panels.stream().filter(panel -> Constant.PanelType.ASSET.getValue().equals(panel.getType()) && panel.getLink().equals(asset.getId()) ? true : false).findAny().orElse(null);

        // 删除 assetPanel 下的 charts信息
        this.delChartsByPanelId(assetPanel.getId());

        // 新增 charts
        this.copyChartsToAssetChartsByModelPanel(modelPanel, assetPanel, asset);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateuAllAssetChartsUnderCurrentModel(Integer modelId, List<Asset> assets) {
        List<Panel> panels = panelService.list();

        Panel modelPanel = panels.stream().filter(panel -> Constant.PanelType.MODEL.getValue().equals(panel.getType()) && panel.getLink().equals(modelId) ? true : false).findAny().orElse(null);

        Map<Asset, Panel> assetAndPanelMap = new HashMap<>();
        Panel tempPanel;
        for (Asset asset : assets) {
            tempPanel = panels.stream().filter(panel -> Constant.PanelType.ASSET.getValue().equals(panel.getType()) && panel.getLink().equals(asset.getId()) ? true : false).findAny().orElse(null);
            assetAndPanelMap.put(asset, tempPanel);
        }

        // 删除 新增
        Panel assetPanel;
        for (Map.Entry<Asset, Panel> entry : assetAndPanelMap.entrySet()) {
            assetPanel = entry.getValue();
            this.delChartsByPanelId(assetPanel.getId());
            this.copyChartsToAssetChartsByModelPanel(modelPanel, assetPanel, entry.getKey());
        }
    }

    private void copyChartsToAssetChartsByModelPanel(Panel modelPanel, Panel targetPanel, Asset asset) {
        List<Chart> charts = this.list(new QueryWrapper<Chart>().lambda().eq(Chart::getPanelId, modelPanel.getId()));
        // 排序
        charts = this.getSequentialChartList(charts);

        List<Integer> chartIds = charts.stream().map(Chart::getId).collect(Collectors.toList());
        List<ChartElement> chartElements = chartElementService.list(new QueryWrapper<ChartElement>().lambda().in(ChartElement::getChartId, chartIds));
        Map<Integer, List<ChartElement>> chartIdAndElementsMap = chartElements.stream().collect(Collectors.groupingBy(ChartElement::getChartId));


        Date now = DateUtil.getUTCTimeByConfigTimeZone();
        Map<Integer, List<ChartElement>> saveElements = new HashMap<>();
        List<ChartElement> elementList;

        for (Chart chart : charts) {
            elementList = chartIdAndElementsMap.get(chart.getId());

            // 设置关联 panel为当前新增asset关联的panel
            chart.setCreateAt(now);
            chart.setPid(chart.getId());
            chart.setParam(ToolUtil.isNotEmpty(chart.getParam()) ? this.replacAssetInfos(chart.getParam().toString(), asset) : null);
            chart.setTitle(this.replacAssetInfos(chart.getTitle(), asset));

            // 设置关联 panel 为当前新增 asset 关联的 panel
            this.saveChart(targetPanel.getId(), chart);
            saveElements.put(chart.getId(), elementList == null ? new ArrayList<>() : elementList);
        }

        Integer chartId;
        for (Map.Entry<Integer, List<ChartElement>> entry : saveElements.entrySet()) {
            chartId = entry.getKey();
            for (ChartElement chartElement : entry.getValue()) {
                chartElement.setChartId(chartId);
                chartElement.setExpression(ToolUtil.isNotEmpty(chartElement.getExpression()) ? this.replacAssetInfos(chartElement.getExpression(), asset) : null);
            }
        }
        // 保存 element
        List<ChartElement> elements = saveElements.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        chartElementService.saveBatch(elements);
    }

    /**
     * model 同步更新时 标签替换
     *
     * @param str
     * @param asset
     * @return
     */
    private String replacAssetInfos(String str, Asset asset) {
        str = str.replaceAll("\\{\\{asset.host\\}\\}", asset.getHost());
        str = str.replaceAll("\\{\\{asset.sn\\}\\}", asset.getSn());
        return str;
    }
}