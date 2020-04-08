/**

 *

 *

 */

package com.nis.common.annotation;

import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.TypeEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 系统日志注解
 *

 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SysLog {

//	String value() default "";

	OperationEnum operation() default OperationEnum.UNKNOWN;
	TypeEnum type() default TypeEnum.UNKNOWN;
}
