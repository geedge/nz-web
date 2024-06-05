package com.nis.modules.asset.entity;


/**
 * dc 批量导入，导入属性&下标枚举类
 */
public enum DcImportEnum {

    NAME("name", 0),
    SUBNET("subnet", 1),
    LOCATION("location", 2),
    TELEPHONE("telephone", 3),
    PRINCIPAL("principal", 4),
    LONGITUDE("longitude", 5),
    LATITUDE("latitude", 6),
    STATE("state", 7),
    REMARK("remark", 8);

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
