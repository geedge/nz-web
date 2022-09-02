package com.nis.modules.agent.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.alibaba.druid.sql.parser.ParserException;
import com.alibaba.druid.stat.TableStat;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.service.MonitorEventLogService;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.AgentService;
import com.nis.modules.sys.entity.SysConfigEntity;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Properties;

/**
 * mybatis sql 拦截器, 拦截影响 agent 数据更新的操作,更新 monitor_event_log 表数据 table :
 * alert_rule alert_severity_conf dc monitor_endpoint agent asset_asset
 * monitor_module sys_config operational: Insert Delete Update
 *
 * @author shizhendong
 * @date 2021-04-16
 */
@Component
@Intercepts({ @Signature(type = Executor.class, method = "update", args = { MappedStatement.class, Object.class }) })
public class EventLogSqlInterceptor implements Interceptor {

	protected Log log = Log.get();

	@Lazy
	@Autowired
	private MonitorEventLogService eventLogService;

	@Lazy
	@Autowired
	private AgentService agentService;

	private static final String[] excludes = {
			"com.nis.modules.alert.dao.AlertHandlerDao.updateCloseExpiredMessageEndAt" };


	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		Object[] args = invocation.getArgs();
		MappedStatement statement = (MappedStatement) args[0];
		String id = statement.getId();

		try {
			if (!Tool.ArrayUtil.contains(excludes, id)) {
				Object parameterObject = args[1];
				String sql = statement.getBoundSql(parameterObject).getSql();
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
					case "agent":
						this.handleAgentChange(operationalType.toString(), parameterObject);
						break;
					case "asset_asset":
						eventLogService.increment("prometheus", "blackbox", "promtail");
						break;
					case "monitor_module":
						// 处理 snmp module
						eventLogService.increment("snmp", "prometheus", "promtail");
						break;
					case "record_rule":
						eventLogService.increment("record");
						break;
					default:
						break;
					}
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
	 * 处理 agent , 只有在 dcid type host port status 改变时才会记录 event log
	 *
	 * @param type
	 * @param parameterObject
	 */
	private void handleAgentChange(String type, Object parameterObject) {
		// 修改时 ， 判断
		if ("Update".equals(type)) {
			if (parameterObject instanceof Agent) {
				this.compareAgentDataChange((Agent) parameterObject);
			}

			if (parameterObject instanceof Map) {
				for (Map.Entry<String, Object> map : ((Map<String, Object>) parameterObject).entrySet()) {
					Object value = map.getValue();
					if (value instanceof Agent) {
						this.compareAgentDataChange((Agent) value);
					}
				}
			}
		} else {
			// insert delete

			//eventLogService.increment("prometheus", "promtail", "cortex", "loki", "rule");
			eventLogService.increment("prometheus", "promtail", "cortex", "loki", "snmp");
		}
	}

	private void compareAgentDataChange(Agent updateEntity) {
		Agent oldEntity = updateEntity.getId() == null ? null : agentService.getById(updateEntity.getId());
		if (oldEntity != null) {
			boolean flag = false;
			if (!oldEntity.getDcId().equals(updateEntity.getDcId()))
				flag = true;
			if (!oldEntity.getHost().equals(updateEntity.getHost()))
				flag = true;
			if (!oldEntity.getPort().equals(updateEntity.getPort()))
				flag = true;
			if (!oldEntity.getType().equals(updateEntity.getType()))
				flag = true;
			if (!oldEntity.getStatus().equals(updateEntity.getStatus()))
				flag = true;
			if (flag) {
				// 元信息变更 修改 prometheus promtail

				// eventLogService.increment("prometheus", "promtail", "cortex", "loki", "rule");
				if(updateEntity.getType().equals(Constant.AgentType.GLOBAL.getValue())) {
					eventLogService.increment("prometheus", "promtail", "cortex", "loki", "snmp");
				}else {
					eventLogService.increment("prometheus", "promtail", "snmp");
				}
				
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
