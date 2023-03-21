package com.nis.common.update.v3_5_to_3_6.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.SqlConnRunner;
import cn.hutool.db.handler.EntityListHandler;
import cn.hutool.db.handler.StringHandler;
import cn.hutool.db.sql.SqlExecutor;
import cn.hutool.log.Log;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.update.DataFormatConversionInterface;
import com.nis.common.utils.Constant;
import com.nis.common.utils.JSONUtil;
import com.nis.common.utils.Tool;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * V3_6_DataFormatConversionServiceImpl
 *
 * @author shizhendong
 * @date 2023/03/15
 */
@Service
public class V3_6_DataFormatConversionService implements DataFormatConversionInterface {

    private static final Log log = Log.get();

    /**
     * back up table name
     */
    private static final String BACK_UP_TABLE_NAME = "asset_model_v3_6";

    private Connection connection;

    private SqlConnRunner sqlRunner;

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
            return false;
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

        // conversion Asset Model Chart Data
        log.info("[dataFormatConversion] [conversion Asset Model Chart Data]");
        this.conversionAssetModelChartData();

        // remove old chart template
        List<Entity> removeChartList = sqlRunner.findAll(this.connection, Entity.create("visual_chart").set("panel_id", 0));
        log.info("[dataFormatConversion] [remove old chart template] [size: {}]", removeChartList.size());

        List<String> removeChartIdList = removeChartList.stream().map(entity -> entity.getStr("id")).collect(Collectors.toList());
        if (Tool.CollUtil.isNotEmpty(removeChartList)) {
            sqlRunner.del(this.connection, Entity.create("visual_chart").set("id", removeChartIdList));
            sqlRunner.del(this.connection, Entity.create("visual_chart_element").set("chart_id", removeChartIdList));
        }

