package com.nis.common.smartvalidate.ann;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Documented
public @interface Max {
    int value();
    String code() default "999";
    String msg() default "error";
}
