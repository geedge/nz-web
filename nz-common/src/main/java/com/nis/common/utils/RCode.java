package com.nis.common.utils;

public enum RCode {

	SUCCESS(200, "success"), //成功


	SYS_ERROR(100000, "system error"), //10xxxx，系统类（业务系统、用户组、权限等）通用错误以及其他系统内部错误

	SYS_DB_DUPLICATERECORD(100001, "duplicate record"), //数据库中已存在该记录
	SYS_DB_AUTH(100002, "Permission denied"), //没有权限

	SYS_LOGIN_CAPTCHA(101001, "Captcha verification failed"), //登录验证码错误
	SYS_LOGIN_UNKNOWNACCOUNT(101003, "Unknown error"), //未知校验错误
	SYS_LOGIN_USERPWD(101004, "Incorrect username or password"), //账号或密码错误
	SYS_LOGIN_LOCK(101005, "Locked account"), //账号被锁定
	SYS_LOGIN_ACCOUNTAUTH(101006, "Incorrect username or password"), //账号验证失败
	SYS_LOGIN_REQUIRED(101007, "Please log in to the system first"), //账号验证失败

	SYS_MENU_SYSMENUDEL(103001, "Delete system menu is not allowed"), //系统菜单，不能删除
	SYS_MENU_DELSUB(103002, "Please delete the submenu first"), //请先删除子菜单或按钮
	SYS_USER_OLDPWD(103003, "user oldpwd error"), //原密码不正确
	SYS_USER_DELADMIN(103004, "del admin error"), //系统管理员不能删除
	SYS_USER_CANNOTDEL(103005, "cannot del user error"), //当前用户不能删除
	SYS_USER_DUPLICATENAME(103006, "user name duplicate error"), //业务系统选择错误

	SYS_I18N_DUPLICATE(105001, "duplicate error (code + lang)"), //国际化配置重复（code+lang）
	SYS_I18N_NOSUCHLANG(105002, "no such lang"), //不合法的lang


	SYS_DICT_NAME_ISNULL(106001,"Field 'name' is not null"),       // 字典名称不能为空
	SYS_DICT_TYPE_ISNULL(106002,"Field 'type' is not null"),       // 字典类型不能为空
	SYS_DICT_VALUE_ISNULL(106003,"Field 'type' is not null"),     // 字典值不能为空
	SYS_DICT_CODE_ISNULL(106004,"Field 'code' is not null"),     // 字典值不能为空
	SYS_DICT_VALUE_DUPLICATE(106005,"Field 'type' is duplicate"),// 字典值重复
	SYS_DICT_ID_INVALIDE(106006,"Invalid system dictionary ID"),
	SYS_DICT_ID_ISNULL(106007,"System dictionary ID is null"),


	SYS_CONFIG_MODULEKEY_ISNULL(107001, "System config init module node exporter can not be null"),// 内置module不能为空

	ALERTRULE_ID_ISNULL(201000, "ID is required"), //告警规则ID不能为空
	ALERTRULE_NAME_ISNULL(201001, "Alert name is required"), //告警规则名称不能为空
	ALERTRULE_NAME_DUPLICATE(201002, "Alert name is duplicate"), //告警规则名称不能为空
	ALERTRULE_EXPR_ISNULL(201003, "Expr is required"), //告警规则表达式不能为空
	ALERTRULE_EXPR_ERROR(201004, "Expr syntax error"), //告警规则表达式不合法
	ALERTRULE_LAST_ISNULL(201005, "Last is required"), //告警持续时间不能为空
	ALERTRULE_LAST_ISNOTPOSITIVE(201006, "Last must be a positive number"), //告警持续时间必须是正数
	ALERTRULE_SEVERITY_ISNULL(201007, "Severity is required"), //告警级别不能为空
	ALERTRULE_SEVERITY_INCORRECT(201008, "Severity is incorrect"), //告警级别不合法
	ALERTRULE_LINK_OBJECT_ISNULL(201009,"Association object does not exist"),
	ALERTRULE_TYPE_ISNULL(201010,"Alarm type is empty"),
	ALERTRULE_TYPE_INVALIDE(201011,"Alarm type is invalide"),
	ALERTRULE_BUILDIN_CAN_NOT_REMOVE(201012,"This rule is built-in and cannot be deleted"),
	ALERTRULE_TOPN_ISNULL(201013,"TopN is required"), //告警信息topn不能为空
	
