package com.nis.common.utils;

/**
 *操作日志相关枚举类型
 */
public enum OperationEnum {
    QUERY("query"),
    ADD("add"),
    UPDATE("update"),
    DELETE("delete"),
    IMPORT("import"),
    EXPORT("export"),
    CANCEL("cancel"),
    UPLOAD("upload"),
    DOWNLOAD("download"),
    RESET("reset"),
    LOGIN("login"),
    LOGOUT("logout"),
    UNKNOWN("unknown");


    private String value;

    public String getValue() {
        return value;
    }

    OperationEnum( String value) {
        this.value = value;
    }


}
