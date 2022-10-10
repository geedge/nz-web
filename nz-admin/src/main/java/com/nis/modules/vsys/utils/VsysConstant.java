package com.nis.modules.vsys.utils;

public class VsysConstant {

    /**
     * 存放需要 下发配置的 vsys 任务信息
     */
    public static final String VSYS_TASK_QUEUE = "vsys_queue_key";

    /**
     * vsys restconf 认证方式 sys_config key
     */
    public static final String VSYS_RESTCONF_AUTH = "vsys_restconf_auth";

    /**
     * SF setting default template name
     */
    public static final String VSYS_DEFAULT_TEMPLATE_NAME = "tsg_traffic_engine_default";

    /**
     * 默认端口
     */
    public static final String VSYS_TSG_OS_DEFAULT_PORT = "8447";

    /**
     * VSYS_ASSET_REL state SUCCESS
     */
    public static final Integer REL_SUCCESS = 1;

    /**
     * VSYS_ASSET_REL state FAIL
     */
    public static final Integer REL_FAIL = 0;

    /**
     * VSYS_ASSET_REL del_flag DELETE
     */
    public static final Integer REL_DEL = 1;

    /**
     * VSYS_ASSET_REL del_flag NOT DELETE
     */
    public static final Integer REL_NOT_DEL = 0;

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