	ALERTMSG_IDS_ISNULL(202000, "Ids is required"), //告警信息ids不能为空
	ALERTMSG_STATE_ISNULL(202001, "State is required"), //告警信息state不能为空
	ALERTMSG_STATE_INCORRECT(202002, "State is incorrect"), //告警信息state不合法
	ALERTMSG_PARSE_ERROR(202003, "Data parse error"), //告警信息解析失败
	ALERTMSG_SAVE_ERROR(202004, "Data save faild"), //告警信息解析失败
	ALERTMSG_TOPN_ISNULL(202005,"TopN is required"),//告警信息topn不能为空

	ACCOUNT_AUTHTYPE_ISNULL(301001,"Authentication type cannot be empty"),
	ACCOUNT_AUTHTYPE_INVALIDE(301002,"Invalid authentication type"),
	ACCOUNT_USER_ISNULL(301003,"Username cannot be empty"),
	ACCOUNT_PWD_ISNULL(301004,"Login password cannot be empty"),
	ACCOUNT_KEYFILE_ISNULL(301005,"Login certificate cannot be empty"),
	ACCOUNT_PORT_ISNULL(301006,"Login port cannot be empty"),
	ACCOUNT_ID_ISNULL(301007,"Account ID cannot be null"),
	ACCOUNT_ID_INVALIDE(301008,"Account ID cannot be null"),
	ACCOUNT_FILE_INSUFFICIENT(301009,"Account insufficient number of documents"), // 文件数量不足
	ACCOUNT_PORT_ERROR(301010,"Account port is incorrect"),                       // 账号端口不正确


	IDC_IDCNAME_ISNULL(401001,"Idc name can not be empty"),              // 数据中心name不能为空
	IDC_IDCID_ISNULL(401002,"Idc id can not be empty"),                  // 数据中心id不能为空
	IDC_IDCPHONE_FORMAT(401003,"Idc phone format error , format such as 18201436079"), // 数据中心电话格式错误
	IDC_PRINCIPAL_NOT_EXISTS(401004,"Idc principal does not exist"),     // 数据中心负责人不存在
	IDC_NAME_DUPLICATE(401005,"Idc name duplicate"),                     // 数据中心名称重复
	IDC_CABINETS_EXISTS(401006,"There are cabinets in the idc"),         // 数据中心内含有机柜
	IDC_ASSETS_EXISTS(401007,"There are assets in the idc"),             // 数据中心内含有资产
	IDC_NOT_EXISTS(401008,"Idc does not exist"),                         // 数据中心不存在
	IDC_NOTHAS_CABINET(401009,"There is no this cabinet in the idc"),    // 数据中心中没有该机柜
	IDC_ID_FORMAT(401010, "Id format error , type should be integer"),   // ID类型错误

	IDC_CABINETNAME_ISNULL(402001, "Cabinet name can not be empty"),     // 机柜名称不能为空
	IDC_CABINETID_ISNULL(402002, "Cabinet id can not be empty"),         // 机柜id不能为空
	IDC_CABINETNAME_DUPLICATE(402003, "Cabinet name duplicate"),         // 机柜名称重复
	IDC_CABINETUSIZE_MINIMUM(402004, "Cabinet u size is a minimum of 1"),// 机柜U位大小最小为1
	IDC_CABINET_ASSETS_EXISTS(402005, "There are assets in the cabinet"),// 机柜中有资产
	IDC_CABINET_MAXU(402006, "The maximum U of current cabinet is 47"),  // 当前机柜最大U为47


