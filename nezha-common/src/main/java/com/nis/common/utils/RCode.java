package com.nis.common.utils;

public enum RCode {
	
	SUCCESS(200, "success"), //成功
	
	
	SYS_ERROR(100000, "system_error"), //10xxxx，系统类（业务系统、用户组、权限等）通用错误以及其他系统内部错误
	
	SYS_DB_DUPLICATERECORD(100001, "DUPLICATE_RECORD"), //数据库中已存在该记录
	SYS_DB_AUTH(100002, "auth_error"), //没有权限
	
	SYS_LOGIN_CAPTCHA(101001, "captcha_error"), //登录验证码错误
	SYS_LOGIN_NOSYSTEM(101002, "no_system_error"), //无可用业务系统
	SYS_LOGIN_UNKNOWNACCOUNT(101003, "unknown_account_error"), //未知校验错误
	SYS_LOGIN_USERPWD(101004, "user_or_pwd_error"), //账号或密码错误
	SYS_LOGIN_LOCK(101005, "account_locked_error"), //账号被锁定
	SYS_LOGIN_ACCOUNTAUTH(101006, "account_auth_error"), //账号验证失败
	SYS_LOGIN_SELECTSYS(101007, "select_system_error"), //业务系统选择错误
	
	SYS_SYS_NULLNAME(102001, "null_systemname_error"), //业务系统名称不能为空
	SYS_SYS_DUPLICATENAME(102002, "duplicate_systemname_error"), //业务系统名称重复
	
	SYS_MENU_SYSMENUDEL(103001, "sysmenu_del_error"), //系统菜单，不能删除
	SYS_MENU_DELSUB(103002, "del_sub_first_error"), //请先删除子菜单或按钮
	SYS_USER_OLDPWD(103003, "user_oldpwd_error"), //原密码不正确
	SYS_USER_DELADMIN(103004, "del_admin_error"), //系统管理员不能删除
	SYS_USER_CANNOTDEL(103005, "cannot_del_user_error"), //当前用户不能删除
	
	
	NODE_ERROR(200000, "node_error"), //20xxxx，设备类（节点、节点组、机房、机柜）通用错误
	
	
	DETE_ERROR(300000, "dete_error"), //30xxxx，监测类（类别、设置、告警）通用错误
	
	
	MISSION_ERROR(400000, "mission_error"), //40xxxx，任务类通用错误
	
	
	DASHBOARD_ERROR(500000, "dashboard_error"), //50xxxx，仪表盘通用错误
	
	
	GRAPH_ERROR(600000, "graph_error"), //60xxxx，图形类通用错误
	
	
	NOT_NULL_ERROR(990, "not_null_error"), //not null错误
	ERROR(999, "error"); //通用错误/未知错误
	

	private RCode(Integer code, String msg) {
		this.code = code;
		this.msg = msg;
	}
	
	public String getMsgByCode(int code) {
		return "";
	}
	
	private Integer code;
	private String msg;
	
	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}
}