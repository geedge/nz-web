package com.nis.modules.module.entity;


/**
 * module 批量导入，导入属性&下标枚举类
 */
public enum MonitorModuleImportEnum {

    NAME("module name", 0),
    PROJECT("project name", 1),
    ENDPOINT_NAME_TEMPLATE("endpoint name template", 2),
    CONFIGURATIONS("endpoint config json array", 3),
    CHART_TEMPLATES("chart templates", 4),
    REMARK("remark", 5);

    private String name;
    private int index;

    MonitorModuleImportEnum(String name, int index) {
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
