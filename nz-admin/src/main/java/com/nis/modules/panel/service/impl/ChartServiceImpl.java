package com.nis.modules.panel.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.AssetTag;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.panel.dao.ChartDao;
import com.nis.modules.panel.entity.Chart;
import com.nis.modules.panel.entity.ChartElement;
import com.nis.modules.panel.entity.Panel;
import com.nis.modules.panel.service.ChartElementService;
import com.nis.modules.panel.service.ChartService;
import com.nis.modules.panel.service.PanelService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.sys.shiro.ShiroUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChartServiceImpl extends ServiceImpl<ChartDao, Chart> implements ChartService {
    @Autowired
    private ChartElementService chartElementService;

    @Autowired
    private SysDictService sysDictService;

    @Autowired
    private PanelService panelService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private ChartDao chartDao;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    private TransactionDefinition transactionDefinition;

    @Override
    public List<Chart> queryCharts(Integer panelId, Integer id, String title) {
        List<Chart> charts = chartDao.queryCharts(panelId, id, title);
        String param;
        for (Chart chart : charts) {
            // param 格式处理
            param = (String) chart.getParam();
            if (StringUtils.isNotEmpty(param)) {
                chart.setParam(JSONObject.parseObject(param, Map.class));
            }
        }
        return charts;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveChart(Integer panelId, Chart chart) {
        // 1. 新增 chart 自动追加至末尾
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
            this.save(chart);
        } else {
            Chart endChart = this.getOne(new LambdaQueryWrapper<Chart>().eq(Chart::getPanelId, chart.getPanelId()).orderByDesc(Chart::getWeight).last("limit 0,1"));
            Integer weight = endChart.getWeight();
            chart.setWeight(++weight);
            this.save(chart);
        }

        // 3. 保存 chart elements信息
        chartElementService.saveOrUpdateElements(chart.getId(), chart.getElements());

        // 4. 同步新增处理
        Integer sync = chart.getSync();
        if (sync != null) {
            Panel panel = panelService.getById(chart.getPanelId());
            // 当前 chart 所属 panel 为 model 类型并且 同步标识 为 1 时 ，对已通过对应 model panel 创建的 asset panel 追加新增的 chart
            if(StrUtil.equals(Constant.PanelType.MODEL.getValue(),panel.getType()) && sync.equals(1)) {
                this.saveAssetChartsSimultaneously(panel, chart);
            }
        }
    }

    /**
     * 新增 model 时对已通过对应 model panel 创建的 asset panel 追加新增的 chart
     *
     * @param modelPanel
     * @param chart
     */
    private void saveAssetChartsSimultaneously(Panel modelPanel, Chart chart) {
        // 1. 通过 panel  linke 的 modelId 查找该 model 下是否关联 asset
        List<Asset> assets = assetService.getAssetsWithTagsByModelId(modelPanel.getLink());
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

        this.updateById(chart);

        // 3. 修改 chart elements
        chartElementService.saveOrUpdateElements(chart.getId(), chart.getElements());

        // 4. 同步修改处理
        Integer sync = chart.getSync();
        if (sync != null) {
            Panel panel = panelService.getById(chart.getPanelId());
            // 当前 chart 所属 panel 为 model 类型并且 同步标识 为 1 时 ，对根据 chart pid 查找通过此 chart 创建的 chart 更新
            if(StrUtil.equals(Constant.PanelType.MODEL.getValue(),panel.getType()) && sync.equals(1)) {
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
/*        List<Chart> chartIdWithAssetEntity = assetService.getAssetsWithTagByChartIds(idList);
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
            entity.setUnit(chart.getUnit());
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
        chartElementService.saveBatch(saveElements); */
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
                if(StrUtil.equals(Constant.PanelType.MODEL.getValue(),panel.getType())) {
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
            case Constant.CHART_TEXT_TYPE: {
                ValidateUtils.is(param).notNull(RCode.CHART_TEXTPARAM_ISNULL);
                Object text = param.get(Constant.CHART_TEXT_TYPE);
                if (ToolUtil.isEmpty(text)) throw new NZException(RCode.CHART_PARAMTEXT_ISNULL);
                break;
            }
            default: {
                // 除 url alertList 类型外，其他类型都需校验 elements
                ValidateUtils.is(elements).notNull(RCode.CHART_ELEMENT_ISNULL);
                chartElementService.validateElements(elements);
            }
        }

        // 4. 校验 unit 是否合法
        Integer unit = chart.getUnit();
        if (unit != null) {
            List<Integer> unitList = new ArrayList(Constant.ALERT_RULE_UNIT_MAP.values());
            if (unit < Collections.min(unitList) || unit > Collections.max(unitList)) {
                throw new NZException(RCode.ALERTRULE_UNIT_INVALIDE);
            }
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
        //chartList = this.getSequentialChartList(chartList);
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
                if(StrUtil.equals(Constant.PanelType.MODEL.getValue(),panel.getType()) && sync.equals(1)) {
                    this.syncDeleteAssetsCharts(deleteIds);
                }
            }

            // 删除自身
            this.baseMapper.deleteByIds(panelId, deleteIds);

            // 删除char element
            chartElementService.delElementByChartIds(deleteIds);

            // 处理删除之后的位置问题
            //this.handlePositionAfterDelete(chartList, deleteIds);
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
        //List<Chart> chartsByPanel = this.list(new QueryWrapper<Chart>().lambda().in(Chart::getPanelId, listMap.keySet()));
        //Map<Integer, List<Chart>> chartsByPanelMap = chartsByPanel.stream().collect(Collectors.groupingBy(Chart::getPanelId));

        List<Integer> allDeleteChartIds = listMap.values().stream().flatMap(Collection::stream).map(Chart::getId).collect(Collectors.toList());
        // 删除自身
        this.removeByIds(allDeleteChartIds);
        // 删除关联的 chart Elements
        chartElementService.delElementByChartIds(allDeleteChartIds);

        /*Integer panid;
        List<Chart> sequentialChartList;
        List<Integer> delIds;
        for (Map.Entry<Integer, List<Chart>> entry : chartsByPanelMap.entrySet()) {
            panid = entry.getKey();
            sequentialChartList = this.getSequentialChartList(entry.getValue());
            delIds = listMap.get(panid).stream().map(Chart::getId).collect(Collectors.toList());
            // 处理删除后的位置信息
            this.handlePositionAfterDelete(sequentialChartList, delIds);
        }*/
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

        // 设置默认值
        chart.setPrev(Constant.START_PREV_VALUE);
        chart.setNext(Constant.END_NEXT_VALUE);

        // 调整大小
        chartDao.updateSizeById(chart);

       /* // 取出改panel下所有的charts
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
        }*/
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

        this.save(target);

        /*if (Constant.END_NEXT_VALUE.equals(source.getNext())) {
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
        }*/
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

    private String getChartTemplateConfigKeyByType(String type) {
        // 默认 dashboard 类型
        type = StringUtils.isEmpty(type) ? Constant.PanelType.DASHBOARD.getValue() : type;
        String chartTemplateTyep;
        switch (type) {
            case "dashboard": {
                chartTemplateTyep = Constant.SYSCONFIG_KEY_CHART_EXPORT_HEADER;
                break;
            }
            case "model": {
                chartTemplateTyep = Constant.SYSCONFIG_KEY_MODEL_CHART_EXPORT_HEADER;
                break;
            }
            case "asset": {
                chartTemplateTyep = Constant.SYSCONFIG_KEY_ASSET_CHART_EXPORT_HEADER;
                break;
            }
            default:
                throw new NZException(RCode.PANEL_CHART_TEMPLATE_TYPE_INCORRECT);
        }
        return chartTemplateTyep;
    }

    @Override
    public void getExportTemplateByLanguage(HttpServletResponse response, String language, String type) {
        // 按类型获取表头模板信息
        String configKey = this.getChartTemplateConfigKeyByType(type);
        SysConfigEntity configEntity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, configKey));
        Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);
        Map<String, String> exportHeaderMap;
        /*switch (language) {
            case "en": {
                exportHeaderMap = (Map) map.get("en");
                break;
            }
            case "cn": {
                exportHeaderMap = (Map) map.get("cn");
                break;
            }
            case "ru": {
                exportHeaderMap = (Map) map.get("ru");
                break;
            }
            default: {
                throw new NZException(RCode.PANEL_EXPORT_HEADER_LANGUAGE_ERROR);
            }
        }*/
        if(map.get(language) != null){
            exportHeaderMap = (Map) map.get(language);
        }else{
            throw new NZException(RCode.PANEL_EXPORT_HEADER_LANGUAGE_ERROR);
        }
        SXSSFWorkbook workbook = null;
        try {
            workbook = ExcelUtils.createExcel07(exportHeaderMap, "Chart", true);
            String fileName = "PanelChartExportTemplate.xlsx";
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            log.error("图表模板生成失败，错误信息是：" + e);
        } catch (RuntimeException e) {
            log.error("获取图表导入模板失败，表头语言是：" + language + " ，错误信息是：" + e);
        } finally {
            if (workbook != null) {
                IOUtils.closeQuietly(workbook);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelImport(String seq) {
        List<Panel> panelList = panelService.list(new QueryWrapper<Panel>().lambda().eq(Panel::getType, Constant.PanelType.DASHBOARD.getValue()));
        List<Integer> delPanelIds = panelList.stream().filter(panel -> seq.equals(panel.getSeq())).map(Panel::getId).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(delPanelIds)) {
            // 删除 panel
            panelService.remove(new QueryWrapper<Panel>().lambda().eq(Panel::getSeq, seq));
            // 处理删除之后其余 panel 的位置
            // panelService.handlePositionAfterDelete(panelService.getSequentialPanelList(panelList), delPanelIds);
        }

        List<Chart> delCharts = this.list(new QueryWrapper<Chart>().lambda().eq(Chart::getSeq, seq));
        if (CollectionUtils.isNotEmpty(delCharts)) {
            // 得到要删除的图表完整顺序
           /* List<Integer> delChartsPanelIds = delCharts.stream().map(Chart::getPanelId).distinct().collect(Collectors.toList());
            List<Chart> charts = this.list(new QueryWrapper<Chart>().lambda().in(Chart::getPanelId, delChartsPanelIds));
            // 按照 panel 分组 获取对象panel 下完整的 chart 顺序
            Map<Integer, List<Chart>> panelIdAndChartsMap = charts.stream().collect(Collectors.groupingBy(Chart::getPanelId));
            Map<Integer, List<Chart>> panelIdAnddelChartsMap = delCharts.stream().collect(Collectors.groupingBy(Chart::getPanelId));
            List<Chart> sequentialChartList;
            List<Chart> delChartsByPanelId;
            for (Map.Entry<Integer, List<Chart>> entry : panelIdAndChartsMap.entrySet()) {
                sequentialChartList = this.getSequentialChartList(entry.getValue());
                delChartsByPanelId = panelIdAnddelChartsMap.get(entry.getKey());
                if (CollectionUtils.isNotEmpty(sequentialChartList) && CollectionUtils.isNotEmpty(delChartsByPanelId)) {
                    this.handlePositionAfterDelete(sequentialChartList, delChartsByPanelId.stream().map(Chart::getId).collect(Collectors.toList()));
                }
            }*/
            // 删除 charts
            this.remove(new QueryWrapper<Chart>().lambda().eq(Chart::getSeq, seq));
        }
        chartElementService.remove(new QueryWrapper<ChartElement>().lambda().eq(ChartElement::getSeq, seq));
    }

    @Override
    public void export(HttpServletResponse response, Map<String, Object> params) {
        // 导出查询与列表查询一致
        List<Chart> charts = this.exportDataByParams(params);
        String type = Constant.PanelType.DASHBOARD.getValue();
        if (CollectionUtils.isNotEmpty(charts)) {
            Chart chart = charts.stream().findFirst().get();
            Panel panel = panelService.getById(chart.getPanelId());
            if(!StrUtil.equals(Constant.PanelType.DASHBOARD.getValue(),panel.getType())) {
                type = Constant.PanelType.ASSET.getValue();
            }
        }

        // 获取导出配置 key 名称
        String chartTemplateConfigKey = this.getChartTemplateConfigKeyByType(type);
        SysConfigEntity configEntity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, chartTemplateConfigKey));
        Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);
        // 添加标题行 默认语言 en
        String language = (String) params.get("language");
        language = ToolUtil.isEmpty(language) ? "en" : language;

        SXSSFWorkbook workbook = null;
        try {
            String sheetName = "Chart";
            workbook = ExcelUtils.createExcel07((Map) map.get(language), sheetName, false);
            SXSSFSheet sheet = workbook.getSheet(sheetName);

            SysConfigEntity i18nConfig = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.I18N_MAPPING).eq(SysConfigEntity::getStatus, 1));
            Map i18nConfigMap = JSONObject.parseObject(i18nConfig.getParamValue(), Map.class);
            Map chartTypeMap = (Map) i18nConfigMap.get(Constant.DICT_CHART_TYPE);
            Map chartTypeMapByLanguage = (Map)  chartTypeMap.get(language);

            // 填充数据
            if(!StrUtil.equals(Constant.PanelType.DASHBOARD.getValue(),type)){
                this.fillAssetModelChartData(charts, 1, sheet, workbook, chartTypeMapByLanguage);
            }else{
                this.fillDashboardChartData(charts, 1, sheet, workbook, chartTypeMapByLanguage);
            }

            String fileName = "PanelChartExport.xlsx";
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            log.error("图表导出文件生成失败，错误信息是：" + e);
        } catch (RuntimeException e) {
            log.error("图表导出失败，参数是：" + params.toString() + " ，错误信息是：" + e);
        } finally {
            if (workbook != null) {
                IOUtils.closeQuietly(workbook);
            }
        }

    }

    private void fillAssetModelChartData(List<Chart> charts, int rowNum, SXSSFSheet sheet, SXSSFWorkbook workbook, Map chartTypeMapByLanguage) {
        Row row;
        String type;
        CellStyle dataStyle = ExcelUtils.createCellStyle(workbook, (short) 13, false);
        for (Chart chart : charts) {
            if (chart == null || Constant.CHART_ASSETINFO_TYPE.equalsIgnoreCase(chart.getType())) continue;
            row = sheet.createRow(rowNum++);
            type = chart.getType();
            row.createCell(0).setCellValue(chart.getTitle());
            row.createCell(1).setCellValue(this.getI18nChartTypeValue(chartTypeMapByLanguage, type));
            row.createCell(2).setCellValue(chart.getSpan());
            row.createCell(3).setCellValue(chart.getHeight());
            if(StrUtil.equals(Constant.CHART_TEXT_TYPE,type)) {
                row.createCell(4).setCellValue("");
            } else {
                row.createCell(4).setCellValue(this.getUnitDescrByVal(chart.getUnit()));
            }
            row.createCell(5).setCellValue(this.handleChartParam(chart.getParam()));
            row.createCell(6).setCellValue(this.handleChartElementForExport(chart.getElements()));

            // 设置单元格样式
            ExcelUtils.setDataCellStyle(dataStyle, row, 6);
        }
    }

    private String getI18nChartTypeValue(Map chartTypeMapByLanguage, String type) {
        Object value = chartTypeMapByLanguage.get(type);
        return value == null ? "" : value.toString();
    }

    @Override
    public Map importChartFromExcelFile(MultipartFile excelFile, String type, Integer linkId, String language) {
        if (!ExcelUtils.validateFileType(excelFile.getOriginalFilename())) {
            throw new NZException(RCode.PANEL_IMPORTFILE_TYPE);
        }

        Workbook work = null;
        try {
            work = ExcelUtils.getWork(excelFile.getOriginalFilename(), excelFile.getInputStream());
        } catch (IOException e) {
            log.error("解析导入图表文件格式有误"+e);
            throw new NZException(RCode.PANEL_IMPORTFILE_FAILED);
        } finally {
            if (work != null) {
                IOUtils.closeQuietly(work);
            }
        }

        Sheet sheet = work.getSheetAt(0);
        // 表头校验
        String configKey = this.getChartTemplateConfigKeyByType(type);
        SysConfigEntity config = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, configKey));
        HashMap<String, Object> map = JSONObject.parseObject(config.getParamValue(), HashMap.class, Feature.OrderedField);
        boolean headerFlag = ExcelUtils.validateExcelHeader(sheet, sheet.getFirstRowNum(), map.values());
        if(!headerFlag){
            throw new NZException(RCode.PANEL_EXPORT_HEADER_TEMPLATE_ERROR);
        }

        Map<String, String> exportHeader = (Map) map.get(language);
        List<String> headerInfos = new ArrayList(exportHeader.keySet());

        List<Map> failDetails = new ArrayList<>();
        String seqUUID = UUID.randomUUID().toString();

        // 保存并返回成功条数
        type = StringUtils.isEmpty(type) ? Constant.PanelType.DASHBOARD.getValue() : type;

        SysConfigEntity i18nConfig = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.I18N_MAPPING).eq(SysConfigEntity::getStatus, 1));
        Map i18nConfigMap = JSONObject.parseObject(i18nConfig.getParamValue(), Map.class);
        Map chartTypeMap = (Map) i18nConfigMap.get(Constant.DICT_CHART_TYPE);
        Map chartTypeMapByLanguage = (Map) chartTypeMap.get(language);
        if (ToolUtil.isEmpty(chartTypeMapByLanguage)) {
            throw new NZException(RCode.PANEL_EXPORT_HEADER_LANGUAGE_ERROR);
        }

        int successNum = 0;
        if(StrUtil.equals(Constant.PanelType.DASHBOARD.getValue(),type)) {
            successNum = this.saveDashboardChartDataFromSheet(sheet, failDetails, seqUUID, chartTypeMapByLanguage, headerInfos);
        } else {
            successNum = this.saveAssetChartDataFromSheet(sheet, failDetails, seqUUID, type, linkId, chartTypeMapByLanguage,headerInfos);
        }
        int totalNum = sheet.getLastRowNum() - sheet.getFirstRowNum();

        Map resultMap = new HashMap(8);
        resultMap.put("seq", seqUUID);
        resultMap.put("totalNum", totalNum);
        resultMap.put("successNum", successNum);
        resultMap.put("failNum", totalNum - successNum);
        resultMap.put("failDetail", CommonUtils.handleImportFailDetails(failDetails));

        return resultMap;
    }

    /**
     * 调整位置接口
     *
     * @param panelId
     * @param chartList
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void weights(Integer panelId, List<Chart> chartList) {
        // 校验 panel 是否存在
        this.validatePanel(panelId);

        if (CollectionUtils.isEmpty(chartList)) throw new NZException(RCode.CHART_WEIGHT_ISNULL);
        for (Chart chart : chartList) {
            if (chart.getId() == null) throw new NZException(RCode.CHART_ID_ISNULL);
            if (chart.getWeight() == null) throw new NZException(RCode.CHART_WEIGHT_ISNULL);
        }

        chartDao.updateWeightBatch(chartList);
    }

    private int saveAssetChartDataFromSheet(Sheet sheet, List<Map> failDetails, String seqUUID, String panelType, Integer linkId, Map chartTypeMapByLanguage, List<String> headerInfos) {
        if (linkId == null) throw new NZException(RCode.PANEL_CHART_IMPORT_LINKID_ISNULL);

        Panel panel = panelService.getOne(new QueryWrapper<Panel>().lambda().eq(Panel::getType, panelType).eq(Panel::getLink, linkId));
        if (panel == null) throw new NZException(RCode.PANEL_NOT_EXIST);

        Row row;
        int successNum = 0;
        for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
            row = sheet.getRow(i);
            if (row == null) continue;
            int dataFromExcelRowNum = i + 1;
            boolean saveFlag = true;
            boolean typeFlag = true;
            String title = ExcelUtils.getCellValue(row.getCell(0));
            if (StringUtils.isEmpty(title)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.CHART_TITLE_ISNULL.getMsg());
            }
            String type = ExcelUtils.getCellValue(row.getCell(1));
            if (StringUtils.isEmpty(type)) {
                saveFlag = typeFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.CHART_TYPE_ISNULL.getMsg());
            } else {
                type = this.validateChartTypeForImport(type, chartTypeMapByLanguage);
                if (StringUtils.isEmpty(type)) {
                    saveFlag = typeFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.CHART_TYPE_INVALIDE.getMsg());
                }
            }
            String span = ExcelUtils.getCellValue(row.getCell(2));
            if (StringUtils.isEmpty(span)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.CHART_SPAN_ISNULL.getMsg());
            } else {
                Integer width = null;
                try {
                    width = Double.valueOf(span).intValue();
                } catch (NumberFormatException e) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.CHART_WIDTH_FORMAT.getMsg());
                }
                if (width != null && (width < 1 || width > 12)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.CHART_WIDTH_INVALIDE.getMsg());
                }
            }
            String height = ExcelUtils.getCellValue(row.getCell(3));
            if (StringUtils.isEmpty(height)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.CHART_HEIGHT_ISNULL.getMsg());
            } else {
                try {
                    Double.valueOf(height).intValue();
                } catch (NumberFormatException e) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.CHART_HEIGHT_FORMAT.getMsg());
                }
            }
            String unit = ExcelUtils.getCellValue(row.getCell(4));
            if (StringUtils.isNotEmpty(unit)) {
                if (ToolUtil.isEmpty(Constant.ALERT_RULE_UNIT_MAP.get(unit))) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.CHART_UNIT_NOTFOUND.getMsg());
                }
            }
            String param = ExcelUtils.getCellValue(row.getCell(5));
            if (typeFlag) {
                String msg = this.validateChartParamForImport(type, param, type);
                if (StringUtils.isNotEmpty(msg)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(5), msg);
                }
            }
            String elements = ExcelUtils.getCellValue(row.getCell(6));
            if (typeFlag) {
                String msg = this.validateChartElementForImport(type, elements);
                if (StringUtils.isNotEmpty(msg)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(6), msg);
                }
            }
            if (saveFlag) {
                TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
                try {
                    Chart chart = new Chart();
                    chart.setSeq(seqUUID);
                    chart.setPanelId(panel.getId());
                    chart.setTitle(title);
                    chart.setType(type);
                    chart.setSpan(Double.valueOf(span).intValue());
                    chart.setHeight(Double.valueOf(height).intValue());
                    // unit 为空 设置默认值为 short
                    unit = StringUtils.isEmpty(unit) ? "short" : unit;
                    chart.setUnit(Constant.ALERT_RULE_UNIT_MAP.get(unit));
                    chart.setParam(param.toString());
                    chart.setCreateAt(DateUtil.getUTCTimeByConfigTimeZone());
                    // 查询当前 panel 下的 charts
                    List<Chart> chartList = this.list(new QueryWrapper<Chart>().lambda().eq(Chart::getPanelId, chart.getPanelId()));
                    if (CollectionUtils.isEmpty(chartList)) {
                        chart.setPrev(Constant.START_PREV_VALUE);
                        this.save(chart);
                    } else {
                        Chart endChart = this.getOne(new LambdaQueryWrapper<Chart>().eq(Chart::getPanelId, chart.getPanelId()).orderByDesc(Chart::getWeight).last("limit 0,1"));
                        Integer weight = endChart.getWeight();
                        chart.setWeight(++weight);
                        this.save(chart);
                    }
                    List<ChartElement> chartElements = JSONArray.parseArray(elements, ChartElement.class);
                    if (CollectionUtils.isNotEmpty(chartElements)) {
                        chartElements.forEach(chartElement -> {
                            chartElement.setChartId(chart.getId());
                            chartElement.setSeq(seqUUID);
                            chartElement.setType(Constant.CHART_ELEMENT_EXPERT_TYPE);
                        });
                        chartElementService.saveBatch(chartElements);
                    }
                    dataSourceTransactionManager.commit(transactionStatus);
                    successNum++;
                } catch (RuntimeException e) {
                    dataSourceTransactionManager.rollback(transactionStatus);
                    this.addFailDetail(dataFromExcelRowNum, failDetails, "System", e.getMessage() + "");
                    log.error(String.format("保存 panel chart chartelement失败,回滚事务,错误信息:%s", e));
                }
            }
        }
        return successNum;
    }

    private int saveDashboardChartDataFromSheet(Sheet sheet, List<Map> failDetails, String seqUUID, Map<String, String> chartTypeMapByLanguage, List<String> headerInfos) {
        List<Panel> panelTreeList = panelService.queryList(new HashMap());
        Map<String, Integer> nodeNamePathAndIdMap = new HashMap<>();
        for (Panel panel : panelTreeList) {
            nodeNamePathAndIdMap.put(panel.getName(), panel.getId());
            this.getPanelNamePathAndIdMapFromTree(panel, panel.getName(), nodeNamePathAndIdMap);
        }

        List<Panel> dashboardTypePanelList = panelService.list(new LambdaQueryWrapper<Panel>().eq(Panel::getType, Constant.PanelType.DASHBOARD.getValue()));
        Integer panelMaxWeight;
        if (CollectionUtils.isEmpty(dashboardTypePanelList)) {
            panelMaxWeight = 0;
        } else {
            Panel endPanel = dashboardTypePanelList.stream().max(Comparator.comparingInt(Panel::getWeight)).get();
            panelMaxWeight = endPanel.getWeight();
        }

        Row row;
        int successNum = 0;
        for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
            row = sheet.getRow(i);
            if (row == null) continue;
            int dataFromExcelRowNum = i + 1;
            boolean saveFlag = true;
            boolean typeFlag = true;
            String panelName = ExcelUtils.getCellValue(row.getCell(0));
            if (StringUtils.isEmpty(panelName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.PANEL_NAME_ISNULL.getMsg());
            }
            String title = ExcelUtils.getCellValue(row.getCell(1));
            if (StringUtils.isEmpty(title)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.CHART_TITLE_ISNULL.getMsg());
            }
            String type = ExcelUtils.getCellValue(row.getCell(2));
            if (StringUtils.isEmpty(type)) {
                saveFlag = typeFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.CHART_TYPE_ISNULL.getMsg());
            } else {
                type = this.validateChartTypeForImport(type, chartTypeMapByLanguage);
                if (StringUtils.isEmpty(type)) {
                    saveFlag = typeFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.CHART_TYPE_INVALIDE.getMsg());
                }
            }
            String span = ExcelUtils.getCellValue(row.getCell(3));
            if (StringUtils.isEmpty(span)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.CHART_SPAN_ISNULL.getMsg());
            }else{
                Integer width = null;
                try {
                    width = Double.valueOf(span).intValue();
                } catch (NumberFormatException e) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.CHART_WIDTH_FORMAT.getMsg());
                }
                if(width != null && (width < 1 || width > 12)){
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.CHART_WIDTH_INVALIDE.getMsg());
                }
            }
            String height = ExcelUtils.getCellValue(row.getCell(4));
            if (StringUtils.isEmpty(height)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.CHART_HEIGHT_ISNULL.getMsg());
            }else{
                try {
                    Double.valueOf(height).intValue();
                } catch (NumberFormatException e) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.CHART_HEIGHT_FORMAT.getMsg());
                }
            }
            String unit = ExcelUtils.getCellValue(row.getCell(5));
            if (StringUtils.isNotEmpty(unit)) {
                if (ToolUtil.isEmpty(Constant.ALERT_RULE_UNIT_MAP.get(unit))) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(5), RCode.CHART_UNIT_NOTFOUND.getMsg());
                }
            }
            String param = ExcelUtils.getCellValue(row.getCell(6));
            if(typeFlag){
                String msg = this.validateChartParamForImport(type, param, "dashboard");
                if(StringUtils.isNotEmpty(msg)){
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(6), msg);
                }
            }
            String elements = ExcelUtils.getCellValue(row.getCell(7));
            if(typeFlag){
                String msg = this.validateChartElementForImport(type, elements);
                if(StringUtils.isNotEmpty(msg)){
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(7), msg);
                }
            }
            if(saveFlag){
                TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
                try {
                    Integer panelId = this.handlePanelNameForCellVal(nodeNamePathAndIdMap, panelName, seqUUID, ++panelMaxWeight);
                    Chart chart = new Chart();
                    chart.setSeq(seqUUID);
                    chart.setPanelId(panelId);
                    chart.setTitle(title);
                    chart.setType(type);
                    chart.setSpan(Double.valueOf(span).intValue());
                    chart.setHeight(Double.valueOf(height).intValue());
                    // unit 为空 设置默认值为 short
                    unit = StringUtils.isEmpty(unit) ? "short" : unit;
                    chart.setUnit(Constant.ALERT_RULE_UNIT_MAP.get(unit));
                    chart.setParam(param.toString());
                    chart.setCreateAt(DateUtil.getUTCTimeByConfigTimeZone());
                    // 查询当前 panel 下的 charts
                    List<Chart> chartList = this.list(new QueryWrapper<Chart>().lambda().eq(Chart::getPanelId, chart.getPanelId()));
                    if (CollectionUtils.isEmpty(chartList)) {
                        this.save(chart);
                    } else {
                        Chart endChart = this.getOne(new LambdaQueryWrapper<Chart>().eq(Chart::getPanelId, chart.getPanelId()).orderByDesc(Chart::getWeight).last("limit 0,1"));
                        Integer weight = endChart.getWeight();
                        chart.setWeight(++weight);
                        this.save(chart);
                    }
                    List<ChartElement> chartElements = JSONArray.parseArray(elements, ChartElement.class);
                    if (CollectionUtils.isNotEmpty(chartElements)) {
                        chartElements.forEach(chartElement -> {
                            chartElement.setChartId(chart.getId());
                            chartElement.setSeq(seqUUID);
                            chartElement.setType(Constant.CHART_ELEMENT_EXPERT_TYPE);
                        });
                        chartElementService.saveBatch(chartElements);
                    }
                    dataSourceTransactionManager.commit(transactionStatus);
                    // panelNameAndIdMap.put(panelName, panelId);
                    successNum++;
                } catch (RuntimeException e) {
                    dataSourceTransactionManager.rollback(transactionStatus);
                    this.addFailDetail(dataFromExcelRowNum, failDetails, "System", e.getMessage() + "");
                    log.error(String.format("保存 panel chart chartelement失败,回滚事务,错误信息:%s", e));
                }
            }
        }
        return successNum;
    }

    public void getPanelNamePathAndIdMapFromTree(Panel panel, String path, Map<String, Integer> nodeNamePathAndIdMap) {
        if (CollectionUtils.isEmpty(panel.getChildren())) {
            return;
        }
        for (Panel cPanel : panel.getChildren()) {
            String cPath = "";
            cPath += path + "/" + cPanel.getName();
            nodeNamePathAndIdMap.put(cPath, cPanel.getId());
            this.getPanelNamePathAndIdMapFromTree(cPanel, cPath, nodeNamePathAndIdMap);
        }
    }

    /**
     * @param panelName 格式： 使用 / 作为字符分隔，表示 父子节点关系
     * @param seqUUID   导入序列号
     */
    private Integer handlePanelNameForCellVal(Map<String, Integer> nodeNamePathAndIdMap, String panelName, String seqUUID, Integer panelWeight) {
        String savePanelNames = panelName;
        Integer panelId;
        while (true) {
            panelId = nodeNamePathAndIdMap.get(panelName);
            if (panelId != null) {
                break;
            }
            // 仍然包含 ‘/’ 证明还是有父子关系
            if (panelName.lastIndexOf("/") != -1) {
                panelName = panelName.substring(0, panelName.lastIndexOf("/"));
            } else {
                // 最后的 panel 也不存在，赋值 为空 ，下面会进行过滤添加
                panelName = "";
                break;
            }
        }
        savePanelNames = savePanelNames.replace(panelName, "");

        // 如果存在需要保存的 panel
        if (StringUtils.isNotEmpty(savePanelNames)) {
            if (savePanelNames.startsWith("/")) {
                savePanelNames = savePanelNames.substring(savePanelNames.indexOf("/") + 1);
            }

            Panel panel = new Panel();
            panel.setType(Constant.PanelType.DASHBOARD.getValue());
            panel.setCreateBy(ShiroUtils.getUserId().intValue());
            panel.setPrev(Constant.START_PREV_VALUE);
            panel.setNext(Constant.END_NEXT_VALUE);
            panel.setSeq(seqUUID);
            panel.setWeight(panelWeight);

            List<String> saveList = Arrays.asList(savePanelNames.split("/"));
            int size = saveList.size();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < size; i++) {
                String name = saveList.get(i);
                panel.setName(name);
                if (panel.getId() == null) {
                    panel.setPid(panelId);
                } else {
                    panel.setPid(panel.getId());
                }
                panelService.save(panel);
                sb.append(name + "/");
                String temp = panelName + "/" + sb.substring(0, sb.lastIndexOf("/"));
                // 因为用户输入根节点没有 ‘/’，所以不保存根节点 '/' 路径作为对比
                if (temp.startsWith("/")) {
                    temp = temp.substring(temp.indexOf("/") + 1);
                }
                nodeNamePathAndIdMap.put(temp, panel.getId());
            }
            return panel.getId();
        }
        return panelId;
    }

    private String validateChartTypeForImport(String type, Map<String,String> chartTypeMapByLanguage) {
        for (Map.Entry<String, String> entry : chartTypeMapByLanguage.entrySet()) {
            if(StrUtil.equals(type,entry.getValue())){
                return entry.getKey();
            }
        }
        return null;
    }

    private String validateChartElementForImport(String type, String elements) {
        // elemrnt 只有 alert  和 url , text 可以没有，其他必须有
        if(!StrUtil.equals(Constant.CHART_ALERTLIST_TYPE,type) && !StrUtil.equals(Constant.CHART_URL_TYPE,type) && !StrUtil.equals(Constant.CHART_TEXT_TYPE,type)){
            if(StringUtils.isEmpty(elements)){
                return RCode.CHART_ELEMENT_ISNULL.getMsg();
            }
            List<ChartElement> elementsList = null;
            try {
                elementsList = JSONArray.parseArray(elements, ChartElement.class);
            } catch (JSONException e) {
                return RCode.CHART_ELEMENTS_FORMAT.getMsg();
            }

            if(CollectionUtils.isEmpty(elementsList)){
                return RCode.CHART_ELEMENT_ISNULL.getMsg();
            }

            for (ChartElement element : elementsList) {
                if(ToolUtil.isEmpty(element.getExpression())){
                    return RCode.CHARTELEMENT_EXPRESSION_ISNULL.getMsg();
                }
            }
        }
        return "";
    }

    private String validateChartParamForImport(String type, String param, String panelType) {
       switch (type){
           // url
           case Constant.CHART_URL_TYPE: {
               if (StringUtils.isEmpty(param)) {
                   return RCode.CHART_URLPARAM_ISNULL.getMsg();
               }
               Map map = null;
               try {
                   map = JSONObject.parseObject(param, Map.class);
               } catch (JSONException e) {
                   return RCode.CHART_PARAM_FORMAT.getMsg();
               }
               Object url = map.get(Constant.CHART_URL_TYPE);
               if (ToolUtil.isEmpty(url)) {
                   return RCode.CHART_PARAMURL_ISNULL.getMsg();
               }

               if(StrUtil.equals(Constant.PanelType.MODEL.getValue(),panelType)) {
                   // 替换全部变量 替换为 a（任意字符） 不影响原来的数据格式 然后进行校验
                   String urlStr = url.toString();
                   urlStr = urlStr.replaceAll("\\{\\{asset\\..*?\\}\\}", "a");

                   boolean b = CommonUtils.checkUrlInModel(urlStr);
                   boolean b1 = CommonUtils.checkUrl(urlStr);

                   if (!b && !b1) {
                       return RCode.CHART_PARAMURL_FORMAT.getMsg();
                   }
               } else {
                   if (!CommonUtils.checkUrl(url.toString())) {
                       return RCode.CHART_PARAMURL_FORMAT.getMsg();
                   }
               }
               break;
           }
           // singleStat
           case Constant.CHART_SINGLESTAT_TYPE: {
               if (StringUtils.isEmpty(param)) {
                   return RCode.CHART_SINGLESTATPARAM_ISNULL.getMsg();
               }
               Map map = null;
               try {
                   map = JSONObject.parseObject(param, Map.class);
               } catch (JSONException e) {
                   return RCode.CHART_PARAM_FORMAT.getMsg();
               }
               Object statistics = map.get(Constant.CHART_SINGLESTAT_PARAMkEY);
               if (ToolUtil.isEmpty(statistics)) {
                   return RCode.CHART_PARAMSINGLESTAT_ISNULL.getMsg();
               }
               if (!Constant.CHART_SINGLESTAT_STATISTICS.contains(statistics.toString())) {
                   return RCode.CHART_PARAMSINGLESTAT_INVALIDE.getMsg();
               }
               break;
           }
           case Constant.CHART_TEXT_TYPE: {
               if (StringUtils.isEmpty(param)) {
                   return RCode.CHART_TEXTPARAM_ISNULL.getMsg();
               }
               Map map = null;
               try {
                   map = JSONObject.parseObject(param, Map.class);
               } catch (JSONException e) {
                   return RCode.CHART_PARAM_FORMAT.getMsg();
               }
               Object text = map.get(Constant.CHART_TEXT_TYPE);
               if (ToolUtil.isEmpty(text)) {
                   return RCode.CHART_PARAMTEXT_ISNULL.getMsg();
               }
               break;
           }
           default:{
               if(StringUtils.isNotEmpty(param)){
                   try {
                       JSONObject.parseObject(param, Map.class);
                   } catch (JSONException e) {
                       return RCode.CHART_PARAM_FORMAT.getMsg();
                   }
               }
           }
       }
       return "";
    }

    private void addFailDetail(int i, List<Map> failDetails, String headerInfo, String msg) {
        Map failDetail = new HashMap(4);
        failDetail.put("lineNo", i);
        failDetail.put("errorMsg", headerInfo + ": " + msg);
        failDetails.add(failDetail);
    }

    private void fillDashboardChartData(List<Chart> charts, int rowNum, SXSSFSheet sheet, SXSSFWorkbook workbook, Map chartTypeMapByLanguage) {
        List<Panel> panelTreeList = panelService.queryList(new HashMap());
        Map<Integer, String> nodeIdAndNamePathMap = new HashMap<>();
        for (Panel panel : panelTreeList) {
            nodeIdAndNamePathMap.put(panel.getId(), panel.getName());
            this.getPanelIdAndNamePathMapFromTree(panel, panel.getName(), nodeIdAndNamePathMap);
        }

        Row row;
        String type;
        CellStyle dataStyle = ExcelUtils.createCellStyle(workbook, (short) 13, false);
        for (Chart chart : charts) {
            if(chart == null) continue;
            row = sheet.createRow(rowNum++);
             type = chart.getType();
            row.createCell(0).setCellValue(nodeIdAndNamePathMap.get(chart.getPanelId()));
            row.createCell(1).setCellValue(chart.getTitle());
            row.createCell(2).setCellValue(this.getI18nChartTypeValue(chartTypeMapByLanguage, type));
            row.createCell(3).setCellValue(chart.getSpan());
            row.createCell(4).setCellValue(chart.getHeight());
            if(StrUtil.equals(Constant.CHART_TEXT_TYPE,type)) {
                row.createCell(5).setCellValue("");
            } else {
                row.createCell(5).setCellValue(this.getUnitDescrByVal(chart.getUnit()));
            }
            row.createCell(6).setCellValue(this.handleChartParam(chart.getParam()));
            row.createCell(7).setCellValue(this.handleChartElementForExport(chart.getElements()));

            // 设置单元格样式
            ExcelUtils.setDataCellStyle(dataStyle, row, 7);
        }
    }

    public void getPanelIdAndNamePathMapFromTree(Panel panel, String path, Map<Integer, String> nodeIdAndNamePathMap) {
        if (CollectionUtils.isEmpty(panel.getChildren())) {
            return;
        }
        for (Panel cPanel : panel.getChildren()) {
            String cPath = "";
            cPath += path + "/" + cPanel.getName();
            nodeIdAndNamePathMap.put(cPanel.getId(), cPath);
            this.getPanelIdAndNamePathMapFromTree(cPanel, cPath, nodeIdAndNamePathMap);
        }
    }

    private String handleChartParam(Object param) {
        if (ToolUtil.isEmpty(param)) {
            return "";
        }
        Map<String, Object> map = JSONObject.parseObject(param.toString(), Map.class);
        map.entrySet().removeIf(entry -> ToolUtil.isEmpty(entry.getValue()));
        return ToolUtil.isEmpty(map) ? "" : JSONObject.toJSONString(map);
    }

    private String handleChartElementForExport(List<ChartElement> chartElements) {
        if (CollectionUtils.isEmpty(chartElements)) {
            return "";
        }
        List<Map> handleMaps = new ArrayList<>();
        Map tempMap;
        for (ChartElement element : chartElements) {
            tempMap = new LinkedHashMap(4);
            tempMap.put("expression", element.getExpression());
            if(ToolUtil.isNotEmpty(element.getLegend())){
                tempMap.put("legend", element.getLegend());
            }
            handleMaps.add(tempMap);
        }
        return JSONArray.toJSONString(handleMaps);
    }

    private String getUnitDescrByVal(Integer unitVal) {
        String unitDescr = null;
        for (Map.Entry<String, Integer> entry : Constant.ALERT_RULE_UNIT_MAP.entrySet()) {
            if (entry.getValue().equals(unitVal)) {
                unitDescr = entry.getKey();
                break;
            }
        }
        return unitDescr;
    }

    private List<Chart> exportDataByParams(Map<String, Object> params) {
        Integer panelId = null;
        if (ToolUtil.isNotEmpty(params.get("panelId"))) {
            panelId = Integer.valueOf(params.get("panelId").toString());
        }
        Integer chartId = null;
        if (ToolUtil.isNotEmpty(params.get("chartId"))) {
            chartId = Integer.valueOf(params.get("chartId").toString());
        }
        List<Chart> chartList = new ArrayList<>();
        if (panelId == null) {
            // 导出所有 dashboard panel
            List<Panel> panelList = panelService.list(new QueryWrapper<Panel>().lambda().eq(Panel::getType, Constant.PanelType.DASHBOARD.getValue()).orderByAsc(Panel::getWeight, Panel::getId));
           // panelList = panelService.getSequentialPanelList(panelList);
            for (Panel panel : panelList) {
                chartList.addAll(this.queryChartsByPanelId(panel.getId(),null));
            }
        } else {
            if (chartId != null) {
                // 只导出当前 chart
                chartList.addAll(this.queryChartsByPanelId(panelId, chartId));
            } else {
                // 导出 panel 下所有chart
                chartList.addAll(this.queryChartsByPanelId(panelId, null));
            }
        }
        return chartList;
    }

    public List<Chart> queryChartsByPanelId(Integer panelId,Integer chartId) {
        List<Chart> allCharts = chartDao.queryCharts(panelId, chartId, null);
       // allCharts = this.getSequentialChartList(allCharts);
        return allCharts;
    }

    private void copyChartsToAssetChartsByModelPanel(Panel modelPanel, Panel targetPanel, Asset asset) {
        // List<Chart> charts = this.list(new QueryWrapper<Chart>().lambda().eq(Chart::getPanelId, modelPanel.getId()));
        List<Chart> charts = chartDao.queryCharts(modelPanel.getId(), null, null);
        // 排序
        //charts = this.getSequentialChartList(charts);

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
            chart.setElements(Collections.emptyList());
            // 设置关联 panel 为当前新增 asset 关联的 panel
            this.saveChart(targetPanel.getId(), chart);
            saveElements.put(chart.getId(), elementList == null ? new ArrayList<>() : elementList);
        }

        Integer chartId;
        for (Map.Entry<Integer, List<ChartElement>> entry : saveElements.entrySet()) {
            chartId = entry.getKey();
            for (ChartElement chartElement : entry.getValue()) {
                chartElement.setId(null);
                chartElement.setChartId(chartId);
                chartElement.setExpression(ToolUtil.isNotEmpty(chartElement.getExpression()) ? this.replacAssetInfos(chartElement.getExpression(), asset) : null);
            }
        }
        // 保存 element
        List<ChartElement> elements = saveElements.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        chartElementService.saveBatch(elements);
    }

    private static Pattern PATTERN = Pattern.compile(".*\\{\\{asset.tag.\\[.*\\]\\}\\}.*");

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
        List<AssetTag> tags = asset.getTags();
        if(CollectionUtils.isNotEmpty(tags)){
            for (AssetTag tag : tags) {
                if(PATTERN.matcher(str).matches()){
                    str = str.replaceAll("\\{\\{asset.tag.\\[" + StringUtils.escapeString(tag.getTag()) + "\\]\\}\\}", tag.getValue());
                }
                str = str.replaceAll("\\{\\{asset.tag." + tag.getTag() + "\\}\\}", tag.getValue());
            }
        }
        return str;
    }
}