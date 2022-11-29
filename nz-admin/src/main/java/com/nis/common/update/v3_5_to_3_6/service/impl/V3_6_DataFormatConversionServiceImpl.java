package com.nis.common.update.v3_5_to_3_6.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nis.common.update.v3_5_to_3_6.dao.V3_5_AssetModelDao;
import com.nis.common.update.v3_5_to_3_6.service.V3_6_DataFormatConversionService;
import com.nis.common.utils.Constant;
import com.nis.common.utils.JSONUtil;
import com.nis.common.utils.Tool;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetModel;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetModelService;
import com.nis.modules.panel.entity.VisualChart;
import com.nis.modules.panel.entity.VisualChartElement;
import com.nis.modules.panel.entity.VisualPanel;
import com.nis.modules.panel.service.VisualChartElementService;
import com.nis.modules.panel.service.VisualChartService;
import com.nis.modules.panel.service.VisualPanelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class V3_6_DataFormatConversionServiceImpl implements V3_6_DataFormatConversionService {

    private static final Log log = Log.get();

    /**
     * back up table name
     */
    private static final String BACK_UP_TABLE_NAME = "asset_model_V3_6";

    @Autowired
    private VisualPanelService panelService;

    @Autowired
    private VisualChartService chartService;

    @Autowired
    private VisualChartElementService chartElementService;

    @Autowired
    private AssetAssetService assetService;

    @Autowired
    private AssetModelService assetModelService;


    @Autowired
    private V3_5_AssetModelDao v3_5_assetModelDao;

    /**
     * before Data Format Conversion
     * check backup table is exists
     *
     * @return
     */
    private boolean beforeDataFormatConversion() {
        Map tableInfo = v3_5_assetModelDao.checkTableExists(BACK_UP_TABLE_NAME);
        tableInfo = Tool.MapUtil.defaultIfEmpty(tableInfo, Tool.MapUtil.empty());
        log.info("[beforeDataFormatConversion] [check the backup table exists] [size: {}] [info: {}]", tableInfo.size(), JSONUtil.toJsonStr(tableInfo));

        if (Tool.MapUtil.isEmpty(tableInfo)) {
            log.warn("[beforeDataFormatConversion] [The backup table does not exist. Do not perform data conversion] [table: {}]", BACK_UP_TABLE_NAME);
            return false;
        }

        log.info("[beforeDataFormatConversion] [The backup table exists, execute data conversion] [table: {}]", BACK_UP_TABLE_NAME);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dataFormatConversion() {
        boolean isConversion = this.beforeDataFormatConversion();
        if (!isConversion) {
            return;
        }

        // conversion Asset Model Chart Data
        log.info("[dataFormatConversion] [conversion Asset Model Chart Data]");
        List<AssetModel> backUpAssetModelList = v3_5_assetModelDao.queryList();
        this.conversionAssetModelChartData(backUpAssetModelList);

        // remove old chart template
        log.info("[dataFormatConversion] [remove old chart template]");
        chartService.delVisualChartsByPanelId(0);

        // remove back up table
        log.info("[dataFormatConversion] [remove back up table]");
        v3_5_assetModelDao.dropTable(BACK_UP_TABLE_NAME);
    }

    /**
     * conversion Asset Model Chart Data
     *
     * @param backUpAssetModelList
     */
    private void conversionAssetModelChartData(List<AssetModel> backUpAssetModelList) {
        // save chart element
        List<VisualChartElement> saveBatchChartElementList = Tool.ListUtil.list(true);
        for (AssetModel backUpAssetModel : backUpAssetModelList) {
            // model id
            Integer modelId = backUpAssetModel.getId();
            // chart id list
            String chartIds = backUpAssetModel.getChartIds();
            List<String> chartIdList = Tool.StrUtil.split(Tool.StrUtil.emptyToDefault(chartIds, Tool.StrUtil.EMPTY), Tool.StrUtil.C_COMMA);

            log.info("[conversionAssetModelChartData] [conversion data begin.] [model: {}]", backUpAssetModel.getId() + "-" + backUpAssetModel.getName());
            log.info("[conversionAssetModelChartData] [asset model chart list] [size: {}] [info: {}]", chartIdList.size(), JSONUtil.toJsonStr(chartIdList));

            if (Tool.CollUtil.isEmpty(chartIdList)) {
                log.warn("[conversionAssetModelChartData] [no config chart. no conversion]");
            } else {
                // add dashboard template
                VisualPanel templateTypeDashboard = this.addTemplateTypeDashboard(backUpAssetModel);
                Integer panelId = templateTypeDashboard.getId();

                List<VisualChart> chartList = chartService.list(new LambdaQueryWrapper<VisualChart>()
                        .eq(VisualChart::getPanelId, 0)
                        .and(wrapper -> wrapper.in(VisualChart::getId, chartIdList)
                                .or()
                                .in(VisualChart::getGroupId, chartIdList))
                );
                log.info("[conversionAssetModelChartData] [conversion data] [model: {}] [chart size: {}]", backUpAssetModel.getName(), chartList.size());

                // assets panel id list
                List<Integer> assetPanelIdListByTheModel = this.getAssetPanelListByTheModel(modelId);
                // group chart
                List<VisualChart> groupChartList = chartList.stream().filter(chart -> Tool.StrUtil.equals(Constant.CHART_GROUP_TYPE, chart.getType())).collect(Collectors.toList());
                Map<Integer, Integer> groupChartIdMapping = Tool.MapUtil.newHashMap();
                for (VisualChart groupChart : groupChartList) {
                    VisualChart tempGroupChart = new VisualChart();
                    tempGroupChart = ObjectUtil.clone(groupChart);

                    tempGroupChart.setId(null);
                    tempGroupChart.setPanelId(panelId);

                    // save group chart
                    chartService.save(tempGroupChart);
                    // save mapping
                    groupChartIdMapping.put(groupChart.getId(), tempGroupChart.getId());
                    // update chart pid val.  only handle asset chart
                    this.updateAssetChartTemplateLinkValByTheModel(tempGroupChart.getId(), assetPanelIdListByTheModel, groupChart.getId());
                }

                // other chart
                List<VisualChart> otherChartList = chartList.stream().filter(chart -> !Tool.StrUtil.equals(Constant.CHART_GROUP_TYPE, chart.getType())).collect(Collectors.toList());
                for (VisualChart chart : otherChartList) {
                    VisualChart tempChart = new VisualChart();
                    tempChart = ObjectUtil.clone(chart);

                    tempChart.setId(null);
                    tempChart.setPanelId(panelId);
                    // new group id
                    Integer groupId = groupChartIdMapping.get(tempChart.getGroupId());
                    tempChart.setGroupId(ObjectUtil.defaultIfNull(groupId, 0));

                    // save chart
                    chartService.save(tempChart);
                    List<VisualChartElement> elementList = chartElementService.list(new LambdaQueryWrapper<VisualChartElement>().eq(VisualChartElement::getChartId, chart.getId()));
                    for (VisualChartElement e : elementList) {
                        e.setId(null);
                        e.setChartId(tempChart.getId());
                    }
                    saveBatchChartElementList.addAll(elementList);
                    // update chart pid val.  only handle asset chart
                    this.updateAssetChartTemplateLinkValByTheModel(tempChart.getId(), assetPanelIdListByTheModel, chart.getId());
                }
                // set panel id
                backUpAssetModel.setPanelId(panelId);
                // update asset dashboard temp_id
                this.updateAssetDashboardTempIdByTheModel(backUpAssetModel, assetPanelIdListByTheModel, panelId);
            }
            // save model
            assetModelService.save(backUpAssetModel);
            log.info("[conversionAssetModelChartData] [conversion data finshed.] [model: {}]", backUpAssetModel.getId() + "-" + backUpAssetModel.getName());
        }
        // save chart element
        this.saveTheListOfNewChartElement(saveBatchChartElementList);
    }

    /**
     * save Template Type Dashboard
     *
     * @param backUpAssetModel
     * @return
     */
    private VisualPanel addTemplateTypeDashboard(AssetModel backUpAssetModel) {
        // panel name
        String panelName = Tool.StrUtil.concat(true, backUpAssetModel.getName(), Tool.StrUtil.SPACE, "template");
        List<VisualPanel> panelListByTempName = panelService.list(new LambdaQueryWrapper<VisualPanel>().eq(VisualPanel::getName, panelName));
        if (Tool.CollUtil.isNotEmpty(panelListByTempName)) {
            // 重复就拼上 4位 随机字符
            panelName = Tool.StrUtil.concat(true, panelName, "-", Tool.RandomUtil.randomString(4));
        }

        VisualPanel panel = new VisualPanel();
        panel.setName(panelName);
        panel.setType(Constant.PanelType.TEMPLATE.getValue());
        panel.setPid(0);
        panel.setWeight(0);
        panel.setBuildIn(0);

        String initParamStr = "{\"chartShare\":\"none\",\"variables\":[],\"report\":{\"schedule\":{\"repeat\":1,\"etime\":\"\",\"stime\":\"\",\"type\":2,\"nums\":[]},\"receivers\":[],\"enable\":false,\"range\":{\"unit\":\"day\",\"interval\":1,\"type\":\"previous\"}}}";
        panel.setParam(initParamStr);

        panel.setVarType(Constant.ChartVariableType.ASSET.getValue());
        panel.setTmplId(-1);

        panel.setCreateBy(-1);
        panel.setUts(System.currentTimeMillis());

        // save
        panelService.save(panel);
        return panel;
    }


    /**
     * get Asset Panel ID List By The Model
     *
     * @param modelId
     * @return
     */
    private List<Integer> getAssetPanelListByTheModel(Integer modelId) {
        List<AssetAsset> assetList = assetService.list(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getModelId, modelId));
        if (Tool.CollUtil.isEmpty(assetList)) {
            return Tool.ListUtil.empty();
        }

        List<Integer> assetIdList = assetList.stream().map(AssetAsset::getId).collect(Collectors.toList());
        List<VisualPanel> panelList = panelService.list(new LambdaQueryWrapper<VisualPanel>()
                .eq(VisualPanel::getType, Constant.PanelType.ASSET.getValue())
                .in(VisualPanel::getLink, assetIdList));

        List<Integer> panelIdList = panelList.stream().map(VisualPanel::getId).collect(Collectors.toList());
        return panelIdList;
    }


    /**
     * update Asset Chart Template Link Val By The Model
     *
     * @param newChartPid
     * @param assetPanelIdList
     * @param oldChartPid
     */
    private void updateAssetChartTemplateLinkValByTheModel(Integer newChartPid, List<Integer> assetPanelIdList, Integer oldChartPid) {
        if (Tool.CollUtil.isNotEmpty(assetPanelIdList)) {
            LambdaUpdateWrapper<VisualChart> wrapper = new LambdaUpdateWrapper();
            // new pid
            wrapper.set(VisualChart::getPid, newChartPid);
            // old pid
            wrapper.eq(VisualChart::getPid, oldChartPid)
                    .in(VisualChart::getPanelId, assetPanelIdList);
            chartService.update(wrapper);
        }
    }


    /**
     * update Asset Dashboard Temp Id By The Model
     *
     * @param model
     * @param assetPanelIdList
     * @param templateDashboardId
     */
    private void updateAssetDashboardTempIdByTheModel(AssetModel model, List<Integer> assetPanelIdList, Integer templateDashboardId) {
        if (Tool.CollUtil.isEmpty(assetPanelIdList)) {
            log.warn("[updateAssetDashboardTempIdByTheModel] [no reference asset dashboard in this model. not update temp_id] [model info: {}]", model.getId() + "-" + model.getName());
        } else {
            log.info("[updateAssetDashboardTempIdByTheModel] [model info: {}] [tempId: {}]", model.getId() + "-" + model.getName(), templateDashboardId);
            LambdaUpdateWrapper<VisualPanel> wrapper = new LambdaUpdateWrapper();
            wrapper.set(VisualPanel::getTmplId, templateDashboardId);
            wrapper.in(VisualPanel::getId, assetPanelIdList);
            panelService.update(wrapper);
        }
    }

    /**
     * save The List Of New Chart Element
     *
     * @param chartElementList
     */
    private void saveTheListOfNewChartElement(List<VisualChartElement> chartElementList) {
        log.info("[saveTheListOfNewChartElement] [size: {}]", chartElementList.size());
        if (Tool.CollUtil.isNotEmpty(chartElementList)) {
            chartElementService.saveBatch(chartElementList);
        }
    }

}
