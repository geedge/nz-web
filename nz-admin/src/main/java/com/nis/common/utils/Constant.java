/**

 *

 *

 */

package com.nis.common.utils;

import org.apache.ibatis.mapping.DatabaseIdProvider;

import javax.sql.DataSource;
import java.io.File;
import java.sql.SQLException;
import java.util.*;

/**
 * 常量
 *

 */
public class Constant {
	
	/** 超级管理员ID */
	public static final int SUPER_ADMIN = 1;
    /** 数据权限过滤 */
	public static final String SQL_FILTER = "sql_filter";
    /**
     * 当前页码
     */
    public static final String PAGE = "pageNo";
    /**
     * 每页显示记录数
     */
    public static final String LIMIT = "pageSize";
    /**
     * 每页显示条数
     */
    public static final long PAGESIZE = 10;
    /**
     * 排序字段
     */
    public static final String ORDER_FIELD = "sidx";
    /**
     * 排序方式
     */
    public static final String ORDER = "order";
    /**
     *  升序
     */
    public static final String ASC = "asc";

	/**
	 * 菜单类型
	 */
    public enum MenuType {
        /**
         * 目录
         */
    	CATALOG(0),
        /**
         * 菜单
         */
        MENU(1),
        /**
         * 按钮
         */
        BUTTON(2);

        private int value;

        MenuType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
    
    /**
     * 当前数据库类型
     */
    public static final String DB_TYPE;
    
    public static final String ORACLE = "oracle";
    public static final String MYSQL = "mysql";
    public static final String POSTGRESQL = "postgresql";


    // 系统重置类型
    public static final String[] SYSCONFIG_RESET_TYPE = {"metric","alert","sysconfig"};

    // 系统输入内容的table
    public static final String[] TABLESWITHCLEAR = {
            "account","alert_rule","asset","cabinet","chart","chart_element","chart_legend",
            "endpoint","idc","maintenance_record","model","module","panel","project","prom_server",
            "snmp_mib","sys_area","sys_dict"};
    
    /**
     * 告警等级
     */
//    public static final List<String> ALERT_LEVEL;
    /**
     * 告警信息状态
     */
    public static final List<Integer> ALERTMESSAGE_STATE;
    /**
     * redis告警信息的key
     */
    public static final String REDIS_KEY_ALERTMESSAGE = "ALERT_MESSAGE";

    /**
     * redis metric集合的key
     */
    public static final String REDIS_KEY_METRICS = "cacheMetricList";

    /**
     *redis metric labels的key
     */
    public static final String REDIS_KEY_METRICLABELS = "cacheMetricLabels";

    /**
     * metricjob 分布式锁
     */
    public static final String REDIS_KEY_METRIClOCK = "metricLock";

    /**
     * 记录endpoint和proserver 对应关系
     */
    public static final String REDIS_KEY_ENDPOINT_PROM = "endpoint_prom";

    /**
     * 临时目录 
     */
    public static final String TEMP_PATH = System.getProperty("user.dir") + File.separator + "tmp";
    
    /**
     * 工具目录 
     */
    public static final String TOOL_PATH = System.getProperty("user.dir") + File.separator + "tools";

    public static final List<String> SPECIAL_SYS_DICT;

    // 当前sys_config 配置中配置项和所属类别map
    public static final Map<String, List<String>> SYSCONFIG_TYPEWITHKEYS_MAP;

    // module 类型 可选值 http、snmp
    public static final String MODULE_TYPE_HTTP = "http";
    public static final String MODULE_TYPE_SNMP = "snmp";

    public static final Integer SNMP_DEFAULT_PORT = 161;

