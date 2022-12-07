package com.nis.modules.panel.utils;

public class DashboardConstant {

    /**
     * dashboard 模板同步锁 key
     */
    public static final String TEMPLATE_SYNC_LOCK_KEY = "dashboardTemplateSynchronizationLock";


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
    }

}
