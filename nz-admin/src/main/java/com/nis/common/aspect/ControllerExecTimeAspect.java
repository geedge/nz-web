package com.nis.common.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Aspect
@Component
@Slf4j
public class ControllerExecTimeAspect {

    @Pointcut("execution(* com.nis.modules.*.controller.*.*(..))")
    public void logPointCut() {
    }

    /**
     * 只记录执行成功的方法执行时长
     *
     * @param point
     * @return
     * @throws Throwable
     */
    @Around("logPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long beginTime = System.currentTimeMillis();
        Object result = point.proceed();
        long time = System.currentTimeMillis() - beginTime;

        MethodSignature signature = (MethodSignature) point.getSignature();
        String className = point.getTarget().getClass().getName();
        String methodName = className + "." + signature.getName() + "()";

        // 保留两位
        double execTime = new BigDecimal(time / 1000.0).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();

        log.info(String.format("方法:%s,执行 %s 秒,执行 %s 毫秒", methodName, execTime, time));
        return result;
    }


    @AfterThrowing(pointcut = "logPointCut()", throwing = "e")
    public void saveExceptionLog(JoinPoint point, Throwable e) {
       /* MethodSignature signature = (MethodSignature) point.getSignature();
        String className = point.getTarget().getClass().getName();
        String methodName = className + "." + signature.getName() + "()";
        String errorMsg;
        if (e instanceof NZException) {
            errorMsg = ((NZException) e).getMsg();
        } else {
            errorMsg = e.getMessage();
        }
        log.error(String.format("方法: %s 执行失败,异常信息是: %s", methodName,errorMsg));*/
    }
}
