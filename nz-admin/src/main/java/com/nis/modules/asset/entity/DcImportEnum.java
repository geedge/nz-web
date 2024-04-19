package com.nis.modules.asset.entity;


/**
 * dc 批量导入，导入属性&下标枚举类
 */
public enum DcImportEnum {

    NAME("name", 0),
    LOCATION("location", 1),
    TELEPHONE("telephone", 2),
    PRINCIPAL("principal", 3),
    LONGITUDE("longitude", 4),
    LATITUDE("latitude", 5),
    STATE("state", 6),
    REMARK("remark", 7);

    private String name;
    private int index;

    DcImportEnum(String name, int index) {
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
