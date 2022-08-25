package com.nis.modules.endpoint.entity;


/**
 * endpoint 批量导入，导入属性&下标枚举类
 */
public enum MonitorEndpointImportEnum {

    PROJECT("project name", 0),
    MODULE("module name", 1),
    NAME("endpoint name", 2),
    ASSET("asset name", 3),
    CONFIGURATIONS("endpoint config json array", 4);

    private String name;
    private int index;

    MonitorEndpointImportEnum(String name, int index) {
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
