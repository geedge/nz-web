package com.nis.common.utils;

public enum RCode {

    /**
     * 第一位对应页面菜单项
     * 1：dashboard
     * 2：project
     * 3：assets
     * 4：alerts
     * 5：settings
     * 6: webshell
     *
     * <p>
     * 第二位对应页面左侧菜单
     * 从上到下顺序
     * 1-~
     * <p>
     *
     * 第三位
     * 1：参数不能为空
     * 2：参数重复
     * 3：参数值与要求不符合
     * 4：参数值不在枚举类型内
     * 5：数值类型参数 不在合理范围内
     * 6：内容不存在
     * 7：权限相关错误
     * 8: 登陆相关错误
     * 9：文件相关错误
     * 0: 第三方服务相关错误
     */


    SUCCESS(200, "success"), //成功

    /**
     * dashboard菜单项相关
     */
    PANEL_NAME_ISNULL(111000, "Panel name can not be empty"),
    PANEL_ID_ISNULL(111001, "Panel ID can not be empty"),
    PANEL_NAME_DUPLICATE(112002, "Panel name is duplicate"),


    CHART_TITLE_ISNULL(111000, "Chart title can not be empty"),
    CHART_SPAN_ISNULL(111001, "Chart span can not be empty"),
    CHART_HEIGHT_ISNULL(111002, "Chart height can not be empty"),
    CHART_TYPE_ISNULL(111003, "Chart type can not be empty"),
    CHART_CREATEAT_ISNULL(111004, "Chart create time can not be empty"),
    CHART_ELEMENT_ISNULL(111005, "Chart element can not be empty"),
    CHART_TYPE_INVALIDE(113006, "Chart type is invalide"),
    CHART_PANEL_ISNULL(116007, "Panel is not exsits"),
    CHART_NOTEXSITS(116008, "Chart is not exsits"),
    CHART_ID_ISNULL(111009, "Chart ID can not be empty"),
    CHART_UNIT_ISNULL(111010, "Chart unit can not be empty"),
    CHART_PREV_ISNULL(111011,"Chart prev can not be empty"),
    CHART_NEXT_ISNULL(111012,"Chart next can not be empty"),
    CHART_CANNOT_MODIFY(116013,"Chart can not modify"),
    CHART_PREVNEXT_INCORRECT(113014,"Chart prev and next is incorrect"),
    CHART_TITLE_DUPLICATE(112015,"Chart title is duplicate"),            // 同一个panel 下 chart title重复


    CHARTELEMENT_EXPRESSION_ISNULL(111000, "Chart element expression can not be empty"),
    CHARTELEMENT_TYPE_ISNULL(111001, "Chart element type can not be empty"),
    CHARTELEMENT_TYPE_INVALIDE(113002, "Chart element type is invalide"),
    CHARTELEMENT_NOTEXSITS(116003, "chart element is not exists"),


    /**
     * project菜单项相关
     */
    PROJECT_ID_ISNULL(211000, "Project id can not be empty"),
    PROJECT_NAME_ISNULL(211001, "Project name can not be empty"),
    PROJECT_BUILDIN_CAN_NOT_REMOVE(217002, "This project is built-in and cannot be deleted"),
    PROJECT_NAME_TOO_LONG(213003, "Project name too long"),
    PROJECT_REMARK_TOO_LONG(213004, "Project remark too long"),
    PROJECT_NAME_DUPLICATE(212005, "Project name is duplicate"),


    MODULE_ID_ISNULL(221000, "Module id can not be empty"),
    MODULE_PROJECTID_ISNULL(221001, "Project id can not be empty"),
    MODULE_NAME_ISNULL(221002, "Module name info can not be empty"),
    MODULE_PORT_ISNULL(221003, "Module port info can not be empty"),
    MODULE_PATH_ISNULL(221004, "Module path info can not be empty"),
    MODULE_PORT_ERROR(221005, "Module port  is incorrect"),
    MODULE_BUILDIN_CAN_NOT_REMOVE(227006, "This module is built-in and cannot be deleted"),
    MODULE_NAME_TOO_LONG(223007, "Module name too long"),
    MODULE_REMARK_TOO_LONG(223008, "Module remark too long"),
    SYS_CONFIG_MODULEKEY_ISNULL(221009, "System config init module node exporter can not be null"),// 内置module不能为空
    MODULE_NAME_DUPLICATE(222010, "Module name is duplicate"),