	ASSET_ID_ISNULL(501001, "Asset id can not be empty"),                // 资产ID不能为空
	ASSET_SN_ISNULL(501002, "Asset serial number can not be empty"),     // 资产序列号不能为空
	ASSET_HOST_ISNULL(501003, "Asset host can not be empty"),            // 资产主机地址不能为空
	ASSET_HOST_FORMAT(501004, "Asset host format error, format such as xxx.xxx.xxx.xxx(192.168.40.247)"),// 资产主机地址格式
	ASSET_MAINTENANCEID_ISNULL(501005, "Asset maintenance id can not be empty"),// 资产维修记录ID不能为空
	ASSET_NOT_EXIST(501006, "Asset does not exist"),                     // 资产不存在
	ASSET_MAINTENANCE_CONTENT_ISNULL(501007, "Asset maintenance content can not be empty"),// 资产维修记录内容不能为空

	ASSET_MODELNAME_ISNULL(502001, "Asset model name can not be null"),  // 资产型号名称不能为空
	ASSET_MODELID_ISNULL(502002, "Asset model id can not be null"),      // 资产型号ID不能为空
	ASSET_MODELNAME_DUPLICATE(502003, "Asset model name duplicate"),     // 资产型号名称重复
	ASSET_MODELTYPE_NOTFOUND(502004, "Asset model type not found"),      // 资产型号类型未找到
	ASSET_MODELRELATED_ASSET(502005, "There are assets in the model"),   // 资产型号关联资产
	ASSET_MODEL_NOTFOUND(502006, "Asset model not found"),               // 资产型号未找到
	ASSET_IDC_NOTFOUND(502007, "Asset idc not found"),                   // 数据中心未找到
	ASSET_CABINET_NOTFOUND(502008, "Asset cabinet not found"),           // 机柜未找到
	ASSET_HOST_DUPLICATE(502009, "Asset host duplicate"),                // 资产主机地址重复
	ASSET_SN_DUPLICATE(502010, "Asset serial number duplicate"),         // 资产序列号重复
	ASSET_PURCHASEDATE_FORMAT(502011, "Asset purchase date format error, format such as yyyy-MM-dd(2019-02-03)"),// 资产购买日期格式错误
	ASSET_PURCHASEDATE_UNAVAILABLE(502012, "Asset purchase date unavailable"),// 资产购买日期不可用,不合法
	ASSET_STATE_NOTFOUND(502013, "Asset state not found"),                    // 资产状态未找到
	ASSET_ID_FORMAT(502014, "Id format error , type should be integer"),      // ID类型错误
	ASSET_VENDOR_ISNULL(502015, "Model vendor can not be empty"),        // 厂商不能为空
	ASSET_TYPE_ISNULL(502016, "Asset type can not be empty"),            // 资产类型不能为空
	ASSET_VENDOR_NOTFOUND(502017, "Asset model vendor not found"),       // 资产型号厂商未找到
	ASSET_ACCOUNT_ISNULL(502018, "Account can not be null, because exporter is specified"),// 账号信息不能为空，因为指明了需要安装exporter
	ASSET_UPLOAD_FAIL(502019, "The asset and account are saved successfully, but export upload error. You can upload files manually, sorry."),
	ASSET_SN_TOO_LONG(502020, "Asset serial number too long"),           // 资产序列号长度过长
	ASSET_CANCELIMPORTSEQ_ISNULL(502021, "Asset cancel import seq can not be null"),//  导入撤销 seq不能为空
	ASSET_IMPORT_FILE_ISNULL(502022, "Asset import file can not be null"),//  导入文件不能为空

	NOT_NULL_ERROR(990, "not null error"), //not null错误
	NOT_NUMBER_ERROR(991, "not number error"), //not number错误
	
	PROJECT_ID_ISNULL(600001,"Project id can not be empty"),
	PROJECT_NAME_ISNULL(600002,"Project name can not be empty"),
	PROJECT_BUILDIN_CAN_NOT_REMOVE(600003,"This project is built-in and cannot be deleted"),
	PROJECT_NAME_TOO_LONG(600004,"Project name too long"),
	PROJECT_REMARK_TOO_LONG(600005,"Project remark too long"),
	
