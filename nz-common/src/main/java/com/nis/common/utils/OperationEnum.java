package com.nis.common.utils;

/**
 *操作日志相关枚举类型
 */
public enum OperationEnum {
    // 操作方式operation
    QUERY("query"),
    ADD("add"),
    UPDATE("update"),
    IMPORT("import"),
    EXPORT("export"),
    DELETE("delete"),
    RESET("reset"),
    UNKNOWN("unknown"),
    LOGIN("login"),
    LOGOUT("logout");


    private String value;

    public String getValue() {
        return value;
    }

    OperationEnum( String value) {
        this.value = value;
    }


}