    ENDPOINT_ID_ISNULL(231000, "Endpoint id can not be empty"),
    ENDPOINT_MODULEID_ISNULL(231001, "Module Id can not be empty"),
    ENDPOINT_ASSETID_ISNULL(231002, "Asset Id can not be empty"),
    ENDPOINT_PORT_ISNULL(231003, "Port can not be empty"),
    ENDPOINT_PATH_ISNULL(231004, "Path can not be empty"),
    ENDPOINT_QUERYACTIVE_ERROR(236005, "Endpoint query active state error"), // 请求prometheus查询endpoint状态失败
    ENDPOINT_IMPORT_FILE_ISNULL(231006, "Endpoint import file can not be null"),//  导入文件不能为空
    ENDPOINT_CANCELIMPORTSEQ_ISNULL(231007, "Endpoint cancel import seq can not be null"),//  导入撤销 seq不能为空
    ENDPOINT_HOST_ERROR(233008, "Endpoint host is incorrect"),
    ENDPOINT_PROMETHEUS_AVAILABLE(230009,"Prometheus server available"),
    ENDPOINT_PROMETHEUS_ON_USE(230010,"Prometheus can be used"),
    ENDPOINT_PROMETHEUS_UNUSE(230011,"Prometheus can not be used"),
    /**
     * assets菜单项相关
     */
    ASSET_ID_ISNULL(311000, "Asset id can not be empty"),                 // 资产ID不能为空
    ASSET_SN_ISNULL(311001, "Asset serial number can not be empty"),     // 资产序列号不能为空
    ASSET_HOST_ISNULL(311002, "Asset host can not be empty"),            // 资产主机地址不能为空
    ASSET_HOST_FORMAT(313003, "Asset host format error, format such as xxx.xxx.xxx.xxx(192.168.40.247)"),// 资产主机地址格式
    ASSET_MAINTENANCEID_ISNULL(311004, "Asset maintenance id can not be empty"),// 资产维修记录ID不能为空
    ASSET_NOT_EXIST(316005, "Asset does not exist"),                     // 资产不存在
    ASSET_MAINTENANCE_CONTENT_ISNULL(311006, "Asset maintenance content can not be empty"),// 资产维修记录内容不能为空
    ASSET_IDC_NOTFOUND(316007, "Asset idc not found"),                   // 数据中心未找到
    ASSET_CABINET_NOTFOUND(336008, "Asset cabinet not found"),           // 机柜未找到
    ASSET_HOST_DUPLICATE(312009, "Asset host duplicate"),                // 资产主机地址重复
    ASSET_SN_DUPLICATE(312010, "Asset serial number duplicate"),         // 资产序列号重复
    ASSET_PURCHASEDATE_FORMAT(313011, "Asset purchase date format error, format such as yyyy-MM-dd(2019-02-03)"),// 资产购买日期格式错误
    ASSET_PURCHASEDATE_UNAVAILABLE(313012, "Asset purchase date unavailable"),// 资产购买日期不可用,不合法
    ASSET_STATE_NOTFOUND(316013, "Asset state not found"),                      // 资产状态未找到
    ASSET_ID_FORMAT(313014, "Id format error , type should be integer"),      // ID类型错误
    ASSET_VENDOR_ISNULL(311015, "Model vendor can not be empty"),        // 厂商不能为空
    ASSET_TYPE_ISNULL(311016, "Asset type can not be empty"),            // 资产类型不能为空
    ASSET_VENDOR_NOTFOUND(316017, "Asset model vendor not found"),       // 资产型号厂商未找到
    ASSET_ACCOUNT_ISNULL(311018, "Account can not be null, because exporter is specified"),// 账号信息不能为空，因为指明了需要安装exporter
    ASSET_UPLOAD_FAIL(319019, "The asset and account are saved successfully, but export upload error. You can upload files manually, sorry."),
    ASSET_SN_TOO_LONG(313020, "Asset serial number too long"),           // 资产序列号长度过长
    ASSET_CANCELIMPORTSEQ_ISNULL(311021, "Asset cancel import seq can not be null"),//  导入撤销 seq不能为空
    ASSET_IMPORT_FILE_ISNULL(311022, "Asset import file can not be null"),//  导入文件不能为空
    ASSET_EXPORTER_UPLOAD_FAIL(319023, "Export upload error. You can upload files manually, sorry."),


