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
     * 7: license
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
    PANEL_NAME_ISNULL(121000, "Panel name can not be empty"),
    PANEL_ID_ISNULL(121001, "Panel ID can not be empty"),
    PANEL_NAME_DUPLICATE(122002, "Panel name is duplicate"),
    PANEL_TYPE_ISNULL(121003, "Panel type can not be empty when link is not empty"),
    PANEL_TYPE_INVALIDE(124004, "Panel type must be dashboard, asset, project, module, endpoint or model"),
    PANEL_LINK_ISNULL(121005,"Panel link can not be empty when the type is not dashboard"),
    PANEL_BUILDIN_CAN_NOT_REMOVE(127006, "This panel is built-in and cannot be deleted"),
    CHART_TITLE_ISNULL(121007, "Chart title can not be empty"),
    CHART_SPAN_ISNULL(121008, "Chart span can not be empty"),
    CHART_HEIGHT_ISNULL(121009, "Chart height can not be empty"),
    CHART_TYPE_ISNULL(121010, "Chart type can not be empty"),
    CHART_CREATEAT_ISNULL(121011, "Chart create time can not be empty"),
    CHART_ELEMENT_ISNULL(121012, "Chart element can not be empty"),
    CHART_TYPE_INVALIDE(123013, "Chart type is invalide"),
    CHART_PANEL_ISNULL(126014, "Panel is not exsits"),
    CHART_NOTEXSITS(126015, "Chart is not exsits"),
    CHART_ID_ISNULL(121016, "Chart ID can not be empty"),
    CHART_UNIT_ISNULL(121017, "Chart unit can not be empty"),
    CHART_PREV_ISNULL(121018,"Chart prev can not be empty"),
    CHART_NEXT_ISNULL(121019,"Chart next can not be empty"),
    CHART_CANNOT_MODIFY(126020,"Chart can not modify"),
    CHART_PREVNEXT_INCORRECT(123021,"Prev and next is incorrect"),
    CHART_TITLE_DUPLICATE(122022,"Chart title is duplicate"),            // 同一个panel 下 chart title重复
    CHART_URLPARAM_ISNULL(121023,"Chart param cannot be empty when type is url"), // 当type为url时，param不能为空
    CHART_PARAM_FORMAT(123024,"Chart param must be JSON format"),               // param类型必须为json格式
    CHART_PARAMURL_ISNULL(121025,"Chart param url not found when type is url"),              // param 中未找到url key值
    CHART_PARAMURL_FORMAT(123026,"Chart param url value must be URL format"),  // param 中url的值必须为网址类型
    CHART_PARAMSINGLESTAT_ISNULL(121027,"Chart param statistics not found when type is singleStat"),
    CHART_PARAMSINGLESTAT_INVALIDE(124028,"Chart param statistics must be min, max, average, total, first, last, range  or different"),
    CHART_SINGLESTATPARAM_ISNULL(121029,"Chart param cannot be empty when type is singleStat"),
    CHART_BUILDIN_CAN_NOT_REMOVE(127030, "This chart is built-in and cannot be deleted"),
    CHARTELEMENT_EXPRESSION_ISNULL(121031, "Chart element expression can not be empty"),
    CHARTELEMENT_TYPE_ISNULL(121032, "Chart element type can not be empty"),
    CHARTELEMENT_TYPE_INVALIDE(123033, "Chart element type is invalide"),
    CHARTELEMENT_NOTEXSITS(126034, "chart element is not exists"),
    PANEL_PREV_ISNULL(121035,"Panel prev can not be empty"),
    PANEL_NEXT_ISNULL(121036,"Panel next can not be empty"),
    PANEL_NOT_EXIST(126037,"Panel does not exist"),
    PANEL_NOTFOUND_BYTYPE(126038,"There is no panel under this type"),
    PANEL_CHARTS_EXISTS(123039, "These panel contain charts and cannot be deleted"),
    PANEL_EXPORT_HEADER_LANGUAGE_ISNULL(121040,"Export header language can not be empty"),
    PANEL_EXPORT_HEADER_LANGUAGE_ERROR(124041,"Export header language must be en, cn and ru"),
    PANEL_IMPORT_FILE_ISNULL(121042, "Panel chart import file can not be null"),
    PANEL_IMPORTFILE_TYPE(129043,"Only support import XLS or XLSX file"),
    PANEL_CANCELIMPORTSEQ_ISNULL(121044, "Panel chart cancel import seq can not be null"),
    PANEL_IMPORTFILE_FAILED(119045,"Import file resolution failed"),     // 导入文件解析失败
    PANEL_EXPORT_HEADER_TEMPLATE_ERROR(114046,"The header row of the excel import template is inconsistent with the system template"),
    CHART_WIDTH_FORMAT(123047, "Chart width format should be a integer"),
    CHART_WIDTH_INVALIDE(123048, "Chart width should be between 1 and 12"),
    CHART_HEIGHT_FORMAT(123049, "Chart height format should be a integer"),
    CHART_UNIT_NOTFOUND(126050, "Chart unit not found"),
    CHART_ELEMENTS_FORMAT(123051,"Chart elements must be JSON format"),
    PANEL_CHART_TEMPLATE_TYPE_INCORRECT(123052, "Panel charts template type must be dashboard, model and asset"),
    PANEL_CHART_IMPORT_LINKID_ISNULL(121053, "When the type is model or asset, link Id cannot be empty"),
    PANEL_CHART_TEMPLATE_TYPE_ISNULL(121054, "Panel charts template type is null"),
    CHART_TEXTPARAM_ISNULL(121055,"Chart param cannot be empty when type is text"),
    CHART_PARAMTEXT_ISNULL(121056,"Chart param text not found when type is text"),
    CHART_WEIGHT_ISNULL(121057,"Chart weight can not be empty"),
    PANEL_PID_ISNULL(121058,"Panel pid can not be empty"),
    PANEL_WEIGHT_ISNULL(121059,"Panel weight not be empty"),
    PANEL_NAME_FORMAT_ERROR(123060,"Panel name format error, can not include '/'"),
    CHART_GROUPID_ISNULL(121061,"Chart group id can not be empty"),
    CHART_VARTYPE_ISNULL(121062,"Chart var type can not be empty when panel id is empty"),
    CHART_PID_ISNULL(121063,"pid can not be empty"),
    CHART_VARID_ISNULL(121064,"varid can not be empty"),
    CHART_SYNCTMPL_ERROR(121065,"Chart sync param can not be null"),
    CHART_VARID_ERROR(121066,"varid param error"),
    CHART_GROUP_NAME_DUPLICATE(121067,"chart type is group name is duplicate"),
    CHART_HEIGHT_INVALIDE(123068, "Chart height should be between 1 and 12"),

    EXPRETMPL_NOT_FOUND(136000, "Expression template not found"),
    EXPRETMPL_NAME_ISNULL(131001, "Expression template name is null"),
    EXPRETMPL_EXPRESSION_ISNULL(131002, "Expression template expression is null"),
    EXPRETMPL_NAME_DUPLICATE(132003, "Expression template name is duplicate"),
    EXPRETMPL_ID_ISNULL(131004, "Expression template id is null"),
    EXPRETMPL_BUILDIN_CAN_NOT_REMOVE(137005, "This expression template is built-in and cannot be deleted"),
    EXPRETMPL_CONTAIN_BABY_CAN_NOT_REMOVE(137006, "This expression template contain sub-templates and cannot be deleted"),
    EXPRETMPL_EXPORT_HEADER_LANGUAGE_ISNULL(131007, "Export header language can not be empty"),
    EXPRETMPL_EXPORT_HEADER_LANGUAGE_ERROR(134008, "Export header language must be en, cn and ru"),
    EXPRETMPL_RENDER_DATA_ISNULL(131009, "Expression template render data is null"),
    EXPRETMPL_IMPORT_FILE_ISNULL(131010, "Expression template import file is null"),
    EXPRETMPL_SEQ_ISNULL(131011, "Cancel import serial number is empty"),
    EXPRETMPL_IMPORTFILE_TYPE(139012, "Only support import XLS or XLSX file"),
    EXPRETMPL_IMPORTFILE_FAILED(139013, "Import file resolution failed"),
    EXPRETMPL_GROUP_NOT_FOUND(136014, "Expression group not found"),
    EXPRETMPL_IMPORT_ERROR(133015, "Expression template import error"),
    EXPRETMPL_BUILDIN_INCORRECT(134016, "Expression template buildin must be 0 or 1"),

    /**
     * project菜单项相关
     */
    PROJECT_ID_ISNULL(211000, "Project id can not be empty"),
    PROJECT_NAME_ISNULL(211001, "Project name can not be empty"),
    PROJECT_BUILDIN_CAN_NOT_REMOVE(217002, "This project is built-in and cannot be deleted"),
    PROJECT_NAME_TOO_LONG(213003, "Project name too long"),
    PROJECT_REMARK_TOO_LONG(213004, "Project remark too long"),
    PROJECT_NAME_DUPLICATE(212005, "Project name is duplicate"),
    PROJECT_REMOVE_ERROR(213006,"These projects contains module can't remove projects"),
    PROJECT_TOPOICON_FILE_ISNULL(211007,"Topo icon file is null"),
    PROJECT_TOPOICON_NAME_ISNULL(211008,"Topo icon name is null"),
    PROJECT_TOPOICON_FILETYPE_ERROR(219009,"Topo icon file type must be jpg, jpeg, png and gif"),
    PROJECT_ICONID_ISNULL(211010, "Topo icon id can not be empty"),
    PROJECT_ICON_NOTFOUND(216011, "Topo icon not found"),
    PROJECT_TOPOCONFIG_ISNULL(211012, "Project topo is null"),
    PROJECT_NOTFOUND(216013, "Project not found"),
    PROJECT_TOPOICON_UNIT_ISNULL(211014,"Topo icon unit is null"),
    PROJECT_BUILDIN_CAN_NOT_EDIT(217015, "The built-in project does not allow editing"),
    PROJECT_ICON_BASE64_VALUEERROR(214016, "The base64 value must be 0 and 1"),

    MODULE_ID_ISNULL(221000, "Module id can not be empty"),
    MODULE_PROJECTID_ISNULL(221001, "Project id can not be empty"),
    MODULE_NAME_ISNULL(221002, "Module name info can not be empty"),
    MODULE_PORT_ISNULL(221003, "Module port info can not be empty"),
    MODULE_PATH_ISNULL(221004, "Module path info can not be empty"),
    MODULE_PORT_ERROR(221005, "Module port is incorrect"),
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
    MODULE_REMOVE_ERROR(223020,"These modules contains endpoint can't remove modules"),
    MODULE_NOTFOUND(226021, "Module not found"),
    MODULE_NOTEXIS_PROJECT(226022, "Module does not exist in the current project"),
    MODULE_TYPE_INCONSISTENT(223023, "Module type is inconsistent with the filling type"),
    MODULE_SNMP_NOTEXIST(226024, "SNMP type module must exist"),
    MODULE_LABELS_FORMAT(223025,"Module labels must be JSON format"),
    MODULE_LABELS_KEY_FORMAT(223026,"Module labels key format is incorrect"),
    MODULE_ENDPOINTNAMETMPL_ISNULL(221027, "Module endpoint name template can not be empty"),
    MODULE_CONFIGS_ISNULL(221028, "Module configs can not be empty"),
    MODULE_CONFIGS_FORMAT(223029,"Module configs must be JSON format"),
    MODULE_SNMP_WALK_ISNULL(221030,"Snmp type module walk oids cannot be empty"),
    MODULE_SNMP_WALK_FORMAT(223031,"Module walk oids format error"),

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
    ENDPOINT_PROMETHEUS_UNUSE(230011,"Endpoint connection refused"),
    ENDPOINT_PARAM_FORMAT_ERROR(233012,"Endpoint param incorrect format"),
    ENDPOINT_IMPORTFILE_TYPE(239013,"Only support import XLS or XLSX file"),
    ENDPOINT_IMPORTFILE_FAILED(239014,"Import file resolution failed"),
    ENDPOINT_EXPORT_HEADER_LANGUAGE_ISNULL(231015, "Export header language can not be empty"),
    ENDPOINT_EXPORT_HEADER_LANGUAGE_ERROR(234016, "Export header language must be en, cn and ru"),
    ENDPOINT_EXPORT_HEADER_TEMPLATE_ERROR(234017, "The header row of the excel import template is inconsistent with the system template"),
    ENDPOINT_SN_HOST_ISNULL(231018, "Sn and host cannot be empty at the same time"),
    ENDPOINT_SN_HOST_MISMATCH(235019, "Asset sn and host address do not correspond"),
    ENDPOINT_LABELS_FORMAT(233020,"Endpoint labels must be JSON format"),
    ENDPOINT_LABELS_KEY_FORMAT(233021,"Endpoint labels key format is incorrect"),
    ENDPOINT_TIMEOUT_UPDATE(233022,"Data has not been updated for more than multiple cycles"),
    ENDPOINT_ENABLED_INVALIDE(234023, "Endpoint enabled must be 0 or 1"),
    ENDPOINT_NOTFOUND(236024, "Endpoint not found"),
    ENDPOINT_NAME_ISNULL(231025, "Endpoint name can not be empty"),
    ENDPOINT_NAME_DUPLICATE(232026, "Endpoint name duplicate"),
    ENDPOINT_PORT_ERROR(231027, "Endpoint port is incorrect"),
    ENDPOINT_ASSET_NAME_HOST_ISNULL(231028, "Asset name and host cannot be empty at the same time"),
    ENDPOINT_IMPORT_ERROR(233029, "Endpoint template import error"),
    ENDPOINT_CONFIGS_FORMAT(233030,"Endpoint configs must be JSON format"),

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
    ASSET_DC_NOTFOUND(316007, "Asset dc not found"),                   // 数据中心未找到
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
    ASSET_TAGKEY_ISNULL(311038,"Asset tag name can no be empty"),
    ASSET_EXPORTER_UPLOADBYSSH(313039,"Must use SSH protocol account to upload exporter"),
    ASSET_EXPORT_HEADER_LANGUAGE_ISNULL(311040,"Export header language can not be empty"),
    ASSET_EXPORT_HEADER_LANGUAGE_ERROR(314041,"Export header language must be en, cn and ru"),
    ASSET_EXPORT_HEADER_TEMPLATE_ERROR(314042,"The header row of the excel import template is inconsistent with the system template"),
    ASSET_TYPE_VALUE_DUPLICATE(312043,"Asset type vlaue is duplicate"),
    ASSET_VENDOR_VALUE_DUPLICATE(312044,"Asset vendor vlaue is duplicate"),
    ASSET_NAME_ISNULL(311045, "Asset name can not be empty"),
    ASSET_MANAGEIP_ISNULL(311046, "Asset manage ip can not be empty"),
    ASSET_MODEL_ID_ISNULL(311047, "Asset model id can not be empty"),
    ASSET_DC_ID_ISNULL(311048, "Asset dc id can not be empty"),
    ASSET_STATE_ID_ISNULL(311049, "Asset state id can not be empty"),
    ASSET_TYPE_ID_ISNULL(311050, "Asset type id can not be empty"),
    ASSET_NAME_DUPLICATE(312051, "Asset name duplicate"),
    ASSET_NUMBER_DUPLICATE(312052, "Asset number duplicate"),
    ASSET_MANAGEIP_DUPLICATE(312053, "Asset manage ip duplicate"),
    ASSET_SSH_PORT_ERROR(315054, "Asset ssh port is incorrect"),
    ASSET_TELNET_PORT_ERROR(315055, "Asset telnet port is incorrect"),
    ASSET_NOT_SELECT_PARENT(311056, "Asset must select the asset host"),
    ASSET_NOT_SUPPORT_MOUNTING(313057, "Asset does not support mounting"),
    ASSET_NOT_UPDATE(313058, "It is currently a virtual machine host and cannot be modified as a virtual machine"),
    ASSET_MANAGEIP_FORMAT(313059, "Asset manage IP address format is incorrect"),
    ASSET_SSHPORT_MORE(312060, "Assets can only fill in one ssh port"),
    ASSET_TELNET_MORE(312061, "Assets can only fill in one telnet port"),
    ASSET_PARENT_NOT_EXIST(316062, "Asset parent does not exist"),
    ASSET_AUTHUSERNAME_ISNULL(311063, "Asset auth username can not be empty"),
    ASSET_AUTHPIN_ISNULL(311064, "Asset auth pin can not be empty"),
    ASSET_AUTHPRIKEY_ISNULL(311065, "Asset auth pri key can not be empty"),
    ASSET_EDITTYPE_ISNULL(311066,"Asset edit type can not be empty"),
    
    ASSET_MODELNAME_ISNULL(321000, "Asset model name can not be null"),  // 资产型号名称不能为空
    ASSET_MODELID_ISNULL(321001, "Asset model id can not be null"),      // 资产型号ID不能为空
    ASSET_MODELNAME_DUPLICATE(322002, "Asset model name duplicate"),     // 资产型号名称重复
    ASSET_MODELTYPE_NOTFOUND(326003, "Asset model type not found"),      // 资产型号类型未找到
    ASSET_MODELRELATED_ASSET(323004, "These mdoels contain assets and cannot be deleted"),   // 资产型号关联资产
    ASSET_MODEL_NOTFOUND(326005, "Asset model not found"),               // 资产型号未找到
    ASSET_MODEL_USIZEMINIMUM(325006, "The minimum value of model usize is 1"), // 资产型号u位最小值为1
    ASSET_MODEL_USIZEERROR(323007,"Model usize cannot be larger than associated asset size"),// 修改的时候model uSize 不能大于关联 asset 的大小
    ASSET_MODEL_EXISTS(322008,"Model already exists"),                     // 型号已经存在
    ASSET_MODEL_PARAM_ISNULL(321009,"The model Id and asset Id cannot both be empty, when the model chart is updated."),
    ASSET_MODELID_INCONSISTENT(323010,"Asset model is inconsistent with model Id"),
    ASSET_IPMITYPE_ERROR(323011,"Asset type must be a server can save ipmi information"),
    ASSET_MODELRELATED_MIBS(323012, "These models contain mibs and cannot be deleted"),
    ASSET_MODEL_BRANDID_ISNULL(321013, "Asset model brand id can not be null"),
    ASSET_MODEL_CHART_ERROR(321014,"Asset model chart template not exist"),

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
    ACCOUNT_PROTOCOL_INCORRECT(334011, "Account protocol must be SSH, TELNET or SNMP"),
    ACCOUNT_PROTOCOL_PARAMS_ISNULL(331012, "Account protocol params can not be empty"),
    ACCOUNT_PROTOCOL_PARAMS_FORMATERROR(333013, "Account protocol params must be JSON format"),
    ACCOUNT_PROTOCOL_EXCEED_COUNT(337014, "An protocol can only have one account"),
    ACCOUNT_SSH_METHOD_INCORRECT(334015,"SSH account login method must be key or password"),
    ACCOUNT_SNMP_USERNAME_ISNULL(331016,"SNMP account username is required for SNMPv3"),
    ACCOUNT_SNMP_SECURITYLEVEL_INVALID(334017,"SNMP account security level must be one of authPriv, authNoPriv or noAuthNoPriv"),
    ACCOUNT_SNMP_VERSION_INVALIDE(333018,"Snmp version must be 2 or 3"),
    ACCOUNT_SNMP_AUTHPROTOCOL_INVALIDE(334019,"Auth protocol must be MD5 or SHA"),
    ACCOUNT_SNMP_PRIVPROTOCOL_INVALIDE(334020,"Priv protocol must be DES or AES"),

    ASSET_ACCOUNT_ID_ISNULL(331001,"asset account id can not be null"),
    ASSET_ACCOUNT_NAME_ISNULL(331002,"asset account name can not be null"),
    ASSET_ACCOUNT_PROTOCOL_ISNULL(331003,"asset account protocol can not be null"),
    ASSET_ACCOUNT_AUTHTYPE_ISNULL(331004,"asset account auth type can not be null"),
    ASSET_ACCOUNT_USERNAME_ISNULL(331005,"asset account username can not be null"),
    ASSET_ACCOUNT_PASSWORD_ISNULL(331006,"asset account password can not be null"),
    ASSET_ACCOUNT_PRIKEY_ISNULL(331007,"asset account prikey can not be null"),
    ASSET_ACCOUNT_USERTIP_ISNULL(331008,"asset account user tip can not be null when auth type is telnet"),
    ASSET_ACCOUNT_PASSWORDTIP_ISNULL(331009,"asset account password tip can not be null when auth type is telnet"),
    ASSET_ACCOUNT_WEIGHT_ISNULL(331010,"asset account id can not be null"),
    ASSET_ACCOUNT_STATE_ISNULL(331011,"asset account id can not be null"),
    ASSET_ACCOUNT_NOT_FOUND(336012, "Asset account not found"),

    STATECONF_NAME_ISNULL(341000,"Asset state conf name is null"),
    STATECONF_PING_ISNULL(341001,"Asset state conf ping is null"),
    STATECONF_MONITOR_ISNULL(341002,"Asset state conf monitor is null"),
    STATECONF_ALERT_ISNULL(341003,"Asset state conf alert is null"),
    STATECONF_NAME_DUPLICATE(342004, "Asset state conf name is duplicate"),
    STATECONF_ID_ISNULL(341005,"Asset state conf id is null"),
    STATECONF_BUILDIN_CAN_NOT_REMOVE(347006, "This config is built-in and cannot be deleted"),
    STATECONF_PING_INCORRECT(344007,"Asset state conf ping must be 0 or 1"),
    STATECONF_MONITOR_INCORRECT(344008,"Asset state conf monitor must be 0 or 1"),
    STATECONF_ALERT_INCORRECT(344009,"Asset state conf alert must be 0 or 1"),
    STATECONF_BUILDIN_INCORRECT(344010,"Asset state conf buildin must be 0 or 1"),
    STATECONF_CAN_NOT_REMOVE(347011, "This config is already used by the asset and cannot be deleted"),
    STATECONF_NOT_FOUND(346012, "Asset state conf not found"),

    TYPECONF_NOT_FOUND(356000, "Asset type config not found"),
    TYPECONF_NAME_ISNULL(351001, "Asset type config name is null"),
    TYPECONF_VM_ISNULL(351002, "Asset type config vm is null"),
    TYPECONF_VMH_ISNULL(351003, "Asset type config vmh is null"),
    TYPECONF_SSH_ISNULL(351004, "Asset type config ssh is null"),
    TYPECONF_TELNET_ISNULL(351005, "Asset type config telnet is null"),
    TYPECONF_NAME_DUPLICATE(352006, "Asset type config name is duplicate"),
    TYPECONF_VM_INCORRECT(354007, "Asset type config vm must be 0 or 1"),
    TYPECONF_VMH_INCORRECT(354008, "Asset type config vmh must be 0 or 1"),
    TYPECONF_SSH_INCORRECT(354009, "Asset type config ssh must be 0 or 1"),
    TYPECONF_TELNET_INCORRECT(354010, "Asset type config telnet must be 0 or 1"),
    TYPECONF_BUILDIN_INCORRECT(354011, "Asset type config buildin must be 0 or 1"),
    TYPECONF_ID_ISNULL(351012, "Asset type config id is null"),
    TYPECONF_BUILDIN_CAN_NOT_REMOVE(357013, "This configuration is built-in and cannot be deleted"),
    TYPECONF_BEUSED_CAN_NOT_REMOVE(357014, "This configurations is already used by the asset and cannot be deleted"),
    TYPECONF_PARENT_NOT_FOUND(356015, "Asset type parent config not found"),
    TYPECONF_CONTAIN_BABY_CAN_NOT_REMOVE(357016, "This configurations contain sub-configuration and cannot be deleted"),
    TYPECONF_AUTHPROTOCOL_ISNULL(351017, "Asset type config auth protocol is null"),
    TYPECONF_SNMPENABLE_ISNULL(351018, "Asset type config snmp enable is null"),
    TYPECONF_SNMPCOLLECT_ISNULL(351019, "Asset type config snmp collect is null"),
    TYPECONF_SSHCOLLECT_ISNULL(351020, "Asset type config ssh collect is null"),
    TYPECONF_SSHCOLLECTSCRIPT_ISNULL(351021, "Asset type config ssh collect script is null"),
    
    ASSET_BRAND_NOT_FOUND(366000, "Asset brand not found"),
    ASSET_BRAND_NAME_ISNULL(361001, "Asset brand name is null"),
    ASSET_BRAND_ID_ISNULL(361002, "Asset brand id is null"),
    ASSET_BRAND_NAME_DUPLICATE(362003, "Asset brand name is duplicate"),
    ASSET_BRAND_BEUSED_CAN_NOT_REMOVE(367004, "This brand is already used and cannot be deleted"),
    ASSET_BRAND_NOT_CONTAIN_MODEL(366005, "This model is not included under this brand"),

    ASSET_FIELD_GROUP_ID_ISNULL(361001,"Asset field group id can not be empty"),
    ASSET_FIELD_GROUP_NAME_ISNULL(361002,"Asset field group name can not be empty"),
    ASSET_FIELD_GROUP_NAME_DUPLICATE(362003,"Asset field group name can not duplicate"),
    
    ASSET_FIELD_META_ID_ISNULL(371001,"Asset field meta id can not be empty"),
    ASSET_FIELD_META_NAME_ISNULL(371002,"Asset field meta name can not be empty"),
    ASSET_FIELD_META_KEY_ISNULL(371003,"Asset field meta key can not be empty"),
    ASSET_FIELD_META_GROUPID_ISNULL(371004,"Asset field meta group id can not be empty"),
    ASSET_FIELD_META_SEARCH_ISNULL(371005,"Asset field meta search can not be empty"),
    ASSET_FIELD_META_DISPLAY_ISNULL(371006,"Asset field meta display can not be empty"),
    ASSET_FIELD_META_TYPE_ISNULL(371007,"Asset field meta type can not be empty"),
    ASSET_FIELD_META_TYPE_ERROR(375008,"Asset field type value out of range"),
    ASSET_FIELD_META_KEY_ERROR(373009,"Asset field key value incorrect format"),
    ASSET_FIELD_META_KEY_DUPLICATE(372010,"Asset field key value duplicate"),
    ASSET_FIELD_META_NOT_FOUND(376011, "Asset field meta not found"),

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
    ALERTRULE_SEVERITY_INCORRECT(424008, "Severity is not exists"),
    ALERTRULE_LINK_OBJECT_ISNULL(426009, "Association object does not exist"),
    ALERTRULE_TYPE_ISNULL(421010, "Alarm type is empty"),
    ALERTRULE_TYPE_INVALIDE(424011, "Alarm type is invalide"),
    ALERTRULE_BUILDIN_CAN_NOT_REMOVE(427012, "This rule is built-in and cannot be deleted"),
    ALERTRULE_TOPN_ISNULL(421013, "TopN is required"), //告警信息topn不能为空
    ALERTRULE_SUMMARY_ISNULL(421014, "Alert summary is required"),
    ALERTRULE_RECEIVER_FORMAT(423015, "Alert receiver id format error , it's should be integer"),
    ALERTRULE_BUILDIN_CAN_NOT_UPDATE(427016, "This rule is built-in and cannot be updated"),
    ALERTRULE_OPERATOR_ISNULL(421017, "Alert operator can not be empty"),
    ALERTRULE_THRESHOLD_ISNULL(421018, "Alert threshold can not be empty"),
    ALERTRULE_UNIT_ISNULL(421019, "Alert unit can not be empty"),
    ALERTRULE_OPERATOR_INVALIDE(423020, "Alert operator is invalide"),
    ALERTRULE_UNIT_INVALIDE(423021, "Alert unit is invalide"),
    ALERTRULE_EXPORT_HEADER_LANGUAGE_ISNULL(421022,"Export header language can not be empty"),
    ALERTRULE_EXPORT_HEADER_LANGUAGE_ERROR(424023,"Export header language must be en, cn and ru"),
    ALERTRULE_IMPORT_FILE_ISNULL(421024, "Alert import file can not be null"),
    ALERTRULE_IMPORTFILE_TYPE(429025,"Only support import XLS or XLSX file"),
    ALERTRULE_CANCELIMPORTSEQ_ISNULL(421026, "Alert rule cancel import seq can not be null"),
    ALERTRULE_THRESHOLD_FORMAT(423027, "Alert threshold format should be a long integer"),
    ALERTRULE_UNIT_FORMAT(423028, "Alert unit format should be a integer"),
    ALERTRULE_DESCRIPTION_ISNULL(4210629, "Alert description can not be empty"),
    ALERTRULE_RECEIVER_NOTFOUND(421030, "Alert receiver user not found"),
    ALERTRULE_IMPORT_ERROR(423031, "Alert import error"),
    ALERTRULE_METHOD_ISNULL(421027, "Alert rule method can not be null"),
    ALERTRULE_METHOD_NOTFOUND(426028, "Alert rule method not found"),

    /**
     * 告警通知脚本
     */
    ALERT_NOTIFISCRIPT_NAME_ISNULL(431001, "Alert notification script name can not be empty"),
    ALERT_NOTIFISCRIPT_FILE_NOTFOUND(436002, "Notification script file not found by file path"),
    ALERT_NOTIFISCRIPT_NOT_FILE(439003, "The file found through this file path is not a file"),
    ALERT_NOTIFISCRIPT_NOT_EXECUTABLE(439004, "The file does not have executable permissions"),
    ALERT_NOTIFISCRIPT_ID_ISNULL(431005, "Alert notification script id can not be empty"),
    ALERT_NOTIFISCRIPT_ACCOUNT_ISNULL(431006, "Alert notification script account can not be empty"),
    ALERT_NOTIFISCRIPT_SCRIPT_NOTFOUND(436007, "Alert notification script not found"),

    /**
     * 告警静默
     */
    ALERTSILENCE_STARTTIME_ISNULL(441001,"AlertSilence start time can not be null"),
    ALERTSILENCE_ENDTIME_ISNULL(441002,"AlertSilence end time can not be null"),
    ALERTSILENCE_ENDTIME_MUSTGTNOW(441003,"AlertSilence end time must great now time"),
    ALERTSILENCE_ID_IS_NOT_NULL(441004,"ids can not be null"),
    ALERTSILENCE_TYPE_ERROR(445005,"AlertSilence type value error"),
    ALERTSILENCE_MATCHER_ISNULL(441006,"AlertSilence matchers can not be null"),

    /**
     *  告警级别
     */
    ALERTSEVERITY_NAME_ISNULL(451001,"Alert severity name can not be null"),
    ALERTSEVERITY_COLOR_ISNULL(451002,"Alert severity color can not be null"),
    ALERTSEVERITY_METHODS_ISNULL(451003,"Alert severity methods can not be null"),
    ALERTSEVERITY_ID_ISNULL(451004,"Alert severity id can not be null"),
    ALERTSEVERITY_REMARK_ISNULL(451005,"Alert severity remark can not be null"),
    ALERTSEVERITY_WEIGHT_PARAM_ISNULL(451006,"Alert severity id or weight is null"),


    /**
     *  告警通知
     */
    ALERTNOTIFY_NOT_EXISTS(461001,"Alert notify method is not exists"),
    ALERTNOTIFY_NAME_ISNULL(461002,"Alert notify method name can not be null"),
    ALERTNOTIFY_FILE_NOTEXISTS(461003,"Alert notify method script file is not exsits"),
    ALERTNOTIFY_ACCOUNT_ISNULL(461004,"Alert notify method account can not be null"),
    ALERTNOTIFY_STATE_ISNULL(461005,"Alert notify method state can not be null"),
    ALERTNOTIFY_ID_ISNULL(461006,"Alert notify method ID can not be null"),
    ALERTNOTIFY_FILEPATH_ISNULL(461007,"Alert notify method file path can not be null"),
    ALERTNOTIFY_NAME_DUPLICATE(462008, "Alert notify method name duplicate"),
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
    SYS_USER_CANNOTDEL(517013, "The currently logged in user cannot be deleted"),          //当前登录用户不能删除
    SYS_USER_DUPLICATENAME(517014, "user name duplicate error"), //业务系统选择错误
    SYS_USER_NOT_EXIST(516015, "System user does not exist"),    //系统用户不存在
    SYS_USER_USERNAME_ISNULL(511016, "System user login name can not be empty"),
    SYS_USER_STATUS_ISNULL(511017, "System user status can not be empty"),
    SYS_USER_PASSWORD_ISNULL(511018, "System user password can not be empty"),
    SYS_USER_ID_ISNULL(511019, "System user id can not be empty"),
    SYS_USER_SCRIPT_ACCOUNT_REPEATED(512020, "System user script id and account cannot be repeated"),
    SYS_USER_STATUS_INVALIDE(514021, "System user status must be 0 or 1"),
    SYS_USER_EMAIL_FORMAT(513022, "System user email format is incorrect"),
    SYS_MENU_ISNULL(516023, "Menu is not exsits"),
    SYS_MENU_ID_ISNULL(511024, "Menu id is required"),
    SYS_MENU_NAME_ISNULL(511025, "Menu name is required"),
    SYS_MENU_CODE_ISNULL(511026, "Menu code is required"),
    SYS_MENU_I18N_ISNULL(511027, "Menu i18n is required"),
    SYS_MENU_TYPE_ISNULL(511028, "Menu type is required"),
    SYS_MENU_ORDERNUM_ISNULL(511029, "Menu order num is required"),
    SYS_MENU_CODE_DUPLICATE(512030, "Menu code duplicate"),
    SYS_MENU_TYPE_INVALIDE(513031,"Menu type not found"),
    SYS_MENU_BUTTON_PARENTID_ISNULL(511032,"Button must have a parent id"),
    SYS_MENU_BUTTON_PARENTTYPE_INVALIDE(513033,"Button cannot be used as a parent menu"),
    SYS_TOKEN_ISNULL(511034,"Cannot get token information from request header"),
    SYS_USER_NAME_ISNULL(511035, "System user name can not be empty"),
    SYS_USER_NAME_DUPLICATE(511036, "System user name duplicate"),
    SYS_MENU_TAB_PARENTTYPE_INVALIDE(513037,"Tab cannot be used as a parent menu"),

    PROMSERVER_ID_ISNULL(521000, "PromServer id can not be empty"),
    PROMSERVER_DCID_ISNULL(521001, "Dc id can not be empty"),
    PROMSERVER_TYPE_ISNULL(521002, "PromServer type can not be empty"),
    PROMSERVER_HOST_ISNULL(521003, "PromServer host can not be empty"),
    PROMSERVER_PORT_ISNULL(521004, "PromServer port can not be empty"),
    PROMSERVER_REQUEST_FAILD(521005, "Request faild"),
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
    PROMSERVER_TYPE_INVALIDE(523016,"PromServer type is incorrect"),
    PROMSERVER_UNAVAILABLE(526017,"No PromServer available"),
    PROMSERVER_TOKEN_ISNULL(521018, "PromServer token info can not be empty"),


    DC_NAME_ISNULL(531000, "Dc name can not be empty"),              // 数据中心name不能为空
    DC_ID_ISNULL(531001, "Dc id can not be empty"),                  // 数据中心id不能为空
    DC_PHONE_FORMAT(533002, "Dc phone format error"),
    DC_PRINCIPAL_NOT_EXISTS(536003, "Dc principal does not exist"),     // 数据中心负责人不存在
    DC_NAME_DUPLICATE(532004, "Dc name duplicate"),                      // 数据中心名称重复
    DC_CABINETS_EXISTS(533005, "These Dc contain cabinets and cannot be deleted"),         // 数据中心内含有机柜
    DC_ASSETS_EXISTS(533006, "These Dc contain assets and cannot be deleted"),             // 数据中心内含有资产
    DC_NOT_EXISTS(536007, "Dc does not exist"),                          // 数据中心不存在
    DC_NOTHAS_CABINET(536008, "There is no this cabinet in the dc"),   // 数据中心中没有该机柜
    DC_ID_FORMAT(533009, "Id format error , type should be integer"),  // ID类型错误
    DC_CABINETNAME_ISNULL(531010, "Cabinet name can not be empty"),     // 机柜名称不能为空
    DC_CABINETID_ISNULL(531011, "Cabinet id can not be empty"),         // 机柜id不能为空
    DC_CABINETNAME_DUPLICATE(532012, "Cabinet name duplicate"),         // 机柜名称重复
    DC_CABINETUSIZE_MINIMUM(535013, "Cabinet u size is a minimum of 1"),// 机柜U位大小最小为1
    DC_CABINET_ASSETS_EXISTS(533014, "These cabinets contain assets and cannot be deleted"),// 机柜中有资产
    DC_CABINET_MAXU(535015, "The maximum U of current cabinet is 47"),  // 当前机柜最大U为47
    DC_Y_MINIMUM(535016,"Dc y is a minimum of 1"),                       // 数据中心行数最少为1
    DC_X_MINIMUM(535017,"Dc x is a minimum of 1"),                       // 数据中心列数最少为1
    DC_Y_ALREADYINUSE(535018,"Dc y is already in use"),                  // 数据中心行已经被使用
    DC_X_ALREADYINUSE(535019,"Dc x is already in use"),                  // 数据中心列已经被使用
    CABINET_Y_MINIMUM(535020,"Cabinet y is a minimum of 1"),               // 机柜所在行至少为1
    CABINET_X_MINIMUM(535021,"Cabinet x is a minimum of 1"),               // 机柜所在列至少为1
    CABINET_Y_INCORRECT(535022,"Cabinet y is not in this dc"),            // 机柜所选行不在dc范围内
    CABINET_X_INCORRECT(535023,"Cabinet x is not in this dc"),            // 机柜所选列不在dc范围内
    CABINET_POSITION_OCCUPIED(535024,"Cabinet position is occupied"),     // 机柜所选位置已被占用
    DC_PROMSERVER_EXISTS(533025, "There are promservers in the dc"),
    DC_TRAFFIC_DIRECTIONS_INVALIDE(533026, "Dc traffic direction must be rx or tx"),
    DC_TRAFFIC_SETTING_ISNULL(531027, "Dc traffic setting can not be empty"),
    DC_TRAFFIC_DIRECTIONS_ISNULL(531028, "Dc traffic direction can not be empty"),
    DC_TRAFFIC_IFINDEX_ISNULL(531029, "Dc traffic Ifindex can not be empty"),
    DC_TRAFFIC_IFDESCR_ISNULL(531030, "Dc traffic ifdescr can not be empty"),
    DC_NOT_INCLUDED_ASSET(533031,"Current asset is not included under the data center"),
    DC_TRAFFIC_HOST_FORMAT(533032,"Dc traffic host must be in IP format"),
    DC_TRAFFIC_HOST_ISNULL(531033,"Dc traffic host can not be empty"),
    DC_TRAFFIC_PORT_ISNULL(531034,"Dc traffic port can not be empty"),
    DC_TRAFFIC_VERSION_ISNULL(531035,"Dc traffic version can not be empty"),
    DC_TRAFFIC_PORT_INVALIDE(535036,"Dc traffic port is invalide"),
    DC_TRAFFIC_TAGSKEY_FORMAT(533037,"Dc traffic tags key format is incorrect"),
    DC_PROMSERVERS_EXISTS(533038, "These dc contain prometheus servers and cannot be deleted"),
    DC_STATE_ISNULL(531039, "Dc state can not be empty"),
    DC_STATE_ERROR(534040, "Dc state must be ON or OFF"),
    DC_LONGITUDE_ERROR(535041, "Dc longitude range is -180 to +180"),
    DC_LATITUDE_ERROR(535042, "Dc latitude range is -90 to +90"),
    CABINET_NOT_EXISTS(536043, "Cabinet does not exist"),

    SYS_I18N_DUPLICATE(542000, "duplicate error (code + lang)"), //国际化配置重复（code+lang）
    SYS_I18N_NOSUCHLANG(543001, "no such lang"),                  //不合法的lang


    SYS_DICT_NAME_ISNULL(551000, "Field 'name' is not null"),        // 字典名称不能为空
    SYS_DICT_TYPE_ISNULL(551001, "Field 'type' is not null"),        // 字典类型不能为空
    SYS_DICT_VALUE_ISNULL(551002, "Field 'type' is not null"),       // 字典值不能为空
    SYS_DICT_CODE_ISNULL(551003, "Field 'code' is not null"),        // 字典值不能为空
    SYS_DICT_VALUE_DUPLICATE(552004, "Field 'type' is duplicate"),  //  字典值重复
    SYS_DICT_ID_INVALIDE(553005, "Invalid system dictionary ID"),
    SYS_DICT_ID_ISNULL(551006, "System dictionary ID is null"),
    SYS_DICT_REMOVE_ERROR(553007,"Those dict params contain model can not remove"),

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
    SNMP_MIB_CONFIG_ISNULL(561024,"Snmp Credential Id and config cannot be empty at the same time"),


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
    SYS_CONFIG_PORT_INVALID(573011,"port must be between 1 and 65535"),
    SYS_CONFIG_EMAILENABLE_ISNULL(571011,"System config email enable can not be empty"),               // email_enable 不能为空
    SYS_CONFIG_EMAILSMTPHOST_ISNULL(571012,"System config email smtp host can not be empty"),         // email_smtp_host 不能为空
    SYS_CONFIG_EMAILSMTPHOST_FORMAT(573013,"System config email smtp host format error"), // email_smtp_host 格式不正确
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
    SYS_CONFIG_EMAILPROTOCOL_INVALIDE(573025,"System config email security type is invalide"),            // email_tls_flag 参数与要求不符合
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
    SYS_CONFIG_LDAPMAPPING_FORMAT_ERROR(571053,"System config ldap mapping format error"),         // ldap_mapping格式错误 非正常json格式
    SYS_CONFIG_TESTEMAIL_TEMPLATE_NOTFOUND(576054,"System config test email sending template not found"),// 发送测试邮件模板未找到
    SYS_CONFIG_TESTEMAIL_TEMPLATE_FAILED(570055,"Test email template generation failed"),                // freemarker生成发送邮件模板内容失败
    SYS_CONFIG_EMAILSENDCCOUNT_FORMAT(573056,"System config email send account format error"),           //email_send_account应为邮件格式
    SYS_CONFIG_RESET_FAIL(573057,"System reset fail, check config and try again later"),
    SYS_CONFIG_MODELCHARTTPL_NOTFOUND(576058,"System config model chart tpl not found"),
    SYS_CONFIG_PROJECTCHARTTPL_NOTFOUND(576059,"System config project chart tpl not found"),
    SYS_CONFIG_DEFAULTCABINETUSIZE_INVALIDE(573060,"System config default cabinet usize should be an integer"),
    SYS_CONFIG_QUERYMAXSERIES_ISNULL(571061, "System config Query max series can not be empty"),
    SYS_CONFIG_QUERYMAXSERIES_INVALIDE(573062, "System config Query max series should be an integer"),
    SYS_CONFIG_QUERYMAXSERIES_INTERVAL(575063, "The interval value of Query max series is between -1 and +1000"),
    SYS_CONFIG_EXPORT_MAX_LINE(579064,"The number of exported data than the configured max export line"),
    SYS_CONFIG_UNSAVEDCHANGE_ISNULL(571065, "System config unsaved change can not be empty"),
    SYS_CONFIG_CODE_FILE_ISNULL(574001,"The validate file is not exist"), //不存在校验使用的文件
    SYS_CONFIG_CODE_INVALID(574002,"The code is invalid"),//校验文件中的code与传递的值不符
    SYS_CONFIG_DB_PARAM_INVALID(574003,"The DB connect params is invalid"),//校验文件中的code与传递的值不符
    SYS_CONFIG_REDIS_PASSWORD_INVALID(574004,"The redis password is invalid"),
    SYS_CONFIG_REDIS_PASSWORD_REQUIRED(574005,"The redis password is required"),
    SYS_CONFIG_REDIS_PARAM_INVALID(574006,"The redis params is invalid"),
    SYS_CONFIG_DB_URL_ISNULL(574007,"The DB host is null"),
    SYS_CONFIG_DB_USERNAME_ISNULL(574008,"The DB username is null"),
    SYS_CONFIG_DB_PASSWORD_ISNULL(574009,"The DB password is null"),
    SYS_CONFIG_REDIS_HOST_ISNULL(574010,"The redis host is null"),
    SYS_CONFIG_REDIS_PORT_ISNULL(574011,"The redis host is null"),
    SYS_CONFIG_HAD_CONFIG(574012,"Someone has started to configure the system"),
    SYS_CONFIG_SESSION_TIMEOUT_INVALID(573013,"Session timeout is not less than 15"),
    SYS_CONFIGI_PROMEFEDEENABLED_NCORRECT(574014, "Prometheus federation enabled must be 0 or 1"),
    SYS_CONFIGI_ASSETPINGFROM_NCORRECT(574015, "Asset ping from must be 1 or 2"),

    SYS_ROLE_ID_ISNULL(581000, "Role id is required"),
    SYS_ROLE_ISNULL(586001, "Role is not exsits"),
    SYS_ROLE_NAME_ISNULL(581002, "Role name is required"),
    SYS_ROLE_REMARK_ISNULL(581003, "Role remark is required"),
    SYS_ROLE_NAME_DUPLICATE(582004, "Role name duplicate"),
    SYS_ROLE_MENUID_ISNULL(581005, "Role related menu id is required"),
    SYS_ROLE_NAME_CANNOT_MODIFY(587006, "The name of the built-in role cannot be modified"),
    SYS_ROLE_CANNOT_DELETE(587007, "Built-in roles cannot be deleted"),


    LINK_ID_ISNULL(581001,"link id can not be null"),
    LINK_NAME_ISNULL(581002,"link name can not be null"),
    LINK_URL_ISNULL(581003,"link url can not be null"),
    LINK_PREV_ISNULL(581004,"link prev can not be null"),
	LINK_NEXT_ISNULL(581005,"link next can not be null"),
	LINK_BUILDIN_CAN_NOT_REMOVE(587006,"This link is built-in and cannot be deleted"),
	LINK_WEIGHT_PARAM_INVALID(587007,"link weight param is invalid"),

	SNMP_CREDENTIAL_ID_ISNULL(591001,"Snmp credential id can not be null"),
	SNMP_CREDENTIAL_TYPE_ISNULL(591002,"Snmp credential type can not be null"),
	SNMP_CREDENTIAL_REMARK_ISNULL(591003,"Snmp credential remark can not be null"),
	SNMP_CREDENTIAL_CONFIG_ISNULL(591004,"Snmp credential config can not be null"),
	SNMP_CREDENTIAL_NAME_ISNULL(591005,"Snmp credential name can not be null"),
	SNMP_CREDENTIAL_PORT_ISNULL(591006,"Snmp credential port can not be null"),
	SNMP_CREDENTIAL_NOT_FOUND(596007,"Snmp credential not found"),
	SNMP_CREDENTIAL_NAME_DUPLICATE(596008,"Snmp credential name duplicate"),
	SNMP_CREDENTIAL_ASSETS_EXISTS(596009,"These Snmp credential contain assets and cannot be deleted"),
    /**
     * webshell相关
     */
    TERMINAL_PARAM_ISNULL(611000, "Terminal param can not be null"),
    TERMINAL_UUID_ISNULL(611001, "Terminal uuid can not be null"),
    TERMINAL_PATH_ISNULL(611002, "Terminal path can not be null"),
    TERMINAL_FILE_ISNULL(611003, "Terminal upload file can not be null"),
    TERMINAL_ISNULL(611004, "Error,Terminal is null"),
    TERMINAL_TELNET_TRANSFER(610005,"Telnet does not allow upload and download"),
    TERMINAL_QUERYSIZE_INVALIDED(613006, "Terminal query size cannot be less than 1"),

    /**
     * agent相关
     */
    AGENT_INSTALL_PARAM_MISS(711000,"Parameter of this API is missed"),
    
    /**
     * stat相关
     */
    DIMENSION_IS_NULL(811001,"dimension param can not be null"),
    SYSAPIKEY_NAME_ISNULL(811002,"sys api key name can not be empty"),
    SYSAPIKEY_ROLEID_ISNULL(811003,"sys api key role id can not be empty"),
    SYSAPIKEY_ID_ISNULL(811004,"sys api key id can not be empty"),
    SYSAPIKEY_NAME_DUPLICATE(811005,"sys api key name is duplicate"),
    /**
     * 导入相关
     */
    IMPORT_EXCELFILE_TYPE(900, "Only support import XLS or XLSX file"),
    IMPORT_EXCELFILE_PARSE_ERROR(901, "Import file resolution failed"),
    IMPORT_EXCELFILE_HEADER_TEMPLATE_ERROR(902,"The header row of the excel import template is inconsistent with the system template"),

    NOT_NULL_ERROR(990, "not null error"), //not null错误
    NOT_NUMBER_ERROR(991, "not number error"), //not number错误
    /**
     * 错误的时间格式
     */
    DateFormatWrong(40090018,"Incorrect Date Format"),
    
    /**
	 * License 相关
	 */
	LICENSE_FILE_IS_NULL(711001, "license file can not be null"),
	LICENSE_FILE_IS_EMPTY(716001, "The license file does not exist"), LICENSE_FILE_INVALID(715001, "Invalid license"),
	LICENSE_PARAM_IS_NULL(711002, "license parameter is null"),
	LICENSE_TOKEN_INVALID(715002, "Invalid license,token error"),
	LICENSE_MAXIMUMASSETS_INVALID(715003, "The number of assets exceeds the maximumAssets"),
	LICENSE_VERSION_INVALID(715004, "The version is not valid"),

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
