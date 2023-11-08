package com.nis.common.utils;

/**
 * 操作日志相关枚举类型
 */
public enum TypeEnum {
	// asset 相关
	HARDWARE_ASSET("Hardware Asset"),
	ASSET_COMMENT("Hardware Asset Comment"),
	ASSET_FIELD_GROUP("Hardware Asset Field Group"),
	ASSET_FIELD_META("Hardware Asset Field Meta"),
	ASSET_STATE_CONF("Hardware Asset State Conf"),
	ASSET_TYPE_CONF("Hardware Asset Type Conf"),
	ASSET_BRAND("Hardware Asset Brand"),
	ASSET_MODEL("Hardware Asset Model"),
	ASSET_MAINTENANCE("Hardware Asset Maintenance"),
	SOFTWARE_ASSET("Software Asset"),
	SOFTWARE_TYPE("Software Type"),
	VSYS("Vsys"),

	// project 相关
	PROJECT("Project"),
	PROJECT_TOPO("Project Topo"),
	PROJECT_ICON("Icon"),
	PROJECT_UNIT("Unit"),

	// module 相关
	MODULE("Module"),

	// endpoint 相关
	ENDPOINT("Endpoint"),

	//alert 相关
	ALERT_RULE("Alert Rule"),
	ALERT_MESSAGE("Alert Message"),
	ALERT_SILENCE("Alert Silence"),
	ALERT_SEVERITY("Alert Severity"),
	ALERT_NOTIFY_METHOD("Notify Method"),

	// dc 相关
	DATACENTER("Datacenter"),
	CABINET("Cabinet"),

	// chart 相关
	CHART("Chart"),
	DASHBOARD("Dashboard"),
	SNAPSHOT("Snapshot"),
	PLAYLIST("Dashboard Playlist"),
	VISUAL_CHART("Chart"),


	// user 相关
	USER("User"),
	ROLE("Role"),
	USER_STARRED("Starred"),
	SYS_USER_PREFERENCE("Preference"),
	SYS_API_KEY("Api Key"),

	// Diagnose
	DIAGNOSE("Diagnose"),
	// tool
	PING("Ping"),
	TRACE_ROUTE("Trace Route"),
	ASSET_DISCOVERY("Asset Discovery"),


	// system 相关
	AGENT("Agent"),
	MIB_FILE("Mib File"),
	SYS_BACKUP("Backup"),
	SYS_CONF("System Config"),
	SYSTEM("System"),
	LINK("Link"),
	EXPRESSION_TMPL("Expression Tmpl"),
	SNMP_CREDENTIAL("Snmp Credential"),
	LICENSE("License"),
	I18N("i18n"),
	CTL("Ctl"),
	IPAM_SUBNET("Ipam Subnet"),
	IPAM_IP("Ipam Ip"),
	RECORD_RULE("Record Rule"),
	ISSUE_RECORD("Issue Record"),
	ISSUE_ACTIVITY("Issue Activity"),
	SYS_FILE("System File"),
	INTEGRATION("Integration"),
	NOTEBOOK("Notebook"),

	TERMINAL("Terminal"),
	SFTP("Sftp"),

	UNKNOWN("unknown");


	private String value;

	public String getValue() {
		return value;
	}

	private TypeEnum(String value) {
		this.value = value;
	}

}
