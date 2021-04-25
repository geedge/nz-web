package com.nis.common.aspect;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableId;
import com.google.gson.Gson;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.sys.entity.SysLogEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysLogService;
import com.nis.modules.sys.shiro.ShiroUtils;
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

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 系统日志，切面处理类
 */
@Aspect
@Component
public class SysLogAspect {
	private static final Log logger = Log.get();
	@Autowired
	private SysLogService sysLogService;

	private static Gson gson = new Gson();

	@Pointcut("@annotation(com.nis.common.annotation.SysLog)")
	public void logPointCut() {

	}

	@Around("logPointCut()")
	public Object around(ProceedingJoinPoint point) throws Throwable {
		// 开始时间
		long beginTime = System.currentTimeMillis();

		// 当前登录用户
		SysUserEntity currentLoginUser = ShiroUtils.getUserEntity();

		// 获取参数 处理参数 方便后续保存操作
		Object[] args = point.getArgs();
		MethodSignature signature = (MethodSignature) point.getSignature();
		Method method = signature.getMethod();
		SysLog syslog = method.getAnnotation(SysLog.class);

		// 处理记录参数
		String paramResult = this.handleArgs(syslog, args, currentLoginUser);

		// 执行方法
		Object result = point.proceed();

		// 执行时长(毫秒)
		long time = System.currentTimeMillis() - beginTime;

		// 保存日志
		saveSysLog(point, time, result, currentLoginUser, paramResult);

		return result;
	}

	/**
	 * 异常返回通知，用于拦截异常日志信息 连接点抛出异常后执行
	 *
	 * @param joinPoint 切入点
	 * @param e         异常信息
	 */
	@AfterThrowing(pointcut = "logPointCut()", throwing = "e")
	public void saveExceptionLog(JoinPoint joinPoint, Throwable e) {
		R r = new R();
		if (e instanceof NZException) {
			r = R.error(((NZException) e).getCode(), ((NZException) e).getMsg());
		} else {
			r = R.error(999, e.getMessage());
		}

		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();

		SysLogEntity sysLog = new SysLogEntity();
		SysLog syslog = method.getAnnotation(SysLog.class);

		String operationType = syslog.operation().getValue();

		sysLog.setOperation(operationType);
		sysLog.setType(syslog.type().getValue());
		sysLog.setResponse(JSON.toJSONString(r));
		sysLog.setException(Tool.StrUtil.utf8Str(r.get("msg")));
		sysLog.setTime(0L);

		// 获取request
		HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
		// 设置IP地址
		sysLog.setIp(IPUtils.getIpAddr(request));
		// 请求的方法名
		String className = joinPoint.getTarget().getClass().getName();
		String methodName = signature.getName();
		sysLog.setMethod(className + "." + methodName + "()");

		// 请求的参数
		Object[] args = joinPoint.getArgs();
		List<Object> params = this.getRecordParams(args);

		// 获取请求参数名
		String oepraId = "";
		if ("update".equals(operationType)) {
			oepraId = getOpId(params);
		}
		sysLog.setOperaId(oepraId);

		// 处理参数记录
		String paramResult = this.handleArgs(syslog, args, ShiroUtils.getUserEntity());
		paramResult = this.handleLogParam(request.getRequestURI(), paramResult);
		if (paramResult.getBytes().length > Constant.MYSQL_TEXT_MAXLENGTH) {
			paramResult = StringUtils.cutString(paramResult, Constant.MYSQL_TEXT_MAXLENGTH);
		}
		sysLog.setParams(paramResult);

		// 登录出现异常时
		if ("login".equals(operationType)) {
			sysLog.setUserId(null);
			Map map = gson.fromJson(gson.toJson(params.get(0)), Map.class);
			map.remove("pin");
			String logParams = gson.toJson(map);
			logParams = ToolUtil.isEmpty(logParams) ? "" : logParams;
			if (logParams.getBytes().length > Constant.MYSQL_TEXT_MAXLENGTH) {
				logParams = StringUtils.cutString(logParams, Constant.MYSQL_TEXT_MAXLENGTH);
			}
			sysLog.setParams(logParams);
		} else {
			Object principal = SecurityUtils.getSubject().getPrincipal();
			if (principal != null) {
				Long userId = ((SysUserEntity) principal).getId();
				sysLog.setUserId(userId == null ? null : userId.intValue());
				sysLog.setApiKeyId(((SysUserEntity) principal).getApiKeyId());
			}
		}

		sysLog.setCreateDate(new Date());
		sysLog.setState("fail");


		sysLogService.save(sysLog);
	}

