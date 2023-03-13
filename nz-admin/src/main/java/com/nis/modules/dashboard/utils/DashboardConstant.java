package com.nis.modules.dashboard.utils;

import cn.hutool.core.util.StrUtil;
import com.nis.common.utils.Tool;

public class DashboardConstant {

    /**
     * dashboard 模板同步锁 key
     */
    public static final String TEMPLATE_SYNC_LOCK_KEY = "dashboardTemplateSynchronizationLock";


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

}