	MODULE_ID_ISNULL(700001,"Module id can not be empty"),
	MODULE_PROJECTID_ISNULL(700002,"Project id can not be empty"),
	MODULE_NAME_ISNULL(700003,"Module name info can not be empty"),
	MODULE_PORT_ISNULL(700004,"Module port info can not be empty"),
	MODULE_PATH_ISNULL(700005,"Module path info can not be empty"),
	MODULE_PORT_ERROR(700006,"Module port  is incorrect"),
	MODULE_BUILDIN_CAN_NOT_REMOVE(700007,"This module is built-in and cannot be deleted"),
	MODULE_NAME_TOO_LONG(700008,"Module name too long"),
	MODULE_REMARK_TOO_LONG(700009,"Module remark too long"),
	
	PROMSERVER_ID_ISNULL(800001,"PromServer id can not be empty"),
	PROMSERVER_IDCID_ISNULL(800002,"Idc id can not be empty"),
	PROMSERVER_TYPE_ISNULL(800003,"PromServer type can not be empty"),
	PROMSERVER_HOST_ISNULL(800004,"PromServer host can not be empty"),
	PROMSERVER_PORT_ISNULL(800005,"PromServer port can not be empty"),
	PROMSERVER_REQUEST_FAILD(800007,"Qequest faild"),
	PROMSERVER_PARAM_TIME_INVALIDE(800008,"Invalid parameter for time type"),
	PROMSERVER_PARAM_QUERY_ISNULL(800006,"Parameter 'query' is required"),
	PROMSERVER_PARAM_START_ISNULL(800009,"Parameter 'start' is required"),
	PROMSERVER_PARAM_END_ISNULL(800010,"Parameter 'end' is required"),
	PROMSERVER_PARAM_STEP_ISNULL(800011,"Parameter 'step' is required"),
	PROMSERVER_PARAM_MATCH_ISNULL(800012,"Parameter 'match[]' is required"),
	PROMSERVER_PORT_ERROR(800013,"PromServer port is incorrect"),
	PROMSERVER_HOST_ERROR(800014,"PromServer host is incorrect"),
	
	ENDPOINT_ID_ISNULL(900001,"Endpoint id can not be empty"),
	ENDPOINT_MODULEID_ISNULL(900002,"Module Id can not be empty"),
	ENDPOINT_ASSETID_ISNULL(900003,"Asset Id can not be empty"),
	ENDPOINT_PORT_ISNULL(900004,"Port can not be empty"),
	ENDPOINT_PATH_ISNULL(900005,"Path can not be empty"),
	ENDPOINT_QUERYACTIVE_ERROR(900006,"Endpoint query active state error"), // 请求prometheus查询endpoint状态失败

	PANEL_NAME_ISNULL(1000001,"Panel name can not be empty"),
	PANEL_ID_ISNULL(1000002,"Panel ID can not be empty"),

	CHART_TITLE_ISNULL(1100001,"Chart title can not be empty"),
	CHART_SPAN_ISNULL(1100002,"Chart span can not be empty"),
	CHART_HEIGHT_ISNULL(1100003,"Chart height can not be empty"),
	CHART_TYPE_ISNULL(1100004,"Chart type can not be empty"),
	CHART_CREATEAT_ISNULL(1100005,"Chart create time can not be empty"),
	CHART_ELEMENT_ISNULL(1100006,"Chart element can not be empty"),
	CHART_TYPE_INVALIDE(1100007,"Chart type is invalide"),
	CHART_PANEL_ISNULL(1100008,"Panel is not exsits"),
	CHART_NOTEXSITS(1100009,"Chart is not exsits"),
	CHART_ID_ISNULL(1100010,"Chart ID can not be empty"),

	CHARTELEMENT_EXPRESSION_ISNULL(12000001,"Chart element expression can not be empty"),
	CHARTELEMENT_TYPE_ISNULL(12000002,"Chart element type can not be empty"),
	CHARTELEMENT_TYPE_INVALIDE(12000003,"Chart element type is invalide"),
	CHARTELEMENT_NOTEXSITS(12000004,"chart element is not exists"),

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