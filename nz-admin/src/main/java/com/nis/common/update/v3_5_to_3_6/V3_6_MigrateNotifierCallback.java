package com.nis.common.update.v3_5_to_3_6;

import cn.hutool.log.Log;
import com.nis.common.update.v3_5_to_3_6.service.V3_6_DataFormatConversionService;
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
 * Flyway V2022.11.26__backup and modify asset_model table.sql 回调类
 * <p>
 * 支持自动将 已有asset model 配置的 chart template 转换为 dashboard template 实现。
 * 1、复制 asset_model_v3.6 表格
 * 2、遍历 asset model
 * 新建 dashboard template,，name： [assetModel.name] + template, 例： TSG-X template
 * 新建 chart （复制 asset model 已关联）
 * 更新使用 chart template 创建的图表 pid
 * 更新 asset panel tmpl_id
 * 3、更新 asset model panel id
 * 4、删除 chart.panel_id == 0 的历史数据
 * 5、删除 asset_model_v3.6
 *
 * @author shizhendong
 * @date 2023/03/15
 */
@Component
@Transactional
public class V3_6_MigrateNotifierCallback implements Callback {

    private static final Log log = Log.get();

    /**
     * callback_version: V2022.11.26__backup and modify asset_model table.sql
     */
    private static final String CALLBACK_VERSION = "2022.11.26";

    @Autowired
    private V3_6_DataFormatConversionService v3_6_dataFormatConversionService;

    @Override
    public String getCallbackName() {
        return "V3_6_MigrateNotifierCallback";
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
        log.info("[V3_6_MigrateNotifierCallback] [Data Format Conversion Begin] [migrate version: {}]", CALLBACK_VERSION);

        StopWatch sw = new StopWatch();
        sw.start();

        try {
            Connection connection = context.getConnection();
            // data Format Conversion
            v3_6_dataFormatConversionService.dataFormatConversion(connection);
        } catch (Exception e) {
            log.error(e, "[V3_6_MigrateNotifierCallback] [error] [shutdown nz-web.service]");
            // 停止服务
            Tool.RuntimeUtil.exec("systemctl stop nz-web.service");
        } finally {
            sw.stop();
        }

        log.info("[V3_6_MigrateNotifierCallback] [Data Format Conversion Finshed] [Run time: {}]", sw.toString());
    }

}
