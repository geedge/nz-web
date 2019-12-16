package com.nis.common.aspect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.annotation.CacheData;
import com.nis.common.utils.MemeryCacheData;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

@Component
@Aspect
public class CacheDataAspect implements ApplicationContextAware, InitializingBean {

    private ApplicationContext applicationContext;

    @Pointcut("@annotation(com.nis.common.annotation.CacheData)")
    public void cachePointCut(){}

    @AfterReturning("cachePointCut()")
    public void cacheData(JoinPoint joinPoint){

        Signature signature = joinPoint.getSignature();
        Object target = joinPoint.getTarget();
        CacheData annotation = target.getClass().getAnnotation(CacheData.class);
        String cacheKey = annotation.cacheKey();

        //目标所在class
        Class clazz = signature.getDeclaringType();
        if(StringUtils.isBlank(cacheKey)){
            String typeName = clazz.getTypeName();
            cacheKey= MemeryCacheData.CACHE_KEY_PREFIX+typeName;
        }
        if (ServiceImpl.class.isAssignableFrom(clazz)){
            try {
                Method listMethod = clazz.getMethod("list", null);

                Object instance=applicationContext.getBean(clazz);
                List list =(List) listMethod.invoke(instance, null);

                MemeryCacheData.memeryCache.put(cacheKey,list);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext=applicationContext;
    }

    @Override
    public void afterPropertiesSet()  {
        Map<String, Object> needCacheBean = applicationContext.getBeansWithAnnotation(CacheData.class);
        for (Map.Entry<String,Object> entry:needCacheBean.entrySet()){
            ServiceImpl service=(ServiceImpl) entry.getValue();
            try {
                service=(ServiceImpl)getCglibProxyTargetObject(service);
            }catch (Exception e){
                e.printStackTrace();
            }


            CacheData annotation = service.getClass().getAnnotation(CacheData.class);
            String cacheKey = annotation.cacheKey();
            if(StringUtils.isBlank(cacheKey)){
                String typeName = service.getClass().getTypeName();
                cacheKey=MemeryCacheData.CACHE_KEY_PREFIX+typeName;
            }

            List list = service.list();

            MemeryCacheData.memeryCache.put(cacheKey,list);


        }
    }
    private static Object getCglibProxyTargetObject(Object proxy) throws Exception {
        Field h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");
        h.setAccessible(true);
        Object dynamicAdvisedInterceptor = h.get(proxy);

        Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
        advised.setAccessible(true);

        Object target = ((AdvisedSupport)advised.get(dynamicAdvisedInterceptor)).getTargetSource().getTarget();

        return target;
    }

}
