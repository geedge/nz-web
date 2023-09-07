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
    CONDITION("condition", 5),
    EXT_LABELS("ext_labels", 6),
    UNIT("unit", 7),
    LAST("last", 8),
    SUMMARY("summary", 9),
    DESCRIPTION("description", 10),
    RECEIVER("receiver", 11),
    NOTICES_METHOD("notices method", 12),
    STATE("state", 13),
    AUTO_EXPIRED("auto expired", 14),
    SCHED_ENABLE("schedule enable", 15),
    SCHED_DAYS("schedule days config", 16),
    SCHED_STIME("schedule start time", 17),
    SCHED_ETIME("schedule end time", 18),
    NOTIFY_ACTIVE("notify active", 19),
    NOTIFY_EXPIRED("notify expired", 20),
    TROUBLE_SHOOTING("trouble shooting", 21),
    PARAM("param", 22);

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
