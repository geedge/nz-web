package com.nis.modules.diagnose.chain;


/**
 * DiagnoseInterceptor 故障排查
 *
 * @author shizhendong
 * @date 2023/1/4
 */
public interface DiagnoseInterceptor {

    /**
     * 拦截方法
     *
     * @param chain
     */
    void intercept(Chain chain);

    interface Chain {

        /**
         * 停止诊断
         */
        void stopDiagnose();

        /**
         * 是否继续诊断
         *
         * @return
         */
        boolean isContinueDiagnosis();

        /**
         * 处理对象
         *
         * @return
         */
        Object object();

        /**
         * 继续执行下一个链
         *
         * @param object
         */
        void proceed(Object object);
    }

    /**
     * Diagnose State
     */
    enum DiagnoseState {
        // 检查通过
        PASS(1),
        // 检查未通过
        DID_NOT_PASS(2),
        // 未检查
        NOT_CHECKED(3);

        private Integer value;

        DiagnoseState(Integer value) {
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
