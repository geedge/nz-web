package com.nis.common.smartvalidate.ann;

import java.lang.annotation.*;

import com.nis.common.utils.RCode;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Documented
public @interface NotNull {
	RCode code() default RCode.ERROR;
}
