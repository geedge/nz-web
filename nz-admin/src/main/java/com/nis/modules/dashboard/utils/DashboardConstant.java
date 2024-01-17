package com.nis.modules.dashboard.utils;

import cn.hutool.core.util.StrUtil;
import com.nis.common.exception.NZException;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;

public class DashboardConstant {

    /**
     * dashboard 模板同步锁 key
     */
    public static final String TEMPLATE_SYNC_LOCK_KEY = "dashboard_template_sync_lock";


    /**
     * dashboard snapshot type
     */
    public final static String DASHBOARD_SNAPSHOT_TYPE = "dashboard";

    /**
     * notebook snapshot type
     */
    public final static String NOTEBOOK_SNAPSHOT_TYPE = "notebook";


    /**
     * dashboard type
     */
    public enum Type {
        DASHBOARD("dashboard"),
        ASSET("asset"),
        PROJECT("project"),
        MODULE("module"),
        ENDPOINT("endpoint"),
        TEMPLATE("template");

        private String value;

        Type(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public static Type getInstanceByType(String type) {
            for (Type v : values()) {
                if (StrUtil.equalsIgnoreCase(v.getValue(), type)) {
                    return v;
                }
            }
            throw new IllegalArgumentException("Failed to find dashboard type with parameter.");
        }
    }

    /**
     * dashboard Variable type enum
     */
    public enum VariableType {
        NONE(0),
        ASSET(1),
        ENDPOINT(2);

        private Integer value;

        VariableType(Integer value) {
            this.value = value;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

        public static VariableType getVariableType(Integer value) {
            for (VariableType v : values())
                if (Tool.ObjectUtil.equal(v.getValue(), value)) return v;
            throw new IllegalArgumentException();
        }
    }

    /**
     * notebook type
     */
    public enum NotebookType {
        NONE("none"),
        RUNBOOK("runbook"),
        POSTMORTEM("postmortem"),
        DOCUMENTATION("documentation"),
        REPORT("report"),
        INVESTIGATION("investigation");

        private String value;

        NotebookType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public static NotebookType getInstanceByValue(String value) {
            for (NotebookType v : values()) {
                if (StrUtil.equalsIgnoreCase(v.getValue(), value)) {
                    return v;
                }
            }
            throw new NZException(RCode.NOTEBOOK_TYPE_INVALID);
        }
    }

    public enum NotebookImportEnum {

        NAME("name", 0),
        TYPE("type", 1),
        REMARK("remark", 2),
        PARAM("param", 3),
        CHARTS("charts", 4);

        private String name;
        private int index;

        NotebookImportEnum(String name, int index) {
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

}
