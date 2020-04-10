package com.nis.common.aspect;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.google.gson.Gson;
import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ann.IdCard;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.R;
import com.nis.modules.sys.entity.SysLogEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysLogService;

import com.nis.common.utils.HttpContextUtils;
import com.nis.common.utils.IPUtils;

import org.apache.shiro.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;

/**
 * 系统日志，切面处理类
 *

 */
@Aspect
@Component
public class SysLogAspect {
	@Autowired
	private SysLogService sysLogService;

	@Pointcut("@annotation(com.nis.common.annotation.SysLog)")
	public void logPointCut() {

	}

	@Around("logPointCut()")
	public Object around(ProceedingJoinPoint point) throws Throwable {
		long beginTime = System.currentTimeMillis();
		//执行方法
		Object result = point.proceed();
		//执行时长(毫秒)
		long time = System.currentTimeMillis() - beginTime;

		//保存日志
		saveSysLog(point, time , result);

		return result;
	}

	private void saveSysLog(ProceedingJoinPoint joinPoint, long time ,Object result) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();

		SysLogEntity sysLog = new SysLogEntity();
		SysLog syslog = method.getAnnotation(SysLog.class);
		// 获取执行结果
		R r = (R)result;
		if(syslog != null){
			//注解上的描述
			sysLog.setOperation(syslog.operation().getValue());
			//类型
			sysLog.setType(syslog.type().getValue());
			// 当 operation = query 时，只记录 code != 200 的内容
			if("query".equals(syslog.operation().getValue())){
				if(!"200".equals(r.get("code").toString())){
					sysLog.setResponse(JSON.toJSONString(r));
				}
			}else {
				sysLog.setResponse(JSON.toJSONString(r));
			}
			if(!"200".equals(r.get("code").toString())){
				sysLog.setException((String) r.get("msg"));
			}
		}


		//请求的方法名
		String className = joinPoint.getTarget().getClass().getName();
		String methodName = signature.getName();
		sysLog.setMethod(className + "." + methodName + "()");

		//请求的参数
		Object[] args = joinPoint.getArgs();
		// 获取请求参数名
		Class clazz = args[0].getClass();
		String oera_id = null;
		if("add".equals(syslog.operation().getValue())||"update".equals(syslog.operation().getValue())){
			Field[] fields = clazz.getDeclaredFields();
			for (Field field: fields){
				Boolean isId = field.isAnnotationPresent(TableId.class);
				if(isId){
					field.setAccessible(true); // 私有属性必须设置访问权限;
					try {
						oera_id = field.get(args[0]).toString();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
					break;
				}
			}
		}
		if("delete".equals(syslog.operation().getValue())){
			oera_id = args[0].toString();
		}
		sysLog.setOperaId(oera_id);
		try{
			String params = new Gson().toJson(args[0]);
			sysLog.setParams(params);
		}catch (Exception e){

		}

		//获取request
		HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
		//设置IP地址
		sysLog.setIp(IPUtils.getIpAddr(request));

		//用户名
		Long userId = ((SysUserEntity) SecurityUtils.getSubject().getPrincipal()).getUserId();
		sysLog.setUserId(userId.intValue());

		sysLog.setTime(time);
		sysLog.setCreateDate(new Date());
		//保存系统日志
		// 查询操作若无异常不记录操作日志
		if(!"query".equals(syslog.operation().getValue()) ||
				("query".equals(syslog.operation().getValue()) && !"200".equals(r.get("code").toString()))){
			sysLogService.save(sysLog);
		}
	}
}
