package com.nis.common.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.shiro.ShiroUtils;

@Aspect
@Component
public class SystemIdAspect {
	
	@Pointcut("execution(* com.nis.modules.*.controller.*.*(..)) && !execution(* com.nis.modules.sys.controller.SysLoginController.*(..)) && !execution(* *login(..))")
    public void systemIdCheck(){}
	
	/**
	 * 对没有业务系统的请求进行登出处理
	 * 
	 * @param point
	 * @return
	 * @throws Throwable
	 */
	@Around("systemIdCheck()")
	public Object check(ProceedingJoinPoint point) throws Throwable {
		SysUserEntity user = ShiroUtils.getUserEntity();
		if (user != null && (user.getCurrSystemId() == null || user.getCurrSystemId() < 1l)) {
			ShiroUtils.logout();
		}
		Object result = point.proceed();
		return result;
	}
}
