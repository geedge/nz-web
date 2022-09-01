package com.nis.modules.alert.entity;


/**
 * rule 批量导入，导入属性&下标枚举类
 */
public enum AlertRuleImportEnum {

    NAME("name", 0),
    TYPE("type", 1),
    INTERVAL("interval", 2),
    TIMEOUT("timeout", 3),
    EXPR("expr", 4),
    OPERATOR("operator", 5),
    THRESHOLD("threshold", 6),
    UNIT("unit", 7),
    LAST("last", 8),
    SEVERITY("severity", 9),
    SUMMARY("summary", 10),
    DESCRIPTION("description", 11),
    RECEIVER("receiver", 12),
    NOTICES_METHOD("notices method", 13),
    STATE("state", 14),
    AUTO_EXPIRED("auto expired", 15),
    SCHED_ENABLE("schedule enable", 16),
    SCHED_DAYS("schedule days config", 17),
    SCHED_STIME("schedule start time", 18),
    SCHED_ETIME("schedule end time", 19),
    NOTIFY_ACTIVE("notify active", 20),
    NOTIFY_EXPIRED("notify expired", 21),
    TROUBLE_SHOOTING("trouble shooting", 22);

    private String name;
    private int index;

    AlertRuleImportEnum(String name, int index) {
        this.name = name;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

}
