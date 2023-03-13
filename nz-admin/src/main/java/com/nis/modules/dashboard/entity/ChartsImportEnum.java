package com.nis.modules.dashboard.entity;

public enum ChartsImportEnum {

    NAME("name",0),
    SPAN("span",1),
    HEIGHT("height",2),
    TYPE("type",3),
    UNIT("unit",4),
    WEIGHT("weight",5),
    PARAM("param",6),
    REMARK("remark",7),
    X("x",8),
    Y("y",9),
    ELEMENTS("elements",10),
    DATASOURCE("datasource",11),
    CHILDREN("children",12);

    private String name;
    private int index;

    ChartsImportEnum(String name, int index) {
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
