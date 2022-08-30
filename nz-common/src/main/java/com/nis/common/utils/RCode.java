package com.nis.common.utils;

import java.text.MessageFormat;

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
    PANEL_WEIGHT_ISNULL(121001,"Panel weight not be empty"),
    PANEL_TYPE_ISNULL(121002,"Panel type can not be empty when link is not empty"),
    PANEL_TYPE_INVALIDE(124003,"Panel type must be dashboard, asset, project, module, endpoint or model"),
    PANEL_PID_ISNULL(121004,"Panel pid can not be empty"),
    PANEL_NOT_EXIST(126005,"Panel does not exist"),
    PANEL_NAME_TOO_LONG(123006,"Panel name too long than 64"),
    PANEL_NAME_ISNULL(121007,"Panel name can not be empty"),
    PANEL_NAME_FORMAT_ERROR(123008,"Panel name format error, can not include /"),
    PANEL_NAME_DUPLICATE(122009,"Panel name is duplicate"),
    PANEL_ID_ISNULL(121010,"Panel id can not be empty"),
    PANEL_CHART_TEMPLATE_TYPE_ISNULL(121011,"Panel charts template type is null"),
    PANEL_CHART_TEMPLATE_TYPE_INCORRECT(123012,"Panel charts template type must be dashboard, asset, endpoint and template"),
    PANEL_CHART_IMPORT_LINKID_ISNULL(121013,"When the type is model or asset, link Id cannot be empty"),
    PANEL_BUILDIN_CAN_NOT_REMOVE(127014,"This panel is built-in and cannot be deleted"),
    CHART_Y_ISNULL(121015,"Chart y can not be empty"),
    CHART_Y_FORMAT(123016,"Chart y format should be a integer"),
    CHART_X_WIDTH_INVALIDE(123017,"Chart x and width sum cannot be greater than 12 "),
    CHART_X_ISNULL(121018,"Chart x can not be empty"),
    CHART_X_INVALIDE(123019,"Chart x should be between 0 and 12"),
    CHART_X_FORMAT(123020,"Chart x format should be a integer"),
    CHART_WIDTH_INVALIDE(123021,"Chart width should be between 1 and 12"),
    CHART_WIDTH_FORMAT(123022,"Chart width format should be a integer"),
    CHART_WEIGHT_ISNULL(121023,"Chart weight can not be empty"),
    CHART_VARTYPE_ISNULL(121024,"Chart var type can not be empty when using templates"),
    CHART_VARTYPE_ERROR(123025,"Chart var type must be Asset or Endpoint"),
    CHART_VARID_ISNULL(121026,"varid can not be empty"),
    CHART_VARID_ERROR(121027,"varid param error"),
    CHART_URLPARAM_ISNULL(121028,"Chart param cannot be empty when type is url"),
    CHART_UNIT_NOTFOUND(126029,"Chart unit not found"),
    CHART_TYPE_ISNULL(121030,"Chart type can not be empty"),
    CHART_TYPE_INVALIDE(123031,"Chart type is invalide"),
    CHART_TITLE_ISNULL(121032,"Chart title can not be empty"),
    CHART_TEXTPARAM_ISNULL(121033,"Chart param cannot be empty when type is text"),
    CHART_SYNCTMPL_ERROR(121034,"Chart sync param can not be null"),
    CHART_SPAN_ISNULL(121035,"Chart span can not be empty"),
    CHART_SINGLESTATPARAM_ISNULL(121036,"Chart param cannot be empty when type is singleStat"),
    CHART_PID_ISNULL(121037,"pid can not be empty"),
    CHART_PARAM_FORMAT(123038,"Chart param must be JSON format"),
    CHART_PARAMURL_FORMAT(123039,"Chart param url value must be URL format"),
    CHART_PARAMSINGLESTAT_ISNULL(121040,"Chart param statistics not found when type is singleStat"),
    CHART_PARAMSINGLESTAT_INVALIDE(124041,"Chart param statistics must be min, max, average, total, first, last, range  or different"),
    CHART_PANEL_ISNULL(126042,"Panel is not exsits"),
    CHART_NOTEXSITS(126043,"Chart is not exsits"),
    CHART_NAME_TOO_LONG(121044,"Chart name too long than 64"),
    CHART_NAME_DUPLICATE_NOTLINK(122045,"Chart name is duplicated and cannot be linked"),
    CHART_ID_ISNULL(121046,"Chart id can not be empty"),
    CHART_HEIGHT_ISNULL(121047,"Chart height can not be empty"),
    CHART_HEIGHT_INVALIDE(123048,"Chart height should be between 1 and 12"),
    CHART_HEIGHT_FORMAT(123049,"Chart height format should be a integer"),
    CHART_GROUP_NAME_DUPLICATE(121050,"chart type is group name is duplicate"),
    CHART_GROUPID_ISNULL(121051,"Chart group id can not be empty"),
    CHART_ELEMENT_ISNULL(121052,"Chart element can not be empty"),
    CHART_ELEMENTS_FORMAT(123053,"Chart elements must be JSON format"),
    CHART_DATASOURCE_ISNULL(121054,"Chart datasource can not be null"),
    CHART_DATASOURCE_INVALIDE(121055,"Chart datasource must be metrics,logs,system,misc"),
    CHART_BUILDIN_CAN_NOT_REMOVE(127056,"This chart is built-in and cannot be deleted"),
    CHARTELEMENT_TYPE_ISNULL(121057,"Chart element type can not be empty"),
    CHARTELEMENT_TYPE_INVALIDE(123058,"Chart element type is invalide"),
    CHARTELEMENT_NAME_ISNULL(121059,"Chart element name can not be empty"),
    CHARTELEMENT_EXPRESSION_ISNULL(121060,"Chart element expression can not be empty"),
    PANEL_PARAM_REPORTRANGE_ISNULL(121082,"Panel param report range can not be empty"),
    PANEL_PARAM_RANGETYPE_ISNULL(121083,"Panel param range type can not be empty"),
    PANEL_PARAM_RANGETYPE_INVALIDE(123084,"Panel param range type must be previous or last"),
    PANEL_PARAM_RANGEINTERVAL_ISNULL(121085,"Panel param range interval can not be empty"),
    PANEL_PARAM_RANGEUNIT_ISNULL(121086,"Panel param range unit can not be empty"),
    PANEL_PARAM_RANGEUNIT_INVALIDE(123087,"Panel param range unit must be hour, day, week or month"),
    PANEL_PARAM_REPORTSCHEDULE_ISNULL(121088,"Panel param report schedule can not be empty"),
    PANEL_PARAM_SCHEDULETYPE_ISNULL(121089,"Panel param schedule type can not be empty"),
    PANEL_PARAM_SCHEDULETYPE_INVALIDE(123090,"Panel param schedule type must be 1, 2, 3 or 4"),
    PANEL_PARAM_SCHEDULEREPEAT_ISNULL(121091,"Panel param schedule repeat can not be empty"),
    PANEL_PARAM_SCHEDULESTIME_ISNULL(121092,"Panel param schedule stime can not be empty"),
    PANEL_PARAM_SCHEDULENUMS_ISNULL(121093,"Panel param schedule nums can not be empty"),
    PANEL_PARAM_SCHEDULENUMS_INVALIDE(123094,"Panel param schedule nums is invalide"),
    PANEL_PARAM_REPORTRECEIVERS_ISNULL(121095,"Panel param report receivers can not be empty"),
    PANEL_PARAM_REPORTCHARTSHARE_INVALIDE(123096,"Panel param report tooltip must be none, crosshair or tooltip"),

    /**
     * EXPRETMPL
     */
    EXPRETMPL_BUILDIN_CAN_NOT_REMOVE(137001,"This expression template is built-in and cannot be deleted"),
    EXPRETMPL_BUILDIN_INCORRECT(134002,"Expression template buildin must be 0 or 1"),
    EXPRETMPL_EXPRESSION_ISNULL(131003,"Expression template expression is null"),
    EXPRETMPL_ID_ISNULL(131004,"Expression template id is null"),
    EXPRETMPL_IMPORT_ERROR(133005,"Expression template import error"),
    EXPRETMPL_NAME_DUPLICATE(132006,"Expression template name is duplicate"),
    EXPRETMPL_NAME_ISNULL(131007,"Expression template name is null"),
    EXPRETMPL_NOT_FOUND(136008,"Expression template not found"),
    EXPRETMPL_RENDER_DATA_ISNULL(131009,"Expression template render data is null"),

    /**
     * project菜单项相关
     */
    PROJECT_ID_ISNULL(211000, "Project id can not be empty"),
    PROJECT_NAME_ISNULL(211001, "Project name can not be empty"),
    PROJECT_BUILDIN_CAN_NOT_REMOVE(217002, "These projects are built-in and cannot be deleted. Details: {0}"),
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
    PROJECT_ICON_BIG_ERROR(214017,"Topo icon file is too big ,Please contact your administrator to modify the database configuration : max_allowed_packet"),
    PROJECT_RELOBJ_CAN_NOT_REMOVE(217018, "Project have associated data and cannot be deleted"),


    /**
     * module
     */
    MODULE_AUTHPROTOCOL_INVALID(224001,"Auth protocol must be SHA or MD5"),
    MODULE_AUTHSECURITYLEVEL_INVALID(224002,"Security level must be one of authPriv, authNoPriv or noAuthNoPriv"),
    MODULE_BUILDIN_CAN_NOT_REMOVE(227003,"These modules are built-in and cannot be deleted. Details: {0}"),
    MODULE_CONFIGS_FORMAT(223004,"Module configs must be JSON format"),
    MODULE_CONFIGS_ISNULL(221005,"Module configs can not be empty"),
    MODULE_CONFIGS_TYPE_DUPLICATE(222006,"Module config type is duplicate"),
    MODULE_CONFIG_FILENAME_ISNULL(221007,"Module config file path is null"),
    MODULE_ENDPOINTNAMETMPL_ISNULL(221008,"Module endpoint name template can not be empty"),
    MODULE_ID_ISNULL(221009,"Module id can not be empty"),
    MODULE_NAME_DUPLICATE(222010,"Module name is duplicate"),
    MODULE_NAME_ISNULL(221011,"Module name info can not be empty"),
    MODULE_NAME_TOO_LONG(223012,"Module name too long"),
    MODULE_NOTEXIS_PROJECT(226013,"Module does not exist in the current project"),
    MODULE_NOTFOUND(226014,"Module not found"),
    MODULE_PASSWORD_INVALID(221015,"Auth password is missing, required for SNMPv3 with auth"),
    MODULE_PORT_ERROR(221016,"Module port is incorrect"),
    MODULE_PRIVPASSWORD_ISNULL(221017,"Priv password is missing, required for SNMPv3 with priv"),
    MODULE_PRIVPROTOCOL_INVALID(224018,"Priv protocol must be DES or AES"),
    MODULE_PROJECTID_ISNULL(221019,"Project id can not be empty"),
    MODULE_PROTOCOL_INVALID(224020,"Module protocol must be http or snmp"),
    MODULE_REMARK_TOO_LONG(223021,"Module remark too long"),
    MODULE_REMOVE_ERROR(223022,"These modules contains endpoint cant remove modules"),
    MODULE_SNMP_WALK_FORMAT(223023,"Module walk oids format error"),
    MODULE_SNMP_WALK_ISNULL(221024,"Snmp type module walk oids cannot be empty"),
    MODULE_TYPE_INCONSISTENT(223025,"Module type is invalid"),
    MODULE_TYPE_ISNULL(221026,"Module type can not be empty"),
    MODULE_USERNAME_INVALID(221027,"Auth username is missing, required for SNMPv3"),
    MODULE_CONFIG_JOURNALUNIT_ISNULL(221028,"Module config journal unit is null"),
    MODULE_CONFIG_SYSLOG_APPNAME_ISNULL(221029,"Module config syslog appName is null"),
    MODULE_CONFIG_SYSLOG_ADDRESS_ISNULL(221030,"Module config syslog listenAddress is null"),
    MODULE_CONFIG_PIPELINE_EXPRESSION_ISNULL(221031,"Module config pipeline expression is null"),
    MODULE_CONFIG_PIPELINE_TEMPLATE_ISNULL(221032,"Module config pipeline template is null"),
    MODULE_CONFIG_PIPELINE_FORMAT_ISNULL(221033,"Module config pipeline format is null"),
    MODULE_CONFIG_PIPELINE_EXPRESSIONS_ISNULL(221034,"Module config pipeline expressions is null"),
    MODULE_CONFIG_PIPELINE_SOURCE_ISNULL(221035,"Module config pipeline source is null"),
    MODULE_CONFIG_RELABEL_SOURCELABELS_ISNULL(221036,"Module config relabel source_labels is null"),
    MODULE_CONFIG_RELABEL_TARGETLABEL_ISNULL(221037,"Module config relabel target_label is null"),
    MODULE_CONFIG_RELABEL_REGEX_ISNULL(221038,"Module config relabel regex is null"),
    MODULE_CONFIG_RELABEL_REPLACEMENT_ISNULL(221039,"Module config relabel replacement is null"),
    MODULE_RELOBJ_CAN_NOT_REMOVE(227040, "Module have associated data and cannot be deleted"),

    /**
     * endpoint
     */
    ENDPOINT_ASSETID_ISNULL(231001,"Asset Id can not be empty"),
    ENDPOINT_CONFIGS_FORMAT(233002,"Endpoint configs must be JSON format"),
    ENDPOINT_CONFIGS_TYPE_DUPLICATE(232003,"Endpoint config type is duplicate"),
    ENDPOINT_EDITTYPE_ISNULL(231004,"Endpoint edit type can not be empty"),
    ENDPOINT_ENABLED_INVALIDE(234005,"Endpoint enabled must be 0 or 1"),
    ENDPOINT_ID_ISNULL(231006,"Endpoint id can not be empty"),
    ENDPOINT_IMPORT_ERROR(233007,"Endpoint template import error"),
    ENDPOINT_LABELS_ACTION_INVALIDE(234008,"Endpoint labels action must be 0 or 1"),
    ENDPOINT_LABELS_ACTION_ISNULL(231009,"Endpoint labels action can not be empty"),
    ENDPOINT_LABELS_ISNULL(231010,"Endpoint labels can not be empty"),
    ENDPOINT_LABELS_KEY_ISNULL(231011,"Endpoint labels key can not be empty"),
    ENDPOINT_MODULEID_ISNULL(231012,"Module Id can not be empty"),
    ENDPOINT_MODULE_ASSET_ID_ISALLNULL(231013,"Endpoint id, Module id and Asset id can not be all empty"),
    ENDPOINT_NAME_DUPLICATE(232014,"Endpoint name duplicate"),
    ENDPOINT_NAME_ISNULL(231015,"Endpoint name can not be empty"),
    ENDPOINT_NOTFOUND(236016,"Endpoint not found"),
    ENDPOINT_PROTOCOL_INVALID(234017,"Endpoint protocol must be http or snmp"),
    ENDPOINT_SNMP_WALK_FORMAT(233018,"Endpoint walk oids format error"),
    ENDPOINT_SNMP_WALK_ISNULL(231019,"Snmp type module walk oids cannot be empty"),
    ENDPOINT_TYPE_INVALID(233020,"Endpoint type is invalid"),
    ENDPOINT_TYPE_ISNULL(231021,"Endpoint type can not be empty"),
    ENDPOINT_PORT_ISNULL(231022,"Endpoint port can not be empty"),
    ENDPOINT_LABELNAME_FORMAT_ERROR(233023,"Expressions label key does not comply with Prometheus labelname format"),
    ENDPOINT_RELOBJ_CAN_NOT_REMOVE(237024, "Endpoint have associated data and cannot be deleted"),

    /**
     * assets菜单项相关
     */
    ASSET_AUTHPIN_ISNULL(311001,"Asset auth pin can not be empty"),
    ASSET_AUTHPRIKEY_ISNULL(311002,"Asset auth pri key can not be empty"),
    ASSET_AUTHUSERNAME_ISNULL(311003,"Asset auth username can not be empty"),
    ASSET_CABINETEND_INVALIDE(313004,"Asset cabinet end must be an integer"),
    ASSET_CABINETSTART_INVALIDE(313005,"Asset cabinet start must be an integer"),
    ASSET_CABINET_ISNULL(311006,"Cabinet must be selected when start or end position is not empty"),
    ASSET_CLIENTPORT_ERROR(315007,"Asset client port is incorrect"),
    ASSET_CLIENT_PROT_ISNULL(311008,"Asset client port can not be empty"),
    ASSET_CLIENT_TOKEN_ISNULL(311009,"Asset client token can not be empty"),
    ASSET_DC_ID_ISNULL(311010,"Asset dc id can not be empty"),
    ASSET_EDITTYPE_ISNULL(311011,"Asset edit type can not be empty"),
    ASSET_ID_FORMAT(313012,"Id format error , type should be integer"),
    ASSET_ID_ISNULL(311013,"Asset id can not be empty"),
    ASSET_MAINTENANCEID_ISNULL(311014,"Asset maintenance id can not be empty"),
    ASSET_MAINTENANCE_CONTENT_ISNULL(311015,"Asset maintenance content can not be empty"),
    ASSET_MANAGEIP_DUPLICATE(312016,"Asset manage ip duplicate"),
    ASSET_MANAGEIP_FORMAT(313017,"Asset manage IP address format is incorrect"),
    ASSET_MANAGEIP_ISNULL(311018,"Asset manage ip can not be empty"),
    ASSET_MODEL_ID_ISNULL(311019,"Asset model id can not be empty"),
    ASSET_NAME_DUPLICATE(312020,"Asset name duplicate"),
    ASSET_NAME_ISNULL(311021,"Asset name can not be empty"),
    ASSET_NOT_EXIST(316022,"Asset does not exist"),
    ASSET_NOT_SELECT_PARENT(311023,"Asset must select the asset host"),
    ASSET_NOT_SUPPORT_MOUNTING(313024,"Asset does not support mounting"),
    ASSET_NUMBER_DUPLICATE(312025,"Asset number duplicate"),
    ASSET_PARENT_NOT_EXIST(316026,"Asset parent does not exist"),
    ASSET_POSITION_INVALID(315027,"Asset cabinet start or end value is invalid"),
    ASSET_POSITION_MISMATCH(315028,"Asset usize does not match the usize of the associated model"),
    ASSET_POSITION_OCCUPY(315029,"Asset selected position is already occupied"),
    ASSET_PURCHASEDATE_FORMAT(313030,"Asset purchase date format error"),
    ASSET_PURCHASEDATE_UNAVAILABLE(313031,"Asset purchase date unavailable"),
    ASSET_SN_DUPLICATE(312032,"Asset serial number duplicate"),
    ASSET_SN_ISNULL(311033,"Asset serial number can not be empty"),
    ASSET_STATE_ID_ISNULL(311034,"Asset state id can not be empty"),
    ASSET_STATE_NOTFOUND(316035,"Asset state not found"),
    ASSET_TYPE_ID_ISNULL(311036,"Asset type id can not be empty"),
    ASSET_RELOBJ_CAN_NOT_REMOVE(317037, "Asset have associated data and cannot be deleted"),
    ASSET_FORCE_INVALIDE(314038,"Force param must be 0 or 1"),

    /**
     * ASSET_MODEL
     */
    ASSET_MODELID_ISNULL(321001,"Asset model id can not be null"),
    ASSET_MODELNAME_DUPLICATE(322002,"Asset model name duplicate"),
    ASSET_MODELNAME_ISNULL(321003,"Asset model name can not be null"),
    ASSET_MODELRELATED_ASSET(323004,"These mdoels contain assets and cannot be deleted"),
    ASSET_MODEL_BRANDID_ISNULL(321005,"Asset model brand id can not be null"),
    ASSET_MODEL_CHART_ERROR(321006,"Asset model chart template not exist"),
    ASSET_MODEL_NOTFOUND(326007,"Asset model not found"),
    ASSET_MODEL_OID_DUPLICATE(322008,"OID duplicate"),
    ASSET_MODEL_OID_INCORRECT(323009,"OID format is incorrect"),
    ASSET_MODEL_RELOBJ_CAN_NOT_REMOVE(317037, "Asset model have associated data and cannot be deleted"),

    /**
     * Account
     */
    ACCOUNT_AUTHTYPE_INVALIDE(333001, "Invalid authentication type"),
    ACCOUNT_PORT_ERROR(335002, "Account port is incorrect"),

    /**
     * STATECONF
     */
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

    /**
     * TYPECONF
     */
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
    TYPECONF_CLIENTENABLE_ISNULL(351022, "Asset type config client enable is null"),
    TYPECONF_CLIENTENABLE_INCORRECT(354023, "Asset type config client enable must be 0 or 1"),
    TYPECONF_AUTHPROTOCOL_INCORRECT(354024, "Asset type config auth protocol must be 0 ,1 or 2"),
    TYPECONF_SNMPENABLE_INCORRECT(354025, "Asset type config snmp enable must be 0 or 1"),
    TYPECONF_SNMPCOLLECT_INCORRECT(354026, "Asset type config snmp collect must be 0 or 1"),
    TYPECONF_SSHCOLLECT_INCORRECT(354027, "Asset type config ssh collect must be 0 or 1"),

    /**
     *  ASSET_BRAND
     */
    ASSET_BRAND_NOT_FOUND(366000, "Asset brand not found"),
    ASSET_BRAND_NAME_ISNULL(361001, "Asset brand name is null"),
    ASSET_BRAND_ID_ISNULL(361002, "Asset brand id is null"),
    ASSET_BRAND_NAME_DUPLICATE(362003, "Asset brand name is duplicate"),
    ASSET_BRAND_BEUSED_CAN_NOT_REMOVE(367004, "This brand is already used and cannot be deleted"),
    ASSET_BRAND_NOT_CONTAIN_MODEL(366005, "This model is not included under this brand"),
    ASSET_BRAND_RELOBJ_CAN_NOT_REMOVE(367006, "Asset brand have associated data and cannot be deleted"),

    /**
     * ASSET_FIELD_META
     */
    ASSET_FIELD_META_ID_ISNULL(371001,"Asset field meta id can not be empty"),
    ASSET_FIELD_META_NAME_ISNULL(371002,"Asset field meta name can not be empty"),
    ASSET_FIELD_META_KEY_ISNULL(371003,"Asset field meta key can not be empty"),
    ASSET_FIELD_META_GROUPID_ISNULL(371004,"Asset field meta group id can not be empty"),
    ASSET_FIELD_META_SEARCH_ISNULL(371005,"Asset field meta search can not be empty"),
    ASSET_FIELD_META_DISPLAY_ISNULL(371006,"Asset field meta display can not be empty"),
    ASSET_FIELD_META_TYPE_ISNULL(371007,"Asset field meta type can not be empty"),
    ASSET_FIELD_META_TYPE_ERROR(375008,"Asset field type value out of range"),
    ASSET_FIELD_META_KEY_ERROR(373009,"Asset field key value incorrect format"),
    ASSET_FIELD_META_KEY_NAME_DUPLICATE(372010,"Asset field key value or name duplicate"),
    ASSET_FIELD_META_NOT_FOUND(376011, "Asset field meta not found"),
    ASSET_FIELD_META_REFERENCED(376012, "Asset field meta referenced"),

    /**
     * ASSET_FIELD_GROUP
     */
    ASSET_FIELD_GROUP_ID_ISNULL(381001,"Asset field group id can not be empty"),
    ASSET_FIELD_GROUP_NAME_ISNULL(381002,"Asset field group name can not be empty"),
    ASSET_FIELD_GROUP_NAME_DUPLICATE(382003,"Asset field group name can not duplicate"),


    /**
     * Asset Issue
     */
    ISSUE_ID_ISNULL(391000, "Issue id can not be empty"),
    ISSUE_NAME_DUPLICATE(392001, "Issue name duplicate"),
    ISSUE_NAME_ISNULL(391002, "Issue name can not be empty"),
    ISSUE_TYPE_ISNULL(391003, "Issue type can not be empty"),
    ISSUE_CONTENT_ISNULL(391004, "Issue content can not be empty"),
    ISSUE_STATE_ISNULL(391005, "Issue state can not be empty"),
    ISSUE_RID_ID_ISNULL(391006, "Issue reporter id can not be empty"),
    ISSUE_AID_ID_ISNULL(391007, "Issue assignee id can not be empty"),
    ISSUE_STATE_INVALID(395008, "Issue state is invalid"),
    ISSUE_PRIORITY_INVALID(395009, "Issue priority is invalid"),
    ISSUE_ACTIVITY_ID_ISNULL(391010, "Issue activity id can not be empty"),
    ISSUE_ACTIVITY_CONTENT_ISNULL(391011, "Issue activity content can not be empty"),
    ISSUE_ACTIVITY_ONLYOWN_COMMENTS(397012, "Issue activity only can operate own comments"),
    ISSUE_ACTIVITY_TYPE_ISNULL(391013, "Issue activity type can not be empty"),

    /**
     * alerts菜单栏相关
     */
    ALERTMSG_ACK_INCORRECT(414001,"Alert message ack is incorrect"),
    ALERTMSG_ACK_ISNULL(411002,"Alert message ack is required"),
    ALERTMSG_IDS_ISNULL(411003,"Ids is required"),
    ALERTMSG_NOT_EXITS(416004,"Alert message not exits"),
    ALERTMSG_PARSE_ERROR(413005,"Data parse error"),
    ALERTMSG_SAVE_ERROR(413006,"Data save faild"),
    ALERTMSG_STATE_INCORRECT(412007,"State is incorrect"),
    ALERTMSG_STATE_ISNULL(411008,"State is required"),


    /**
     * RULE
     */
    ALERTRULE_AUTOEXPIRED_INCORRECT(424001,"Alert rule autoExpired must be 0 or 1"),
    ALERTRULE_BUILDIN_CAN_NOT_REMOVE(427002,"This rule is built-in and cannot be deleted"),
    ALERTRULE_BUILDIN_CAN_NOT_UPDATE(427003,"This rule is built-in and cannot be updated"),
    ALERTRULE_EXPR_ERROR(423004,"Expr syntax error"),
    ALERTRULE_EXPR_ISNULL(421005,"Expr is required"),
    ALERTRULE_ID_ISNULL(421006,"ID is required"),
    ALERTRULE_IMPORT_ERROR(423007,"Alert import error"),
    ALERTRULE_LAST_ISNOTPOSITIVE(425008,"Last must be a positive number"),
    ALERTRULE_LAST_ISNULL(421009,"Last is required"),
    ALERTRULE_METHOD_ISNULL(421010,"Alert rule method can not be null"),
    ALERTRULE_METHOD_NOTFOUND(426011,"Alert rule method not found"),
    ALERTRULE_NAME_DUPLICATE(421012,"Alert name is duplicate"),
    ALERTRULE_NAME_ISNULL(421013,"Alert name is required"),
    ALERTRULE_NOTIFYACTIVE_INCORRECT(424014,"Alert rule notifyActive must be 0 or 1"),
    ALERTRULE_NOTIFYEXPIRED_INCORRECT(424015,"Alert rule notifyExpired must be 0 or 1"),
    ALERTRULE_OIDFORMAT_ERROR(424016,"OID format is incorrect"),
    ALERTRULE_OPERATOR_INVALIDE(423017,"Alert operator is invalide"),
    ALERTRULE_OPERATOR_ISNULL(421018,"Alert operator can not be empty"),
    ALERTRULE_RECEIVER_FORMAT(423019,"Alert receiver id format error , its should be integer"),
    ALERTRULE_RECEIVER_NOTFOUND(421020,"Alert receiver user not found"),
    ALERTRULE_SCHEDDAYS_INCORRECT(424021,"Alert rule schedDays must be 1-7"),
    ALERTRULE_SCHEDDAYS_ISNULL(421022,"Alert rule schedDays can not be null"),
    ALERTRULE_SCHEDENABLE_INCORRECT(424023,"Alert rule schedEnable must be 0 or 1"),
    ALERTRULE_SCHEDETIME_ERROR(424024,"Alert rule schedEtime should be late than schedEtime"),
    ALERTRULE_SCHEDETIME_INCORRECT(424025,"Alert rule schedEtime format is HH:mm"),
    ALERTRULE_SCHEDETIME_ISNULL(421026,"Alert rule schedEtime can not be null"),
    ALERTRULE_SCHEDSTIME_INCORRECT(424027,"Alert rule schedStime format is HH:mm"),
    ALERTRULE_SCHEDSTIME_ISNULL(421028,"Alert rule schedStime can not be null"),
    ALERTRULE_SEVERITY_INCORRECT(424029,"Severity is not exists"),
    ALERTRULE_SEVERITY_ISNULL(421030,"Severity is required"),
    ALERTRULE_STATE_INCORRECT(424031,"Alert rule state must be 0 or 1"),
    ALERTRULE_SUMMARY_ISNULL(421032,"Alert summary is required"),
    ALERTRULE_THRESHOLD_FORMAT(423033,"Alert threshold format should be a long integer"),
    ALERTRULE_THRESHOLD_ISNULL(421034,"Alert threshold can not be empty"),
    ALERTRULE_TYPE_INCORRECT(424035,"Alert rule type must be 1 ,2 or 3"),
    ALERTRULE_TYPE_INVALIDE(424036,"Alarm type is invalide"),
    ALERTRULE_TYPE_ISNULL(421037,"Alert rule type is empty"),
    ALERTRULE_UNIT_FORMAT(423038,"Alert unit format should be a integer"),
    ALERTRULE_UNIT_INVALIDE(423039,"Alert unit is invalide"),
    ALERTRULE_UNIT_ISNULL(421040,"Alert unit can not be empty"),



    /**
     * 告警静默
     */
    ALERTSILENCE_STARTTIME_ISNULL(441001,"AlertSilence start time can not be null"),
    ALERTSILENCE_ENDTIME_ISNULL(441002,"AlertSilence end time can not be null"),
    ALERTSILENCE_ENDTIME_MUSTGTNOW(441003,"AlertSilence end time must great now time"),
    ALERTSILENCE_ID_IS_NOT_NULL(441004,"ids can not be null"),
    ALERTSILENCE_MATCHER_ISNULL(441005,"AlertSilence matchers can not be null"),
    ALERTSILENCE_NAME_ISNULL(441006,"AlertSilence name can not be null"),


    /**
     *  告警级别
     */
    ALERTSEVERITY_NAME_ISNULL(451001,"Alert severity name can not be null"),
    ALERTSEVERITY_COLOR_ISNULL(451002,"Alert severity color can not be null"),
    ALERTSEVERITY_ID_ISNULL(451003,"Alert severity id can not be null"),
    ALERTSEVERITY_REMARK_ISNULL(451004,"Alert severity remark can not be null"),
    ALERTSEVERITY_WEIGHT_PARAM_ISNULL(451005,"Alert severity id or weight is null"),


    /**
     *  告警通知
     */
    ALERTNOTIFY_NAME_ISNULL(461002,"Alert notify method name can not be null"),
    ALERTNOTIFY_FILE_NOTEXISTS(461003,"Alert notify method script file is not exsits"),
    ALERTNOTIFY_ACCOUNT_ISNULL(461004,"Alert notify method account can not be null"),
    ALERTNOTIFY_STATE_ISNULL(461005,"Alert notify method state can not be null"),
    ALERTNOTIFY_ID_ISNULL(461006,"Alert notify method ID can not be null"),
    ALERTNOTIFY_FILEPATH_ISNULL(461007,"Alert notify method file path can not be null"),
    ALERTNOTIFY_NAME_DUPLICATE(462008, "Alert notify method name duplicate"),
    
    /**
     * 告警判断
     */
    ALERT_JUDGE_RESULT_ISNULL(471001,"query result is null"),
    ALERT_JUDGE_RESULT_FAILE(475002,"query result status is not success"),
    ALERT_JUDGE_RESULT_TYPE_ERROR(475003,"query result type is not vetor"),
    
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
    SYS_LOGIN_ACCOUNTAUTH(518007, "Authentication failed"), //认证失败
    SYS_LOGIN_REQUIRED(518008, "Please log in to the system first"), //账号验证失败
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
    SYS_USER_STATUS_INVALIDE(514021, "System user status must be 0 or 1"),
    SYS_USER_EMAIL_FORMAT(513022, "System user email format is incorrect"),
    SYS_USER_DISABLE_MFA_ERROR(513023, "Forced on, unable to close by itself"),
    SYS_USER_PIN_TOO_SHORT(513024,"The System user password minimum length is {0}"),
    SYS_USER_PIN_NOT_CONTAIN_NUMBER(513025,"System user pin must contain number"),
    SYS_USER_PIN_NOT_CONTAIN_LOWERCASE_LETTERS(513026,"System user pin must contain lowercase letters"),
    SYS_USER_PIN_NOT_CONTAIN_UPPERCASE_LETTERS(513027,"System user pin must contain uppercase letters"),
    SYS_USER_PIN_NOT_CONTAIN_SPECIAL_LETTERS(513028,"System user pin must contain special letters"),
    SYS_USER_PIN_LONGTIME_NO_MODIFY(513029,"System user pin has not been modified for a long time more than {0}"),
    SYS_USER_PIN_RETRY_ERROR(513030,"System user pin too many errors more than {0} , please try again after {1} minutes"),
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
    SYS_TOKEN_OR_AUTHTOKEN_ISNULL(511038,"Token information is null"),
    SYS_AUTHTOKEN_ISVALID(518039,"Authtoken information is invalid"),
    SYS_TOKEN_REFRESH_FREQUENT(518040,"More than 10 operations are frequent. Please try again after 30 minutes"),
    SYS_AUTHCODE_ISNULL(511041,"Authcode information is null"),
    SYS_AUTHCODE_IS_INVALID(511042,"Authcode information is invalid"),
    SYS_RECOVERYCODE_IS_INVALID(518043,"Recoverycode is invalid"),
    SYS_AUTHTOKEN_ISNULL(511044,"Authtoken information is null"),
    SYS_USER_STARRED_ISNULL(511045,"Starred can not be empty"),
    SYS_USER_STARRED_TYPE_ISNULL(511046,"Starred type can not be empty"),
    SYS_USER_STARRED_TID_ISNULL(511047,"Starred tid can not be empty"),
    SYS_USER_STARRED_ID_TYPE_ISNULL(511048,"Starred id and type at the same time null"),
    SYS_USER_NEWPWD_ISNULL(511049, "User new password can not be empty"), //新密码不能为空

    AGENT_ID_ISNULL(521000, "Agent id can not be empty"),
    AGENT_DCID_ISNULL(521001, "Agent Dc id can not be empty"),
    AGENT_TYPE_ISNULL(521002, "Agent type can not be empty"),
    AGENT_HOST_ISNULL(521003, "Agent host can not be empty"),
    AGENT_PORT_ISNULL(521004, "Agent port can not be empty"),
    AGENT_PARAM_QUERY_ISNULL(521007, "Parameter 'query' is required"),
    AGENT_PARAM_START_ISNULL(521008, "Parameter 'start' is required"),
    AGENT_PARAM_END_ISNULL(521009, "Parameter 'end' is required"),
    AGENT_PARAM_STEP_ISNULL(521010, "Parameter 'step' is required"),
    AGENT_PARAM_MATCH_ISNULL(521011, "Parameter 'match[]' is required"),
    AGENT_PORT_ERROR(523012, "Agent port is incorrect"),
    AGENT_HOST_ERROR(523013, "Agent host is incorrect"),
    AGENT_NOTEXSITS(526014, "Agent is not exsits"),    // Agent 不存在
    AGENT_EXIST_ERROR(526015, "Agent is exists : host,type,port repeat"),
    AGENT_TYPE_INVALIDE(523016,"Agent type is incorrect"),
    AGENT_UNAVAILABLE(526017,"No agent available"),
    AGENT_TOKEN_ISNULL(521018, "Agent token info can not be empty"),
    AGENT_NAME_ISNULL(521019, "Agent name can not be empty"),
    AGENT_NAME_EXISTED(521020, "Agent name existed"),
    AGENT_PARAM_AGENTID_ISNULL(521021, "Parameter 'agentId' is required"),


    CABINET_NOT_EXISTS(536001,"Cabinet does not exist"),
    CABINET_POSITION_OCCUPIED(535002,"Cabinet position is occupied"),
    CABINET_USIZE_ERROR(535003,"Cabinet u size range is 1 to 47"),
    CABINET_USIZE_FORMAT(533004,"Cabinet u size format should be a integer"),
    CABINET_X_FORMAT(533005,"Cabinet x format should be a integer"),
    CABINET_X_INCORRECT(535006,"Cabinet x is not in this dc"),
    CABINET_X_MINIMUM(535007,"Cabinet x is a minimum of 1"),
    CABINET_Y_FORMAT(533008,"Cabinet y format should be a integer"),
    CABINET_Y_INCORRECT(535009,"Cabinet y is not in this dc"),
    CABINET_Y_MINIMUM(535010,"Cabinet y is a minimum of 1"),
    DC_ASSETS_EXISTS(533011,"These Dc contain assets and cannot be deleted"),
    DC_CABINETID_ISNULL(531012,"Cabinet id can not be empty"),
    DC_CABINETNAME_DUPLICATE(532013,"Cabinet name duplicate"),
    DC_CABINETNAME_ISNULL(531014,"Cabinet name can not be empty"),
    DC_CABINETS_EXISTS(533015,"These Dc contain cabinets and cannot be deleted"),
    DC_CABINETUSIZE_MINIMUM(535016,"Cabinet u size is a minimum of 1"),
    DC_CABINET_ASSETS_EXISTS(533017,"These cabinets contain assets and cannot be deleted"),
    DC_CABINET_MAXU(535018,"The maximum U of current cabinet is 47"),
    DC_ID_ISNULL(531019,"Dc id can not be empty"),
    DC_LATITUDE_ERROR(535020,"Dc latitude range is -90 to +90"),
    DC_LATITUDE_FORMAT(533021,"Dc latitude format should be a double"),
    DC_LONGITUDE_ERROR(535022,"Dc longitude range is -180 to +180"),
    DC_LONGITUDE_FORMAT(533023,"Dc longitude format should be a double"),
    DC_NAME_DUPLICATE(532024,"Dc name duplicate"),
    DC_NAME_ISNULL(531025,"Dc name can not be empty"),
    DC_NOTHAS_CABINET(536026,"There is no this cabinet in the dc"),
    DC_NOT_EXISTS(536027,"Dc does not exist"),
    DC_PRINCIPAL_NOT_EXISTS(536028,"Dc principal does not exist"),
    DC_STATE_ERROR(534029,"Dc state must be ON or OFF"),
    DC_STATE_ISNULL(531030,"Dc state can not be empty"),
    DC_X_ALREADYINUSE(535031,"Dc x is already in use"),
    DC_X_MINIMUM(535032,"Dc x is a minimum of 1"),
    DC_Y_ALREADYINUSE(535033,"Dc y is already in use"),
    DC_Y_MINIMUM(535034,"Dc y is a minimum of 1"),
    DC_RELOBJ_CAN_NOT_REMOVE(537035, "Dc have associated data and cannot be deleted"),
    CABINET_RELOBJ_CAN_NOT_REMOVE(537037, "Cabinet have associated data and cannot be deleted"),


    /**
     * SYS_I18N
     */
    SYS_I18N_DUPLICATE(542000, "duplicate error (code + lang)"), //国际化配置重复（code+lang）
    SYS_I18N_NOSUCHLANG(543001, "no such lang"),                  //不合法的lang


    /**
     * SYS_DICT
     */
    SYS_DICT_NAME_ISNULL(551000, "Field 'name' is not null"),        // 字典名称不能为空
    SYS_DICT_TYPE_ISNULL(551001, "Field 'type' is not null"),        // 字典类型不能为空
    SYS_DICT_VALUE_ISNULL(551002, "Field 'value' is not null"),       // 字典值不能为空
    SYS_DICT_CODE_ISNULL(551003, "Field 'code' is not null"),        // 字典值不能为空
    SYS_DICT_VALUE_DUPLICATE(552004, "Field 'type' is duplicate"),  //  字典值重复
    SYS_DICT_ID_INVALIDE(553005, "Invalid system dictionary ID"),
    SYS_DICT_ID_ISNULL(551006, "System dictionary ID is null"),
    SYS_DICT_REMOVE_ERROR(553007,"Those dict params contain model can not remove"),

    /**
     * SNMP_MIB
     */
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


    /**
     * SYS_CONFIG
     */
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
    SYS_CONFIG_CODE_FILE_ISNULL(574066,"The validate file is not exist"), //不存在校验使用的文件
    SYS_CONFIG_CODE_INVALID(574067,"The code is invalid"),//校验文件中的code与传递的值不符
    SYS_CONFIG_DB_PARAM_INVALID(574068,"The DB connect params is invalid"),//校验文件中的code与传递的值不符
    SYS_CONFIG_REDIS_PASSWORD_INVALID(574069,"The redis password is invalid"),
    SYS_CONFIG_REDIS_PASSWORD_REQUIRED(574070,"The redis password is required"),
    SYS_CONFIG_REDIS_PARAM_INVALID(574071,"The redis params is invalid"),
    SYS_CONFIG_DB_URL_ISNULL(574072,"The DB host is null"),
    SYS_CONFIG_DB_USERNAME_ISNULL(574073,"The DB username is null"),
    SYS_CONFIG_DB_PASSWORD_ISNULL(574074,"The DB password is null"),
    SYS_CONFIG_REDIS_HOST_ISNULL(574075,"The redis host is null"),
    SYS_CONFIG_REDIS_PORT_ISNULL(574076,"The redis host is null"),
    SYS_CONFIG_HAD_CONFIG(574077,"Someone has started to configure the system"),
    SYS_CONFIG_SESSION_TIMEOUT_INVALID(573078,"Session timeout is not less than 15"),
    SYS_CONFIGI_PROMEFEDEENABLED_NCORRECT(574079, "Prometheus federation enabled must be 0 or 1"),
    SYS_CONFIGI_ASSETPINGFROM_NCORRECT(574080, "Asset ping from must be 1 or 2"),
    SYS_CONFIGI_METRICSSTORAGETYPE_NCORRECT(574081, "Metrics storage type must be 1 or 2"),
    SYS_CONFIGI_LOGSSTORAGETYPE_NCORRECT(574082, "Logs storage type must be 1 or 2"),
    SYS_CONFIG_DB_NAME_ISNULL(574083,"The DB name is null"),


    /**
     * SYS_ROLE
     */
    SYS_ROLE_ID_ISNULL(581000, "Role id is required"),
    SYS_ROLE_ISNULL(586001, "Role is not exsits"),
    SYS_ROLE_NAME_ISNULL(581002, "Role name is required"),
    SYS_ROLE_REMARK_ISNULL(581003, "Role remark is required"),
    SYS_ROLE_NAME_DUPLICATE(582004, "Role name duplicate"),
    SYS_ROLE_MENUID_ISNULL(581005, "Role related menu id is required"),
    SYS_ROLE_NAME_CANNOT_MODIFY(587006, "The name of the built-in role cannot be modified"),
    SYS_ROLE_CANNOT_DELETE(587007, "Built-in roles cannot be deleted"),
    SYS_ROLE_REMOVE_ERROR(587008,"These roles contains user can't remove roles"),
    SYS_ROLE_RELOBJ_CAN_NOT_REMOVE(587009, "Role have associated data and cannot be deleted"),


    /**
     * SNMP_CREDENTIAL
     */
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
     * sys_backup 相关
     */
    SYS_BACKUP_FILENAME_ISNULL(601000, "System backup file name cannot be empty"),
    SYS_BACKUP_FILENAME_NOTEXIST(601001, "System backup file does not exist"),
    SYS_BACKUP_ERROR(609002, "System data backup is abnormal"),


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
     * LINK
     */
    LINK_ID_ISNULL(621001,"link id can not be null"),
    LINK_NAME_ISNULL(621002,"link name can not be null"),
    LINK_URL_ISNULL(621003,"link url can not be null"),
    LINK_PREV_ISNULL(621004,"link prev can not be null"),
    LINK_NEXT_ISNULL(621005,"link next can not be null"),
    LINK_BUILDIN_CAN_NOT_REMOVE(627006,"This link is built-in and cannot be deleted"),
    LINK_WEIGHT_PARAM_INVALID(627007,"link weight param is invalid"),

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
    JSON_PARAM_INVALIDED(811006,"param must be json"),
    SYSSTAT_TYPE_IS_NULL(811007,"param type can not be null"),
    SYSSTAT_TYPE_INVALIDED(811008,"param type is invalide, allow asset,datacenter,project,module,endpoint,alert rule,alert message,agent"),
    SYSSTAT_FILTER_IS_NULL(811009,"param filter can not be null"),
    SYSSTAT_NAME_IS_NULL(811010,"param name can not be null"),
    SYSSTAT_SELECT_IS_NULL(811011,"param select can not be null"),
    SYSSTAT_TABLE_INVALIDED(813012,"param table invalided"),
    SYSSTAT_Q_IS_NULL(813013,"param q can not be null"),
    FILTER_NAME_NOTFOUND(814014, "filter name not found in system configuration"),
    FILTER_OPER_INVALIDED(813015, "filter oper value is invalide"),
    FILTER_DATACENTER_IS_NULL(811016, "filter datacenter is null"),
    FILTER_ASSET_IS_NULL(811017, "filter asset is null"),
    FILTER_ASSET_TYPE_IS_NULL(811018, "filter asset type is null"),
    FILTER_ASSET_STATE_IS_NULL(811019, "filter asset state is null"),
    FILTER_ASSET_BRAND_IS_NULL(811020, "filter asset brand is null"),
    FILTER_ASSET_MODEL_IS_NULL(811021, "filter asset model is null"),
    FILTER_ASSET_ID_IS_NULL(811022, "filter asset id is null"),
    FILTER_PROJECT_IS_NULL(811023, "filter project is null"),
    FILTER_MODULE_IS_NULL(811024, "filter module is null"),
    FILTER_PRIORITY_IS_NULL(811025, "filter priority is null"),
    FILTER_ALERT_RULE_IS_NULL(811026, "filter alert rule is null"),
    FILTER_ENDPOINT_IS_NULL(811027, "filter endpoint is null"),


    /**
     * 导入相关
     */
    EXCELFILE_TYPE_ERROR(900, "The type can only be xlsx, json, csv"),
    EXCELFILE_PARSE_ERROR(901, "Import file resolution failed"),
    EXCELFILE_HEADER_TEMPLATE_ERROR(902,"The header row of the import template is inconsistent with the system template"),
    EXCELFILE_HEADER_LANGUAGE_ERROR(903, "Language must be en, zh or ru"),
    EXCELFILE_IMPORT_FILE_ISNULL(904, "Import file is null"),
    EXCELFILE_HEADER_LANGUAGE_ISNULL(905, "Language can not be empty"),
    EXCELFILE_IMPORT_ERROR(906, "File import error"),
    UNDO_IMPORT_SEQ_ISNULL(907, "Undo import sequence number cannot be empty"),
    IMPORT_FIELD_REQUIRED(908, "{0} Required"),
    IMPORT_FIELD_DUPLICATE(909, "{0} Duplicate"),
    IMPORT_FORMAT_ERROR(910, "{0} Format error"),
    IMPORT_RELATED_OBJ_NOT_EXIST(911, "{0} Related content does not exist"),
    IMPORT_ENUM_VALUE_ERROR(912, "{0} Not in enum value list"),
    IMPORT_IGNORE(913, "Duplicate {0}, ignore the data in this row"),
    IMPORT_NOT_SUPPORT(914, "{0} Configuration not support"),
    IMPORT_NOT_WITHIN_REASON(915, "{0} Not within reason"),
    IMPORT_NOT_MATCH(916, "{0} does not match {1} configuration"),


    /**
     * Prometheus 相关错误
     */
    PROMETHEUS_LABELNAME_FORMAT_ERROR(910,"Label name does not comply with Prometheus labelname format"),

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
	LICENSE_FILE_IS_EMPTY(716001, "The license file does not exist"), 
	LICENSE_FILE_INVALID(715001, "Invalid license,code: {0}"),
	LICENSE_PARAM_IS_NULL(711002, "license parameter is null"),
	LICENSE_TOKEN_INVALID(715003, "Invalid license,token error"),
	LICENSE_MAXIMUMASSETS_INVALID(715004, "The number of assets exceeds the maximumAssets"),
	LICENSE_VERSION_INVALID(715005, "The version is not valid"),
	LICENSE_ERROR_CODE(715006,"License error,code:{0}"),
	LICENSE_DIGESTVALUE_INVALID(715007,"Lincese digest value error ,The file may have been modified"),
	LICENSE_SUPPORT_ID_ISNULL(715008,"License params support id can not be null"),
	LICENSE_ORGANIZATION_ISNULL(715009,"License params organization can not be null"),
	LICENSE_TYPE_ISNULL(715010,"License params type can not be null"),
	
	/**
	 * Ipamsubnet 相关
	 */
	IPAM_SUBNET_ID_ISNULL(721001,"Ipamsubnet id can not be null"),
	IPAM_SUBNET_NAME_ISNULL(721002,"Ipamsubnet name can not be null"),
	IPAM_SUBNET_TYPE_ISNULL(721003,"Ipamsubnet type can not be null"),
	IPAM_SUBNET_ADDR_ISNULL(721004,"Ipamsubnet addr can not be null"),
	IPAM_SUBNET_MASK_ISNULL(721005,"Ipamsubnet mask can not be null"),
	IPAM_SUBNET_DCID_ISNULL(721006,"Ipamsubnet dc id can not be null"),
	IPAM_SUBNET_VLAN_ISNULL(721007,"Ipamsubnet vlan can not be null"),
	IPAM_SUBNET_TYPE_ERROR(723008,"Ipamsubnet type only allow 4 or 6"),
	IPAM_SUBNET_ADDR_ERROR(723009,"Ipamsubnet addr format error,please check ip format for ipv4/6"),
	IPAM_SUBNET_MASK_ERROR(723010,"Ipamsubnet mask error, allow ipv4(0~32),ipv6(64-128)"),
	IPAM_SUBNET_DC_ISNULL(721011,"Ipamsubnet datacenter can not be null"),
	IPAM_SUBNET_INTERVAL_ERROR(723012,"Ipamsubnet interval already exists"),
    IPAM_SUBNET_NAME_ERROR(723013,"Ipamsubnet name already exists"),
    IPAM_SUBNET_RELOBJ_CAN_NOT_REMOVE(587009, "Ipamsubnet have associated data and cannot be deleted"),

    /**
     * IpamIp 相关
     */
    IPAM_IP_ID_ISNULL(731001,"Ipamip id can not be null"),
    IPAM_IP_NAME_ISNULL(731002,"Ipamip name can not be null"),
    IPAM_IP_ADDR_ISNULL(731003,"Ipamip addr can not be null"),
    IPAM_IP_MAC_ISNULL(731004,"Ipamip mac can not be null"),
    IPAM_IP_ADDR_ERROR(731005,"Ipamip addr format error,please check ip format for ipv4/6"),
    IPAM_IP_INTERVAL_ERROR(731006,"Ipamip interval already exists"),
    IPAM_IP_ASSET_ISNULL(731007,"Ipamip asset can not be null"),
    IPAM_IP_SUBNET_ISNULL(731008,"Ipamip subnet can not be null"),
    IPAM_IP_MAC_ERROR(731009,"Ipamip mac error, mac repeat"),
    IPAM_IP_SUBNET_ADDR_ERROR(731010,"Ipamip addr error, ip addr different subnet addr "),
    IPAM_IP_MAC_ADDR_ERROR(731011,"Ipamip mac and addr exist"),
    IPAM_IP_NAME_ERROR(731012,"Ipamip name already exists"),

    /**
     * Record rule 相关
     */
    RECORD_RULE_ID_ISNULL(741001,"Record rule id can not be null"),
    RECORD_RULE_NAME_ISNULL(741002,"Record rule name can not be null"),
    RECORD_RULE_TYPE_ISNULL(741003,"Record rule type can not be null"),
    RECORD_RULE_EXPR_ISNULL(741004,"Record rule expr can not be null"),
    RECORD_RULE_LABELS_ISNULL(741005,"Record rule labels can not be null"),
    RECORD_RULE_STATE_ISNULL(741006,"Record rule state can not be null"),
    RECORD_RULE_NAME_EXIST(742001,"Record rule name already exists"),
    RECORD_RULE_BUILD_IN_EXIST(740001,"Record rule built-in allows deletion"),
    RECORD_RULE_TYPE_ERROR(743001,"Record rule type error"),
    RECORD_RULE_STATE_ERROR(743002,"Record rule state error"),


    /**
     * Tool
     */
    TOOL_PING_IP_ISNULL(741000, "Ping ip can not be empty"),
    TOOL_TRACEROUTE_IP_ISNULL(741001, "Traceroute ip can not be empty"),

    ERROR(999, "error"); //通用错误/未知错误


	private RCode(Integer code, String msg) {
		this.code = code;
		this.msg = msg;
	}

	private Integer code;
	private String msg;
	private Object[] param;
	
	public RCode setParam(Object... param) {
		this.param = param;
//		this.msg = MessageFormat.format(msg, param);
		return this;
	}
	
	public Object[] getParam() {
		return param;
	}
	
	public Integer getCode() {
		return code;
	}

	public String getMsg() {
		return MessageFormat.format(msg, param);
	}
}
