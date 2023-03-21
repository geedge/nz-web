package com.nis.common.update.v3_2_to_3_3.service;

import cn.hutool.db.Entity;
import cn.hutool.db.SqlConnRunner;
import cn.hutool.db.handler.EntityListHandler;
import cn.hutool.db.handler.StringHandler;
import cn.hutool.db.sql.SqlExecutor;
import cn.hutool.log.Log;
import com.nis.common.update.DataFormatConversionInterface;
import com.nis.common.utils.Tool;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * V3_3_DataFormatConversionServiceImpl
 *
 * @author shizhendong
 * @date 2023/03/15
 */
@Service
public class V3_3_DataFormatConversionService implements DataFormatConversionInterface {

    private static final Log log = Log.get();

    /**
     * back up table name
     */
    private static final String BACK_UP_TABLE_NAME = "asset_model_v3_2";

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

        log.info("[dataFormatConversion] [conversion V3_2 to V3_3 Asset Model Data]");

        // conversion Asset Model Data
        this.conversionAssetModelData();

        // remove back up table
        log.info("[dataFormatConversion] [remove back up table] [table_name: {}]", BACK_UP_TABLE_NAME);
        SqlExecutor.execute(this.connection, Tool.StrUtil.concat(true, "DROP TABLE ", BACK_UP_TABLE_NAME));
    }

    /**
     * conversion Asset Model Data
     * 注：未被 asset 引用的 model 数据直接删除，不再保留
     *
     * @throws SQLException
     */
    private void conversionAssetModelData() throws SQLException {
        log.info("[conversionAssetModelData] [begin]");

        List<Entity> backUpModelList = sqlRunner.findAll(this.connection, Entity.create("asset_model_v3_2"));
        Map<String, Entity> backUpModelIdAndEntityMap = backUpModelList.stream().collect(Collectors.toMap(entity -> entity.getStr("id"), Function.identity()));
        log.info("[conversionAssetModelData] [conversion size: {}]", backUpModelList.size());

        List<Entity> assetTypeConfList = sqlRunner.findAll(this.connection, Entity.create("asset_type_conf"));
        Map<String, String> assetTypeIdAndNameMap = assetTypeConfList.stream()
                .collect(Collectors.toMap(entity -> entity.getStr("id"), entity -> entity.getStr("name")));

        // 统计 asset_asset 表 model_id 与 type_id 关联关系
        // 注：未被 asset 引用的 model 数据直接删除，不再保留
        List<Entity> modelIdAndTypeIdsMapList = SqlExecutor.query(this.connection, "SELECT model_id, GROUP_CONCAT( DISTINCT type_id ) AS type_ids FROM asset_asset GROUP BY model_id", new EntityListHandler());
        Map<String, String> modelIdAndTypeIdsMap = modelIdAndTypeIdsMapList.stream()
                .collect(Collectors.toMap(entity -> entity.getStr("model_id"), entity -> entity.getStr("type_ids")));

        // 判断 typeids 是否包含 ','
        // 包含',':    model 对应多个的 asset_type_conf
        // 不包含 ',': model 对应唯一的 asset_type_conf
        for (Map.Entry<String, String> entry : modelIdAndTypeIdsMap.entrySet()) {
            String modelId = entry.getKey();
            String typeIds = entry.getValue();
            if (Tool.StrUtil.contains(typeIds, ",")) {
                // 包含',':  model 对应多个的 asset_type_conf
                // 新增 model，并设置不同的 type_id，model.name = model.name-type.name
                List<String> typeIdStrList = Tool.StrUtil.split(typeIds, ',');
                for (String typeIdStr : typeIdStrList) {
                    Entity entity = backUpModelIdAndEntityMap.get(modelId);

                    entity.remove("id");
                    entity.set("type_id", Integer.valueOf(typeIdStr));
                    entity.set("sys_object_id", Tool.StrUtil.EMPTY);

                    // save model.name = oldModelName - typeConfName
                    String typeConfName = assetTypeIdAndNameMap.get(typeIdStr);
                    entity.set("name", Tool.StrUtil.concat(true, entity.getStr("name"), "-", typeConfName));

                    // insert model
                    entity.setTableName("asset_model");
                    Long saveEntityId = sqlRunner.insertForGeneratedKey(this.connection, entity);

                    // 修改 asset 对应 model_id
                    // sql: update asset_asset set model_id = {newModelID} where model_id = {oldModelID} and type_id = {typeID}
                    sqlRunner.update(this.connection,
                            Entity.create().set("model_id", saveEntityId),
                            Entity.create("asset_asset").set("model_id", modelId).set("type_id", typeIdStr));
                }
            } else {
                // 不包含 ','  model 对应唯一的 asset_type_conf, set model.typeId = typeId
                Entity entity = backUpModelIdAndEntityMap.get(modelId);
                entity.set("type_id", Integer.valueOf(typeIds));
                entity.set("sys_object_id", Tool.StrUtil.EMPTY);

                // insert model
                entity.setTableName("asset_model");
                sqlRunner.insert(this.connection, entity);
            }
        }
        log.info("[conversionAssetModelData] [finshed]");
    }

}

