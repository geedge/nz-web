package com.nis.common.annotation;

import com.baomidou.mybatisplus.extension.service.IService;

import java.lang.annotation.*;

//类和方法上均要添加此注解
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface  CacheData {
    //缓存的key值，默认为"",为""时，key值为  "memery_cache_"+注解所在类的名称
    String cacheKey() default "";
}