    ASSET_MODELNAME_ISNULL(321000, "Asset model name can not be null"),  // 资产型号名称不能为空
    ASSET_MODELID_ISNULL(321001, "Asset model id can not be null"),      // 资产型号ID不能为空
    ASSET_MODELNAME_DUPLICATE(322002, "Asset model name duplicate"),     // 资产型号名称重复
    ASSET_MODELTYPE_NOTFOUND(326003, "Asset model type not found"),      // 资产型号类型未找到
    ASSET_MODELRELATED_ASSET(323004, "There are assets in the model"),   // 资产型号关联资产
    ASSET_MODEL_NOTFOUND(326005, "Asset model not found"),               // 资产型号未找到


    ACCOUNT_AUTHTYPE_ISNULL(331000, "Authentication type cannot be empty"),
    ACCOUNT_AUTHTYPE_INVALIDE(333001, "Invalid authentication type"),
    ACCOUNT_USER_ISNULL(331002, "Username cannot be empty"),
    ACCOUNT_PWD_ISNULL(331003, "Login password cannot be empty"),
    ACCOUNT_KEYFILE_ISNULL(331004, "Login certificate cannot be empty"),
    ACCOUNT_PORT_ISNULL(331005, "Login port cannot be empty"),
    ACCOUNT_ID_ISNULL(331006, "Account ID cannot be null"),
    ACCOUNT_ID_INVALIDE(331007, "Account ID cannot be null"),
    ACCOUNT_FILE_INSUFFICIENT(333008, "Account insufficient number of documents"), // 文件数量不足
    ACCOUNT_PORT_ERROR(335009, "Account port is incorrect"),                         // 账号端口不正确
    ACCOUNT_PROTOCOL_ISNULL(331010, "Account protocol cannot be empty"),            // 协议信息不能为空
    ACCOUNT_PROTOCOL_INCORRECT(334011, "Account protocol is incorrect"),            // 协议信息不合法


    /**
     * alerts菜单栏相关
     */
    ALERTMSG_IDS_ISNULL(411000, "Ids is required"),           //告警信息ids不能为空
    ALERTMSG_STATE_ISNULL(411001, "State is required"),      //告警信息state不能为空
    ALERTMSG_STATE_INCORRECT(412002, "State is incorrect"), //告警信息state不合法
    ALERTMSG_PARSE_ERROR(413003, "Data parse error"),       //告警信息解析失败
    ALERTMSG_SAVE_ERROR(413004, "Data save faild"),         //告警信息解析失败
    ALERTMSG_TOPN_ISNULL(411005, "TopN is required"),        //告警信息topn不能为空


    ALERTRULE_ID_ISNULL(421000, "ID is required"), //告警规则ID不能为空
    ALERTRULE_NAME_ISNULL(421001, "Alert name is required"), //告警规则名称不能为空
    ALERTRULE_NAME_DUPLICATE(421002, "Alert name is duplicate"), //告警规则名称不能为空
    ALERTRULE_EXPR_ISNULL(421003, "Expr is required"), //告警规则表达式不能为空
    ALERTRULE_EXPR_ERROR(423004, "Expr syntax error"), //告警规则表达式不合法
    ALERTRULE_LAST_ISNULL(421005, "Last is required"), //告警持续时间不能为空
    ALERTRULE_LAST_ISNOTPOSITIVE(425006, "Last must be a positive number"), //告警持续时间必须是正数
    ALERTRULE_SEVERITY_ISNULL(421007, "Severity is required"),               //告警级别不能为空
    ALERTRULE_SEVERITY_INCORRECT(424008, "Severity is incorrect"),          //告警级别不合法
    ALERTRULE_LINK_OBJECT_ISNULL(426009, "Association object does not exist"),
    ALERTRULE_TYPE_ISNULL(421010, "Alarm type is empty"),
    ALERTRULE_TYPE_INVALIDE(424011, "Alarm type is invalide"),
    ALERTRULE_BUILDIN_CAN_NOT_REMOVE(427012, "This rule is built-in and cannot be deleted"),
    ALERTRULE_TOPN_ISNULL(421013, "TopN is required"), //告警信息topn不能为空


