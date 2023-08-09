package com.nis.common.utils;

/**
 * 操作日志相关枚举类型
 */
public enum TypeEnum {
	// 类型type
	ASSET("asset"), ASSETACCOUNT("asset account"), ASSETACCOUNTREL("asset account rel"),
	ASSETFIELDGROUP("asset field group"), ASSETFIELDMETA("asset field meta"), ENDPOINT("endpoint"), PROJECT("project"),
	TOPO("project topo"), MODULE("module"), ALERTRULE("alert rule"), ALERTMESSAGE("alert message"),
	CHART("chart"), DASHBOARD("dashboard"), PLAYLIST("dashboard playlist"), VISUALCHART("visual chart"), DATACENTER("datacenter"),
	CABINET("cabinet"), UNKNOWN("unknown"), MODEL("model"), PROMSERVER("promServer"), ACCOUNT("account"), MIB("mib"),
	SYSTEM("system"), LINK("link"), SCRIPT("notification script"), ALERTSILENCE("alert silence"),
	ALERTSEVERITY("alert severity"), ALERTNOTIFYMETHOD("alert notify method"), ASSETSTATECONF("asset state conf"),
	ASSETTYPECONF("asset type conf"), EXPRESSIONTMPL("expression tmpl"), ASSETBRAND("asset brand"),
	SNMPCREDENTIAL("snmp credential"), LICENSE("license"), SYSTEMAPIKEY("sys api key"),I18N("i18n"),CTL("ctl"),IPAMSUBNET("ipam subnet"),
	IPAMIP("ipam ip"),RECORD_RULE("record rule"), ISSUERECORD("issue record"), ISSUEACTIVITY("issue activity"), SYSFILE("sys file"), ASSETCOMMENT("asset comment"),
	VSYS("vsys"), TERMINAL("terminal"), INTEGRATION("integration"), SYS_USER_PREFERENCE("sys user preference");

	private String value;

	public String getValue() {
		return value;
	}

	private TypeEnum(String value) {
		this.value = value;
	}

}
