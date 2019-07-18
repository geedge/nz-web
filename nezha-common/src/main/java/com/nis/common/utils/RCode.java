package com.nis.common.utils;

import java.util.HashMap;
import java.util.Map;

public class RCode {
	
	public static Map<String, String> codeMap = new HashMap<>(); 
	
	public static final String SUCCESS = "200"; //成功
	
	public static final String SYS_ERROR = "100000"; //10xxxx，系统类（业务系统、用户组、权限等）通用错误以及其他系统内部错误
	public static final String SYS_DB_DUPLICATERECORD = "100001"; //数据库中已存在该记录
	public static final String SYS_DB_AUTH = "100002"; //没有权限
	public static final String SYS_LOGIN_CAPTCHA = "101001"; //登录验证码错误
	public static final String SYS_LOGIN_NOSYSTEM = "101002"; //无可用业务系统
	public static final String SYS_LOGIN_UNKNOWNACCOUNT = "101003"; //未知校验错误
	public static final String SYS_LOGIN_USERPWD = "101004"; //账号或密码错误
	public static final String SYS_LOGIN_LOCK = "101005"; //账号被锁定
	public static final String SYS_LOGIN_ACCOUNTAUTH = "101006"; //账号验证失败
	public static final String SYS_LOGIN_SELECTSYS = "101007"; //业务系统选择错误
	
	public static final String SYS_SYS_NULLNAME = "102001"; //业务系统为空
	public static final String SYS_SYS_DUPLICATENAME = "102002"; //业务系统名称重复
	
	public static final String SYS_MENU_SYSMENUDEL = "103001"; //系统菜单，不能删除
	public static final String SYS_MENU_DELSUB = "103002"; //请先删除子菜单或按钮
	public static final String SYS_USER_OLDPWD = "103003"; //原密码不正确
	public static final String SYS_USER_DELADMIN = "103004"; //系统管理员不能删除
	public static final String SYS_USER_CANNOTDEL = "103005"; //当前用户不能删除
	
	
	public static final String NODE_ERROR = "200000"; //20xxxx，设备类（节点、节点组、机房、机柜）通用错误
	public static final String DETE_ERROR = "300000"; //30xxxx，监测类（类别、设置、告警）通用错误
	public static final String MISSION_ERROR = "400000"; //40xxxx，任务类通用错误
	public static final String DASHBOARD_ERROR = "500000"; //50xxxx，仪表盘通用错误
	public static final String GRAPH_ERROR = "600000"; //60xxxx，图形类通用错误
	
	
	public static final String NOT_NULL_ERROR = "990"; //通用错误/未知错误
	public static final String ERROR = "999"; //通用错误/未知错误
	
	static {
		codeMap.put(SUCCESS, "success");
		codeMap.put(SYS_ERROR, "system_error");
		codeMap.put(SYS_DB_DUPLICATERECORD, "duplicate_record");
		codeMap.put(SYS_DB_AUTH, "auth_error");
		codeMap.put(SYS_LOGIN_CAPTCHA, "captcha_error");
		codeMap.put(SYS_LOGIN_NOSYSTEM, "no_system_error");
		codeMap.put(SYS_LOGIN_UNKNOWNACCOUNT, "unknown_account_error");
		codeMap.put(SYS_LOGIN_USERPWD, "user_or_pwd_error");
		codeMap.put(SYS_LOGIN_LOCK, "account_locked_error");
		codeMap.put(SYS_LOGIN_ACCOUNTAUTH, "account_auth_error");
		codeMap.put(SYS_LOGIN_SELECTSYS, "select_system_error");
		codeMap.put(SYS_SYS_NULLNAME, "null_systemname_error");
		codeMap.put(SYS_SYS_DUPLICATENAME, "duplicate_systemname_error");
		codeMap.put(SYS_MENU_SYSMENUDEL, "sysmenu_del_error");
		codeMap.put(SYS_MENU_DELSUB, "del_submenu_first_error");
		codeMap.put(SYS_USER_OLDPWD, "user_oldpwd_error");
		codeMap.put(SYS_USER_DELADMIN, "del_adminuser_error");
		codeMap.put(SYS_USER_CANNOTDEL, "cannot_del_user_error");
		
		codeMap.put(NODE_ERROR, "node_error");
		
		codeMap.put(DETE_ERROR, "dete_error");
		
		codeMap.put(MISSION_ERROR, "mission_error");
		
		codeMap.put(DASHBOARD_ERROR, "mission_error");
		
		codeMap.put(GRAPH_ERROR, "graph_error");
		
		codeMap.put(NOT_NULL_ERROR, "not_null_error");
		codeMap.put(ERROR, "error");
	}
}