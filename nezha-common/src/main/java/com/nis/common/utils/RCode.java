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

	SYS_SYS_NULLNAME(102001, "systemname_null_error"), //业务系统名称不能为空
	SYS_SYS_DUPLICATENAME(102002, "systemname_duplicate_error"), //业务系统名称重复
	SYS_SYS_DELGLOBAL(102003, "delete global error"), //不能删除global业务系统

	SYS_MENU_SYSMENUDEL(103001, "sysmenu_del_error"), //系统菜单，不能删除
	SYS_MENU_DELSUB(103002, "del_sub_first_error"), //请先删除子菜单或按钮
	SYS_USER_OLDPWD(103003, "user_oldpwd_error"), //原密码不正确
	SYS_USER_DELADMIN(103004, "del_admin_error"), //系统管理员不能删除
	SYS_USER_CANNOTDEL(103005, "cannot_del_user_error"), //当前用户不能删除

	SYS_USERGROUP_DUPLICATENAME(104001, "usergroup name duplicate error"), //用户组名称重复
	

	NODE_ERROR(200000, "node_error"), //20xxxx，设备类（节点、节点组、机房、机柜）通用错误

	NODE_IPADDRESS_FORMAT(201001, "ipaddress_format_error"),// ip 地址不合法 格式错误
	NODE_IPADDRESS_DUPLICATE(201002, "ipaddress_duplicate_error"), // IP 地址重复
	NODE_NAME_DUPLICATE(201003, "name_duplicate_error"), // name 名称  重复
	NODE_USTART_OCCUPIED(201004, "ustart_occupied_error"), // 节点 U 位已被占用 或 不存在

	NODEGROUP_NAME_DUPLICATE(202001, "groupname_duplicate_error"), // name 节点组 名称  重复

	NODEROOM_NAME_DUPLICATE(203001, "roomname_duplicate_error"),// 机房名称重复
	NODEROOM_ROW_OCCUPIED(203002,"row_occupied_error"), // 机房行号已被占用
	NODEROOM_COL_OCCUPIED(203003,"col_occupied_error"), // 机房列号已被占用
	NODEROOM_EXISTS_CABINET(203004,"cabinet_exists_error"), // 机房下存在机柜 错误
	NODEROOM_POSITION_OCCUPIED(203005,"position_occupied_error"), // 机房该行列被占用 机柜无法使用此位置

	NODECABINET_NAME_DUPLICATE(204001, "cabinetname_duplicate_error"), // 机柜名称重复
	NODECABINET_ROW_INCORRECT(204002,"rownumber_incorrect_error"),   // 行值不能为0 零
	NODECABINET_COL_NCORRECT(204003,"colnumber_incorrect_error"),    // 列值不能为0 零
	NODECABINET_ROW_MAXNUM(204004,"rownumber_maxnum_error"),    // 行超过机房最大值
	NODECABINET_COL_MAXNUM(204005,"colnumber_maxnum_error"),    // 列超过机房最大值
	NODECABINET_NODES_EXISTS(204006,"related_nodes_error"),       // 机柜存在关联节点
	NODECABINET_UBIT_OCCUPIED(204007,"ubit_occupied_error"),       // U位中存在节点 U位修改值小了

	DETE_ERROR(300000, "dete_error"), //30xxxx，监测类（类别、设置、告警）通用错误
	/**
	 * 301xxx 监测类别相关异常
	 * 302xxx 监测设置相关异常
	 * 303xxx 监测告警相关异常
	 * */
	TYPE_NAME_ISNULL(301001,"type_name_isNull"),//监测类别名称为空
	TYPE_NAME_EXISTS(301002,"type_name_exists"),//监测类别名称已存在
    META_FIELDNAME_EXISTS(301003,"meta_fieldname_exists"),//监测类别元数据字段名称重复
	META_NAME_ISNULL(301004,"meta_name_isnull"),//监测类别元数据名称为空
	PARAM_NAME_ISNULL(301005,"param_name_isnull"),//监测类别参数名称为空
	META_NAME_EXISTS(301006,"meta_name_exists"), //监测类别元数据名称重复
	PARAM_NAME_EXISTS(301007,"param_name_exists"), //监测类别参数名称重复
	META_NAME_INVAILD(301008,"meta_name_invaild"),	//监测类别元数据名称不合法
	META_NAME_HAVEKEYWORD(301009,"meta_name_haveKeyword"),	//监测类别元数据名称是数据库关键字
	PARAM_INDEX_ISNULL(301010,"param_index_isNull"),//监测类别参数下标为空
	TYPE_TABLE_EXISTS(301011,"type_table_exists"),//监测类别对应表已存在
	DICT_TYPE_ISNULL(301012,"dict_type_isnull"),//监测类别字典类别名称为空
	DICT_MAPPINGS_ISNULL(301013,"dict_mappings_isnull"),//监测类别字典数据为空
	DICT_TYPE_USED(301014,"DICT_TYPE_USED"),//监测类别字典已经被使用，不允许删除

	SET_NAME_ISNULL(302001,"set_name_isnull"),//监测设置名称为空
	SET_NAME_EXISTS(302002,"set_name_exists"),//监测设置名称已经存在
	SET_TYPE_ISNULL(302003,"set_type_isnull"),//监测设置所属类别为空

	MISSION_ERROR(400000, "mission_error"), //40xxxx，任务类通用错误
	MISSION_TYPE_ISNULL(400001,"mission_type_isnull"),   // 任务类型为空
	MISSION_NAME_ISNULL(400002,"mission_name_isnull"),   //任务名称不能为空
	MISSION_PARAM_ISNULL(400003,"mission_param_isnull"), //任务参数不能为空
	MISSION_PARAM_ERROR(400004,"mission_param_error"),   //参数格式错误或部分缺失
	MISSION_ID_ISNULL(400005,"mission_id_isnull"),   //任务id为空
	MISSION_GROUPNODE_ISNULL(400006,"mission_groupnode_isnull"),  //任务节点组信息为空
	MISSION_FILEPUSH_FILE_ISNULL(400101,"mission_filepush_file_isnull"),  //文件推送文件为空
	MISSION_FILEPUSH_DESTPATH_ISNULL(400102,"mission_filepush_destpath_isnull"), // 文件推送路径为空
	MISSION_FILEPUSH_PASSWORD_HANDLE_ERROR(400103,"mission_filepush_password_handle_error"), //文件推送密码处理错误
	MISSION_FILEPUSH_FILE_UPLOAD_ERROR(400104,"mission_filepush_file_upload_error"), //文件推送上传文件错误
	MISSION_FILEPUSH_MD5_ERROR(400105,"mission_filepush_md5_error"), // 文件推送md5处理错误
	MISSION_FILEPUSH_OUTSIZE_ERROR(400106,"mission_filepush_outsize_error"),  // 上传文件超过限制
	MISSION_EXECUTE_INSTRUCT_COMMANDENNAME_ISNULL(400201,"mission_execute_instruct_killprocess_commandenname_isnull"), // 指令具体任务类型不能为空
	MISSION_EXECUTE_INSTRUCT_KILLPROCESS_PIDFILE_ISNULL(400202,"mission_execute_instruct_killprocess_pidfile_isnull"),  //  停止进程不能为空 
	MISSION_EXECUTE_INSTRUCT_KILLPROCESS_LOOPTIME_ERROR(400203,"mission_execute_instruct_killprocess_looptime_error"),  // 周期结束时间小于当前时间
	MISSION_EXECUTE_INSTRUCT_BACKUP_FILE_ISNULL(400301,"mission_execute_instruct_backup_file_isnull"),   //备份文件不能为空
	MISSION_EXECUTE_INSTRUCT_BACKUP_PATH_ISNULL(400302,"mission_execute_instruct_backup_path_isnull"),   //备份路径不能为空
	MISSION_EXECUTE_INSTRUCT_BACKUP_LOOPTIME_ERROR(400303,"mission_execute_instruct_backup_looptime_error"),  // 周期结束时间小于当前时间
	MISSION_EXECUTE_INSTRUCT_UPDATE_SOURCE_ISNULL(400401,"mission_execute_instruct_update_source_isnull"),   //源路径不能为空
	MISSION_EXECUTE_INSTRUCT_UPDATE_COVER_ISNULL(400402,"mission_execute_instruct_update_cover_isnull"),   //覆盖路径不能为空
	MISSION_EXECUTE_INSTRUCT_UPDATE_LOOPTIME_ERROR(400403,"mission_execute_instruct_update_looptime_error"),  // 周期结束时间小于当前时间
	MISSION_EXECUTE_INSTRUCT_START_EXECCMD_ISNULL(400501,"mission_execute_instruct_start_execcmd_isnull"),   // 执行命令不能为空
	MISSION_EXECUTE_INSTRUCT_START_EXECRESULT_ISNULL(400502,"mission_execute_instruct_start_execresult_isnull"), // 执行结果不能为空
	MISSION_EXECUTE_INSTRUCT_START_RETURNPATH_ISNULL(400503,"mission_execute_instruct_start_returnpath_isnull"), // 回执文件不能为空
	MISSION_EXECUTE_INSTRUCT_START_LOOPTIME_ERROR(400504,"mission_execute_instruct_start_looptime_error"), // 周期结束时间小于当前时间
	MISSION_EXECUTE_INSTRUCT_RECOVER_SOURCE_ISNULL(400601,"mission_execute_instruct_recover_source_isnull"),   //源路径不能为空
	MISSION_EXECUTE_INSTRUCT_RECOVER_COVER_ISNULL(400602,"mission_execute_instruct_recover_cover_isnull"),   //覆盖路径不能为空
	MISSION_EXECUTE_INSTRUCT_RECOVER_LOOPTIME_ERROR(400603,"mission_execute_instruct_recover_looptime_error"),  // 周期结束时间小于当前时间
	MISSION_EXECUTE_INSTRUCT_SINGLEEXEC_EXECCMD_ISNULL(400701,"mission_execute_instruct_start_execcmd_isnull"),   // 执行命令不能为空
	MISSION_EXECUTE_INSTRUCT_SINGLEEXEC_RETURNPATH_ISNULL(400702,"mission_execute_instruct_start_returnpath_isnull"), // 回执文件不能为空
	MISSION_EXECUTE_INSTRUCT_SINGLEEXEC_LOOPTIME_ERROR(400703,"mission_execute_instruct_start_looptime_error"), // 周期结束时间小于当前时间
	MISSION_UPGRADE_UPLOADFILE_ISNULL(400801,"mission_upgrade_uploadfile_isnull"),  // 升级部署任务上传文件为空
	MISSION_UPGRADE_UPLOADFILE_OUTSIZE(400802,"mission_upgrade_uploadfile_outsize"), //升级部署任务上传文件过大
	MISSION_UPGRADE_PASSWORD_HANDLE_ERROR(400803,"mission_upgrade_password_handle_error"), // 升级部署任务密码处理错误
	MISSION_UPGRADE_FILE_UPLOAD_ERROR(400804,"mission_upgrade_file_upload_error"),  // 升级部署任务文件上传错误
	MISSION_UPGRADE_MD5_ERROR(400805,"mission_upgrade_md5_error"),   // 升级部署任务md5加密错误
	MISSION_UPGRADE_COVER_ISNULL(400806,"mission_upgrade_cover_isnull"),  // 升级部署任务覆盖路径为空
	MISSION_UPGRADE_BACKUP_PATH_ISNULL(400807,"mission_upgrade_backup_path_isnull"),  // 备份路径为空
	MISSION_UPGRADE_BACKUPTO_ISNULL(400808,"mission_upgrade_backupto_isnull"),  // 升级部署任务备份源路径为空
	MISSION_UPGRADE_STARTUPFILE_ISNULL(400809,"mission_upgrade_startupfile_isnull"),// 升级部署任务启动文件或命令为空
	MISSION_UPGRADE_PIDFILE_ISNULL(400810,"mission_upgrade_pidfile_isnull"),// 升级部署任务PID文件为空

	DASHBOARD_ERROR(500000, "dashboard_error"), //50xxxx，仪表盘通用错误
	DASHBOARD_ID_NULL(500001,"dashboard_id_null"),  //仪表盘id为空
	DASHBOARD_X_NULL(500002,"dashboard_x_postion_null"),  //仪表盘x轴位置为空
	DASHBOARD_Y_NULL(500003,"dashboard_Y_postion_null"),  //仪表盘y轴位置为空
	DASHBOARD_HEIGHT_NULL(500004,"dashboard_height_null"),  //仪表盘高度为空
	DASHBOARD_WIDTH_NULL(500005,"dashboard_width_null"),  //仪表盘宽度为空
	DASHBOARD_TYPE_NULL(500006,"dashboard_type_null"),  //仪表盘类型为空
	DASHBOARD_USERSQL_NULL(500007,"dashboard_user_sql_null"),  //仪表盘查询语句为空
	DASHBOARD_USERSQL_ERROR(500008,"dashboard_user_sql_error"),  //仪表盘查询语句错误
	
	GRAPH_ERROR(600000, "graph_error"), //60xxxx，图形类通用错误


	NOT_NULL_ERROR(990, "not_null_error"), //not null错误
	NOT_NUMBER_ERROR(991, "not_number_error"), //not number错误
	NO_PERMISSION_ERROR(998, "no permission error"), //无权限错误
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