    /**
     * setting菜单项相关
     */
    SYS_ERROR(517000, "system error"), //10xxxx，系统类（业务系统、用户组、权限等）通用错误以及其他系统内部错误

    SYS_DB_DUPLICATERECORD(512001, "duplicate record"), //数据库中已存在该记录
    SYS_DB_AUTH(517002, "Permission denied"),            //没有权限
    SYS_LOGIN_CAPTCHA(518003, "Captcha verification failed"),    //登录验证码错误
    SYS_LOGIN_UNKNOWNACCOUNT(518004, "Unknown error"),            //未知校验错误
    SYS_LOGIN_USERPWD(518005, "Incorrect username or password"), //账号或密码错误
    SYS_LOGIN_LOCK(518006, "Locked account"),                      //账号被锁定
    SYS_LOGIN_ACCOUNTAUTH(518007, "Incorrect username or password"), //账号验证失败
    SYS_LOGIN_REQUIRED(518008, "Please log in to the system first"), //账号验证失败
    SYS_MENU_SYSMENUDEL(517009, "Delete system menu is not allowed"), //系统菜单，不能删除
    SYS_MENU_DELSUB(517010, "Please delete the submenu first"),       //请先删除子菜单或按钮
    SYS_USER_OLDPWD(518011, "user oldpwd error"), //原密码不正确
    SYS_USER_DELADMIN(517012, "del admin error"), //系统管理员不能删除
    SYS_USER_CANNOTDEL(517013, "cannot del user error"),          //当前用户不能删除
    SYS_USER_DUPLICATENAME(517014, "user name duplicate error"), //业务系统选择错误


    PROMSERVER_ID_ISNULL(521000, "PromServer id can not be empty"),
    PROMSERVER_IDCID_ISNULL(521001, "Idc id can not be empty"),
    PROMSERVER_TYPE_ISNULL(521002, "PromServer type can not be empty"),
    PROMSERVER_HOST_ISNULL(521003, "PromServer host can not be empty"),
    PROMSERVER_PORT_ISNULL(521004, "PromServer port can not be empty"),
    PROMSERVER_REQUEST_FAILD(521005, "Qequest faild"),
    PROMSERVER_PARAM_TIME_INVALIDE(523006, "Invalid parameter for time type"),
    PROMSERVER_PARAM_QUERY_ISNULL(521007, "Parameter 'query' is required"),
    PROMSERVER_PARAM_START_ISNULL(521008, "Parameter 'start' is required"),
    PROMSERVER_PARAM_END_ISNULL(521009, "Parameter 'end' is required"),
    PROMSERVER_PARAM_STEP_ISNULL(521010, "Parameter 'step' is required"),
    PROMSERVER_PARAM_MATCH_ISNULL(521011, "Parameter 'match[]' is required"),
    PROMSERVER_PORT_ERROR(523012, "PromServer port is incorrect"),
    PROMSERVER_HOST_ERROR(523013, "PromServer host is incorrect"),
    PROMSERVER_NOTEXSITS(526014, "PromServer is not exsits"),    // PromServer 不存在
    PROMSERVER_EXIST_ERROR(526015, "PromServer is exists : host,type,port repeat"),


