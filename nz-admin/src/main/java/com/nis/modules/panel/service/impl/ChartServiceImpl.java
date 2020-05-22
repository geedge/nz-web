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
import com.nis.modules.panel.entity.Panel;
import com.nis.modules.panel.service.ChartElementService;
import com.nis.modules.panel.service.ChartService;
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

    @Override
    public List<Chart> queryCharts(Integer panelId, Integer id, String title) {

        List<Chart> charts = this.baseMapper.queryCharts(panelId);
        if (CollectionUtils.isEmpty(charts)) {
            return charts;
        }

        Map<Integer, Chart> idAndChartMap = new HashMap<>();
        for (Chart chart1 : charts) {
            idAndChartMap.put(chart1.getId(), chart1);
        }

        // 按照顺序排序后返回前端
        List<Chart> resultList = new ArrayList<>();

        // 后台查询时 order by prev
        Chart firstChart = charts.get(0);
        Chart tempChar = firstChart;
        boolean idFlag = (id != null);
        boolean titleFlag = StringUtils.isNotEmpty(title);

        // 如果 id 条件参数不为空
        if (idFlag) {
            List<Chart> list = new ArrayList<>();
            Chart chart1 = null;
            for (Chart obj : charts) {
                if (obj.getId().equals(id)) {
                    chart1 = obj;
                    break;
                }
            }
            // 通过id找到实体
            if (chart1 != null) {
                if (titleFlag) {
                    if (chart1.getTitle().contains(title)) {
                        list.add(chart1);
                    }
                } else {
                    list.add(chart1);
                }
            }
            return list;
        }


        if (titleFlag) {
            if (firstChart.getTitle().contains(title)) {
                resultList.add(firstChart);
            }
        } else {
            resultList.add(firstChart);
        }

        Chart nextChart = null;

        while (true) {
            nextChart = idAndChartMap.get(tempChar.getNext());
            if (nextChart != null) {
                // 模糊查询title
                if (titleFlag) {
                    if (nextChart.getTitle().contains(title)) {
                        resultList.add(nextChart);
                    }
                } else {
                    resultList.add(nextChart);
                }
                tempChar = nextChart;
            } else {
                break;
            }
        }
        String param;
        for (Chart chart : resultList) {
            param = (String) chart.getParam();
            if (StringUtils.isNotEmpty(param)) {
                chart.setParam(JSONObject.parseObject(param, Map.class));
            }
        }
        return resultList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveChart(Chart chart) {

        // 保存chart信息
        chart.setCreateAt(new Date());
        chart.setNext(-1);

        // 判断当前panel下是否存在chart
        List<Chart> list = this.list(new QueryWrapper<Chart>().lambda().eq(Chart::getPanelId, chart.getPanelId()));

        if (CollectionUtils.isEmpty(list)) {
            // 一开始没有内容
            chart.setPrev(0);
            this.save(chart);
        } else {
            // 已存在chart 再次新增时自动排到末尾
            Chart endChart = this.getOne(new QueryWrapper<Chart>().lambda().eq(Chart::getNext, -1).eq(Chart::getPanelId, chart.getPanelId()));
            chart.setPrev(endChart.getId());
            this.save(chart);
            // 更新原来最后图表顺序
            endChart.setNext(chart.getId());
            this.updateById(endChart);
        }

        if (CollectionUtils.isNotEmpty(chart.getElements())) {
            // 保存chart elements信息
            for (ChartElement element : chart.getElements()) {
                element.setChartId(chart.getId());
            }
            chartElementService.saveBatch(chart.getElements());
        }

        // 同步新增处理
        Integer sync = chart.getSync();
        if (sync != null) {
            Panel panel = panelService.getById(chart.getPanelId());
            if ("model".equals(panel.getType()) && sync.equals(1)) {
                List<Asset> assets = assetDao.selectList(new QueryWrapper<Asset>().lambda().eq(Asset::getModelId, panel.getLink()));
                // 当前没有需要同步的asset
                if(CollectionUtils.isEmpty(assets)){
                    return;
                }
                Map<Integer, Asset> assetIdAndEntityMap = new HashMap<>();
                for (Asset asset : assets) {
                    assetIdAndEntityMap.put(asset.getId(), asset);
                }
                List<Integer> assetsIds = assets.stream().map(Asset::getId).collect(Collectors.toList());
                // 得到 asset panel
                List<Panel> panels = panelService.list(new QueryWrapper<Panel>().lambda().eq(Panel::getType, "asset").in(Panel::getLink, assetsIds));

                Map<Integer, Asset> panelIdAndAsset = new HashMap<>();
                for (Panel entity : panels) {
                    panelIdAndAsset.put(entity.getId(), assetIdAndEntityMap.get(entity.getLink()));
                }
                this.saveAssetChartsSimultaneously(panelIdAndAsset, chart);
            }
        }
    }

    private void saveAssetChartsSimultaneously(Map<Integer, Asset> panelIdAndAsset, Chart chart) {
        Chart targetChart = new Chart();
        String title;
        Asset asset;
        for (Map.Entry<Integer, Asset> entry : panelIdAndAsset.entrySet()) {
            asset = entry.getValue();
            targetChart = JSONObject.parseObject(JSON.toJSONString(chart), Chart.class);
            targetChart.setPanelId(entry.getKey());
            targetChart.setSync(null);
            targetChart.setId(null);
            targetChart.setPid(chart.getId());

            title = targetChart.getTitle();
            targetChart.setTitle(this.replacAssetInfos(title, asset));

            targetChart.setParam(ToolUtil.isNotEmpty(targetChart.getParam()) ? this.replacAssetInfos(targetChart.getParam().toString(), asset) : null);

            if(CollectionUtils.isNotEmpty(targetChart.getElements())){
                for (ChartElement element : targetChart.getElements()) {
                    element.setExpression(ToolUtil.isNotEmpty(element.getExpression()) ? this.replacAssetInfos(element.getExpression(), asset) : null);

                }
            }

            // 添加
            this.saveChart(targetChart);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChart(Chart chart) {
        Integer prev = chart.getPrev();
        Integer next = chart.getNext();
        // 修改位置了
        if (prev != null && next != null) {
            this.modify(chart.getPanelId(), chart);
        }
        this.updateById(chart);

        // 处理 chart element 信息
        QueryWrapper<ChartElement> chartElementQueryWrapper = new QueryWrapper<>();
        chartElementQueryWrapper.lambda().eq(ChartElement::getChartId, chart.getId());
        List<ChartElement> list = chartElementService.list(chartElementQueryWrapper);

        List<ChartElement> saveList = new ArrayList<>();
        List<ChartElement> updateList = new ArrayList<>();

        for (ChartElement element : chart.getElements()) {
            element.setChartId(chart.getId());

            if (element.getId() == null) {
                saveList.add(element);
            } else {
                updateList.add(element);
            }

            // 数据库存在 element列表中没有的element，删除
            if (list.contains(element)) {
                list.remove(element);
            }
        }
        if (saveList.size() > 0) {
            chartElementService.saveBatch(saveList);
        }
        if (updateList.size() > 0) {
            chartElementService.updateBatchById(updateList);
        }
        if (list.size() > 0) {
            List<Integer> elementIds = list.stream().map(ChartElement::getId).collect(Collectors.toList());

            chartElementService.removeByIds(elementIds);
        }

        // 同步修改处理
        Integer sync = chart.getSync();
        if (sync != null) {
            Panel panel = panelService.getById(chart.getPanelId());
            if ("model".equals(panel.getType()) && sync.equals(1)) {
                this.updateAssetChartsSimultaneously(chart);
            }
        }
    }

    private void updateAssetChartsSimultaneously(Chart chart) {
        // 引用的charts
        List<Chart> charts = this.list(new QueryWrapper<Chart>().lambda().eq(Chart::getPid, chart.getId()));

        // 没有引用的charts
        if (CollectionUtils.isEmpty(charts)) {
            return;
        }

        // 删除 element
        List<Integer> idList = charts.stream().map(Chart::getId).collect(Collectors.toList());
        chartElementService.delElementByChartIds(idList);


        // 返回引用chartId 和 对应的 asset
        List<Chart> chartIdWithAssetEntity = assetDao.getAssetsByChartIds(idList);
        Map<Integer, Asset> chartIdAndAssetEntityMap = chartIdWithAssetEntity.stream().collect(Collectors.toMap(Chart::getId, Chart::getAsset));

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

    @Override
    public void validateChart(Integer panelId, Chart chart) {
        //panel是否存在
        validatePanel(chart.getPanelId());
        //chart是否属于panel
        if (chart.getId() != null) {
            validateChartExists(panelId, chart.getId());
        }

        String type = chart.getType();

        ValidateUtils.is(chart.getTitle()).notNull(RCode.CHART_TITLE_ISNULL)
                .and(chart.getSpan()).notNull(RCode.CHART_SPAN_ISNULL)
                .and(chart.getHeight()).notNull(RCode.CHART_HEIGHT_ISNULL)
                .and(type).notNull(RCode.CHART_TYPE_ISNULL);

        // 验证type是否在枚举范围内
        sysDictService.checkSysDicValue(Constant.DICT_CHART_TYPE, chart.getType(), RCode.CHART_TYPE_INVALIDE);

        Map param = (Map) chart.getParam();
        switch (type) {
            // url类型
            case Constant.CHART_URL_TYPE: {
                ValidateUtils.is(param).notNull(RCode.CHART_URLPARAM_ISNULL);
                Object url = param.get(Constant.CHART_URL_TYPE);
                if (ToolUtil.isEmpty(url)) {
                    throw new NZException(RCode.CHART_PARAMURL_ISNULL);
                }
                if (!CommonUtils.checkUrl(url.toString())) {
                    throw new NZException(RCode.CHART_PARAMURL_FORMAT);
                }
                String strParam = JSONObject.toJSONString(param);
                chart.setParam(strParam);

                chart.setElements(new ArrayList<>());
                break;
            }
            // singleStat 类型
            case Constant.CHART_SINGLESTAT_TYPE: {
                ValidateUtils.is(param).notNull(RCode.CHART_SINGLESTATPARAM_ISNULL);
                Object statistics = param.get(Constant.CHART_SINGLESTAT_PARAMkEY);
                if (ToolUtil.isEmpty(statistics)) {
                    throw new NZException(RCode.CHART_PARAMSINGLESTAT_ISNULL);
                }
                if (!Constant.CHART_SINGLESTAT_STATISTICS.contains(statistics.toString())) {
                    throw new NZException(RCode.CHART_PARAMSINGLESTAT_INVALIDE);
                }
                String strParam = JSONObject.toJSONString(param);
                chart.setParam(strParam);

                ValidateUtils.is(chart.getElements()).notNull(RCode.CHART_ELEMENT_ISNULL);
                chartElementService.validateElements(chart.getId(), chart.getElements());
                break;
            }
            default: {
                chart.setParam(null);

                // url 类型不需要校验 elements
                ValidateUtils.is(chart.getElements()).notNull(RCode.CHART_ELEMENT_ISNULL);
                chartElementService.validateElements(chart.getId(), chart.getElements());
            }
        }
    }

    private void validateChartExists(Integer panelId, Integer id) {
        QueryWrapper<Chart> chartQueryWrapper = new QueryWrapper<>();
        chartQueryWrapper.lambda().eq(Chart::getPanelId, panelId);
        chartQueryWrapper.lambda().eq(Chart::getId, id);

        Chart chart = this.getOne(chartQueryWrapper);

        if (chart == null) {
            throw new NZException(RCode.CHART_NOTEXSITS);
        }
    }

    @Override
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

        List<Chart> chartList = this.list(new QueryWrapper<Chart>().lambda().eq(Chart::getPanelId, panelId));
        // 排序
        chartList = this.getSequentialChartList(chartList);


        // 内置chart不能删除
        this.validateBuildInCharts(Arrays.asList(ids));
        List<Integer> chartIdList = chartList.stream().map(Chart::getId).collect(Collectors.toList());

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
                if ("model".equals(panel.getType()) && sync.equals(1)) {
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
        firstChart.setPrev(0);

        // 剩余一个
        if (deletedChartList.size() == 1) {
            firstChart.setNext(-1);
            Object param = firstChart.getParam();
            if (param != null) {
                firstChart.setParam(param.toString());
            }
            this.updateById(firstChart);
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
            updateCharts.get(updateCharts.size() - 1).setNext(-1);
            Object param;
            for (Chart chart : updateCharts) {
                param = chart.getParam();
                if (param != null) {
                    chart.setParam(param.toString());
                }
            }
            this.saveOrUpdateBatch(updateCharts);
        }
    }

    private void syncDeleteAssetsCharts(List<Integer> deleteIds) {
        // 关联charts
        List<Chart> list = this.list(new QueryWrapper<Chart>().lambda().in(Chart::getPid, deleteIds));
        Map<Integer, List<Chart>> listMap = list.stream().collect(Collectors.groupingBy(Chart::getPanelId));

        // 需要同步删除的panel 关联的 charts
        List<Chart> chartsByPanel = this.list(new QueryWrapper<Chart>().lambda().in(Chart::getPanelId, listMap.keySet()));
        Map<Integer, List<Chart>> chartsByPanelMap = chartsByPanel.stream().collect(Collectors.groupingBy(Chart::getPanelId));

        List<Integer> allDeleteChartIds = listMap.values().stream().flatMap(Collection::stream).map(Chart::getId).collect(Collectors.toList());
        // 删除自身
        this.removeByIds(allDeleteChartIds);
        // 删除关联的chart Elements
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

    private List<Chart> getSequentialChartList(List<Chart> charts) {
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

        // 要删除的 charts id集合
        List<Chart> charts = this.list(new QueryWrapper<Chart>().lambda().in(Chart::getPanelId, panelIds));
        List<Integer> chartIds = charts.stream().map(Chart::getId).collect(Collectors.toList());

        // 内置chart不能删除
        this.validateBuildInCharts(chartIds);

        if (CollectionUtils.isNotEmpty(chartIds)) {
            // 删除 charts
            this.removeByIds(chartIds);

            // 删除关联 element
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
        // 取出改panel下所有的charts
        List<Chart> chartList = this.list(new QueryWrapper<Chart>().lambda().eq(Chart::getPanelId, panelId).orderByAsc(Chart::getPrev));

        String str;
        for (Chart entity : chartList) {
            str = (String) entity.getParam();
            if (StringUtils.isNotEmpty(str)) {
                entity.setParam(str);
            }
        }

        Map<Integer, Chart> idAndChartMap = new HashMap<>();
        for (Chart chart1 : chartList) {
            idAndChartMap.put(chart1.getId(), chart1);
        }

        Chart oldEntity = idAndChartMap.get(chart.getId());
        String param = (String) oldEntity.getParam();
        if (param != null) {
            chart.setParam(param);
        }

        // 就只有一个图表 不让其调整位置
        if (chartList.size() == 1) {
            chart.setPrev(0);
            chart.setNext(-1);
            this.updateById(chart);
            return;
        }

        Chart oldChart = idAndChartMap.get(chart.getId());

        // chart不存在于当前panel
        if (oldChart == null) {
            throw new NZException(RCode.CHART_NOTEXSITS);
        }

        // 校验 prev next 参数是否合法
        if (this.validatePrevAndNext(idAndChartMap, chart)) {
            // 没有改变位置 直接修改库中 span height属性
            this.updateById(chart);
        }

        Integer firstIndex = 0;
        Integer lastIndex = -1;
        // 最后需要更改的实体集合
        List<Chart> updateCharts = new ArrayList<>();

        Integer id = chart.getId();
        // 判断旧位置是否为第一位 并且移动了位置
        if (firstIndex.equals(oldChart.getPrev()) && !firstIndex.equals(chart.getPrev())) {
            // 新的第一位
            Chart newFirstIndex = idAndChartMap.get(oldChart.getNext());
            newFirstIndex.setPrev(firstIndex);
            // 第一种 移动到末尾
            if (lastIndex.equals(chart.getNext())) {
                // 新的倒数第二位
                Chart newCountdownSecond = idAndChartMap.get(chart.getPrev());
                newCountdownSecond.setNext(id);

                updateCharts.add(newCountdownSecond);
            } else {
                // 第二种 不是移动到最后 而是移动到中间位置 即 有前有后
                // 移动后 前面的
                Chart newPrev = idAndChartMap.get(chart.getPrev());
                // 移动后 后面的
                Chart newNext = idAndChartMap.get(chart.getNext());
                newPrev.setNext(id);
                newNext.setPrev(id);
                updateCharts.add(newPrev);
                updateCharts.add(newNext);
            }
            updateCharts.add(chart);
            updateCharts.add(newFirstIndex);
        }

        // 原来是末尾 并且移动了位置
        if (lastIndex.equals(oldChart.getNext()) && !lastIndex.equals(chart.getNext())) {

            // 新的最后一位
            Chart newLastIndex = idAndChartMap.get(oldChart.getPrev());
            newLastIndex.setNext(lastIndex);
            // 第一种 末尾移动到了首位
            if (firstIndex.equals(chart.getPrev())) {
                // 新的第二位
                Chart newSecond = idAndChartMap.get(chart.getNext());
                newSecond.setPrev(id);

                updateCharts.add(newSecond);
            } else {
                // 第二种 不是移动到开始 而是移动到中间位置 即 有前有后
                // 移动后 前面的
                Chart newPrev = idAndChartMap.get(chart.getPrev());
                // 移动后 后面的
                Chart newNext = idAndChartMap.get(chart.getNext());
                newPrev.setNext(id);
                newNext.setPrev(id);
                updateCharts.add(newPrev);
                updateCharts.add(newNext);
            }

            updateCharts.add(chart);
            updateCharts.add(newLastIndex);
        }

        // 原来是中间的情况
        if (!firstIndex.equals(oldChart.getPrev()) && !lastIndex.equals(oldChart.getNext())) {
            boolean flag = false;

            // 原来位置的前后
            Chart oldPrevChart = idAndChartMap.get(oldChart.getPrev());
            Chart oldNextChart = idAndChartMap.get(oldChart.getNext());
            // 第一种 移动到了开头
            if (firstIndex.equals(chart.getPrev())) {
                // 新的第二位
                Chart newSecondIndex = idAndChartMap.get(chart.getNext());
                newSecondIndex.setPrev(id);

                if (oldPrevChart.getId().equals(newSecondIndex.getId())) {
                    flag = true;
                    newSecondIndex.setNext(oldNextChart.getId());
                    oldNextChart.setPrev(newSecondIndex.getId());
                } else {
                    oldPrevChart.setNext(oldNextChart.getId());
                    oldNextChart.setPrev(oldPrevChart.getId());
                }

                updateCharts.add(chart);
                updateCharts.add(newSecondIndex);
                updateCharts.add(oldNextChart);
                if (!flag) {
                    updateCharts.add(oldPrevChart);
                }
            } else if (lastIndex.equals(chart.getNext())) {
                // 第二种移动到末尾
                // 新的倒数第二位
                Chart newCountdownSecond = idAndChartMap.get(chart.getPrev());
                newCountdownSecond.setNext(id);

                // 原来位置的前后
                if (oldNextChart.getId().equals(newCountdownSecond.getId())) {
                    flag = true;
                    newCountdownSecond.setPrev(oldPrevChart.getId());
                    oldPrevChart.setNext(newCountdownSecond.getId());
                } else {
                    oldPrevChart.setNext(oldNextChart.getId());
                    oldNextChart.setPrev(oldPrevChart.getId());
                }

                updateCharts.add(chart);
                updateCharts.add(newCountdownSecond);
                updateCharts.add(oldPrevChart);

                if (!flag) {
                    updateCharts.add(oldNextChart);
                }
            } else {
                // 第三种 中间调整到中间
                Chart newPrevChart = idAndChartMap.get(chart.getPrev());
                Chart newNextChart = idAndChartMap.get(chart.getNext());

                oldPrevChart.setNext(oldNextChart.getId());
                oldNextChart.setPrev(oldPrevChart.getId());

                newNextChart.setPrev(id);

                // 换位前后面的 和 换位后前面的是同一个
                if (oldNextChart.getId().equals(newPrevChart.getId())) {
                    flag = true;
                    oldNextChart.setNext(id);
                } else {
                    newPrevChart.setNext(id);
                }

                updateCharts.add(oldPrevChart);
                updateCharts.add(oldNextChart);
                updateCharts.add(newNextChart);

                if (!flag) {
                    updateCharts.add(newPrevChart);
                }
                updateCharts.add(chart);
            }
        }

        // 更新库中数据
        if (CollectionUtils.isNotEmpty(updateCharts)) {
            this.saveOrUpdateBatch(updateCharts);
        }
    }

    /**
     * 校验参数是否合法
     *
     * @param idAndChartMap
     * @param chart
     */
    private boolean validatePrevAndNext(Map<Integer, Chart> idAndChartMap, Chart chart) {
        // 如果没有改变位置
        Chart entity = this.getById(chart.getId());
        if (entity.getPrev().equals(chart.getPrev()) && entity.getNext().equals(chart.getNext())) {
            return true;
        }

        Integer prev = chart.getPrev();
        Integer next = chart.getNext();

        Integer firstIndex = 0;
        Integer lastIndex = -1;
        // 移动至开头的
        if (firstIndex.equals(prev)) {
            // 校验 next 参数值是否正确
            Chart oldFirstChart = idAndChartMap.get(next);
            if (oldFirstChart == null || !firstIndex.equals(oldFirstChart.getPrev())) {
                throw new NZException(RCode.CHART_PREVNEXT_INCORRECT);
            }
        } else if (lastIndex.equals(next)) { // 移动至末尾的
            // 校验 prev 参数是否正确
            Chart oldLastChart = idAndChartMap.get(prev);
            if (oldLastChart == null || !lastIndex.equals(oldLastChart.getNext())) {
                throw new NZException(RCode.CHART_PREVNEXT_INCORRECT);
            }
        } else {
            // 移动到中间的
            Chart oldMiddlePrev = idAndChartMap.get(prev);
            Chart oldMiddleNext = idAndChartMap.get(next);
            if (oldMiddlePrev == null || oldMiddleNext == null) {
                throw new NZException(RCode.CHART_PREVNEXT_INCORRECT);
            }

            // 判断是否连续
            if (!oldMiddlePrev.getNext().equals(oldMiddleNext.getId())) {
                throw new NZException(RCode.CHART_PREVNEXT_INCORRECT);
            }
        }
        return false;
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
		// 图表复制
		Chart source = this.getById(chartId);
		Chart target =new Chart();
        Integer lastIndex = -1;
        BeanUtils.copyProperties(source, target);
        
       /* // 防止同一个panel下面的title有重复值
        target.setTitle(source.getTitle()+" copy"+ System.currentTimeMillis());*/
		
        if(lastIndex.equals(source.getNext())) {
			//位于图表中最后一位时
			target.setPrev(source.getId());
			target.setNext(source.getNext());
			this.save(target);
			source.setNext(target.getId());
			this.updateById(source);
		}else {
			target.setPrev(source.getId());
			target.setNext(source.getNext());
			this.save(target);
			
			List<Chart> charts =new ArrayList<Chart>();
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
	     list.forEach(chartElement-> {
	    	 chartElement.setChartId(target.getId());
         });
	     chartElementService.saveBatch(list);
	     target.setElements(list);
	     return target;
	}

    @Override
    public void saveAssetPanelAndChart(Asset asset) {
        // 当前型号关联的panel对象
        Panel modelPanel = panelService.getOne(new QueryWrapper<Panel>().lambda().eq(Panel::getType, "model").eq(Panel::getLink, asset.getModelId()));

        Panel panel = new Panel();
        panel.setType("asset");
        panel.setLink(asset.getId());
        panel.setCreateBy(ShiroUtils.getUserId().intValue());
        panel.setBuildIn(modelPanel.getBuildIn());
        // 固定格式 panel name format is asset_panel_link
        panel.setName("asset_panel_" + asset.getId());
        panelService.save(panel);

        // 复制一份charts
        this.copyChartsToAssetChartsByPanel(modelPanel, panel, asset);
    }

    @Override
    public void updateAssetChartsByNewModel(Asset asset) {
        List<Panel> panels = panelService.list();

        // 修改的Model 关联的panel
        Panel modelPanel = panels.stream().filter(panel -> "model".equals(panel.getType()) && panel.getLink().equals(asset.getModelId()) ? true : false).findAny().orElse(null);
        Panel assetPanel = panels.stream().filter(panel -> "asset".equals(panel.getType()) && panel.getLink().equals(asset.getId()) ? true : false).findAny().orElse(null);

        List<Chart> list = this.list(new QueryWrapper<Chart>().lambda().eq(Chart::getPanelId, assetPanel.getId()));
        List<Integer> delIds = list.stream().filter(chart -> chart.getPid() != null ? true : false).map(Chart::getId).collect(Collectors.toList());
        // 删除之前model关联的图表
        Integer[] ids = new Integer[delIds.size()];
        delIds.toArray(ids);

        // 删除旧model关联的charts
        this.delCharts(assetPanel.getId(), ids, null);
        // 复制一份charts
        this.copyChartsToAssetChartsByPanel(modelPanel, assetPanel, asset);
    }

    private void copyChartsToAssetChartsByPanel(Panel modelPanel, Panel targetPanel, Asset asset) {
        List<Chart> charts = this.list(new QueryWrapper<Chart>().lambda().eq(Chart::getPanelId, modelPanel.getId()));
        // 排序
        charts = this.getSequentialChartList(charts);

        List<Integer> chartIds = charts.stream().map(Chart::getId).collect(Collectors.toList());
        List<ChartElement> chartElements = chartElementService.list(new QueryWrapper<ChartElement>().lambda().in(ChartElement::getChartId, chartIds));
        Map<Integer, List<ChartElement>> chartIdAndElementsMap = chartElements.stream().collect(Collectors.groupingBy(ChartElement::getChartId));


        Date now = new Date();
        Map<Integer, List<ChartElement>> saveElements = new HashMap<>();
        List<ChartElement> elementList;

        String title;
        for (Chart chart : charts) {
            elementList = chartIdAndElementsMap.get(chart.getId());

            // 设置关联 panel为当前新增asset关联的panel
            chart.setPanelId(targetPanel.getId());
            chart.setCreateAt(now);
            chart.setParam(ToolUtil.isNotEmpty(chart.getParam()) ? this.replacAssetInfos(chart.getParam().toString(), asset) : null);
            chart.setElements(new ArrayList<>());
            chart.setPid(chart.getId());

            title = chart.getTitle();
            chart.setTitle(this.replacAssetInfos(title, asset));

            // 保存 chart
            this.saveChart(chart);
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

    private String replacAssetInfos(String str, Asset asset){
        str = str.replaceAll("\\{\\{asset.host\\}\\}", asset.getHost());
        str = str.replaceAll("\\{\\{asset.sn\\}\\}", asset.getSn());
        return str;
    }
}
