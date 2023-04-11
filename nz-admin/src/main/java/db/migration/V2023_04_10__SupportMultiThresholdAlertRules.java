package db.migration;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.DbUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.SqlConnRunner;
import cn.hutool.db.sql.SqlExecutor;
import cn.hutool.log.Log;
import com.nis.common.utils.Constant.AlertRuleType;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.entity.AlertRuleEntity;
import org.apache.commons.lang3.time.StopWatch;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

/**
 * V2023_04_10__SupportMultiThresholdAlertRules.java
 * 支持多阈值告警规则
 * <p>
 * 1. alert_rule drop operator & threshold & severity_id columns
 * 2. alert_rule add condition & ext_labels columns
 * <p>
 * 历史数据格式升级
 *
 * @author shizhendong
 * @date 2023-04-10
 */
public class V2023_04_10__SupportMultiThresholdAlertRules extends BaseJavaMigration {

    private static final Log log = Log.get();

    private static String MIGRATION_SQL =
            "ALTER TABLE alert_rule \n" +
                    "DROP COLUMN IF EXISTS operator,\n" +
                    "DROP COLUMN IF EXISTS threshold,\n" +
                    "DROP COLUMN IF EXISTS severity_id,\n" +
                    "ADD COLUMN IF NOT EXISTS `condition` varchar(4096) NOT NULL DEFAULT '' COMMENT '判断条件\\r\\noperator可选值：\\r\\n== (equal)\\r\\n!= (not-equal)\\r\\n> (greater-than)\\r\\n< (less-than)\\r\\n>= (greater-or-equal)\\r\\n<= (less-or-equal)\\r\\n~= (regex)\\r\\n示例：\\r\\n[{\"id\":1,\"weight\":1,\"operator\":\">\",\"value\":\"80\"},{\"id\":1,\"weight\":2,\"operator\":\">\",\"value\":\"70\"}]',\n" +
                    "ADD COLUMN IF NOT EXISTS `ext_labels` varchar(4096) NOT NULL DEFAULT '' COMMENT '扩展标签\\r\\n示例：\\r\\n[{\"name\":\"test\",\"value\":\"ddd\"},{\"name\":\"tas\",\"value\":\"ccc\"}]';";

    @Override
    public void migrate(Context context) throws Exception {
        log.info("[migrate] [begin]");
        StopWatch sw = new StopWatch();
        sw.start();

        Connection connection = context.getConnection();
        SqlConnRunner sqlConnRunner = DbUtil.newSqlConnRunner(connection);

        // query history data
        log.info("[migrate] [query history alert rule list]");
        List<Entity> ruleHistoryDataList = sqlConnRunner.findAll(connection, Entity.create("alert_rule"));
        log.info("[migrate] [rule history data] [size: {}]", ruleHistoryDataList.size());

        // alert table
        log.info("[migrate] [ALTER TABLE alert_rule] [begin]");
        PreparedStatement prepareStatement = null;
        try {
            prepareStatement = connection.prepareStatement(MIGRATION_SQL);
            SqlExecutor.execute(prepareStatement);
        } finally {
            // close
            Tool.IoUtil.close(prepareStatement);
        }
        log.info("[migrate] [ALTER TABLE alert_rule] [finshed]");

        // history data format conversion
        log.info("[migrate] [rule history data format conversion] [begin]");
        for (Entity entity : ruleHistoryDataList) {
            // type
            Integer type = entity.getInt("type");
            type = Tool.ObjectUtil.defaultIfNull(type, AlertRuleType.METRICS.getValue());
            AlertRuleType alertRuleType = AlertRuleType.getRuleType(type);

            // severity_id
            Integer severityId = entity.get("severity_id", 1);

            AlertRuleEntity.ConditionItem conditionItem = new AlertRuleEntity.ConditionItem();
            conditionItem.setId(severityId);
            conditionItem.setWeight(1);
            switch (alertRuleType) {
                case METRICS:
                case LOGS: {
                    String operator = entity.getStr("operator");
                    operator = StrUtil.emptyToDefault(operator, ">");

                    BigDecimal threshold = entity.getBigDecimal("threshold");
                    threshold = Tool.ObjectUtil.defaultIfNull(threshold, new BigDecimal(0.0));

                    conditionItem.setOperator(operator);
                    conditionItem.setValue(threshold.toPlainString());
                    break;
                }
                case SNMP_TRAP: {
                    conditionItem.setOperator("~=");
                    conditionItem.setValue(".*");
                    break;
                }
                default:
                    break;
            }
            // update
            sqlConnRunner.update(
                    connection,
                    Entity.create()
                            .set("condition", Tool.JSONUtil.toJsonStr(Tool.ListUtil.of(conditionItem)))
                            .set("ext_labels", Tool.StrUtil.EMPTY),
                    Entity.create("alert_rule").set("id", entity.get("id")));
        }
        log.info("[migrate] [rule history data format conversion] [finshed]");

        sw.stop();
        log.info("[migrate] [finshed] [Run time: {}]", sw.toString());
    }

    @Override
    public MigrationVersion getVersion() {
        return MigrationVersion.fromVersion("2023.04.10");
    }

    @Override
    public String getDescription() {
        return "Support multi threshold alert rules";
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