    IDC_IDCNAME_ISNULL(531000, "Idc name can not be empty"),              // 数据中心name不能为空
    IDC_IDCID_ISNULL(531001, "Idc id can not be empty"),                  // 数据中心id不能为空
    IDC_IDCPHONE_FORMAT(533002, "Idc phone format error , format such as 18201436079"), // 数据中心电话格式错误
    IDC_PRINCIPAL_NOT_EXISTS(536003, "Idc principal does not exist"),     // 数据中心负责人不存在
    IDC_NAME_DUPLICATE(532004, "Idc name duplicate"),                      // 数据中心名称重复
    IDC_CABINETS_EXISTS(533005, "There are cabinets in the idc"),         // 数据中心内含有机柜
    IDC_ASSETS_EXISTS(533006, "There are assets in the idc"),             // 数据中心内含有资产
    IDC_NOT_EXISTS(536007, "Idc does not exist"),                          // 数据中心不存在
    IDC_NOTHAS_CABINET(536008, "There is no this cabinet in the idc"),   // 数据中心中没有该机柜
    IDC_ID_FORMAT(533009, "Id format error , type should be integer"),  // ID类型错误
    IDC_CABINETNAME_ISNULL(531010, "Cabinet name can not be empty"),     // 机柜名称不能为空
    IDC_CABINETID_ISNULL(531011, "Cabinet id can not be empty"),         // 机柜id不能为空
    IDC_CABINETNAME_DUPLICATE(532012, "Cabinet name duplicate"),         // 机柜名称重复
    IDC_CABINETUSIZE_MINIMUM(535013, "Cabinet u size is a minimum of 1"),// 机柜U位大小最小为1
    IDC_CABINET_ASSETS_EXISTS(533014, "There are assets in the cabinet"),// 机柜中有资产
    IDC_CABINET_MAXU(535015, "The maximum U of current cabinet is 47"),  // 当前机柜最大U为47


    SYS_I18N_DUPLICATE(542000, "duplicate error (code + lang)"), //国际化配置重复（code+lang）
    SYS_I18N_NOSUCHLANG(543001, "no such lang"),                  //不合法的lang


    SYS_DICT_NAME_ISNULL(551000, "Field 'name' is not null"),        // 字典名称不能为空
    SYS_DICT_TYPE_ISNULL(551001, "Field 'type' is not null"),        // 字典类型不能为空
    SYS_DICT_VALUE_ISNULL(551002, "Field 'type' is not null"),       // 字典值不能为空
    SYS_DICT_CODE_ISNULL(551003, "Field 'code' is not null"),        // 字典值不能为空
    SYS_DICT_VALUE_DUPLICATE(552004, "Field 'type' is duplicate"),  //  字典值重复
    SYS_DICT_ID_INVALIDE(553005, "Invalid system dictionary ID"),
    SYS_DICT_ID_ISNULL(551006, "System dictionary ID is null"),


    SNMP_MIB_FILE_ISNULL(561000,"Snmp mib file can not be empty"), // snmp mib文件不能为空
    SNMP_MIB_ID_ISNULL(561001,"Snmp mib id can not be empty"),     // snmp mib id 不能为空


