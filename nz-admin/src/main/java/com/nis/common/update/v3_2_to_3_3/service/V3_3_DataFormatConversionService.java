package com.nis.common.update.v3_2_to_3_3.service;

import cn.hutool.db.DbUtil;
import cn.hutool.db.SqlConnRunner;
import cn.hutool.log.Log;

import java.sql.Connection;

/**
 * V3_3_DataFormatConversionService
 *
 * @author shizhendong
 * @date 2023/03/15
 */
public interface V3_3_DataFormatConversionService {

    Log log = Log.get();

    /**
     * init Sql Runner
     *
     * @param connection
     * @return
     */
    default SqlConnRunner initSqlRunner(Connection connection) {
        log.info("[initSqlRunner] [begin]");
        SqlConnRunner sqlConnRunner = DbUtil.newSqlConnRunner(connection);
        log.info("[initSqlRunner] [finshed]");
        return sqlConnRunner;
    }

    /**
     * 1. asset_model 新增 type_id、sys_object_id 列
     * 2. 根据 asset_asset 表关联 model_id & type_id 对应关系，处理 model.typeId
     * 注：未被 asset 引用的 model 数据直接删除，不再保留
     *
     * @throws Exception
     * @param connection
     */
    void dataFormatConversion(Connection connection) throws Exception;

}
