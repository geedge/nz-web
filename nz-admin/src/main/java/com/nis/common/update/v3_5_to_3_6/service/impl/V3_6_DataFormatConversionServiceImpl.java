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
import com.nis.modules.dashboard.entity.VisualChart;
import com.nis.modules.dashboard.entity.VisualChartElement;
import com.nis.modules.dashboard.entity.VisualDashboard;
import com.nis.modules.dashboard.service.VisualChartElementService;
import com.nis.modules.dashboard.service.VisualChartService;
import com.nis.modules.dashboard.service.VisualDashboardService;
import com.nis.modules.dashboard.utils.DashboardConstant;
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
    private VisualDashboardService dashboardService;

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
        chartService.delVisualChartsByDashboardId(0);

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
                VisualDashboard templateTypeDashboard = this.addTemplateTypeDashboard(backUpAssetModel);
                Integer dashboardId = templateTypeDashboard.getId();

                List<VisualChart> chartList = chartService.list(new LambdaQueryWrapper<VisualChart>()
                        .eq(VisualChart::getDashboardId, 0)
                        .and(wrapper -> wrapper.in(VisualChart::getId, chartIdList)
                                .or()
                                .in(VisualChart::getGroupId, chartIdList))
                );
                log.info("[conversionAssetModelChartData] [conversion data] [model: {}] [chart size: {}]", backUpAssetModel.getName(), chartList.size());

                // assets dashboard id list
                List<Integer> assetDashboardIdListByTheModel = this.getAssetDashboardListByTheModel(modelId);
                // group chart
                List<VisualChart> groupChartList = chartList.stream().filter(chart -> Tool.StrUtil.equals(Constant.CHART_GROUP_TYPE, chart.getType())).collect(Collectors.toList());
                Map<Integer, Integer> groupChartIdMapping = Tool.MapUtil.newHashMap();
                for (VisualChart groupChart : groupChartList) {
                    VisualChart tempGroupChart = new VisualChart();
                    tempGroupChart = ObjectUtil.clone(groupChart);

                    tempGroupChart.setId(null);
                    tempGroupChart.setDashboardId(dashboardId);

                    // save group chart
                    chartService.save(tempGroupChart);
                    // save mapping
                    groupChartIdMapping.put(groupChart.getId(), tempGroupChart.getId());
                    // update chart pid val.  only handle asset chart
                    this.updateAssetChartTemplateLinkValByTheModel(tempGroupChart.getId(), assetDashboardIdListByTheModel, groupChart.getId());
                }

                // other chart
                List<VisualChart> otherChartList = chartList.stream().filter(chart -> !Tool.StrUtil.equals(Constant.CHART_GROUP_TYPE, chart.getType())).collect(Collectors.toList());
                for (VisualChart chart : otherChartList) {
                    VisualChart tempChart = new VisualChart();
                    tempChart = ObjectUtil.clone(chart);

                    tempChart.setId(null);
                    tempChart.setDashboardId(dashboardId);
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
                    this.updateAssetChartTemplateLinkValByTheModel(tempChart.getId(), assetDashboardIdListByTheModel, chart.getId());
                }
                // set dashboard id
                backUpAssetModel.setDashboardId(dashboardId);
                // update asset dashboard temp_id
                this.updateAssetDashboardTempIdByTheModel(backUpAssetModel, assetDashboardIdListByTheModel, dashboardId);
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
    private VisualDashboard addTemplateTypeDashboard(AssetModel backUpAssetModel) {
        // dashboard name
        String dashboardName = Tool.StrUtil.concat(true, backUpAssetModel.getName(), Tool.StrUtil.SPACE, "template");
        List<VisualDashboard> dashboardListByTempName = dashboardService.list(new LambdaQueryWrapper<VisualDashboard>().eq(VisualDashboard::getName, dashboardName));
        if (Tool.CollUtil.isNotEmpty(dashboardListByTempName)) {
            // 重复就拼上 4位 随机字符
            dashboardName = Tool.StrUtil.concat(true, dashboardName, "-", Tool.RandomUtil.randomString(4));
        }

        VisualDashboard dashboard = new VisualDashboard();
        dashboard.setName(dashboardName);
        dashboard.setType(DashboardConstant.Type.TEMPLATE.getValue());
        dashboard.setPid(0);
        dashboard.setWeight(0);
        dashboard.setBuildIn(0);

        String initParamStr = "{\"chartShare\":\"none\",\"variables\":[],\"report\":{\"schedule\":{\"repeat\":1,\"etime\":\"\",\"stime\":\"\",\"type\":2,\"nums\":[]},\"receivers\":[],\"enable\":false,\"range\":{\"unit\":\"day\",\"interval\":1,\"type\":\"previous\"}}}";
        dashboard.setParam(initParamStr);

        dashboard.setVarType(Constant.ChartVariableType.ASSET.getValue());
        dashboard.setTmplId(-1);

        dashboard.setCreateBy(-1);
        dashboard.setUts(System.currentTimeMillis());

        // save
        dashboardService.save(dashboard);

        // add Link Object Info Chart
        dashboardService.addLinkObjectInfoChart(dashboard);
        return dashboard;
    }


    /**
     * get Asset Dashboard ID List By The Model
     *
     * @param modelId
     * @return
     */
    private List<Integer> getAssetDashboardListByTheModel(Integer modelId) {
        List<AssetAsset> assetList = assetService.list(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getModelId, modelId));
        if (Tool.CollUtil.isEmpty(assetList)) {
            return Tool.ListUtil.empty();
        }

        List<Integer> assetIdList = assetList.stream().map(AssetAsset::getId).collect(Collectors.toList());
        List<VisualDashboard> dashboardList = dashboardService.list(new LambdaQueryWrapper<VisualDashboard>()
                .eq(VisualDashboard::getType, DashboardConstant.Type.ASSET.getValue())
                .in(VisualDashboard::getLink, assetIdList));

        List<Integer> dashboardIdList = dashboardList.stream().map(VisualDashboard::getId).collect(Collectors.toList());
        return dashboardIdList;
    }


    /**
     * update Asset Chart Template Link Val By The Model
     *
     * @param newChartPid
     * @param assetDashboardIdList
     * @param oldChartPid
     */
    private void updateAssetChartTemplateLinkValByTheModel(Integer newChartPid, List<Integer> assetDashboardIdList, Integer oldChartPid) {
        if (Tool.CollUtil.isNotEmpty(assetDashboardIdList)) {
            LambdaUpdateWrapper<VisualChart> wrapper = new LambdaUpdateWrapper();
            // new pid
            wrapper.set(VisualChart::getPid, newChartPid);
            // old pid
            wrapper.eq(VisualChart::getPid, oldChartPid)
                    .in(VisualChart::getDashboardId, assetDashboardIdList);
            chartService.update(wrapper);
        }
    }


    /**
     * update Asset Dashboard Temp Id By The Model
     *
     * @param model
     * @param assetDashboardIdList
     * @param templateDashboardId
     */
    private void updateAssetDashboardTempIdByTheModel(AssetModel model, List<Integer> assetDashboardIdList, Integer templateDashboardId) {
        if (Tool.CollUtil.isEmpty(assetDashboardIdList)) {
            log.warn("[updateAssetDashboardTempIdByTheModel] [no reference asset dashboard in this model. not update temp_id] [model info: {}]", model.getId() + "-" + model.getName());
        } else {
            log.info("[updateAssetDashboardTempIdByTheModel] [model info: {}] [tempId: {}]", model.getId() + "-" + model.getName(), templateDashboardId);
            LambdaUpdateWrapper<VisualDashboard> wrapper = new LambdaUpdateWrapper();
            wrapper.set(VisualDashboard::getTmplId, templateDashboardId);
            wrapper.in(VisualDashboard::getId, assetDashboardIdList);
            dashboardService.update(wrapper);
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
