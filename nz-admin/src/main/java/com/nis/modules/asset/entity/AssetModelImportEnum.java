package com.nis.modules.asset.entity;


/**
 * asset model 批量导入，导入属性&下标枚举类
 */
public enum AssetModelImportEnum {

    NAME("model name", 0),
    BRAND("brand name", 1),
    CHART_TEMPLATES("chart templates", 2),
    DESCRIPTION("description", 3),
    ASSET_TYPE_NAME("asset type name", 4),
    OID("oid", 5);

    private String name;
    private int index;

    AssetModelImportEnum(String name, int index) {
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
