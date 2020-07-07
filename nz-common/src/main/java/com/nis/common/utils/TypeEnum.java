package com.nis.common.utils;

/**
 *操作日志相关枚举类型
 */
public enum TypeEnum {
    //类型type
    ASSET("asset"),
    ENDPOINT("endpoint"),
    PROJECT("project"),
    MODULE("module"),
    ALERTRULE("alert rule"),
    ALERTMESSAGE("alert message"),
    PANEL("panel"),
    CHART("chart"),
    DATACENTER("datacenter"),
    CABINET("cabinet"),
    UNKNOWN("unknown"),
    MODEL("model"),
    PROMSERVER("promServer"),
    ACCOUNT("account"),
    MIB("mib"),
    SYSTEM("system"),
	LINK("link");
	
    private String value;

    public String getValue(){
        return value;
    }


    private TypeEnum(String value) {
        this.value = value;
    }

}