	private void saveSysLog(ProceedingJoinPoint joinPoint, long time, Object result, SysUserEntity currentLoginUser,
			String paramResult) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();

		SysLogEntity sysLog = new SysLogEntity();
		SysLog syslog = method.getAnnotation(SysLog.class);
		String operationType = syslog.operation().getValue();

		// 获取执行结果
		R r = (R) result;

		// 注解上的描述
		sysLog.setOperation(operationType);
		// 类型
		sysLog.setType(syslog.type().getValue());
		// 当 operation = query 时，只记录 code != 200 的内容
		if (!"query".equals(operationType)) {
			if ("login".equals(operationType)) {
				Map map = JSONObject.parseObject(JSONObject.toJSONString(r), Map.class);
				map.remove("data");
				sysLog.setResponse(JSON.toJSONString(map));
			} else {
				sysLog.setResponse(JSON.toJSONString(r));
			}
		}

		// 由于 导出功能没有返回值 记录为 success , 原因为导出中若有异常会经过 saveExceptionLog（） 方法
		if ("export".equals(operationType)) {
			sysLog.setState("success");
		} else {
			String state = "200".equals(r.get("code").toString()) ? "success" : "fail";
			sysLog.setState(state);
		}

		// 请求的方法名
		String className = joinPoint.getTarget().getClass().getName();
		String methodName = signature.getName();
		sysLog.setMethod(className + "." + methodName + "()");

		// 请求的参数
		Object[] args = joinPoint.getArgs();
		List<Object> params = this.getRecordParams(args);
		// 获取请求参数名
		String oepraId = "";
		if ("add".equals(operationType) || "update".equals(operationType)) {
			oepraId = getOpId(params);
		}
		if ("delete".equals(operationType)) {
			oepraId = JSON.toJSONString(params.get(0)).replace("\"", "");
		}
		sysLog.setOperaId(oepraId);
		// 获取request
		HttpServletRequest request = HttpContextUtils.getHttpServletRequest();

		// 处理 参数信息
		paramResult = this.handleLogParam(request.getRequestURI(), paramResult);
		if (paramResult.getBytes().length > Constant.MYSQL_TEXT_MAXLENGTH) {
			paramResult = StringUtils.cutString(paramResult, Constant.MYSQL_TEXT_MAXLENGTH);
		}
		sysLog.setParams(paramResult);

		Long userId = null;
		Integer apiKeyId = null;
		if (ToolUtil.isEmpty(currentLoginUser)) {
			Object principal = SecurityUtils.getSubject().getPrincipal();
			if (principal != null) {
				userId = ((SysUserEntity) principal).getId();
				apiKeyId = ((SysUserEntity) principal).getApiKeyId();
			}
		} else {
			userId = currentLoginUser.getId();
			apiKeyId = currentLoginUser.getApiKeyId();
		}
		sysLog.setUserId(userId == null ? null : userId.intValue());

		// 设置IP地址
		sysLog.setIp(IPUtils.getIpAddr(request));

		sysLog.setTime(time);
		sysLog.setCreateDate(new Date());

