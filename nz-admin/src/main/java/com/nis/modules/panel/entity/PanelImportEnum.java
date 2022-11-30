package com.nis.modules.panel.entity;

public enum PanelImportEnum {

    NAME("name", 0),
    TYPE("type", 1),
    VARTYPE("varType", 2),
    PARAM("param", 3),
    REMARK("remark", 4),
    CHARTS("charts", 5),
    CHILDREN("children",6);

    private String name;
    private int index;

    PanelImportEnum(String name, int index) {
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
