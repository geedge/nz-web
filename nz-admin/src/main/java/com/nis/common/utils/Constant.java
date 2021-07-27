/**

 *

 *

 */

package com.nis.common.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.poi.util.TempFile;

import javax.sql.DataSource;
import java.io.File;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 常量
 *
 * 
 */
public class Constant {
	private final static Log logger = Log.get();
	/** 超级管理员ID */
	public static final int SUPER_ADMIN = 1;

	// 系统 普通用户角色
	public static final int SYSTEM_COMMON_ROLE = 2;
	/** 数据权限过滤 */
	public static final String SQL_FILTER = "sql_filter";
	/**
	 * 当前页码
	 */
	public static final String PAGE = "pageNo";
	/**
	 * 每页显示记录数
	 */
	public static final String LIMIT = "pageSize";
	/**
	 * 每页显示条数
	 */
	public static final long PAGESIZE = 10;

	/**
	 * 排序方式
	 */
	public static final String ORDER = "orderBy";
	/**
	 * 升序
	 */
	public static final String ASC = "asc";

	/**
	 * mysql text 最大长度 65535
	 */
	public static final Integer MYSQL_TEXT_MAXLENGTH = 65535;

	/**
	 * 统一utf-8字符编码
	 */
	public static final String DEFAULT_CHARTSET_NAME = "UTF-8";

	/**
	 * 菜单类型
	 */
	public enum MenuType {
		/**
		 * 目录
		 */
		CATALOG(0),
		/**
		 * 菜单
		 */
		MENU(1),
		/**
		 * 按钮
		 */
		BUTTON(2),

		/**
		 * tab标签
		 */
		TAB(3);

		private int value;

