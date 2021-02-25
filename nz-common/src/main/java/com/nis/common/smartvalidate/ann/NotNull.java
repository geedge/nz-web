package com.nis.common.smartvalidate.ann;

import com.nis.common.utils.RCode;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Documented
public @interface NotNull {
	RCode code() default RCode.ERROR;
}
