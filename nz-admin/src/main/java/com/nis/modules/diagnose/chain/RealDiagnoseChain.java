package com.nis.modules.diagnose.chain;

import com.nis.common.exception.NZException;
import com.nis.common.utils.RCode;

import java.util.List;

/**
 * RealDiagnoseChain 故障排查
 *
 * @author shizhendong
 * @date 2023/1/4
 */
public class RealDiagnoseChain implements DiagnoseInterceptor.Chain {

    private int index;
    private List<DiagnoseInterceptor> interceptors;
    private Object obj;

    private Boolean isContinue = true;


    public RealDiagnoseChain(List<DiagnoseInterceptor> interceptors, int index, Object obj, Boolean isContinue) {
        this.interceptors = interceptors;
        this.index = index;
        this.obj = obj;
        this.isContinue = isContinue;
    }

    @Override
    public void stopDiagnose() {
        isContinue = false;
    }

    @Override
    public boolean isContinueDiagnosis() {
        return this.isContinue;
    }

    @Override
    public Object object() {
        return this.obj;
    }


    @Override
    public void proceed(Object obj) {
        if (index >= interceptors.size()) {
            throw new NZException(RCode.ERROR);
        }

        RealDiagnoseChain next = new RealDiagnoseChain(interceptors, index + 1, obj, this.isContinue);
        DiagnoseInterceptor interceptor = interceptors.get(index);
        interceptor.intercept(next);
    }
}
