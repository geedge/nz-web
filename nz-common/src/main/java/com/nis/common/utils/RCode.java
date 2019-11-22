package com.nis.common.utils;

public enum RCode {

	SUCCESS(200, "success"), //成功


	SYS_ERROR(100000, "system_error"), //10xxxx，系统类（业务系统、用户组、权限等）通用错误以及其他系统内部错误

	SYS_DB_DUPLICATERECORD(100001, "DUPLICATE_RECORD"), //数据库中已存在该记录
	SYS_DB_AUTH(100002, "auth_error"), //没有权限

	SYS_LOGIN_CAPTCHA(101001, "captcha_error"), //登录验证码错误
	SYS_LOGIN_UNKNOWNACCOUNT(101003, "unknown_account_error"), //未知校验错误
	SYS_LOGIN_USERPWD(101004, "user_or_pwd_error"), //账号或密码错误
	SYS_LOGIN_LOCK(101005, "account_locked_error"), //账号被锁定
	SYS_LOGIN_ACCOUNTAUTH(101006, "account_auth_error"), //账号验证失败

	SYS_MENU_SYSMENUDEL(103001, "sysmenu del error"), //系统菜单，不能删除
	SYS_MENU_DELSUB(103002, "del sub first error"), //请先删除子菜单或按钮
	SYS_USER_OLDPWD(103003, "user oldpwd error"), //原密码不正确
	SYS_USER_DELADMIN(103004, "del admin error"), //系统管理员不能删除
	SYS_USER_CANNOTDEL(103005, "cannot del user error"), //当前用户不能删除
	SYS_USER_DUPLICATENAME(103006, "user name duplicate error"), //业务系统选择错误

	SYS_I18N_DUPLICATE(105001, "duplicate error (code + lang)"), //国际化配置重复（code+lang）
	SYS_I18N_NOSUCHLANG(105002, "no such lang"), //不合法的lang


	SYS_DICT_NAME_ISNULL(106001,"sys_dict_name_isnull"),       // 字典名称不能为空
	SYS_DICT_TYPE_ISNULL(106002,"sys_dict_type_isnull"),       // 字典类型不能为空
	SYS_DICT_VALUE_ISNULL(106003,"sys_dict_value_isnull"),     // 字典值不能为空
	SYS_DICT_VALUE_DUPLICATE(106004,"sys_dict_value_duplicate"),// 字典值重复



	NOT_NULL_ERROR(990, "not null error"), //not null错误
	NOT_NUMBER_ERROR(991, "not number error"), //not number错误
	
	SYSTEM_ID_ISNULL(600001,"System id can not be empty"),
	SYSTEM_NAME_ISNULL(600002,"System name can not be empty"),
	
	MODULE_ID_ISNULL(700001,"Module id can not be empty"),
	MODULE_ASSETID_ISNULL(700002,"Asset id can not be empty"),
	MODULE_SYSTEMID_ISNULL(700003,"System id can not be empty"),
	MODULE_HOST_ISNULL(700004,"Module host info can not be empty"),
	MODULE_PORT_ISNULL(700005,"Module port info can not be empty"),
	MODULE_PATH_ISNULL(700006,"Module path info can not be empty"),
	
	ERROR(999, "error"); //通用错误/未知错误

	
	
	private RCode(Integer code, String msg) {
		this.code = code;
		this.msg = msg;
	}

	private Integer code;
	private String msg;

	public Integer getCode() {
		return code;
	}

	public String getMsg() {
		return msg;
	}
}