package com.nis.common.update.v3_2_to_3_3;

import cn.hutool.log.Log;
import com.nis.common.update.v3_2_to_3_3.service.V3_3_DataFormatConversionService;
import com.nis.common.utils.Tool;
import org.apache.commons.lang3.time.StopWatch;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;

/**
 * V3_3_MigrateNotifierCallback
 * Flyway V2022.03.11__modify_asset_model_table_structure.sql 回调类
 * 用于处理 3.2 - 3.3  asset_model 表数据转换
 * <p>
 * 1. asset_model 新增 type_id、sys_object_id 列
 * 2. 根据 asset_asset 表关联 model_id & type_id 对应关系，处理 model.typeId
 * 注：未被 asset 引用的 model 数据直接删除，不再保留
 *
 * @author shizhendong
 * @date 2023/03/15
 */
@Component
@Transactional
public class V3_3_MigrateNotifierCallback implements Callback {

    private static final Log log = Log.get();

    /**
     * callback_version: V2022.03.11__modify_asset_model_table_structure.sql
     */
    private static final String CALLBACK_VERSION = "2022.03.11";

    @Autowired
    private V3_3_DataFormatConversionService v3_3_dataFormatConversionService;

    @Override
    public String getCallbackName() {
        return "V3_3_MigrateNotifierCallback";
    }

    /**
     * Not relevant if we don't interact with the database
     *
     * @param event
     * @param context
     * @return
     */
    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return true;
    }

    /**
     * Ensures that this callback handles events
     *
     * @param event
     * @param context
     * @return
     */
    @Override
    public boolean supports(Event event, Context context) {
        // afterEachMigrate
        if (Tool.ObjectUtil.equal(event, Event.AFTER_EACH_MIGRATE)) {
            MigrationInfo migrationInfo = context.getMigrationInfo();
            MigrationVersion version = migrationInfo.getVersion();
            if (Tool.ObjectUtil.isNotNull(version)) {
                if (Tool.StrUtil.equals(CALLBACK_VERSION, version.getVersion())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Process data upgrade
     *
     * @param event
     * @param context
     */
    @Override
    public void handle(Event event, Context context) {
        log.info("[V3_3_MigrateNotifierCallback] [Trigger callback] [begin] [migrate version: {}]", CALLBACK_VERSION);

        StopWatch sw = new StopWatch();
        sw.start();

        try {
            Connection connection = context.getConnection();
            // data Format Conversion
            v3_3_dataFormatConversionService.dataFormatConversion(connection);
        } catch (Exception e) {
            log.error(e, "[V3_3_MigrateNotifierCallback] [error] [shutdown nz-web.service]");
            // 停止服务
            Tool.RuntimeUtil.exec("systemctl stop nz-web.service");
        } finally {
            sw.stop();
        }

        log.info("[V3_3_MigrateNotifierCallback] [Data Format Conversion Finshed] [Run time: {}]", sw.toString());
    }

}
