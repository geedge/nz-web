package com.nis.common.aspect;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.google.gson.Gson;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ann.IdCard;
import com.nis.common.utils.*;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.sys.entity.SysLogEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysLogService;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统日志，切面处理类
 *

 */
@Aspect
@Component
@Slf4j
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
		Object result = new Object();
		//try {
			 result = point.proceed();
//		}catch (Exception e){
//			log.error("日志记录错误",e.getMessage());
//			result = R.error().put("msg",e.getMessage());
//		}

		//执行时长(毫秒)
		long time = System.currentTimeMillis() - beginTime;

		//保存日志
		saveSysLog(point, time , result);

		return result;
	}

	/**
	 131      * 异常返回通知，用于拦截异常日志信息 连接点抛出异常后执行
	 132      *
	 133      * @param joinPoint 切入点
	 134      * @param e         异常信息
	 135      */
    @AfterThrowing(pointcut = "logPointCut()", throwing = "e")
     public void saveExceptionLog(JoinPoint joinPoint, Throwable e) {
    	R r =new R();
    	if(e instanceof NZException){
    		r = R.error(((NZException) e).getCode(),((NZException) e).getMsg());
		}else {
    		r = R.error(999,e.getMessage());
		}
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();

		SysLogEntity sysLog = new SysLogEntity();
		SysLog syslog = method.getAnnotation(SysLog.class);
		if(syslog != null) {
			sysLog.setOperation(syslog.operation().getValue());
			sysLog.setType(syslog.type().getValue());
		}
		sysLog.setResponse(JSON.toJSONString(r));
		sysLog.setException(r.get("msg").toString());
		sysLog.setTime(0L);
		//获取request
		HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
		//设置IP地址
		sysLog.setIp(IPUtils.getIpAddr(request));
		//请求的方法名
		String className = joinPoint.getTarget().getClass().getName();
		String methodName = signature.getName();
		sysLog.setMethod(className + "." + methodName + "()");

		//请求的参数
		Object[] args = joinPoint.getArgs();
		// 获取请求参数名
		String oepra_id = "";
		if("update".equals(syslog.operation().getValue())){
			oepra_id = getOpId(syslog,args);
		}
		sysLog.setOperaId(oepra_id);
		sysLog.setParams(JSON.toJSONString(args));
		//用户名
		Long userId = ((SysUserEntity) SecurityUtils.getSubject().getPrincipal()).getUserId();
		sysLog.setUserId(userId.intValue());

		sysLog.setCreateDate(new Date());
		sysLog.setState("fail");
		sysLogService.save(sysLog);


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
			if(!"query".equals(syslog.operation().getValue())){
				sysLog.setResponse(JSON.toJSONString(r));
			}
		}
		String state = r.get("code").toString().equals("200")?"success":"fail";
		sysLog.setState(state);
		//请求的方法名
		String className = joinPoint.getTarget().getClass().getName();
		String methodName = signature.getName();
		sysLog.setMethod(className + "." + methodName + "()");

		//请求的参数
		Object[] args = joinPoint.getArgs();
		// 获取请求参数名
		String oera_id = "";
		if("add".equals(syslog.operation().getValue())||"update".equals(syslog.operation().getValue())){
			oera_id = getOpId(syslog,args);
		}
		if("delete".equals(syslog.operation().getValue())){
			oera_id = JSON.toJSONString(args[0]).replace("\"","");
		}
		sysLog.setOperaId(oera_id);
		try{
			String params = new Gson().toJson(args[0]);
			sysLog.setParams(params);
		}catch (Exception e){
			Class clazz = args[0].getClass();
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
		if(!"query".equals(syslog.operation().getValue())){
			sysLogService.save(sysLog);
		}
	}


	private static String getOpId(SysLog syslog,Object[] object){
		String oepraId = "";
		if(syslog.type().getValue().equals(TypeEnum.ENDPOINT.getValue())){
			List<Object> list = (List<Object>) object[0];
			for (Object obj:list){
				Class clazz = obj.getClass();
				Field[] fields = clazz.getDeclaredFields();
				for (Field field : fields) {
					Boolean isId = field.isAnnotationPresent(TableId.class);
					if (isId) {
						// 私有属性必须设置访问权限;
						field.setAccessible(true);
						try {
							oepraId += field.get(obj).toString()+",";
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						}
						break;
					}
				}
			}
			oepraId = oepraId.substring(0, oepraId.length() -1);
		}else {
			Class clazz = object[0].getClass();
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				Boolean isId = field.isAnnotationPresent(TableId.class);
				if (isId) {
					field.setAccessible(true); // 私有属性必须设置访问权限;
					try {
						oepraId = field.get(object[0]).toString();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
					break;
				}
			}
		}
		return oepraId;
	}
}
