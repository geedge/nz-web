package com.nis.modules.agent.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.alibaba.druid.sql.parser.ParserException;
import com.alibaba.druid.stat.TableStat;
import com.nis.common.utils.Constant;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.agent.service.MonitorEventLogService;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.module.service.MonitorModuleService;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * mybatis sql 拦截器, 拦截影响 prom_server 数据更新的操作,更新 monitor_event_log 表数据
 * table : alert_rule alert_severity_conf dc monitor_endpoint prom_server asset_asset  monitor_module sys_config
 * operational: Insert Delete Update
 *
 * @author shizhendong
 * @date 2021-04-16
 */
@Component
@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
public class EventLogSqlInterceptor implements Interceptor {

    protected Log log = Log.get();

    @Lazy
    @Autowired
    private SysConfigService sysConfigService;

    @Lazy
    @Autowired
    private MonitorEventLogService eventLogService;

    @Lazy
    @Autowired
    private MonitorModuleService monitorModuleService;

    @Lazy
    @Autowired
    private PromserverService promserverService;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement statement = (MappedStatement) args[0];
        Object parameterObject = args[1];
        String sql = statement.getBoundSql(parameterObject).getSql();

        try {
            SQLStatement sqlStatement = SQLUtils.parseSingleMysqlStatement(sql);
            MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();
            sqlStatement.accept(visitor);

            for (Map.Entry<TableStat.Name, TableStat> entry : visitor.getTables().entrySet()) {
                TableStat.Name tableName = entry.getKey();
                TableStat operationalType = entry.getValue();
                switch (tableName.getName()) {
                    case "alert_rule":
                    case "alert_severity_conf":
                        eventLogService.increment("rule");
                        break;
                    case "dc":
                    case "monitor_project":
                    case "monitor_endpoint":
                    case "monitor_endpoint_config":
                        eventLogService.increment("prometheus", "promtail");
                        break;
                    case "prom_server":
                        this.handlePromServerChange(operationalType.toString(), parameterObject);
                        break;
                    case "asset_asset":
                        eventLogService.increment("prometheus", "blackbox", "promtail");
                        break;
                    case "monitor_module":
                        // 处理 snmp module
                        eventLogService.increment("snmp", "prometheus", "promtail");
                        break;
                    case "sys_config":
                        eventLogService.increment("loki", "cortex", "cmd", "prometheus");
                        /*// cmd 会造成 prom_server 重启,影响较大,对比值是否发生变化
                        List<SysConfigEntity> sysConfigList = sysConfigService.list();
                        Map<String, String> configMap = sysConfigList.stream().collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));

                        // Insert / Update
                        if (parameterObject instanceof SysConfigEntity) {
                            boolean b = this.compareConfigDataChange(configMap, Constant.SYSCONFIG_KEY_SCRAPEINTERVAL, ((SysConfigEntity) parameterObject));
                            boolean b1 = this.compareConfigDataChange(configMap, Constant.SYSCONFIG_KEY_STORAGE_LOCAL_RETENTION, ((SysConfigEntity) parameterObject));

                            if (b || b1) eventLogService.increment("cmd", "prometheus");

                            boolean b2 = this.compareConfigDataChange(configMap, Constant.SYSCONFIG_KEY_ASSET_PING_FROM, ((SysConfigEntity) parameterObject));
                            boolean b3 = this.compareConfigDataChange(configMap, Constant.SYSCONFIG_KEY_PROM_FEDER_ENABLED, ((SysConfigEntity) parameterObject));
                            boolean b4 = this.compareConfigDataChange(configMap, Constant.SYSCONFIG_KEY_ALERT_API, ((SysConfigEntity) parameterObject));
                            if (b2 || b3 || b4) eventLogService.increment("prometheus");
                        }

                        if (parameterObject instanceof Map) {
                            for (Map.Entry<String, Object> map : ((Map<String, Object>) parameterObject).entrySet()) {
                                if (map.getValue() instanceof SysConfigEntity) {
                                    boolean b = this.compareConfigDataChange(configMap, Constant.SYSCONFIG_KEY_SCRAPEINTERVAL, ((SysConfigEntity) map.getValue()));
                                    boolean b1 = this.compareConfigDataChange(configMap, Constant.SYSCONFIG_KEY_STORAGE_LOCAL_RETENTION, ((SysConfigEntity) map.getValue()));

                                    if (b || b1) eventLogService.increment("cmd", "prometheus");

                                    boolean b2 = this.compareConfigDataChange(configMap, Constant.SYSCONFIG_KEY_ASSET_PING_FROM, ((SysConfigEntity) map.getValue()));
                                    boolean b3 = this.compareConfigDataChange(configMap, Constant.SYSCONFIG_KEY_PROM_FEDER_ENABLED, ((SysConfigEntity) map.getValue()));
                                    boolean b4 = this.compareConfigDataChange(configMap, Constant.SYSCONFIG_KEY_ALERT_API, ((SysConfigEntity) map.getValue()));
                                    if (b2 || b3 || b4) eventLogService.increment("prometheus");
                                }
                            }
                        }*/
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            if (!(e instanceof ParserException))
                log.error("event log 记录变更失败", e);
        } finally {
            return invocation.proceed();
        }
    }

    /**
     * 处理 promserver , 只有在  dcid  type host port status 改变时才会记录 event log
     * @param type
     * @param parameterObject
     */
    private void handlePromServerChange(String type, Object parameterObject) {
        // 修改时 ， 判断
        if ("Update".equals(type)) {
            if (parameterObject instanceof Promserver) {
                this.comparePromserverDataChange((Promserver) parameterObject);
            }

            if (parameterObject instanceof Map) {
                for (Map.Entry<String, Object> map : ((Map<String, Object>) parameterObject).entrySet()) {
                    Object value = map.getValue();
                    if (value instanceof Promserver) {
                        this.comparePromserverDataChange((Promserver) value);
                    }
                }
            }
        } else {
            // insert delete
            eventLogService.increment("prometheus", "promtail", "cortex", "loki", "rule");
        }
    }

    private void comparePromserverDataChange(Promserver updateEntity) {
        Promserver oldEntity = updateEntity.getId() == null ? null : promserverService.getById(updateEntity.getId());
        if (oldEntity != null) {
            boolean flag = false;
            if (!oldEntity.getDcId().equals(updateEntity.getDcId())) flag = true;
            if (!oldEntity.getHost().equals(updateEntity.getHost())) flag = true;
            if (!oldEntity.getPort().equals(updateEntity.getPort())) flag = true;
            if (!oldEntity.getType().equals(updateEntity.getType())) flag = true;
            if (!oldEntity.getStatus().equals(updateEntity.getStatus())) flag = true;
            if (flag) {
                // 元信息变更 修改 prometheus promtail
                eventLogService.increment("prometheus", "promtail", "cortex", "loki", "rule");
            }
        }
    }

    /**
     * 对比 config key 变化
     *
     * @param configMap
     * @param key
     * @param entity
     * @return
     */
    private boolean compareConfigDataChange(Map<String, String> configMap, String key, SysConfigEntity entity) {
        if (key.equals(entity.getParamKey())) {
            String paramValue = configMap.get(key);
            if (!StrUtil.equals(paramValue, entity.getParamValue())) {
                // 发生改变
                return true;
            }
        }
        return false;
    }


    @Override
    public Object plugin(Object obj) {
        return Plugin.wrap(obj, this);
    }

    @Override
    public void setProperties(Properties arg0) {
    }
}
