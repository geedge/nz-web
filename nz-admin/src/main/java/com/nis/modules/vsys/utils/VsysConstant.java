package com.nis.modules.vsys.utils;

public class VsysConstant {

    // 存放需要 下发配置的 vsys 任务信息
    public static final String VSYS_TASK_QUEUE = "vsys_queue_key";

    // SF setting default template name
    public static final String VSYS_DEFAULT_TEMPLATE_NAME = "tsg_traffic_engine_default";


    /**
     * vsys result enum
     */
    public enum ResultState {

        /**
         * 未执行
         */
        NOT_EXEC(0),

        /**
         * 执行中
         */
        IN_EXEC(1),

        /**
         * 执行失败
         */
        FAILURE(2),

        /**
         * 执行成功
         */
        SUCCESS(3);

        private int value;

        ResultState(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

}