    SYS_CONFIG_TYPE_ISNULL(571000,"System config type can not be null"), // sysConfig 查询type参数不能为空
    SYS_CONFIG_TYPE_INVALIDE(574001,"System config type is invalide"),   // type 不在枚举值类
    SYS_CONFIG_SAVEENTITY_ISNULL(571002,"System config save entity can not be null"),// 保存实体数据不能为空
    SYS_CONFIG_MAXTERMINALNUM_ISNULL(571003, "System config max terminal num can not be null"),// 每个用户最大打开窗口数量不能为空
    SYS_CONFIG_MAXTERMINALNUM_INVALIDE(575004, "System config max terminal num is invalide, Its minimum value is 1"),// 参数值不正确
    SYS_CONFIG_ALERTAPI_ISNULL(571005,"System config alert api can not be null"),    //  alert api 不能为空
    SYS_CONFIG_SWITCHVAUE_INVALIDE(574006,"System config switch value is invalida"), // 开关值不在枚举范围内
    SYS_CONFIG_ASSETPINGSWITCH_ISNULL(571007,"System config asset pings witch can not be null"), // assetpingswitch不能为空
    SYS_CONFIG_NODETARGETPATH_ISNULL(571008,"System config node export target path can not be null"), // node exporter targer path不能为空
    SYS_CONFIG_ASSETPINGINTERVAL_ISNULL(571009,"System config asset ping interval can not be null"),  // asset_ping_interval不能为空
    SYS_CONFIG_ASSETPINGINTERVAL_INVALIDE(573010,"System config asset ping interval is invalide"),    // asset_ping_interval参数与要求不符合
    SYS_CONFIG_EMAILENABLE_ISNULL(571011,"System config email enable can not be empty"),               // email_enable 不能为空
    SYS_CONFIG_EMAILSMTPHOST_ISNULL(571012,"System config email smtp host can not be empty"),         // email_smtp_host 不能为空
    SYS_CONFIG_EMAILSMTPHOST_FORMAT(573013,"System config email smtp host format error, format such as xxx.xxx.xxx.xxx(192.168.40.247)"), // email_smtp_host 格式不正确
    SYS_CONFIG_EMAILSMTPPORT_ISNULL(571014,"System config email smtp port can not be empty"), // email_smtp_port 不能为空
    SYS_CONFIG_EMAILSMTPPORT_ERROR(575015,"System config email smtp port is invalide"),        // email_smtp_port 端口错误
    SYS_CONFIG_EMAILSMTPACCOUNT_ISNULL(571016,"System config email smtp account can not be empty"), // email_smtp_account 不能为空
    SYS_CONFIG_EMAILSMTPPASSWORD_ISNULL(571017,"System config email smtp password can not be empty"), // email_smtp_password 不能为空
    SYS_CONFIG_EMAILSENDACCOUNT_ISNULL(571018,"System config email send account can not be empty"), // email_send_account 不能为空
    SYS_CONFIG_EMAILTESTRECIVER_ISNULL(571019,"System config email test reciver can not be empty"), // email_test_reciver 不能为空
    SYS_CONFIG_EMAILTESTRECIVER_FORMAT(573020,"System config email test reciver format error, format such as xxx@xxx.com"), // email_test_reciver 邮箱格式错误
    SYS_CONFIG_EMAILSSLFLAG_FORMAT(571021,"System config email ssl flag can not be empty"),         // email_ssl_flag 不能为空
    SYS_CONFIG_EMAILTLSFLAG_FORMAT(571022,"System config email tls flag can not be empty"),         // email_tls_flag 不能为空
    SYS_CONFIG_EMAILSSLFLAG_INVALIDE(573023,"System config email ssl flag is invalide"),            // email_ssl_flag 参数与要求不符合
    SYS_CONFIG_EMAILTLSFLAG_INVALIDE(573024,"System config email tls flag is invalide"),            // email_tls_flag 参数与要求不符合
    SYS_CONFIG_LDAPENABLE_ISNULL(571025,"System config ldap enable can not be empty"),              // ldap_enable 不能为空
    SYS_CONFIG_LDAPADDRESS_ISNULL(571026,"System config ldap address can not be empty"),            // ldap_address 不能为空
    SYS_CONFIG_LDAPDN_ISNULL(571027,"System config ldap dn can not be empty"),                      // ldap_dn 不能为空
    SYS_CONFIG_LDAPPASSWORD_ISNULL(571028,"System config ldap password can not be empty"),         // ldap_password 不能为空
    SYS_CONFIG_LDAPUSERFILTER_ISNULL(571029,"System config ldap user filter can not be empty"),    // ldap_user_filter 不能为空
    SYS_CONFIG_LDAPMAPPING_ISNULL(571030,"System config ldap mapping can not be empty"),            // ldap_mapping 不能为空

    /**
     * webshell相关
     */
    TERMINAL_PARAM_ISNULL(611000, "Terminal param can not be null"),
    TERMINAL_UUID_ISNULL(611001, "Terminal uuid can not be null"),
    TERMINAL_PATH_ISNULL(611002, "Terminal path can not be null"),
    TERMINAL_FILE_ISNULL(611003, "Terminal upload file can not be null"),
    TERMINAL_ISNULL(611004, "Error,Terminal is null"),
    TERMINAL_TELNET_TRANSFER(610005,"Telnet does not allow upload and download"),
    
    NOT_NULL_ERROR(990, "not null error"), //not null错误
    NOT_NUMBER_ERROR(991, "not number error"), //not number错误

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