        // remove back up table
        log.info("[dataFormatConversion] [remove back up table] [table_name: {}]", BACK_UP_TABLE_NAME);
        SqlExecutor.execute(this.connection, Tool.StrUtil.concat(true, "DROP TABLE ", BACK_UP_TABLE_NAME));
    }

    /**
     * conversion Asset Model Chart Data
     */
    private void conversionAssetModelChartData() throws SQLException {
        log.info("[conversionAssetModelChartData] [begin]");

        // save chart element
        List<Entity> saveBatchChartElementList = Tool.ListUtil.list(true);

        List<Entity> backUpModelList = sqlRunner.findAll(this.connection, Entity.create("asset_model_v3_6"));
        log.info("[conversionAssetModelChartData] [conversion size: {}]", backUpModelList.size());
        for (Entity backUpAssetModel : backUpModelList) {
            // model id
            String modelId = backUpAssetModel.getStr("id");
            // model name
            String modelName = backUpAssetModel.getStr("name");
            // chart id list
            String chartIds = backUpAssetModel.getStr("chart_ids");

            log.info("[conversionAssetModelChartData] [conversion data begin.] [model id: {}]", modelId);
            if (Tool.StrUtil.isEmpty(chartIds)) {
                log.warn("[conversionAssetModelChartData] [no config chart. no conversion] [model id: {}]", modelId);
            } else {
                List<String> chartIdList = Tool.StrUtil.split(Tool.StrUtil.emptyToDefault(chartIds, Tool.StrUtil.EMPTY), Tool.StrUtil.C_COMMA);
                log.info("[conversionAssetModelChartData] [asset model chart list] [size: {}] [info: {}]", chartIdList.size(), JSONUtil.toJsonStr(chartIdList));

                // add panel template
                Long insertPanelId = this.addTemplateTypePanel(modelName);

                // model rel chart list
                List<Entity> chartList = sqlRunner.findAll(this.connection, Entity.create("visual_chart").set("panel_id", 0).set("id", chartIdList));
                List<Entity> chartListUnderGroup = sqlRunner.findAll(this.connection, Entity.create("visual_chart").set("panel_id", 0).set("group_id", chartIdList));
                chartList.addAll(chartListUnderGroup);

                // distinct
                chartList = chartList.stream().distinct().collect(Collectors.toList());
                log.info("[conversionAssetModelChartData] [conversion data] [model: {}] [chart size: {}]", modelName, chartList.size());

                // assets panel id list
                List<String> assetPanelIdListByTheModel = this.getAssetPanelListByTheModel(modelId);

                // group chart
                Map<String, String> groupChartIdMapping = Tool.MapUtil.newHashMap();
                List<Entity> groupChartList = chartList.stream().filter(chart -> Tool.StrUtil.equals(Constant.CHART_GROUP_TYPE, chart.getStr("type"))).collect(Collectors.toList());
                for (Entity groupChart : groupChartList) {
                    Entity tempGroupChart = ObjectUtil.clone(groupChart);
                    tempGroupChart.remove("id");
                    tempGroupChart.set("panel_id", insertPanelId);

                    // save group chart
                    Long insertTempGroupChartId = sqlRunner.insertForGeneratedKey(this.connection, tempGroupChart);
                    String insertTempGroupChartIdStr = StrUtil.toString(insertTempGroupChartId);

                    String groupChartId = groupChart.getStr("id");
                    // save mapping
                    groupChartIdMapping.put(groupChartId, insertTempGroupChartIdStr);
                    // update chart pid val.  only handle asset chart
                    this.updateAssetChartTemplateLinkValByTheModel(insertTempGroupChartIdStr, assetPanelIdListByTheModel, groupChartId);
                }

                // other chart
                List<Entity> otherChartList = chartList.stream().filter(chart -> !Tool.StrUtil.equals(Constant.CHART_GROUP_TYPE, chart.getStr("type"))).collect(Collectors.toList());
                for (Entity chart : otherChartList) {
                    Entity tempChart = ObjectUtil.clone(chart);

                    tempChart.remove("id");
                    tempChart.set("panel_id", insertPanelId);
                    // new group id
                    String groupId = groupChartIdMapping.get(tempChart.getStr("group_id"));
                    tempChart.set("group_id", StrUtil.emptyToDefault(groupId, "0"));

                    // save chart
                    Long insertTempChartId = sqlRunner.insertForGeneratedKey(this.connection, tempChart);
                    String insertTempChartIdStr = StrUtil.toString(insertTempChartId);

                    List<Entity> elementList = sqlRunner.findAll(this.connection, Entity.create("visual_chart_element").set("chart_id", chart.getStr("id")));
                    for (Entity e : elementList) {
                        e.remove("id");
                        e.set("chart_id", insertTempChartIdStr);
                    }
                    saveBatchChartElementList.addAll(elementList);
                    // update chart pid val.  only handle asset chart
                    this.updateAssetChartTemplateLinkValByTheModel(insertTempChartIdStr, assetPanelIdListByTheModel, chart.getStr("id"));
                }
                // set panel id
                backUpAssetModel.set("panel_id", insertPanelId);
                // update asset panel temp_id
                this.updateAssetPanelTempIdByTheModel(backUpAssetModel, assetPanelIdListByTheModel, insertPanelId);
            }
            // save model
            backUpAssetModel.setTableName("asset_model");
            Long insertBackUpAssetModelId = sqlRunner.insertForGeneratedKey(this.connection, backUpAssetModel);
            log.info("[conversionAssetModelChartData] [conversion data finshed.] [insert id: {}] [name: {}]", insertBackUpAssetModelId, backUpAssetModel.getStr("name"));
        }
        // save chart element
        this.saveTheListOfNewChartElement(saveBatchChartElementList);
        log.info("[conversionAssetModelChartData] [finshed]");
    }

    /**
     * save Template Type Panel
     * name format: 'model.name template'
     *
     * @param modelName
     * @return
     */
    private Long addTemplateTypePanel(String modelName) throws SQLException {
        // panel name
        String panelName = Tool.StrUtil.concat(true, modelName, Tool.StrUtil.SPACE, "template");
        List<Entity> panelListByTempName = SqlExecutor.query(this.connection, "SELECT * FROM visual_panel WHERE type = 'template' and name = ?", new EntityListHandler(), panelName);
        if (Tool.CollUtil.isNotEmpty(panelListByTempName)) {
            // 重复就拼上 4位 随机字符
            panelName = Tool.StrUtil.concat(true, panelName, "-", Tool.RandomUtil.randomString(4));
        }

        Entity panelEntity = Entity.create("visual_panel");
        panelEntity.set("name", panelName);
        panelEntity.set("type", "template");
        panelEntity.set("pid", 0);
        panelEntity.set("weight", 0);
        panelEntity.set("build_in", 0);

        String initParamStr = "{\"chartShare\":\"none\",\"variables\":[],\"report\":{\"schedule\":{\"repeat\":1,\"etime\":\"\",\"stime\":\"\",\"type\":2,\"nums\":[]},\"receivers\":[],\"enable\":false,\"range\":{\"unit\":\"day\",\"interval\":1,\"type\":\"previous\"}}}";
        panelEntity.set("param", initParamStr);

        panelEntity.set("var_type", Constant.ChartVariableType.ASSET.getValue());
        panelEntity.set("tmpl_id", -1);

        panelEntity.set("create_by", -1);
        panelEntity.set("uts", System.currentTimeMillis());

        // save
        Long insertPanelId = sqlRunner.insertForGeneratedKey(this.connection, panelEntity);

        // add Asset Info Chart
        String assetInfoTemplJsonStr = SqlExecutor.query(this.connection, "SELECT param_value FROM sys_config WHERE param_key = ?", new StringHandler(), Constant.SYSCONFIG_KEY_ASSET_CHART_TPL);

        Entity chartEntity = Entity.create("visual_chart");

        JSONObject chartTemplate = JSONObject.parseObject(assetInfoTemplJsonStr);
        for (Map.Entry<String, Object> entry : chartTemplate.entrySet()) {
            chartEntity.set(entry.getKey(), entry.getValue());
        }
        chartEntity.set("panel_id", insertPanelId);
        chartEntity.set("group_id", 0);
        chartEntity.set("weight", 0);
        chartEntity.set("datasource", Constant.ChartDatasourceType.SYSTEM.getValue());
        chartEntity.set("update_by", panelEntity.get("create_by"));
        chartEntity.set("update_at", new Date());

        // insert asset info chart
        sqlRunner.insert(this.connection, chartEntity);
        return insertPanelId;
    }


    /**
     * get Asset Panel ID List By The Model
     *
     * @param modelId
     * @return
     * @throws SQLException
     */
    private List<String> getAssetPanelListByTheModel(String modelId) throws SQLException {
        List<Entity> assetList = sqlRunner.findAll(this.connection, Entity.create("asset_asset").set("model_id", modelId));
        if (Tool.CollUtil.isEmpty(assetList)) {
            return Tool.ListUtil.empty();
        }

        List<String> assetIdList = assetList.stream().map(entity -> entity.getStr("id")).collect(Collectors.toList());

        List<Entity> assetPanelList = sqlRunner.findAll(this.connection, Entity.create("visual_panel").set("type", "asset").set("link", assetIdList));
        List<String> assetPanelIdList = assetPanelList.stream().map(entity -> entity.getStr("id")).collect(Collectors.toList());
        return assetPanelIdList;
    }


    /**
     * update Asset Chart Template Link Val By The Model
     *
     * @param newChartPid
     * @param assetPanelIdList
     * @param oldChartPid
     */
    private void updateAssetChartTemplateLinkValByTheModel(String newChartPid, List<String> assetPanelIdList, String oldChartPid) throws SQLException {
        if (Tool.CollUtil.isNotEmpty(assetPanelIdList)) {
            sqlRunner.update(this.connection,
                    // record
                    Entity.create().set("pid", newChartPid),
                    // where
                    Entity.create("visual_chart").set("pid", oldChartPid).set("panel_id", assetPanelIdList));
        }
    }


    /**
     * update Asset Panel Temp Id By The Model
     *
     * @param model
     * @param assetPanelIdList
     * @param templatePanelId
     */
    private void updateAssetPanelTempIdByTheModel(Entity model, List<String> assetPanelIdList, Long templatePanelId) throws SQLException {
        if (Tool.CollUtil.isEmpty(assetPanelIdList)) {
            log.warn("[updateAssetPanelTempIdByTheModel] [no reference asset panel in this model. not update temp_id] [model info: {}]", model.getStr("id"));
        } else {
            log.info("[updateAssetPanelTempIdByTheModel] [model info: {}] [tempId: {}]", model.getStr("id"), templatePanelId);
            sqlRunner.update(this.connection,
                    Entity.create().set("tmpl_id", templatePanelId),
                    Entity.create("visual_panel").set("id", assetPanelIdList)
            );
        }
    }

    /**
     * save The List Of New Chart Element
     *
     * @param chartElementList
     */
    private void saveTheListOfNewChartElement(List<Entity> chartElementList) throws SQLException {
        log.info("[saveTheListOfNewChartElement] [size: {}]", chartElementList.size());
        if (Tool.CollUtil.isNotEmpty(chartElementList)) {

            // 防止 sql 过长，分割后拼接 sql 修改
            Integer batchSize = 1000;
            Integer limit = (chartElementList.size() + batchSize - 1) / batchSize;
            List<List<Entity>> splitVisualChartElementList = Stream.iterate(0, n -> n + 1).limit(limit).parallel().map(a -> chartElementList.stream().skip(a * batchSize).limit(batchSize).parallel().collect(Collectors.toList())).collect(Collectors.toList());

            SQLUtils.FormatOption formatOption = new SQLUtils.FormatOption(false, false);
            for (List<Entity> batchUpdateElementList : splitVisualChartElementList) {
                List<String> list = Tool.ListUtil.list(true);
                for (Entity entity : batchUpdateElementList) {
                    String sqlStr = "INSERT INTO visual_chart_element(`id`, `chart_id`, `expression`, `type`, `legend`, `build_in`, `seq`, `name`, `state`) VALUES (null, ?, ?, ?, ?, ?, ?, ?, ?)";
                    List<Object> mappingValues = Tool.ListUtil.of(
                            entity.get("chart_id"),
                            entity.getStr("expression"),
                            entity.getStr("type"),
                            entity.getStr("legend"),
                            entity.get("build_in"),
                            entity.getStr("seq"),
                            entity.getStr("name"),
                            entity.get("state")
                    );
                    String sql = SQLUtils.format(sqlStr, DbType.mariadb, mappingValues, formatOption);
                    list.add(sql);
                }
                // execute Batch
                SqlExecutor.executeBatch(this.connection, list);
            }
        }
    }

}