		sysLog.setApiKeyId(apiKeyId);
		// 保存系统日志 注： 查询操作若无异常不记录操作日志
		if (!"query".equals(operationType)) {
			sysLogService.save(sysLog);
		}
	}

	/**
	 * 处理保存参数信息 1. system ldap 不保存 ldap_password 2. system email 不保存
	 * email_smtp_password 3. project topo 不保存 更新配置信息 topo
	 * 
	 * @param requestURI
	 * @param paramResult
	 */
	private String handleLogParam(String requestURI, String paramResult) {
		if (!requestURI.endsWith("project/topo") && !requestURI.endsWith("sysConfig")) {
			return paramResult;
		}
		if (StringUtils.isNotEmpty(paramResult)) {
			try {
				Map<String, Object> map = JSONObject.parseObject(paramResult, Map.class);
				for (Map.Entry<String, Object> obj : map.entrySet()) {
					Object value = obj.getValue();
					if (value instanceof Map) {
						for (Object pojo : ((Map) value).values()) {
							if (pojo instanceof Map && pojo != null) {
								((Map) pojo).remove("ldap_password");
								((Map) pojo).remove("email_smtp_password");
							}
						}
						((Map) value).remove("topo");
						obj.setValue(value);
					}
				}
				return JSONObject.toJSONString(map);
			} catch (Exception e) {
			}
		}
		return paramResult;
	}

	/**
	 * 获取POST/PUT请求回添的实体 ID
	 *
	 * @param args
	 * @return
	 */
	private static String getOpId(List<Object> args) {
		String oepraId = "";
		List<Object> list;
		Map<String, Object> map;
		for (Object arg : args) {
			if (arg == null) {
				continue;
			}
			// endpoint 保存方法参数为 集合形式 List<Endpoint>
			if (arg instanceof List) {
				list = (List<Object>) arg;
				for (Object o : list) {
					oepraId += getEntityId(o) + ",";
				}
				if (ToolUtil.isNotEmpty(oepraId)) {
					oepraId = oepraId.substring(0, oepraId.length() - 1);
				}
			} else if (arg instanceof Map) {
				// 如果参数为 Map 则记录包含id的参数作为回添Id
				map = (Map) arg;
				if (ToolUtil.isNotEmpty(map)) {
					for (Map.Entry<String, Object> entry : map.entrySet()) {
						if (entry.getKey().toLowerCase().indexOf("id") != -1) {
							oepraId += entry.getValue() + ",";
						}
					}
					if (ToolUtil.isNotEmpty(oepraId)) {
						oepraId = oepraId.substring(0, oepraId.length() - 1);
					}
				}
			} else {
				// 单个实体对象方式
				oepraId += getEntityId(arg) + ",";
				;
				if (ToolUtil.isNotEmpty(oepraId)) {
					oepraId = oepraId.substring(0, oepraId.length() - 1);
				}
			}
		}
		return oepraId;
	}

	/**
	 * 获取回添实体Id
	 *
	 * @param arg
	 * @return
	 */
	private static String getEntityId(Object arg) {
		String oepraId = "";
		Class aClass = arg.getClass();
		String name = aClass.getPackage().getName();
		// 只有是本项目实体对象 才获取回添 ID
		if (ToolUtil.isNotEmpty(name) && name.indexOf("com.nis") != -1) {
			Field[] a = aClass.getFields();
			Field[] b = aClass.getDeclaredFields();
			Field[] fields = new Field[a.length + b.length];

			for (int x = 0; x < a.length; x++) {
				fields[x] = a[x];
			}
			for (int y = 0; y < b.length; y++) {
				fields[a.length + y] = b[y];
			}
			for (Field field : fields) {
				Boolean isId = field.isAnnotationPresent(TableId.class);
				if (isId) {
					Object fieldValue = ReflectUtil.getFieldValue(arg, field);
					oepraId = ToolUtil.isNotEmpty(fieldValue) ? fieldValue.toString() : "";
					break;
				}
			}
		}
		return oepraId;
	}

	/**
	 * 去除形参列表中无用参数 例如 ： ServletRequest Response...
	 *
	 * @param args
	 * @return
	 */
	private List<Object> getRecordParams(Object[] args) {
		List<Object> list = new ArrayList<>(Arrays.asList(args));
		Iterator<Object> iterator = list.iterator();
		while (iterator.hasNext()) {
			Object next = iterator.next();
			if (next instanceof ServletResponse || next instanceof ServletRequest) {
				iterator.remove();
			}
		}
		return list;
	}

	/**
	 * 处理记录方法的形参列表
	 *
	 * @param syslog
	 * @param args
	 * @param currentLoginUser
	 * @return
	 */
	public String handleArgs(SysLog syslog, Object[] args, SysUserEntity currentLoginUser) {
		// 需要记录的
		List<Object> list = this.getRecordParams(args);

		String result = "";
		String operationType = syslog.operation().getValue();
		// 退出操作
		if ("logout".equals(operationType)) {
			// 补全 params 为 当前退出者
			Map map = new HashMap();
			map.put("username", currentLoginUser.getName());
			result = gson.toJson(map);
		} else if ("login".equals(operationType)) {
			String params = gson.toJson(list.get(0));
			Map map = gson.fromJson(params, Map.class);
			map.remove("pin");
			result = gson.toJson(map);
		} else {
			if (ToolUtil.isNotEmpty(list)) {
				Map map = new LinkedHashMap();
				for (int i = 0; i < list.size(); i++) {
					map.put("arg" + (i + 1), list.get(i));
				}
				result = gson.toJson(map);
			}
		}
		return result;
	}
}
