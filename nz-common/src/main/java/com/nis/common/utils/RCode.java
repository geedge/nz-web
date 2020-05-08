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
    CHART_URLPARAM_ISNULL(111016,"Chart param cannot be empty when type is url"), // 当type为url时，param不能为空
    CHART_PARAM_FORMAT(113017,"Chart param must be JSON format"),               // param类型必须为json格式
    CHART_PARAMURL_ISNULL(111018,"Chart param url not found when type is url"),              // param 中未找到url key值
    CHART_PARAMURL_FORMAT(113019,"Chart param url value must be URL format"),  // param 中url的值必须为网址类型
    CHART_PARAMSINGLESTAT_ISNULL(111020,"Chart param statistics not found when type is singleStat"),
    CHART_PARAMSINGLESTAT_INVALIDE(114021,"Chart param statistics must be min, max, average, total, first, last, range  or different"),
    CHART_SINGLESTATPARAM_ISNULL(111022,"Chart param cannot be empty when type is singleStat"),


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
    MODULE_TYPE_ISNULL(221011, "Module type can not be empty"),
    MODULE_TYPE_INVALID(224012, "Module type must be http or snmp"),
    MODULE_SNMPVERSION_INVALID(224013,"Snmp version must be 2 or 3"),
    MODULE_AUTHSECURITYLEVEL_INVALID(224014,"Security level must be one of authPriv, authNoPriv or noAuthNoPriv"),
    MODULE_PRIVPASSWORD_ISNULL(221015,"Priv password is missing, required for SNMPv3 with priv"),
    MODULE_PRIVPROTOCOL_INVALID(224016,"Priv protocol must be DES or AES"),
    MODULE_AUTHPROTOCOL_INVALID(224017,"Auth protocol must be SHA or MD5"),
    MODULE_PASSWORD_INVALID(221018,"Auth password is missing, required for SNMPv3 with auth"),
    MODULE_USERNAME_INVALID(221019,"Auth username is missing, required for SNMPv3"),


    ENDPOINT_ID_ISNULL(231000, "Endpoint id can not be empty"),
    ENDPOINT_MODULEID_ISNULL(231001, "Module Id can not be empty"),
    ENDPOINT_ASSETID_ISNULL(231002, "Asset Id can not be empty"),
    ENDPOINT_PORT_ISNULL(231003, "Port can not be empty"),
    ENDPOINT_PATH_ISNULL(231004, "Path can not be empty"),
    ENDPOINT_QUERYACTIVE_ERROR(236005, "Endpoint query active state error"), // 请求prometheus查询endpoint状态失败
    ENDPOINT_IMPORT_FILE_ISNULL(231006, "Endpoint import file can not be null"),//  导入文件不能为空
    ENDPOINT_CANCELIMPORTSEQ_ISNULL(231007, "Endpoint cancel import seq can not be null"),//  导入撤销 seq不能为空
    ENDPOINT_HOST_ERROR(233008, "Endpoint host is incorrect"),
    ENDPOINT_PROMETHEUS_UNAVAILABLE(230009,"Prometheus server unavailable"),
    ENDPOINT_PROMETHEUS_ON_USE(230010,"Prometheus can be used"),
    ENDPOINT_PROMETHEUS_UNUSE(230011,"Prometheus can not be used"),
    ENDPOINT_PARAM_FORMAT_ERROR(233012,"Endpoint param incorrect format"),
    ENDPOINT_IMPORTFILE_TYPE(239013,"Only support import XLS or XLSX file"),
    ENDPOINT_IMPORTFILE_FAILED(239014,"Import file resolution failed"),
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
    ASSET_PURCHASEDATE_FORMAT(313011, "Asset purchase date format error"),// 资产购买日期格式错误
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
    ASSET_CABINET_ISNULL(311024,"Cabinet must be selected when start or end position is not empty"), // 机柜起始位置不为空 必须指定机柜才可以
    ASSET_POSITION_INVALID(315025,"Asset cabinet start or end value is invalid"),   // 位置选值有误 不在合理返回内
    ASSET_POSITION_MISMATCH(315026,"Asset usize does not match the usize of the associated model"),   // 位置选值有误 不在合理返回内
    ASSET_POSITION_OCCUPY(315027,"Asset selected position is already occupied"),   // 位置已被占用
    ASSET_CABINETSTART_INVALIDE(313028,"Asset cabinet start must be an integer"),  // 开始位置必须为数字类型
    ASSET_CABINETEND_INVALIDE(313029,"Asset cabinet end must be an integer"),      // 结束位置必须为数字类型
    ASSET_SERVERPORT_ERROR(315030,"Asset ipmi port is incorrect"),
    ASSET_SERVERHOST_ERROR(313031,"Asset ipmi host format error, format such as xxx.xxx.xxx.xxx(192.168.40.247)"),
    ASSET_LOGINSERVER_FAILED(310032,"exporter error: User login to target server failed, It may be caused by 1.Authentication failed 2.Server is down or not reachable 3.Server is too slow to respond."),
    ASSET_UPLOADFILE_FAILED(310033,"exporter error: Built in exporter upload failed"),
    ASSET_EXECCOMMAND_FAILED(310034,"exporter error: Failed to run exporter program"),
    ASSET_STATE_INVALIDE(313035,"Asset state must be an integer"),       // 资产状态必须为数字类型
    ASSET_IMPORTFILE_TYPE(319036,"Only support import XLS or XLSX file"),// 导入文件格式只能为xls xlsx
    ASSET_IMPORTFILE_FAILED(319037,"Import file resolution failed"),     // 导入文件解析失败


    ASSET_MODELNAME_ISNULL(321000, "Asset model name can not be null"),  // 资产型号名称不能为空
    ASSET_MODELID_ISNULL(321001, "Asset model id can not be null"),      // 资产型号ID不能为空
    ASSET_MODELNAME_DUPLICATE(322002, "Asset model name duplicate"),     // 资产型号名称重复
    ASSET_MODELTYPE_NOTFOUND(326003, "Asset model type not found"),      // 资产型号类型未找到
    ASSET_MODELRELATED_ASSET(323004, "There are assets in the model"),   // 资产型号关联资产
    ASSET_MODEL_NOTFOUND(326005, "Asset model not found"),               // 资产型号未找到
    ASSET_MODEL_USIZEMINIMUM(325006, "The minimum value of model usize is 1"), // 资产型号u位最小值为1
    ASSET_MODEL_USIZEERROR(323007,"Model usize cannot be smaller than associated asset size"),// 修改的时候model uSize 不能小于关联 asset 的大小
    ASSET_MODEL_EXISTS(322008,"Model already exists"),                     // 型号已经存在


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
    ALERTMSG_REMARK_ISNULL(411006, "Remark is required"),    //告警信息topn不能为空


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
    ALERTRULE_SUMMARY_ISNULL(421014, "Alert summary is required"),
    ALERTRULE_RECEIVER_FORMAT(423015, "Alert receiver id format error , it's should be integer"),


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
    SYS_USER_NOT_EXIST(516015, "System user does not exist"),    //系统用户不存在


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
    IDC_Y_MINIMUM(535016,"Idc y is a minimum of 1"),                       // 数据中心行数最少为1
    IDC_X_MINIMUM(535017,"Idc x is a minimum of 1"),                       // 数据中心列数最少为1
    IDC_Y_ALREADYINUSE(535018,"Idc y is already in use"),                  // 数据中心行已经被使用
    IDC_X_ALREADYINUSE(535019,"Idc x is already in use"),                  // 数据中心列已经被使用
    CABINET_Y_MINIMUM(535020,"Cabinet y is a minimum of 1"),               // 机柜所在行至少为1
    CABINET_X_MINIMUM(535021,"Cabinet x is a minimum of 1"),               // 机柜所在列至少为1
    CABINET_Y_INCORRECT(535022,"Cabinet y is not in this idc"),            // 机柜所选行不在idc范围内
    CABINET_X_INCORRECT(535023,"Cabinet x is not in this idc"),            // 机柜所选列不在idc范围内
    CABINET_POSITION_OCCUPIED(535024,"Cabinet position is occupied"),     // 机柜所选位置已被占用
    IDC_PROMSERVER_EXISTS(533025, "There are promservers in the idc"),
    IDC_TRAFFIC_DIRECTIONS_INVALIDE(533026, "Idc traffic direction must be rx or tx"),
    IDC_TRAFFIC_SETTING_ISNULL(531027, "Idc traffic setting can not be empty"),
    IDC_TRAFFIC_DIRECTIONS_ISNULL(531028, "Idc traffic direction can not be empty"),
    IDC_TRAFFIC_IFINDEX_ISNULL(531029, "Idc traffic Ifindex can not be empty"),
    IDC_TRAFFIC_IFDESCR_ISNULL(531030, "Idc traffic ifdescr can not be empty"),
    IDC_NOT_INCLUDED_ASSET(533031,"Current asset is not included under the data center"),
    IDC_TRAFFIC_HOST_FORMAT(533032,"Idc traffic host must be in IP format"),
    IDC_TRAFFIC_HOST_ISNULL(531033,"Idc traffic host can not be empty"),
    IDC_TRAFFIC_PORT_ISNULL(531034,"Idc traffic port can not be empty"),
    IDC_TRAFFIC_VERSION_ISNULL(531035,"Idc traffic version can not be empty"),
    IDC_TRAFFIC_PORT_INVALIDE(535036,"Idc traffic port is invalide"),
    IDC_TRAFFIC_TAGSKEY_FORMAT(533037,"Idc traffic tags key format is incorrect"),

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
    SNMP_MIB_PARSING_FAILED(569002,"SNMP MIB file parsing failed"),// snmp mib 解析失败
    SNMP_MIB_OPERATION_ISNULL(561003,"Snmp mib operation can not be empty"),// operation不能为空
    SNMP_MIB_HOST_ISNULL(561004,"Snmp mib host can not be empty"),// host 不能为空
    SNMP_MIB_HOST_FORMAT(563005,"Snmp mib host must be in IP format"),// host 必须为IP格式
    SNMP_MIB_PORT_ISNULL(561006,"Snmp mib port can not be empty"),// port 不能为空
    SNMP_MIB_PORT_INVALIDE(565007,"Snmp mib port is invalide"),   // port 错误
    SNMP_MIB_VERSION_ISNULL(561008,"Snmp mib version can not be empty"),// version 不能为空
    SNMP_MIB_OID_ISNULL(561009,"Snmp mib oid can not be empty"),// oid 不能为空
    SNMP_MIB_OPERATION_INVALIDE(563010,"Snmp operation type must be get, walk, getnext or set "),// operation值不在合理返回内
    SNMP_MIB_VERSION_INVALIDE(563011,"Snmp version must be 2 or 3"),// version值不在合理返回内
    SNMP_MIB_AUTH_ISNULL(561012,"Snmp auth can not be empty when version is 3"),      // auth值不在合理返回内
    SNMP_MIB_BROWSER_FAILED(560013,"Failed to execute Broswer according by oid"),
    SNMP_MIB_BROWSER_TIMEOUT(560014,"Browse timed out.It may be caused by 1.The SNMP version number is not supported by agent 2.Agent is down or not reachable 3.Agent is too slow to respond.You can increase the timeout value 4.Browser request has been blocked by firewall 5.Authentication failed.Community name or password is not right"),
    SNMP_MIB_VALUE_ISNULL(561015,"Snmp value cannot be empty when operation is set"),
    SNMP_MIB_TOOBIG(560016,"PDU encoding is too big for the transport used."),
    SNMP_MIB_NOSUCHNAME(560017,"No such variable binding name."),
    SNMP_MIB_BADVALUE(560018,"Bad value in variable binding"),
    SNMP_MIB_READONLY(560019,"The variable binding is read-only"),
    SNMP_MIB_NOACCESS(560020,"The variable binding is not accessible by the current MIB view,No assess"),
    SNMP_MIB_TYPE_ISNULL(561021,"Snmp value type cannot be empty when operation is set"),
    SNMP_MIB_TYPE_INVALIDE(563022,"Snmp value type is invalide"),
    SNMP_MIB_NOTWRITABLE(560023,"The variable's value cannot be modified"),


    SYS_CONFIG_TYPE_ISNULL(571000,"System config type can not be null"), // sysConfig 查询type参数不能为空
    SYS_CONFIG_TYPE_INVALIDE(574001,"System config type is invalide"),   // type 不在枚举值类
    SYS_CONFIG_SAVEENTITY_ISNULL(571002,"System config save entity can not be null"),// 保存实体数据不能为空
    SYS_CONFIG_MAXTERMINALNUM_ISNULL(571003, "System config max terminal num can not be null"),// 每个用户最大打开窗口数量不能为空
    SYS_CONFIG_MAXTERMINALNUM_INVALIDE(575004, "System config max terminal num is invalide, Its minimum value is integer 1"),// 参数值不正确
    SYS_CONFIG_ALERTAPI_ISNULL(571005,"System config alert api can not be null"),    //  alert api 不能为空
    SYS_CONFIG_SWITCHVAUE_INVALIDE(574006,"System config switch value is invalid, must be on or off"), // 开关值不在枚举范围内
    SYS_CONFIG_ASSETPINGSWITCH_ISNULL(571007,"System config asset pings witch can not be null"), // assetpingswitch不能为空
    SYS_CONFIG_NODETARGETPATH_ISNULL(571008,"System config node export target path can not be null"), // node exporter targer path不能为空
    SYS_CONFIG_ASSETPINGINTERVAL_ISNULL(571009,"System config asset ping interval can not be null"),  // asset_ping_interval不能为空
    SYS_CONFIG_ASSETPINGINTERVAL_INVALIDE(573010,"System config asset ping interval should be an integer"),    // asset_ping_interval参数与要求不符合
    SYS_CONFIG_EMAILENABLE_ISNULL(571011,"System config email enable can not be empty"),               // email_enable 不能为空
    SYS_CONFIG_EMAILSMTPHOST_ISNULL(571012,"System config email smtp host can not be empty"),         // email_smtp_host 不能为空
    SYS_CONFIG_EMAILSMTPHOST_FORMAT(573013,"System config email smtp host format error, format such as xxx.xxx.com"), // email_smtp_host 格式不正确
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
    SYS_CONFIG_PASSWORDORTYPE_INVALID(571031,"The password or type is invalid when reset"),
    SYS_CONFIG_PASSWORD_ERROR(571032,"The password is different from the original one when reset"),
    SYS_CONFIG_QUERYPARAMS_ISNULL(571033,"Type and paramKey cannot be empty at the same time when sysconfig queries"),// 查询sysConfig时， type 和 paramKey不能同时为空
    SYS_CONFIG_SCRAPEINTERVAL_ISNULL(571034,"System config scrape interval can not be empty"),            // scrape_interval 不能为空
    SYS_CONFIG_LOCALRETENTION_ISNULL(571035,"System config storage local retention can not be empty"),   // storage_local_retention 不能为空
    SYS_CONFIG_SYSTEMNAME_ISNULL(571036,"System config system name can not be empty"),                    // system_name 不能为空
    SYS_CONFIG_SCRAPEINTERVAL_INVALIDE(573037,"System config scrape interval should be an integer"),    // scrape_interval参数与要求不符合
    SYS_CONFIG_LOCALRETENTION_INVALIDE(573038,"System config storage local retention should be an integer"),    // storage_local_retention参数与要求不符合
    SYS_CONFIG_EMAILSMTPPORT_INVALIDE(573039,"System config email smtp port should be an integer"),     // email_smtp_port参数与要求不符合
    SYS_CONFIG_EMAILSMTPACCOUNT_FORMAT(573040,"System config email smtp account format error, format such as xxx@xxx.com"), //email_smtp_account应为邮件格式
    SYS_CONFIG_LDAPOU_ISNULL(571041,"System config ldap ou can not be empty"),                            // ldap_ou 不能为空
    SYS_CONFIG_TEST_ISNULL(571042,"System config test can not be empty"),                                 // test 不能为空
    SYS_CONFIG_EMAIL_AUTHENTICATIONFAIL(570043,"Failed to connect to SMTP service: username/password wrong"),
    SYS_CONFIG_EMAIL_CONNECTFAIL(570044,"Unable to establish a connection with the SMTP service. Please check whether the SMTP service is running normally and can connect externally, such as host and port information."),
    SYS_CONFIG_EMAIL_OTHERFAILURES(570045,"The connection to the SMTP service timed out. Please check the network condition"),
    SYS_CONFIG_LDAP_AUTHENTICATIONFAIL(570046,"ldap service config connection failed : username/password wrong"),
    SYS_CONFIG_LDAP_CONNECTIONFAILED(570047,"Unable to establish a connection with the ldap service. Please check whether the LDAP service is running normally and can connect externally."),
    SYS_CONFIG_TIMEZONE_ISNULL(571048,"System config timezone can not be empty"),                    // timezone 不能为空
    SYS_CONFIG_TIMEZONE_INVALIDE(573049,"System config timezone should be an integer"),             // timezone参数与要求不符合
    SYS_CONFIG_TIMEZONE_INTERVAL(575050,"The interval value of timezone is between -12 and +12"),  // timezone 区间值为-12至+12之间
    SYS_CONFIG_DEFALUTCABINETU_INTERVAL(573051,"System config default cabinet usize is invalide"), // 配置表中机柜默认值有误
    SYS_CONFIG_LDAP_OU_NOT_EXIST(570052,"Ldap ou not exist"),                                      // ldap连接ou不存在



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
    /**
     * 错误的时间格式
     */
    DateFormatWrong(40090018,"Incorrect Date Format"),

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