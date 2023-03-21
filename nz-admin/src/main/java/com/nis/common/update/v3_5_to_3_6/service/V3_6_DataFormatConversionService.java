package com.nis.common.update.v3_5_to_3_6.service;

import cn.hutool.db.DbUtil;
import cn.hutool.db.SqlConnRunner;
import cn.hutool.log.Log;

import java.sql.Connection;

/**
 * V3_6_DataFormatConversionService
 *
 * @author shizhendong
 * @date 2023/03/15
 */
public interface V3_6_DataFormatConversionService {

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
     * 支持自动将 已有asset model 配置的 chart template 转换为 dashboard template 实现。
     *
     * @param connection
     * @throws Exception
     */
    void dataFormatConversion(Connection connection) throws Exception;

}