		MenuType(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	/**
	 * 当前数据库类型
	 */
	public static final String DB_TYPE;

	public static final String ORACLE = "oracle";
	public static final String MYSQL = "mysql";
	public static final String POSTGRESQL = "postgresql";

	// 系统重置类型
	public static final String[] SYSCONFIG_RESET_TYPE = { "metric", "alert", "sysconfig" };

	/**
	 * 告警信息状态
	 */
	public static final List<Integer> ALERTMESSAGE_STATE;
	/**
	 * redis告警信息的key
	 */
	public static final String REDIS_KEY_ALERTMESSAGE = "ALERT_MESSAGE";

	/**
	 * 记录endpoint和proserver 对应关系
	 */
	public static final String REDIS_KEY_ENDPOINT_PROM = "endpoint_prom";

	/**
	 * 支持告警的 promserver id
	 */
	public static final String REDIS_KEY_SUPPORT_ALERT_PROMSERVER_ID = "support_alert_promserver_id";

	/**
	 * 记录
	 */
	public static final String REDIS_KEY_CONF_EVENT = "conf_event";

	/**
	 * 告警消息邮件通知
	 */
	public static final String REDIS_KEY_EMAIL_ALERT_MESSAGE = "alert_message_list";

	/**
	 * prometheus query range 代理接口 限制查询最大条数
	 */
	public static final String QUERY_MAX_SERIES = "query_max_series";

	/**
	 * prometheus server up 也就是endpoint状态 'endpoint'键值对应endpointId
	 */
	public static final String PROMSERVERUP_ENDPOINT_LABEL = "endpoint";

	/**
	 * 临时目录
	 */
	public static final String TEMP_PATH = System.getProperty("user.dir") + File.separator + "tmp";

	/**
	 * 工具目录
	 */
	public static final String TOOL_PATH = System.getProperty("user.dir") + File.separator + "tools";

	// 当前sys_config 配置中配置项和所属类别map
	public static final Map<String, List<String>> SYSCONFIG_TYPEWITHKEYS_MAP;

	// module 类型 可选值 http、snmp
	public static final String MODULE_TYPE_HTTP = "http";
	public static final String MODULE_TYPE_SNMP = "snmp";

	// module http 类型默认 path
	public static final String MODULE_HTTP_DEFAULT_PATH = "/metrics";

	public static final Integer SNMP_DEFAULT_PORT = 161;

	// snmp set value 可选类型集合
	public static final List<String> SNMP_SETVLAUE_TYPE;

	// 图表 singleStat statistics可选值列表
	public static final List<String> CHART_SINGLESTAT_STATISTICS;

	// alert rule 比较符号
	public static final List<String> RULE_OPERATORS;

	// snmp 采集指标相关 oid 信息
	public static final Map<String, String> SNMP_OID_MAP;

	public static final Map<String, String> SNMP_IFENTRY_MAP;

	// ssh 采集相关服务器信息指令
	public static final Map<String, String> SSH_SHELL_MAP;

	// alert rule unit
	public static final Map<String, Integer> ALERT_RULE_UNIT_MAP;

	// asset field meta 配置值
	public static final List<String> ASSET_FIELD_META_VALUE;
	/*
	 * alert message 不同状态对应表
	 */
	public static final String ALERT_MESSAGE_ACTIVE_TABLE = "ALERT_MESSAGE_ACTIVE";
	public static final String ALERT_MESSAGE_SILENCE_TABLE = "ALERT_MESSAGE_SILENCE";
	public static final String ALERT_MESSAGE_HISTORY_TABLE = "ALERT_MESSAGE_HISTORY";
	/*
	 * alert message 不同状态
	 */
	public static final Integer ALERT_MESSAGE_ACTIVE_STATE = 1;
	public static final Integer ALERT_MESSAGE_SILENCE_STATE = 2;
	public static final Integer ALERT_MESSAGE_HISTORY_STATE = 3;

	/**
	 * 可选值：1:active 2:pending 3:expired
	 */
	public static final Integer ALERT_SILENCE_STATE_ACTIVE = 1;
	public static final Integer ALERT_SILENCE_STATE_PENDING = 2;
	public static final Integer ALERT_SILENCE_STATE_EXPIRED = 3;

	/**
	 * alert message 表 与 状态 对应关系 KEY :tablename
	 */
	public static final Map<String, Integer> ALERT_MESSAGE_TABLE_STATE_MAPPING = Tool.MapUtil.newHashMap();
	/**
	 * alert message 状态 与 表 对应关系 KEY :state
	 */
	public static final Map<Integer, String> ALERT_MESSAGE_STATE_TABLE_MAPPING = Tool.MapUtil.newHashMap();

    /**
     * alert message 状态 与 名称 对应关系 KEY :state
     */
	public static final Map<Integer, String> ALERT_MESSAGE_STATE_NAME_MAPPING = Tool.MapUtil.newHashMap();

	/**
	 * 表名 和 排序字段对应关系  KEY: tablename
	 */
	public static final Map<String, Map<String, String>> TABLE_NAME_ORDER_FIELD_MAPPING = Tool.MapUtil.newHashMap();

	static {
		DatabaseIdProvider databaseIdProvider = SpringContextUtils.getBean("getDatabaseIdProvider",
				DatabaseIdProvider.class);
		DataSource dataSource = SpringContextUtils.getBean("dataSource", DataSource.class);
		String databaseId = null;
		try {
			databaseId = databaseIdProvider.getDatabaseId(dataSource);
		} catch (SQLException e) {
			logger.error(e);
		}
		DB_TYPE = databaseId;

		File tempPath = FileUtil.file(TEMP_PATH);
		if (!tempPath.isDirectory()) {
			boolean mkdir = tempPath.mkdir();
			if (mkdir) {
				logger.debug("临时目录tmp创建成功");
			} else {
				logger.debug("临时目录tmp创建失败");
			}
		}

		// 设置 POI 临时目录为项目 临时目录
		TempFile.setTempFileCreationStrategy(new TempFile.DefaultTempFileCreationStrategy(tempPath));

		ALERTMESSAGE_STATE = new ArrayList<Integer>();
		ALERTMESSAGE_STATE.add(1);
		ALERTMESSAGE_STATE.add(2);

		SYSCONFIG_TYPEWITHKEYS_MAP = new HashMap<>();

		SYSCONFIG_TYPEWITHKEYS_MAP.put("basic",
				Arrays.asList(new String[] { "alert_api", "asset_ping_switch", "asset_ping_interval",
						"node_exporter_target_path", "scrape_interval", "storage_local_retention", "system_name",
						"timezone", "default_cabinet_usize", "query_max_series", "map_center_config",
						"unsaved_change" }));
		SYSCONFIG_TYPEWITHKEYS_MAP.put("email",
				Arrays.asList(new String[] { "email_enable", "email_smtp_host", "email_smtp_port", "email_smtp_account",
						"email_smtp_password", "email_send_account", "email_test_reciver", "email_ssl_flag",
						"email_tls_flag", "test" }));
		SYSCONFIG_TYPEWITHKEYS_MAP.put("terminal", Arrays.asList(new String[] { "max_terminal_num" }));
		SYSCONFIG_TYPEWITHKEYS_MAP.put("ldap", Arrays.asList(new String[] { "ldap_address", "ldap_dn", "ldap_password",
				"ldap_ou", "ldap_user_filter", "ldap_mapping", "ldap_enable", "test" }));

		SNMP_SETVLAUE_TYPE = new ArrayList<>();
		SNMP_SETVLAUE_TYPE.add("OctetString");
		SNMP_SETVLAUE_TYPE.add("Integer");
		SNMP_SETVLAUE_TYPE.add("OID");
		SNMP_SETVLAUE_TYPE.add("Counter32");
		SNMP_SETVLAUE_TYPE.add("IpAddress");
		SNMP_SETVLAUE_TYPE.add("TimeTicks");
		SNMP_SETVLAUE_TYPE.add("UnsignedInteger");
		SNMP_SETVLAUE_TYPE.add("BITS");
		SNMP_SETVLAUE_TYPE.add("Float");
		SNMP_SETVLAUE_TYPE.add("DateAndTime");

		CHART_SINGLESTAT_STATISTICS = new ArrayList<>();
		CHART_SINGLESTAT_STATISTICS.add("min");
		CHART_SINGLESTAT_STATISTICS.add("max");
		CHART_SINGLESTAT_STATISTICS.add("average");
		CHART_SINGLESTAT_STATISTICS.add("total");
		CHART_SINGLESTAT_STATISTICS.add("first");
		CHART_SINGLESTAT_STATISTICS.add("last");
		CHART_SINGLESTAT_STATISTICS.add("range");
		CHART_SINGLESTAT_STATISTICS.add("different");

		RULE_OPERATORS = Arrays.asList(new String[] { "==", "!=", ">", "<", ">=", "<=" });

		SNMP_OID_MAP = new HashMap<>();
		SNMP_OID_MAP.put("sysName", "1.3.6.1.2.1.1.5");
		SNMP_OID_MAP.put("sysLocation", "1.3.6.1.2.1.1.6");
		SNMP_OID_MAP.put("sysContact", "1.3.6.1.2.1.1.4");
		SNMP_OID_MAP.put("sysServices", "1.3.6.1.2.1.1.7");
		SNMP_OID_MAP.put("sysUpTime", "1.3.6.1.2.1.1.3");
		SNMP_OID_MAP.put("ifNumber", "1.3.6.1.2.1.2.1");
		SNMP_OID_MAP.put("ifEntry", "1.3.6.1.2.1.2.2.1");

		SNMP_IFENTRY_MAP = new HashMap<>();
		SNMP_IFENTRY_MAP.put("1.3.6.1.2.1.2.2.1.1", "ifIndex");
		SNMP_IFENTRY_MAP.put("1.3.6.1.2.1.2.2.1.2", "ifDescr");
		SNMP_IFENTRY_MAP.put("1.3.6.1.2.1.2.2.1.3", "ifType");
		SNMP_IFENTRY_MAP.put("1.3.6.1.2.1.2.2.1.4", "ifMtu");
		SNMP_IFENTRY_MAP.put("1.3.6.1.2.1.2.2.1.5", "ifSpeed");
		SNMP_IFENTRY_MAP.put("1.3.6.1.2.1.2.2.1.6", "ifPhysAddress");
		SNMP_IFENTRY_MAP.put("1.3.6.1.2.1.2.2.1.7", "ifAdminStatus");
		SNMP_IFENTRY_MAP.put("1.3.6.1.2.1.2.2.1.8", "ifOperStatus");
		SNMP_IFENTRY_MAP.put("1.3.6.1.2.1.2.2.1.9", "ifLastChange");

		SSH_SHELL_MAP = new HashMap<>();
		SSH_SHELL_MAP.put("hostname", "hostname");
		SSH_SHELL_MAP.put("cpu",
				"cat /proc/cpuinfo| grep \"physical id\"| sort| uniq| wc -l;echo `cat /proc/cpuinfo|grep \"model name\" | uniq -d|awk -F:  '{print $2}'` \\* `cat /proc/cpuinfo| grep \"processor\"| wc -l`");
		SSH_SHELL_MAP.put("memory", "free -m  | grep Mem | awk -F ' ' '{printf (\"%.3f\\n\",$2/1024)}'");
		SSH_SHELL_MAP.put("disk",
				"lsblk|grep disk|awk '{print $1,$4}';cat /proc/scsi/scsi|grep Vendor|awk '{print $2,$4}'|grep -v DVD");
		SSH_SHELL_MAP.put("core", "cat /proc/version");
		// put("os","cat /etc/redhat-release");
		SSH_SHELL_MAP.put("uptime", "uptime -s");

		ALERT_RULE_UNIT_MAP = new HashMap<>();
		ALERT_RULE_UNIT_MAP.put("none", 1);
		ALERT_RULE_UNIT_MAP.put("short", 2);
		ALERT_RULE_UNIT_MAP.put("percent(0-100)", 3);
		ALERT_RULE_UNIT_MAP.put("percent(0.0-1.0)", 4);
		ALERT_RULE_UNIT_MAP.put("local format", 5);
		ALERT_RULE_UNIT_MAP.put("bits", 6);
		ALERT_RULE_UNIT_MAP.put("bytes", 7);
		ALERT_RULE_UNIT_MAP.put("kilobytes", 8);
		ALERT_RULE_UNIT_MAP.put("megabytes", 9);
		ALERT_RULE_UNIT_MAP.put("gigabytes", 10);
		ALERT_RULE_UNIT_MAP.put("terabytes", 11);
		ALERT_RULE_UNIT_MAP.put("petabytes", 12);
		ALERT_RULE_UNIT_MAP.put("packets/sec", 13);
		ALERT_RULE_UNIT_MAP.put("bits/sec", 14);
		ALERT_RULE_UNIT_MAP.put("bytes/sec", 15);
		ALERT_RULE_UNIT_MAP.put("kilobytes/sec", 16);
		ALERT_RULE_UNIT_MAP.put("kilobits/sec", 17);
		ALERT_RULE_UNIT_MAP.put("megabytes/sec", 18);
		ALERT_RULE_UNIT_MAP.put("megabits/sec", 19);
		ALERT_RULE_UNIT_MAP.put("gigabytes/sec", 20);
		ALERT_RULE_UNIT_MAP.put("gigabits/sec", 21);
		ALERT_RULE_UNIT_MAP.put("terabytes/sec", 22);
		ALERT_RULE_UNIT_MAP.put("terabits/sec", 23);
		ALERT_RULE_UNIT_MAP.put("petabytes/sec", 24);
		ALERT_RULE_UNIT_MAP.put("petabits/sec", 25);
		ALERT_RULE_UNIT_MAP.put("Hertz(1/s)", 26);
		ALERT_RULE_UNIT_MAP.put("nanoseconds(ns)", 27);
		ALERT_RULE_UNIT_MAP.put("nanoseconds", 27);
		ALERT_RULE_UNIT_MAP.put("ns", 27);
		ALERT_RULE_UNIT_MAP.put("microseconds(us)", 28);
		ALERT_RULE_UNIT_MAP.put("microseconds", 28);
		ALERT_RULE_UNIT_MAP.put("us", 28);
		ALERT_RULE_UNIT_MAP.put("milliseconds(ms)", 29);
		ALERT_RULE_UNIT_MAP.put("milliseconds", 29);
		ALERT_RULE_UNIT_MAP.put("ms", 29);
		ALERT_RULE_UNIT_MAP.put("seconds(s)", 30);
		ALERT_RULE_UNIT_MAP.put("seconds", 30);
		ALERT_RULE_UNIT_MAP.put("s", 30);
		ALERT_RULE_UNIT_MAP.put("minutes(m)", 31);
		ALERT_RULE_UNIT_MAP.put("minutes", 31);
		ALERT_RULE_UNIT_MAP.put("m", 31);
		ALERT_RULE_UNIT_MAP.put("hours(h)", 32);
		ALERT_RULE_UNIT_MAP.put("hours", 32);
		ALERT_RULE_UNIT_MAP.put("h", 32);
		ALERT_RULE_UNIT_MAP.put("days(d)", 33);
		ALERT_RULE_UNIT_MAP.put("days", 33);
		ALERT_RULE_UNIT_MAP.put("d", 33);
		ALERT_RULE_UNIT_MAP.put("YYYY-MM-DD HH:mm:ss", 34);
		ALERT_RULE_UNIT_MAP.put("MM/DD/YYYY h:mm:ss a", 35);

		ASSET_FIELD_META_VALUE = Arrays.asList(new String[] { "text", "multitext", "textarea", "radio", "checkbox",
				"select", "integer", "double", "datetime", "email" });

		ALERT_MESSAGE_TABLE_STATE_MAPPING.put(ALERT_MESSAGE_ACTIVE_TABLE, ALERT_MESSAGE_ACTIVE_STATE);
		ALERT_MESSAGE_TABLE_STATE_MAPPING.put(ALERT_MESSAGE_SILENCE_TABLE, ALERT_MESSAGE_SILENCE_STATE);
		ALERT_MESSAGE_TABLE_STATE_MAPPING.put(ALERT_MESSAGE_HISTORY_TABLE, ALERT_MESSAGE_HISTORY_STATE);

		ALERT_MESSAGE_STATE_TABLE_MAPPING.put(ALERT_MESSAGE_ACTIVE_STATE, ALERT_MESSAGE_ACTIVE_TABLE);
		ALERT_MESSAGE_STATE_TABLE_MAPPING.put(ALERT_MESSAGE_SILENCE_STATE, ALERT_MESSAGE_SILENCE_TABLE);
		ALERT_MESSAGE_STATE_TABLE_MAPPING.put(ALERT_MESSAGE_HISTORY_STATE, ALERT_MESSAGE_HISTORY_TABLE);

        ALERT_MESSAGE_STATE_NAME_MAPPING.put(ALERT_MESSAGE_ACTIVE_STATE, "active");
        ALERT_MESSAGE_STATE_NAME_MAPPING.put(ALERT_MESSAGE_SILENCE_STATE, "silence");
        ALERT_MESSAGE_STATE_NAME_MAPPING.put(ALERT_MESSAGE_HISTORY_STATE, "expired");

		Map<String, String> assetOrderFieldMap = new HashMap<>();
		assetOrderFieldMap.put("name", "asset.name");
		assetOrderFieldMap.put("id", "asset.id");
		assetOrderFieldMap.put("manageIp", "asset.manage_ip");
		assetOrderFieldMap.put("type", "type.name");
		assetOrderFieldMap.put("state", "state.name");
		assetOrderFieldMap.put("dc", "dc.name");
		assetOrderFieldMap.put("cabinet", "cab.name");
		assetOrderFieldMap.put("brand", "brand.name");
		assetOrderFieldMap.put("model", "model.name");
		assetOrderFieldMap.put("endpointNum", "endpointNum");
		assetOrderFieldMap.put("alertNum", "alertNum");
		TABLE_NAME_ORDER_FIELD_MAPPING.put("asset_asset", assetOrderFieldMap);

		Map<String, String> modelOrderFieldMap = new HashMap<>();
		modelOrderFieldMap.put("name", "model.name");
		modelOrderFieldMap.put("id", "model.id");
		modelOrderFieldMap.put("brand", "brand.name");
		modelOrderFieldMap.put("assetNum", "assetNum");
		TABLE_NAME_ORDER_FIELD_MAPPING.put("asset_model", modelOrderFieldMap);

		Map<String, String> labelsOrderFieldMap = new HashMap<>();
		labelsOrderFieldMap.put("name", "afm.name");
		labelsOrderFieldMap.put("id", "afm.id");
		labelsOrderFieldMap.put("metaKey", "afm.meta_key");
		labelsOrderFieldMap.put("group", "afg.name");
		TABLE_NAME_ORDER_FIELD_MAPPING.put("asset_field_meta", labelsOrderFieldMap);

		Map<String, String> assetTypeOrderFieldMap = new HashMap<>();
		assetTypeOrderFieldMap.put("name", "name");
		assetTypeOrderFieldMap.put("id", "id");
		TABLE_NAME_ORDER_FIELD_MAPPING.put("asset_type_conf", assetTypeOrderFieldMap);

		Map<String, String> projectOrderFieldMap = new HashMap<>();
		projectOrderFieldMap.put("name", "mp.name");
		projectOrderFieldMap.put("id", "mp.id");
		projectOrderFieldMap.put("moduleNum", "moduleNum");
		projectOrderFieldMap.put("endpointNum", "endpointNum");
		projectOrderFieldMap.put("alertNum", "alertNum");
		TABLE_NAME_ORDER_FIELD_MAPPING.put("monitor_project", projectOrderFieldMap);

		Map<String, String> moduleOrderFieldMap = new HashMap<>();
		moduleOrderFieldMap.put("name", "m.name");
		moduleOrderFieldMap.put("id", "m.id");
		moduleOrderFieldMap.put("project", "p.name");
		moduleOrderFieldMap.put("endpointNum", "endpointNum");
		moduleOrderFieldMap.put("alertNum", "alertNum");
		TABLE_NAME_ORDER_FIELD_MAPPING.put("monitor_module", moduleOrderFieldMap);

		Map<String, String> endpointOrderFieldMap = new HashMap<>();
		endpointOrderFieldMap.put("name", "e.name");
		endpointOrderFieldMap.put("id", "e.id");
		endpointOrderFieldMap.put("project", "p.name");
		endpointOrderFieldMap.put("module", "m.name");
		endpointOrderFieldMap.put("asset", "a.name");
		endpointOrderFieldMap.put("alerts", "alertNum");
		endpointOrderFieldMap.put("state", "es.state");
		TABLE_NAME_ORDER_FIELD_MAPPING.put("monitor_endpoint", endpointOrderFieldMap);

		Map<String, String> alertMsgOrderFieldMap = new HashMap<>();
		alertMsgOrderFieldMap.put("priority", "alsc.name");
		alertMsgOrderFieldMap.put("id", "am.id");
		alertMsgOrderFieldMap.put("alertRule", "ar.name");
		alertMsgOrderFieldMap.put("labels", "am.labels");
		alertMsgOrderFieldMap.put("startAt", "am.start_at");
		alertMsgOrderFieldMap.put("severityId","am.severity_id");
		TABLE_NAME_ORDER_FIELD_MAPPING.put("alert_message_active", alertMsgOrderFieldMap);

		Map<String, String> alertRuleOrderFieldMap = new HashMap<>();
		alertRuleOrderFieldMap.put("name", "ar.name");
		alertRuleOrderFieldMap.put("id", "ar.name");
		alertRuleOrderFieldMap.put("severity", "alsc.name");
		alertRuleOrderFieldMap.put("alertNum", "alertNum");
		TABLE_NAME_ORDER_FIELD_MAPPING.put("alert_rule", alertRuleOrderFieldMap);

		Map<String, String> alertSilenceOrderFieldMap = new HashMap<>();
		alertSilenceOrderFieldMap.put("state", "state");
		alertSilenceOrderFieldMap.put("id", "id");
		alertSilenceOrderFieldMap.put("matchers", "matchers");
		alertSilenceOrderFieldMap.put("startAt", "start_at");
		TABLE_NAME_ORDER_FIELD_MAPPING.put("alert_silence_conf", alertSilenceOrderFieldMap);

		Map<String, String> dcOrderFieldMap = new HashMap<>();
		dcOrderFieldMap.put("name", "dc.name");
		dcOrderFieldMap.put("id", "dc.id");
		dcOrderFieldMap.put("cabinetNum", "cabinetNum");
		dcOrderFieldMap.put("assetNum", "assetNum");
		dcOrderFieldMap.put("alertNum", "alertNum");
		dcOrderFieldMap.put("state", "dc.state");
		TABLE_NAME_ORDER_FIELD_MAPPING.put("dc", dcOrderFieldMap);

		Map<String, String> agentOrderFieldMap = new HashMap<>();
		agentOrderFieldMap.put("datacenter", "dc.name");
		agentOrderFieldMap.put("id", "ps.id");
		agentOrderFieldMap.put("type", "ps.type");
		agentOrderFieldMap.put("status", "ps.status");
		TABLE_NAME_ORDER_FIELD_MAPPING.put("prom_server", agentOrderFieldMap);

		Map<String, String> expreOrderFieldMap = new HashMap<>();
		expreOrderFieldMap.put("name", "name");
		expreOrderFieldMap.put("id", "id");
		expreOrderFieldMap.put("gname", "gname");
		TABLE_NAME_ORDER_FIELD_MAPPING.put("visual_expression_tmpl", expreOrderFieldMap);

		Map<String, String> chartTempOrderFieldMap = new HashMap<>();
		chartTempOrderFieldMap.put("name", "vch.name");
		chartTempOrderFieldMap.put("id", "vch.id");
		chartTempOrderFieldMap.put("type", "vch.type");
		chartTempOrderFieldMap.put("varType", "vch.var_type");
		TABLE_NAME_ORDER_FIELD_MAPPING.put("visual_chart", chartTempOrderFieldMap);

		Map<String, String> mibOrderFieldMap = new HashMap<>();
		mibOrderFieldMap.put("name", "sm.name");
		mibOrderFieldMap.put("id", "sm.id");
		TABLE_NAME_ORDER_FIELD_MAPPING.put("snmp_mib", mibOrderFieldMap);

		Map<String, String> snmpCerdenOrderFieldMap = new HashMap<>();
		snmpCerdenOrderFieldMap.put("name", "name");
		snmpCerdenOrderFieldMap.put("id", "id");
		snmpCerdenOrderFieldMap.put("type", "type");
		TABLE_NAME_ORDER_FIELD_MAPPING.put("snmp_credential", snmpCerdenOrderFieldMap);
	}

	/**
	 * 支持的语言 [{"name":"简体中文","value":"zh"},...]
	 */
	public static final List<Map<String, String>> langList = new ArrayList<>();

	/**
	 * 字典中语言的名称和类型
	 */
	public static final String DICT_NAME_LANG = "lang";

	/**
	 * 定时任务状态
	 */
	public enum ScheduleStatus {
		/**
		 * 正常
		 */
		NORMAL(0),
		/**
		 * 暂停
		 */
		PAUSE(1);

		private int value;

		ScheduleStatus(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	// delFlag
	public static final String DELFLAG_FALSE = "0"; // 未删除

	public static final Integer AUTH_PASSWORD = 1;// 密码认证
	public static final Integer AUTH_CERTIFICATE = 2;// 私钥证书认证

	public static final String AUTH_TOKEN_CODE = "Authorization";

	public static final Integer SNMPV1 = 1;
	public static final Integer SNMPV2 = 2;
	public static final Integer SNMPV3 = 3;
	// snmp默认团体名
	public static final String SNMP_DEFAULT_COMMUNITY = "public";

	public enum SnmpOperationType {
		GET("get"), WALK("walk"), GETNEXT("getnext"), SET("set");

		private String value;

		SnmpOperationType(String value) {
			this.value = value;
		}

		public static String getOperationType(String value) {
			for (SnmpOperationType type : SnmpOperationType.values()) {
				if (StrUtil.equals(type.getValue(), value)) {
					return type.getValue();
				}
			}
			return null;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	public enum PanelType {
		DASHBOARD("dashboard"), ASSET("asset"), PROJECT("project"), MODULE("module"), ENDPOINT("endpoint"),
		MODEL("model"),TEMPLATE("template");

		private String value;

		PanelType(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	public enum PromserverType {
		GLOBAL("global", 1),
		PER_DATACENTER("per_datacenter", 2),
		THANOS_QUERY("thanos_query", 3),
		THANOS_RULE("thanos_rule", 4),
		THANOS_SIDECAR("thanos_sidecar", 5),
		THANOS_STORE("thanos_store", 6),
		THANOS_COMPACTOR("thanos_compactor", 7),
		LOKI("loki", 11);

		private String name;
		private Integer value;

		PromserverType(String name, Integer value) {
			this.value = value;
			this.name = name;
		}

		public Integer getValue() {
			return value;
		}

		public void setValue(Integer value) {
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public static PromserverType getPromserverType(Integer value) {
			for (PromserverType server : PromserverType.values()) {
				if (server.getValue().equals(value)) {
					return server;
				}
			}
			return null;
		}
	}

	// nz-web 新增常量

	// 字典表中 chart 类型的 type 值
	public static final String DICT_CHART_TYPE = "chartType";

	// 字典表中 chart element 表达式类型的 type 值
	public static final String DICT_EXPRESSION_TYPE = "exprType";

	// 目前机柜最大值限制
	public static final Integer CURRENT_CABINET_MAXU = 47;

	// 默认告警间隔为15s
	public static final Integer ALERT_INTERVAL = 15;

	// ip 校验正则
	public static final String REGEX_IP = "(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d|\\*)\\.(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d|\\*)\\.(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d|\\*)\\.(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d|\\*)";

	// 二级域名
	public static final String REGEX_DOMAIN = "(\\w*\\.?){2}\\.(com.cn|net.cn|gov.cn|org\\.nz|org.cn|com|net|org|gov|cc|biz|info|cn|co)$";

	// 配置表中 告警间隔 key
	public static final String SYSCONFIG_KEY_ALERTINTERVAL = "alert_interval";

	// 配置表中 刮取周期 key
	public static final String SYSCONFIG_KEY_SCRAPEINTERVAL = "default_scrape_interval";

	// 配置表中 刮取超时时间
	public static final String SYSCONFIG_KEY_SCRAPETIMEOUT = "default_scrape_timeout";

	// 配置表中 blackbox job scrape_interval 刮取周期 key
	public static final String SYSCONFIG_KEY_ASSET_PING_INTERVAL = "asset_ping_interval";

	// 配置表中 prometheus yml 模板 key
	public static final String SYSCONFIG_KEY_PROM_YML_TMPL = "prom_yml_tmpl";

	// 配置表中 prometheus cmd 模板 Key
	public static final String SYSCONFIG_KEY_PROM_CMD_TMPL = "prom_cmd_tmpl";

	// 配置表中 prometheus rule.yml 模板 Key
	public static final String SYSCONFIG_KEY_PROM_RULE_TMPL = "prom_rule_tmpl";

	// 配置表中 prometheus 本地保存天数配置
	public static final String SYSCONFIG_KEY_STORAGE_LOCAL_RETENTION = "storage_local_retention";

	// 配置表中 alert_api
	public static final String SYSCONFIG_KEY_ALERT_API = "alert_api";

	// 配置表中 alert_path_prefix
	public static final String SYSCONFIG_KEY_ALERT_PATH_PREFIX = "alert_path_prefix";

	// endpoint state 定时任务执行间隔时间
	public static final String SYSCONFIG_KEY_ENDPOINT_QUERYINTERVAL = "endpoint_query_interval";

	// 邮件告警开关
	public static final String SYSCONFIG_KEY_EMAIL_ENABLE = "email_enable";

	// nezha 系统版本
	public static final String SYSCONFIG_KEY_SYSTEM_VERSION = "system_version";

	// 内置 generator 路径
	public static final String SYSCONFIG_KEY_GENERATOR_PATH = "generator_path";
	// 配置表中 blackbox 源， on 时，通过 global 做 ping 检测，off 时，通过 per-datacenter 检测
	public static final String SYSCONFIG_KEY_ASSET_PING_FROM = "asset_ping_from";

	// 配置表中 配置prometheus是否开启联邦模式，1：开启，0：关闭（界面禁用 添加选择 per-datacenter agent，所有endpoint
	// 配置信息 hash 分布到 global prometheus）
	public static final String SYSCONFIG_KEY_PROM_FEDER_ENABLED = "prometheus_federation_enabled";

	// 机柜默认大小
	public static final String SYSCONFIG_KEY_DEFAULT_CABINETU = "default_cabinet_usize";

	// 测试邮件发送模板
	public static final String SYSCONFIG_KEY_TESTEMAIL_SEND_TEMPLATE = "test_email_send_template";

	// 新增 asset 添加chart配置
	public static final String SYSCONFIG_KEY_ASSET_CHART_TPL = "asset_chart_tpl";

	// 新增 endpoint 添加chart配置
	public static final String SYSCONFIG_KEY_ENDPOINT_CHART_TPL = "endpoint_chart_tpl";

	// 新增model 添加chart配置
	public static final String SYSCONFIG_KEY_MODEL_CHART_TPL = "model_chart_tpl";

	// 新增project 添加chart配置
	public static final String SYSCONFIG_KEY_PROJECT_CHART_TPL = "project_chart_tpl";

	// asset 导入表头
	public static final String SYSCONFIG_KEY_ASSET_ASSET_EXPORT_HEADER = "asset_asset_export_header";

	// alert rule 导入表头
	public static final String SYSCONFIG_KEY_ALERTRULE_EXPORT_HEADER = "rule_export_header";

	// chart dashboard 类型导入表头
	public static final String SYSCONFIG_KEY_CHART_EXPORT_HEADER = "chart_export_header";

	// model 类型导入表头
	public static final String SYSCONFIG_KEY_MODEL_CHART_EXPORT_HEADER = "model_chart_export_header";

	// asset 类型导入表头
	public static final String SYSCONFIG_KEY_ASSET_CHART_EXPORT_HEADER = "asset_chart_export_header";

	// endpoint 导入表头
	public static final String SYSCONFIG_KEY_MONITORENDPOINT_EXPORT_HEADER = "monitor_endpoint_export_header";

	// expression template 导入表头
	public static final String SYSCONFIG_KEY_EXPRETMPL_EXPORT_HEADER = "expretmpl_export_header";

	// dc 导入表头
	public static final String SYSCONFIG_KEY_DC_EXPORT_HEADER = "dc_export_header";

	// model 导入表头
	public static final String SYSCONFIG_KEY_MODEL_EXPORT_HEADER = "model_export_header";

	// cabinet 导入表头
	public static final String SYSCONFIG_KEY_CABINET_EXPORT_HEADER = "cabinet_export_header";

	// 登录时 ldap template 超时时间
	public static final String SYSCONFIG_KEY_LDAP_QUERY_TIMEOUT = "ldap_query_timeout";

	// 告警级别 高
	public static final String ALERT_SEVERITY_HIGH = "P1";

	// 告警级别 中
	public static final String ALERT_SEVERITY_MEDIUM = "P2";

	// 告警级别 低
	public static final String ALERT_SEVERITY_LOW = "P3";

	// account protocol ssh协议
	public static final String ACCOUNT_SSH_PROTOCOL = "SSH";

	// account protocol telnet协议
	public static final String ACCOUNT_TELNET_PROTOCOL = "TELNET";

	// account protocol snmp协议
	public static final String ACCOUNT_SNMP_PROTOCOL = "SNMP";

	// confagent程序 端口
	public static final String CONFAGENT_PORT = "confagent_port";
	// confagent项目名称
	public static final String CONFAGENT_PATH = "confagent_path";

	// 开关开启状态
	public static final String SWITCH_OPEN = "on";

	// 开关关闭状态
	public static final String SWITCH_CLOSE = "off";

	// 资产服务器类型
	public static final String ASSET_SERVER_TYPE = "server";

	// panel chart link 链表结构 首位 prev 值
	public static final Integer START_PREV_VALUE = 0;

	// panel chart link 链表结构 末尾 next 值
	public static final Integer END_NEXT_VALUE = -1;

	// 连接smtp邮箱服务器超时时间 10秒
	public static final String CONNECTION_SMTP_TIMEOUT = "10000";

	/**
	 * MIB四种数据类型 IDENTIFIER 、OBJECT、TABLE、ENTRY
	 */
	public static final String MIB_IDENTIFIER_TYPE = "IDENTIFIER";
	public static final String MIB_OBJECT_TYPE = "OBJECT";
	public static final String MIB_TABLE_TYPE = "TABLE";
	public static final String MIB_ENTRY_TYPE = "ENTRY";
	// mib定义中 entry采用的格式为sequence 用以区分type 为 entry
	public static final String MIB_ENTRY_SYNTAX = "SEQUENCE";

	// snmp browser 中返回oid 与 mib文件中name对应，当前截取位置为 倒数1位
	public static final int OIDNAME_TRUNCATION_INDEX = 1;

	// 图表 url类型
	public static final String CHART_URL_TYPE = "url";

	// 图表 singleStat 类型
	public static final String CHART_SINGLESTAT_TYPE = "singleStat";

	// 图表singleStat 类型 paramkey名称
	public static final String CHART_SINGLESTAT_PARAMkEY = "statistics";

	// IDC traffic中tags key正则校验表达式 注：同 prometheus 标签正则
	public static final Pattern PROMETHEUS_LABELSKEY_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
	// node_exporter shell 脚本中 替换路径的名称
	public static final String NODEEXPORTER_SHELLREPLACE_PATH = "target_path";

	// 测试邮件发送标题
	public static final String TESTEMAIL_SEND_TITLE = "Test";

	// reset操作处理的表信息数据
	public static final String RESET_TABLE_INFOS = "reset_table_infos";

	// 图表类型为line bar stackArea
	public static final String CHART_DEFAULT_PARAMkEY = "threshold";

	// 图表类型为alertList
	public static final String CHART_ALERTLIST_TYPE = "alertList";

	// 图表类型为 diagram
	public static final String CHART_DIAGRAM_TYPE = "diagram";

	// 图表类型为 text
	public static final String CHART_TEXT_TYPE = "text";

	// 图表类型为 assetInfo
	public static final String CHART_ASSETINFO_TYPE = "assetInfo";
	
	// 图表类型为 endpointInfo
	public static final String CHART_ENDPOINTINFO_TYPE = "endpointInfo";

	public static final String CHART_GROUP_TYPE = "group";

	public static final String CHART_ELEMENT_EXPERT_TYPE = "expert";

	// 国际化对应
	public static final String I18N_MAPPING = "i18n_mapping";

	// endpoint state功能是否开启 0 不开启 1 开启
	public static final String ENDPOINT_ALERT_PUSH = "endpoint_alert_push";

	// endpoint state告警规则
	public static final String ENDPOINT_ALERT_RULE = "endpoint_alert_rule";

	// 存放prometheus推送过来的告警消息
	public static final String ALERT_MESSAGE_RECV = "alert_message_recv";

	/**
	 * 存放 active 状态 告警消息
	 */
	public static final String ALERT_MESSAGE_ACTIVE_PRIX = "alert_message_active_";
	/**
	 * 通过redis 自增实现 alert message 表中id 全局唯一
	 */
	public static final String ALERT_MESSAGE_ID_PRIX = "alert_message_id_";
	/**
	 * alert message 自增主键时间前缀
	 */
	public static final SimpleDateFormat ALERT_MESSAGE_ID_FORMAT = new SimpleDateFormat("yyMMddHHmm");
	/**
	 * 存放 silence 状态 告警消息
	 */
	public static final String ALERT_MESSAGE_SILENCE_PRIX = "alert_message_silence_";

	// 存放prometheus已经处理过的告警信息
	public static final String ALERT_MESSAGE_SILENCE = "alert_message_silence";

	// 存放需要通知的告警信息
	public static final String ALERT_MESSAGE_NOTIFY = "alert_message_notify";

	public static final SimpleDateFormat ALERT_MESSAGE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	public static final Integer ADMIN_USER_ID = 1;

	public static final String PIN = "password";

	// 高可用
	public static final String SERVER_ID = Tool.IdUtil.fastUUID();
	public static final String SYS_HA_LOCK = "sys_ha_lock";

	public static final String NEZHA_ROOT_PATH = StrUtil.subBefore(System.getProperty("user.dir"), StrUtil.C_SLASH,
			true);
	public static final String NZWEB_ROOT_PATH = NEZHA_ROOT_PATH + "/nz-web";
	public static final String AGENT_ROOT_PATH = NEZHA_ROOT_PATH + "/nz-agent";
	public static final String TMP_ROOT_PATH = NEZHA_ROOT_PATH + "/nz-web/tmp";
	public static final String GUI_ROOT_PATH = NEZHA_ROOT_PATH + "/nz-web/webRoot";
	public static final String RESOURCE_ROOT_PATH = NEZHA_ROOT_PATH + "/nz-web/resource";// nzagent.rpm,install.sh.template,
	public static final String BIN_ROOT_PATH = NEZHA_ROOT_PATH + "/nz-web/bin";// promtool,generator

	/*
	 * alert message label 中自定义key
	 */
	public static final String LABEL_ENDPOINT_ID_KEY = "endpoint_id";
	public static final String LABEL_MODULE_ID_KEY = "module_id";
	public static final String LABEL_PROJECT_ID_KEY = "project_id";
	public static final String LABEL_ASSET_ID_KEY = "asset_id";
	public static final String LABEL_DC_ID_KEY = "datacenter_id";
	public static final String LABEL_RULE_ID_KEY = "alertname";
	public static final String LABEL_SEVERITY_ID_KEY = "severity_id";

	public static final String ALERT_API = "alert_api";
	public static final String ALERT_PATH_PREFIX = "alert_path_prefix";

	public static final String SYSCONFIG_SNMP_PORT_KEY = "snmp_trap_listen_port";
	public static final String SYSCONFIG_SESSION_TIMEOUT_KEY = "session_timeout";

	public static final String SETUP_FILE_NAME = "setup.json";
	
	
}
