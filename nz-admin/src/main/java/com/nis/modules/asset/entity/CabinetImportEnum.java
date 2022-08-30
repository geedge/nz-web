package com.nis.modules.asset.entity;


/**
 * cabinet 批量导入，导入属性&下标枚举类
 */
public enum CabinetImportEnum {

    NAME("name", 0),
    DC("dc name", 1),
    U_SIZE("u size", 2),
    Y("y", 3),
    X("x", 4),
    REMARK("remark", 5);

    private String name;
    private int index;

    CabinetImportEnum(String name, int index) {
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
