package com.nis.modules.diagnose.chain;

import com.nis.common.utils.Tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DiagnoseClient 故障排查
 *
 * @author shizhendong
 * @date 2023/1/4
 */
public class DiagnoseClient {

    public static final ThreadLocal<List<Map<Object, Object>>> THREAD_LOCAL = new ThreadLocal();

    /**
     * 创建一个集合来存储拦截器
     */
    private List<DiagnoseInterceptor> interceptors = Tool.ListUtil.list(true);

    public DiagnoseClient(Builder builder) {
        this.interceptors = builder.arrayList;
    }

    public List<DiagnoseInterceptor> interceptors() {
        return interceptors;
    }

    public static class Builder {

        private List<DiagnoseInterceptor> arrayList = Tool.ListUtil.list(true);

        public Builder addInterceptor(DiagnoseInterceptor interceptor) {
            arrayList.add(interceptor);
            return this;
        }

        public DiagnoseClient build() {
            return new DiagnoseClient(this);
        }

    }


    /**
     * diagnose
     *
     * @param obj
     */
    public void diagnose(Object obj) {
        List<DiagnoseInterceptor> interceptors = new ArrayList<>();
        interceptors.addAll(this.interceptors());

        // proceed first interceptor
        DiagnoseInterceptor.Chain chain = new RealDiagnoseChain(interceptors, 0, obj, true);
        chain.proceed(obj);
    }


    /**
     * query Diagnose Msg
     *
     * @return
     */
    public static List<Map<Object, Object>> queryDiagnoseMsg() {
        List<Map<Object, Object>> mapList = Tool.CollUtil.defaultIfEmpty(THREAD_LOCAL.get(), Tool.ListUtil.list(true));
        return mapList;
    }


    /**
     * add Diagnose Msg
     *
     * @param diagnoseMsg
     */
    public static void addDiagnoseMsg(Map<Object, Object> diagnoseMsg) {
        // query Diagnose Msg
        List<Map<Object, Object>> mapList = DiagnoseClient.queryDiagnoseMsg();
        mapList.add(diagnoseMsg);

        THREAD_LOCAL.set(mapList);
    }

    /**
     * add Diagnose Msg List
     *
     * @param diagnoseMsgList
     */
    public static void addDiagnoseMsg(List<Map<Object, Object>> diagnoseMsgList) {
        // query Diagnose Msg
        List<Map<Object, Object>> mapList = DiagnoseClient.queryDiagnoseMsg();
        mapList.addAll(diagnoseMsgList);

        THREAD_LOCAL.set(mapList);
    }

    /**
     * clean Diagnose Msg
     */
    public static void cleanDiagnoseMsg() {
        THREAD_LOCAL.remove();
    }
}
