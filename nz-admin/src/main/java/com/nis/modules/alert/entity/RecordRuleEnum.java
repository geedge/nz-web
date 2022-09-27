package com.nis.modules.alert.entity;

public enum RecordRuleEnum {

    NAME("name", 0),
    TYPE("type", 1),
    EXPR("expr", 2),
    INR("inr", 3),
    STATE("state", 4),
    LABELS("labels", 5),
    REMARK("remark", 6);

    private String name;
    private int index;

    RecordRuleEnum(String name, int index) {
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
