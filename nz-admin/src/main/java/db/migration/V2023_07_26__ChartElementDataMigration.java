package db.migration;

import cn.hutool.db.DbUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.SqlConnRunner;
import cn.hutool.db.sql.SqlExecutor;
import cn.hutool.log.Log;
import com.nis.common.utils.JSONUtil;
import com.nis.common.utils.Tool;
import com.nis.modules.dashboard.entity.VisualChartElement;
import org.apache.commons.lang3.time.StopWatch;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * V2023_07_26__ChartElementDataMigration
 * ChartElement 数据迁移
 * <p>
 * 1. visual_chart add column `element_arr`
 * 2. drop table visual_chart_element
 * </p>
 * 历史数据格式升级
 *
 * @author shizhendong
 * @date 2023-07-26
 */
public class V2023_07_26__ChartElementDataMigration extends BaseJavaMigration {

    private static final Log log = Log.get();

    @Override
    public void migrate(Context context) throws Exception {
        log.info("[migrate] [begin]");
        StopWatch sw = new StopWatch();
        sw.start();

        Connection connection = context.getConnection();
        SqlConnRunner sqlConnRunner = DbUtil.newSqlConnRunner(connection);

        // query history data
        log.info("[migrate] [query history visual_chart_element list]");
        List<Entity> elementHistoryDataList = sqlConnRunner.findAll(connection, Entity.create("visual_chart_element"));
        log.info("[migrate] [visual_chart_element history data] [size: {}]", elementHistoryDataList.size());

        // visual_chart add column element_arr
        log.info("[migrate] [ALTER TABLE visual_chart ADD COLUMN `element_arr`] [begin]");
        PreparedStatement prepareStatement = null;
        try {
            prepareStatement = connection.prepareStatement("ALTER TABLE visual_chart ADD COLUMN IF NOT EXISTS `element_arr` text DEFAULT '' COMMENT 'element 表达式数组';");
            SqlExecutor.execute(prepareStatement);
        } finally {
            Tool.IoUtil.close(prepareStatement);
        }
        log.info("[migrate] [ALTER TABLE visual_chart ADD COLUMN `element_arr`] [finshed]");

        // drop table visual_chart_element
        log.info("[migrate] [DROP TABLE visual_chart_element] [begin]");
        try {
            prepareStatement = connection.prepareStatement("DROP TABLE IF EXISTS visual_chart_element;");
            SqlExecutor.execute(prepareStatement);
        } finally {
            Tool.IoUtil.close(prepareStatement);
        }
        log.info("[migrate] [DROP TABLE visual_chart_element] [finshed]");


        // history data format conversion
        log.info("[migrate] [visual_chart_element history data format conversion] [begin]");
        Map<Integer, List<Entity>> elementGroupedByChart = elementHistoryDataList.stream().collect(Collectors.groupingBy(entity -> entity.getInt("chart_id")));
        log.info("[migrate] [update the number of rows in table visual_chart record] [size: {}]", elementGroupedByChart.size());
        for (Map.Entry<Integer, List<Entity>> entry : elementGroupedByChart.entrySet()) {
            Integer chartId = entry.getKey();
            List<VisualChartElement> elementListForChart = entry.getValue().stream().map(entity -> entity.toBean(VisualChartElement.class)).collect(Collectors.toList());
            String elementArrStr = JSONUtil.toJsonStr(elementListForChart);
            // update visual_chart set element_arr = ? where id = ?
            sqlConnRunner.update(
                    connection,
                    Entity.create().set("element_arr", elementArrStr),
                    Entity.create("visual_chart").set("id", chartId));
        }
        log.info("[migrate] [visual_chart_element history data format conversion] [finshed]");

        sw.stop();
        log.info("[migrate] [finshed] [Run time: {}]", sw.toString());
    }


    @Override
    public MigrationVersion getVersion() {
        return MigrationVersion.fromVersion("2023.07.26");
    }

    @Override
    public String getDescription() {
        return "add element_arr colnum to visual_chart and drop visual_chart_element";
    }

    @Override
    public Integer getChecksum() {
        return null;
    }

    @Override
    public boolean isUndo() {
        return super.isUndo();
    }

    @Override
    public boolean isBaselineMigration() {
        return super.isBaselineMigration();
    }

    @Override
    public boolean canExecuteInTransaction() {
        return super.canExecuteInTransaction();
    }

}
