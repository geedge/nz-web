package com.nis.common.update.v3_0_to_3_2;

import cn.hutool.log.Log;
import com.nis.common.update.v3_0_to_3_2.service.V3_2_DataFormatConversionService;
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
 * V3_2_MigrateNotifierCallback
 * Flyway V2021.12.14__backup and add new visual_chart table.sql 回调类
 * 用于处理 3.0 - 3.2 visual_chart 表数据转换
 * <p>
 * 1. 自动添加 chart element name
 * 2. 根据图表类型处理 param 内容
 * 3. chart 新增 datasource
 * 4. 删除 alertList 类型图表
 *
 * @author shizhendong
 * @date 2023/03/15
 */
@Component
@Transactional
public class V3_2_MigrateNotifierCallback implements Callback {

    private static final Log log = Log.get();

    /**
     * callback_version: V2021.12.14__backup and add new visual_chart table.sql
     */
    private static final String CALLBACK_VERSION = "2021.12.14";

    @Autowired
    private V3_2_DataFormatConversionService v3_2_dataFormatConversionService;

    @Override
    public String getCallbackName() {
        return "V3_2_MigrateNotifierCallback";
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
        log.info("[V3_2_MigrateNotifierCallback] [Data Format Conversion Begin] [migrate version: {}]", CALLBACK_VERSION);

        StopWatch sw = new StopWatch();
        sw.start();

        try {
            Connection connection = context.getConnection();
            // data Format Conversion
            v3_2_dataFormatConversionService.dataFormatConversion(connection);
        } catch (Exception e) {
            log.error(e, "[V3_2_MigrateNotifierCallback] [error] [shutdown nz-web.service]");
            // 停止服务
            Tool.RuntimeUtil.exec("systemctl stop nz-web.service");
        } finally {
            sw.stop();
        }

        log.info("[V3_2_MigrateNotifierCallback] [Data Format Conversion Finshed] [Run time: {}]", sw.toString());
    }

}
