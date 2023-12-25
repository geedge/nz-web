package com.nis.modules.alert.interceptor;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.alibaba.druid.sql.parser.ParserException;
import com.alibaba.druid.stat.TableStat;
import com.nis.common.protobuf.Types;
import com.nis.common.protobuf.Types.Label;
import com.nis.common.protobuf.Types.Sample;
import com.nis.common.protobuf.Types.TimeSeries;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.RecordRuleService;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * 新增 nz_alert_change 监控指标
 * 增强 alert_message save 方法，当 alert message 成功插入后，构造 TimeSeries 保存到 prometheus/cortex
 *
 * @author HUAWEI
 */
@Component
@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
public class AlertMessageSqlInterceptor implements Interceptor {

    protected Log log = Log.get();

    private final String ALERT_METRICS_NAME = "nz_alert_change";

    @Lazy
    @Autowired
    private RecordRuleService recordRuleService;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement statement = (MappedStatement) args[0];
        try {
            Object parameterObject = args[1];
            String sql = statement.getBoundSql(parameterObject).getSql();
            SQLStatement sqlStatement = SQLUtils.parseSingleMysqlStatement(sql);
            MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();
            sqlStatement.accept(visitor);

            for (Map.Entry<TableStat.Name, TableStat> entry : visitor.getTables().entrySet()) {
                TableStat.Name tableName = entry.getKey();
                TableStat operationalType = entry.getValue();
                // Insert
                if (StrUtil.equalsIgnoreCase("Insert", StrUtil.toString(operationalType))) {
                    switch (tableName.getName()) {
                        case "alert_message_active":
                        case "alert_message_history":
                        case "alert_message_silence":
                            List<AlertMessageEntity> alertMsgList = Tool.ListUtil.list(true);
                            // IService
                            if (parameterObject instanceof AlertMessageEntity) {
                                alertMsgList.add((AlertMessageEntity) parameterObject);
                            }

                            // 针对 mapper 形参做具体处理，Entity & List<Entity>
                            if (parameterObject instanceof Map) {
                                // Entity
                                Map<String, Object> parameterObjectMap = (Map<String, Object>) parameterObject;
                                List<AlertMessageEntity> objectList1 = parameterObjectMap.entrySet().stream()
                                        .filter(entryMap -> BooleanUtil.and(entryMap.getValue() instanceof AlertMessageEntity))
                                        .map(entryMap -> (AlertMessageEntity) entryMap.getValue()).distinct().collect(Collectors.toList());
                                alertMsgList.addAll(objectList1);

                                // List<Entity>
                                List<AlertMessageEntity> objectList2 = parameterObjectMap.entrySet().stream()
                                        .filter(entryMap -> BooleanUtil.and(entryMap.getValue() instanceof List))
                                        .map(entryMap -> (List<Object>) entryMap.getValue())
                                        .flatMap(Collection::stream)
                                        .filter(obj -> BooleanUtil.and(obj instanceof AlertMessageEntity))
                                        .map(obj -> (AlertMessageEntity) obj)
                                        .distinct().collect(Collectors.toList());
                                alertMsgList.addAll(objectList2);
                            }
                            // 去重
                            alertMsgList = alertMsgList.stream().distinct().collect(Collectors.toList());

                            // push local cache record_data_cache 队列，等待消费
                            if (Tool.CollUtil.isNotEmpty(alertMsgList)) {
                                List tsList = Tool.ListUtil.list(true);
                                for (AlertMessageEntity entity : alertMsgList) {
                                    // build timeSeries
                                    TimeSeries timeSeries = this.buildAlertTimeSeries(entity);
                                    tsList.add(timeSeries);
                                }
                                recordRuleService.rightPushRecordToCache(tsList);
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        } catch (Exception e) {
            if (!(e instanceof ParserException)) {
                log.error(e);
            }
        } finally {
            return invocation.proceed();
        }
    }

    /**
     * build TimeSeries
     *
     * @param entity
     * @return
     */
    private TimeSeries buildAlertTimeSeries(AlertMessageEntity entity) {
        List<Types.Label> labelList = Tool.ListUtil.list(true);
        labelList.add(Label.newBuilder().setName("id").setValue(String.valueOf(entity.getId())).build());
        labelList.add(Label.newBuilder().setName("rule_id").setValue(String.valueOf(entity.getRuleId())).build());
        labelList.add(Label.newBuilder().setName("dc_id").setValue(String.valueOf(entity.getDcId())).build());
        labelList.add(Label.newBuilder().setName("asset_id").setValue(String.valueOf(entity.getAssetId())).build());
        labelList.add(Label.newBuilder().setName("project_id").setValue(String.valueOf(entity.getProjectId())).build());
        labelList.add(Label.newBuilder().setName("module_id").setValue(String.valueOf(entity.getModuleId())).build());
        labelList.add(Label.newBuilder().setName("endpoint_id").setValue(String.valueOf(entity.getEndpointId())).build());
        labelList.add(Label.newBuilder().setName("hash_key").setValue(String.valueOf(entity.getHashKey())).build());
        labelList.add(Label.newBuilder().setName("severity_id").setValue(String.valueOf(entity.getSeverityId())).build());
        labelList.add(Label.newBuilder().setName("state").setValue(String.valueOf(entity.getState())).build());

        // __name__ ALERT_METRICS_NAME
        labelList.add(Label.newBuilder().setName("__name__").setValue(ALERT_METRICS_NAME).build());

        Sample sample = Sample.newBuilder()
                .setTimestamp(System.currentTimeMillis())
                .setValue(entity.getStartAt().getTime())
                .build();

        TimeSeries alertMsgTs = TimeSeries.newBuilder().addAllLabels(labelList).addSamples(sample).build();
        return alertMsgTs;
    }

    @Override
    public Object plugin(Object obj) {
        return Plugin.wrap(obj, this);
    }

    @Override
    public void setProperties(Properties arg0) {
    }

}
