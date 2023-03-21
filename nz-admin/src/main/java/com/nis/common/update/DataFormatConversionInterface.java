package com.nis.common.update;

import cn.hutool.db.DbUtil;
import cn.hutool.db.SqlConnRunner;
import cn.hutool.log.Log;

import java.sql.Connection;

/**
 * Data Format Conversion Interface
 *
 * @author shizhendong
 * @date 2023/03/15
 */
public interface DataFormatConversionInterface {

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
     * data Format Conversion
     *
     * @param connection
     * @throws Exception
     */
    void dataFormatConversion(Connection connection) throws Exception;

}
