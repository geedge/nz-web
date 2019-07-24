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

    SYS_SYS_NULLNAME(102001, "systemname_null_error"), //业务系统名d称不能为空
    SYS_SYS_DUPLICATENAME(102002, "systemname_duplicate_error"), //业务系统名称重复

    SYS_MENU_SYSMENUDEL(103001, "sysmenu_del_error"), //系统菜单，不能删除
    SYS_MENU_DELSUB(103002, "del_sub_first_error"), //请先删除子菜单或按钮
    SYS_USER_OLDPWD(103003, "user_oldpwd_error"), //原密码不正确
    SYS_USER_DELADMIN(103004, "del_admin_error"), //系统管理员不能删除
    SYS_USER_CANNOTDEL(103005, "cannot_del_user_error"), //当前用户不能删除


    /**
     * 节点相关异常200XXX
     */
    NODE_ERROR(200000, "node_error"), //20xxxx，设备类（节点、节点组、机房、机柜）通用错误

    NODE_IPADDRESS_FORMAT(201001, "ipaddress_format_error"),// ip 地址不合法 格式错误
    NODE_IPADDRESS_DUPLICATE(201002, "duplicate_ipaddress_error"), // IP 地址重复
    NODE_NAME_DUPLICATE(201003, "duplicate_name_error"), // name 名称  重复

    NODEGROUP_NAME_DUPLICATE(202001, "duplicate_group_name_error"), // name 节点组 名称  重复

    NODEROOM_NAME_DUPLICATE(203001, "duplicate_room_name_error"),// 机房名称重复

    NODECABINET_NAME_DUPLICATE(204001, "duplicate_cabinet_name_error"), // 机柜名称重复




    DETE_ERROR(300000, "dete_error"), //30xxxx，监测类（类别、设置、告警）通用错误
    /**
     * 301xxx 监测类别相关异常
     * 302xxx 监测设置相关异常
     * 303xxx 监测告警相关异常
     */
    TYPE_NAME_ISNULL(301001, "type_name_isNull"),//监测类别名称为空
    TYPE_NAME_EXISTS(301002, "type_name_exists"),//监测类别名称已存在
    TYPE_SAVE_IDISNULL(301003, "type_save_idIsNull"),//监测类别ID为空
    META_NAME_ISNULL(301004, "meta_name_isnull"),//监测类别元数据名称为空
    PARAM_NAME_ISNULL(301005, "param_name_isnull"),//监测类别参数名称为空
    META_NAME_EXISTS(301006, "meta_name_exists"), //监测类别元数据名称重复
    PARAM_NAME_EXISTS(301007, "param_name_exists"), //监测类别参数名称重复
    META_NAME_INVAILD(301008, "meta_name_invaild"),    //监测类别元数据名称不合法
    META_NAME_HAVEKEYWORD(301009, "meta_name_haveKeyword"),    //监测类别元数据名称是数据库关键字
    PARAM_INDEX_ISNULL(301010, "param_index_isNull"),//监测类别参数下标为空
    TYPE_TABLE_EXISTS(301011, "type_table_exists"),//监测类别对应表已存在

    MISSION_ERROR(400000, "mission_error"), //40xxxx，任务类通用错误


    DASHBOARD_ERROR(500000, "dashboard_error"), //50xxxx，仪表盘通用错误


    GRAPH_ERROR(600000, "graph_error"), //60xxxx，图形类通用错误


    NOT_NULL_ERROR(990, "not_null_error"), //not null错误
    NOT_NUMBER_ERROR(991, "not_number_error"), //not number错误
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

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }
}