    static {
    	DatabaseIdProvider databaseIdProvider = SpringContextUtils.getBean("getDatabaseIdProvider", DatabaseIdProvider.class);
        DataSource dataSource = SpringContextUtils.getBean("dataSource",DataSource.class);
        String databaseId = null;
		try {
			databaseId = databaseIdProvider.getDatabaseId(dataSource);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		DB_TYPE = databaseId;
		
		File tempPath = new File(TEMP_PATH);
		if (!tempPath.isDirectory()) {
			tempPath.mkdir();
		}
		
//		ALERT_LEVEL = new ArrayList<String>();
//		ALERT_LEVEL.add("warning");
//		ALERT_LEVEL.add("critical");
//		ALERT_LEVEL.add("emergency");
		
		ALERTMESSAGE_STATE = new ArrayList<Integer>();
		ALERTMESSAGE_STATE.add(1);
		ALERTMESSAGE_STATE.add(2);

        SPECIAL_SYS_DICT = new ArrayList<String>();
        SPECIAL_SYS_DICT.add(Constant.DICT_ASSET_MODEL_TYPE);
        SPECIAL_SYS_DICT.add(Constant.DICT_ASSET_MODEL_VENDOR);

        SYSCONFIG_TYPEWITHKEYS_MAP = new HashMap<>();

        SYSCONFIG_TYPEWITHKEYS_MAP.put("basic", Arrays.asList(new String[]{"alert_api", "asset_ping_switch", "asset_ping_interval", "node_exporter_target_path", "scrape_interval", "storage_local_retention","system_name"}));
        SYSCONFIG_TYPEWITHKEYS_MAP.put("email", Arrays.asList(new String[]{"email_enable", "email_smtp_host", "email_smtp_port", "email_smtp_account", "email_smtp_password", "email_send_account", "email_test_reciver", "email_ssl_flag", "email_tls_flag"}));
        SYSCONFIG_TYPEWITHKEYS_MAP.put("terminal", Arrays.asList(new String[]{"max_terminal_num"}));
        SYSCONFIG_TYPEWITHKEYS_MAP.put("ldap", Arrays.asList(new String[]{"ldap_address", "ldap_dn", "ldap_password", "ldap_user_filter", "ldap_mapping", "ldap_enable"}));
    }
    
    /**
     * 支持的语言 [{"name":"简体中文","value":"zh"},...]
     */
    public static List<Map<String, String>> langList = new ArrayList<>();
    
    /**
     * 字典中语言的名称和类型
     */
    public static final String DICT_NAME_LANG = "lang";
    
    /**
     * 定时任务状态
     */
    public enum ScheduleStatus {
        /**
         * 正常
         */
    	NORMAL(0),
        /**
         * 暂停
         */
    	PAUSE(1);

        private int value;

        ScheduleStatus(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }

    /**
     * 云服务商
     */
    public enum CloudService {
        /**
         * 七牛云
         */
        QINIU(1),
        /**
         * 阿里云
         */
        ALIYUN(2),
        /**
         * 腾讯云
         */
        QCLOUD(3);

        private int value;

        CloudService(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static final Long GLOBAL_ID = 1l;
    
    //delFlag
    public static final String DELFLAG_TRUE = "1"; //删除
    public static final String DELFLAG_FALSE = "0"; //未删除

    public static final Integer AUTH_PASSWORD=1;//密码认证
    public static final Integer AUTH_CERTIFICATE=2;//私钥证书认证

    public static final String AUTH_TOKEN_CODE="Authorization";

    public enum AlertType{
        ALERT_TYPE_ASSET(3,"asset"),
        ALERT_TYPE_MODULE(2,"module"),
        ALERT_TYPE_PROJECT(1,"project");

        private Integer code;
        private String type;
        AlertType(Integer code,String type){
            this.code=code;
            this.type=type;
        }

        public static AlertType getAlertType(Integer code){
            for (AlertType  type:AlertType.values()){
                if (type.getCode()==code){
                    return type;
                }
            }
            return null;
        }


        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }



    //permission type
    public static final String PM_TYPE_DETESET = "2";
    public static final String PM_TYPE_DETEWARN = "3";
    public static final String PM_TYPE_MISSION = "4";
    
    //view level
    public static final String VIEWLEVEL_UG = "1"; //usergroup
    public static final String VIEWLEVEL_SYS = "2";
    public static final String VIEWLEVEL_ALL = "3";
    
    public static final Long DETESET_NODE_RELTYPE_NODE = 1L;
    public static final Long DETESET_NODE_RELTYPE_NODEGROUP = 2L;

    //mission result state
    public static final String MISSION_RESULT_SUCCESS = "1";  //成功
    public static final String MISSION_RESULT_FAIL = "2";  //失败
    public static final String MISSION_RESULT_TIMEOUT = "3";  //超时

    //mission loop state
    public static final String MISSION_LOOP_RUN = "1";  //运行
    public static final String MISSION_LOOP_SUSPEND = "2"; //暂停
    public static final String MISSION_LOOP_STOP = "3";  // 终止
    // mission loop
    public static final String MISSION_LOOP_TRUE = "1";   //周期任务
    public static final String MISSION_LOOP_FALSE = "0";   //周期任务

    public static final String STR_PARAM_SEPRATOR=";";

    // mission node rel
    public static final String MISSION_NODE_REL_TYPE_GROUP="1";
    public static final String MISSION_NODE_REL_TYPE_NODE="2";

    //topo图默认宽高
    public static final Integer TOPO_DEFAULT_HEIGHT = 1600;
    public static final Integer TOPO_DEFAULT_WIDTH = 1600;

    public static final String WARN_REL_TYPE="1";
    public static final String WARN_REL_SET="2";

    // 节点类型
    public static final String NODE_TYPE_SERVER = "1";  // server
    public static final String NODE_TYPE_NET = "2";     // net

    // 数据字典定义nodeTag type 类型为nodeTag + server(1) / net (2)
    public static final String DICT_NODE_TAG_TYPE = "nodeTag";

    // 节点组 分配节点/添加子节点组
    public static final String NODEGROUP_REFUSE= "0"; // 不可以
    public static final String NODEGROUP_ALLOW = "1"; // 可以
    
    // system built_in 2非内置，1内置
    public static final String SYSTEM_BUILDIN_YES = "1"; // 内置
    public static final String SYSTEM_BUILDIN_NO = "2"; // 非内置

    //监测类别 监测方式
    public static final String DETE_DETEMETHOD_SCRIPT="1"; //脚本
    public static final String DETE_DETEMETHOD_SNMP="2"; //snmp
    public static final String DETE_DETEMETHOD_INNER="3"; //内置

    //监测告警 回调方式
    public static final String DETE_WARN_CALLBACK_SCRIPT="1";
    public static final String DETE_WARN_CALLBACK_HTTP="2";
    public static final String DETE_WARN_CALLBACK_NONE="0";

    // 顶级节点组PID
    public static final Long NODEGROUP_TOP_PID = 0L;


    // nz-web 新增常量

    // 字典表中资产型号厂商type值
    public static final String DICT_ASSET_MODEL_VENDOR = "vendor";

    // 字典表中资产型号类型type值
    public static final String DICT_ASSET_MODEL_TYPE  = "assetType";

    // 字典表中资产状态 type 值
    public static final String DICT_ASSET_STATE_TYPE = "assetState";

    // 字典表中 account 认证类型的 type 值
    public static final String DICT_ACCOUNT_AUTH_TYPE = "authType";
    // 字典表中 account 认证类型的 type 值

    public static final String DICT_CHART_TYPE = "chartType";

    // 字典表中 account 认证类型的 type 值
    public static final String DICT_EXPRESSION_TYPE = "exprType";

    // 字典表中 告警周期 类型的 type 值
    public static final String DICT_ALERTINTERVAL_TYPE = "alertInterval";

    // 目前机柜最大值限制
    public static final Integer CURRENT_CABINET_MAXU= 47;

    // 默认告警间隔为15s
    public static final Integer ALERT_INTERVAL = 15;

    // ip 校验正则
    public static final String REGEX_IP = "(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d|\\*)\\.(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d|\\*)\\.(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d|\\*)\\.(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d|\\*)";

    // 系统配置表中 内置 module 的 key名
    public static final String SYSCONFIG_KEY_INIT_MODULE = "init_module_node_exporter";

    // 内置 node_exporter 默认安装服务器路径
    public static final String SYSCONFIG_KEY_NODEEXPORTER_TARGETPATH = "node_exporter_target_path";

    // 内置 node_exporter 项目中的路径
    public static final String SYSCONFIG_KEY_NODEEXPORTER_PATH = "node_exporter_path";

    // 内置 node_exporter 压缩包名称
    public static final String SYSCONFIG_KEY_NODEEXPORTER_NAME = "node_exporter_name";

    // 内置 node_exporter 初始执行脚本名称
    public static final String SYSCONFIG_KEY_NODEEXPORTER_SHELLNAME = "node_exporter_shell_name";

    // asset 内置node exporter 标识
    public static final Integer ASSET_BUILD_NODE_EXPORTER_FLAG = 1;

    // 主机连通性检查开关，1：开启，0：关闭
    public static final String SYSCONFIG_KEY_ASSET_PING_SWITCH = "asset_ping_switch";

    // 主机连通性检查开关  打开状态 1
    public static final Integer ASSET_PING_OPEN_SWITCH = 1;

    // asset IP 不可达时返回表述信息
    public static final String ASSET_PING_DOWN_FLAG = "unreachable";

    // asset fping 命令语句 key
    public static final String SYSCONFIG_KEY_ASSET_FPING_COMMAND = "asset_ping_command";

    // asset ping 命令中文件名称 会替换成文件的路径
    public static final String ASSET_PING_COMMAND_FILENAME = "{filename}";

    // 告警级别 高
    public static final String ALERT_SEVERITY_HIGH = "high";

    // 告警级别 中
    public static final String ALERT_SEVERITY_MEDIUM = "medium";

    // 告警级别 低
    public static final String ALERT_SEVERITY_LOW = "low";

    // 系统登陆用户集合
    public static Set<String> loginUsers = new HashSet<>();

    // account protocol ssh协议
    public static final String ACCOUNT_SSH_PROTOCOL = "SSH";

    // account protocol telnet协议
    public static final String ACCOUNT_TELNET_PROTOCOL = "TELNET";
    
    //confagent程序 端口
    public static final String CONFAGENT_PORT="confagent_port";
    //confagent项目名称
    public static final String CONFAGENT_PATH="confagent_path";
    
    //最大窗口允许的连接数
    public static final String MAX_TERMINAL_NUM = "max_terminal_num";

    // 开关开启
    public static final Integer SWITCH_OPEN = 1;

    // 开关关闭
    public static final Integer SWITCH_CLOSE = 0;

}
