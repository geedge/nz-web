package com.nis.common.update.v3_0_to_3_2.service;

import cn.hutool.db.DbUtil;
import cn.hutool.db.SqlConnRunner;
import cn.hutool.log.Log;

import java.sql.Connection;

/**
 * V3_2_DataFormatConversionService
 *
 * @author shizhendong
 * @date 2023/03/15
 */
public interface V3_2_DataFormatConversionService {

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
     * 3.0 - 3.2 visual_chart 表数据转换
     * <p>
     * 1. 自动添加 chart element name
     * 2. 根据图表类型处理 param 内容
     * 3. chart 新增 datasource
     * 4. 删除 alertList 类型图表
     *
     * @param connection
     * @throws Exception
     */
    void dataFormatConversion(Connection connection) throws Exception;

}
