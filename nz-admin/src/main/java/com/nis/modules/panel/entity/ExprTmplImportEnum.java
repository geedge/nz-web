package com.nis.modules.panel.entity;


/**
 * expr template 批量导入，导入属性&下标枚举类
 */
public enum ExprTmplImportEnum {

    NAME("name", 0),
    GROUP_NAME("gname", 1),
    EXPRESSION("expression", 2),
    REMARK("remark", 3);

    private String name;
    private int index;

    ExprTmplImportEnum(String name, int index) {
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
