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
import java.util.regex.Pattern;

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
     * prometheus server up 也就是endpoint状态 'endpoint'键值对应endpointId
     */
    public static final String PROMSERVERUP_ENDPOINT_LABEL = "endpoint";

    /**
     * redis缓存区域配置信息key值
     */
    public static final String REDIS_KEY_AREA="area";
    
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

    // snmp set value 可选类型集合
    public static final List<String> SNMP_SETVLAUE_TYPE;

    // idc trafficSetting direction 流量方向 可选值 rx(接收） tx（发送）
    public static final List<String> TRAFFIC_DIRECTIONS;

    // 图表 singleStat statistics可选值列表
    public static final List<String> CHART_SINGLESTAT_STATISTICS;

    // panel type可选值集合
    public static final List<String> PANEL_TYPES;

    // alert rule 比较符号
    public static final List<String> RULE_OPERATORS;

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

        SYSCONFIG_TYPEWITHKEYS_MAP.put("basic", Arrays.asList(new String[]{"alert_api", "asset_ping_switch", "asset_ping_interval", "node_exporter_target_path", "scrape_interval", "storage_local_retention","system_name", "timezone"}));
        SYSCONFIG_TYPEWITHKEYS_MAP.put("email", Arrays.asList(new String[]{"email_enable", "email_smtp_host", "email_smtp_port", "email_smtp_account", "email_smtp_password", "email_send_account", "email_test_reciver", "email_ssl_flag", "email_tls_flag", "test"}));
        SYSCONFIG_TYPEWITHKEYS_MAP.put("terminal", Arrays.asList(new String[]{"max_terminal_num"}));
        SYSCONFIG_TYPEWITHKEYS_MAP.put("ldap", Arrays.asList(new String[]{"ldap_address", "ldap_dn", "ldap_password", "ldap_ou", "ldap_user_filter", "ldap_mapping", "ldap_enable", "test"}));

        SNMP_SETVLAUE_TYPE = new ArrayList<>();
        SNMP_SETVLAUE_TYPE.add("OctetString");
        SNMP_SETVLAUE_TYPE.add("Integer");
        SNMP_SETVLAUE_TYPE.add("OID");
        SNMP_SETVLAUE_TYPE.add("Counter32");
        SNMP_SETVLAUE_TYPE.add("IpAddress");
        SNMP_SETVLAUE_TYPE.add("TimeTicks");
        SNMP_SETVLAUE_TYPE.add("UnsignedInteger");
        SNMP_SETVLAUE_TYPE.add("BITS");
        SNMP_SETVLAUE_TYPE.add("Float");
        SNMP_SETVLAUE_TYPE.add("DateAndTime");

        TRAFFIC_DIRECTIONS = new ArrayList<>();
        TRAFFIC_DIRECTIONS.add("rx");
        TRAFFIC_DIRECTIONS.add("tx");

        CHART_SINGLESTAT_STATISTICS = new ArrayList<>();
        CHART_SINGLESTAT_STATISTICS.add("min");
        CHART_SINGLESTAT_STATISTICS.add("max");
        CHART_SINGLESTAT_STATISTICS.add("average");
        CHART_SINGLESTAT_STATISTICS.add("total");
        CHART_SINGLESTAT_STATISTICS.add("first");
        CHART_SINGLESTAT_STATISTICS.add("last");
        CHART_SINGLESTAT_STATISTICS.add("range");
        CHART_SINGLESTAT_STATISTICS.add("different");

        PANEL_TYPES = Arrays.asList(new String[]{"dashboard", "asset", "project", "module", "endpoint", "model"});

        RULE_OPERATORS = Arrays.asList(new String[]{"==", "!=", ">", "<", ">=", "<="});
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


    public static final Integer SNMPV2 = 2;
    public static final Integer SNMPV3 = 3;
    // snmp默认团体名
    public static final String SNMP_DEFAULT_COMMUNITY = "public";

    public enum SnmpOperationType{
        GET("get"),
        WALK("walk"),
        GETNEXT("getnext"),
        SET("set");

        private String value;
        SnmpOperationType(String value){
            this.value=value;
        }

        public static String getOperationType(String value){
            for (SnmpOperationType type:SnmpOperationType.values()){
                if(type.getValue().equals(value)){
                    return type.getValue();
                }
            }
            return null;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
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

    // 二级域名
    public static final String  REGEX_DOMAIN = "(\\w*\\.?){2}\\.(com.cn|net.cn|gov.cn|org\\.nz|org.cn|com|net|org|gov|cc|biz|info|cn|co)$";


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

    // 主机连通性检查开关  打开状态 on
    public static final String ASSET_PING_OPEN_SWITCH = "on";

    // asset IP 不可达时返回表述信息
    public static final String ASSET_PING_DOWN_FLAG = "unreachable";

    // asset fping 命令语句 key
    public static final String SYSCONFIG_KEY_ASSET_FPING_COMMAND = "asset_ping_command";

    // 内置 generator 路径
    public static final String SYSCONFIG_KEY_GENERATOR_PATH = "generator_path";

    // 机柜默认大小
    public static final String SYSCONFIG_KEY_DEFAULT_CABINETU = "default_cabinet_uszie";

    // 测试邮件发送模板
    public static final String SYSCONFIG_KEY_TESTEMAIL_SEND_TEMPLATE = "test_email_send_template";

    // 新增model 添加chart配置
    public static final String SYSCONFIG_KEY_MODEL_CHART_TPL = "model_chart_tpl";

    // 新增project 添加chart配置
    public static final String SYSCONFIG_KEY_PROJECT_CHART_TPL = "project_chart_tpl";

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

    // 开关开启状态
    public static final String SWITCH_OPEN = "on";

    // 开关关闭状态
    public static final String SWITCH_CLOSE = "off";

    // 资产服务器类型
    public static final String ASSET_SERVER_TYPE = "server";

    // 资产excel表头
    public static final String[] ASSET_EXCEL_IMPORT_HEADER = {"Sn", "Type", "Vendor", "Model", "Idc", "Cabinet", "CabinetStart", "CabinetEnd", "Host", "State", "PurchaseDate", "Protocol", "AuthType", "User", "Pwd", "Cert", "Port", "UserTip", "PasswordTip", "ReloginPasswordTip", "Exporter","IpmiHost","IpmiPort"};
    public static final String[] ASSET_EXCEL_EXPORT_HEADER = {"Sn", "Type", "Vendor", "Model", "Idc", "Cabinet", "CabinetStart", "CabinetEnd", "Host", "State", "PurchaseDate", "Protocol", "AuthType", "User", "Pwd", "Cert", "Port", "UserTip", "PasswordTip", "ReloginPasswordTip","IpmiHost","IpmiPort"};

    // 连接smtp邮箱服务器超时时间 10秒
    public static final String CONNECTION_SMTP_TIMEOUT = "10000";

    /**
     * MIB四种数据类型
     * IDENTIFIER 、OBJECT、TABLE、ENTRY
     */
    public static final String MIB_IDENTIFIER_TYPE = "IDENTIFIER";
    public static final String MIB_OBJECT_TYPE = "OBJECT";
    public static final String MIB_TABLE_TYPE = "TABLE";
    public static final String MIB_ENTRY_TYPE = "ENTRY";
    // mib定义中 entry采用的格式为sequence 用以区分type 为 entry
    public static final String MIB_ENTRY_SYNTAX = "SEQUENCE";

    // snmp browser 中返回oid 与 mib文件中name对应，当前截取位置为  倒数1位
    public static final int OIDNAME_TRUNCATION_INDEX = 1;

    // 图表 url类型
    public static final String CHART_URL_TYPE = "url";

    // 图表 singleStat 类型
    public static final String CHART_SINGLESTAT_TYPE = "singleStat";

    // 图表singleStat 类型 paramkey名称
    public static final String CHART_SINGLESTAT_PARAMkEY = "statistics";

    // IDC traffic中tags key正则校验表达式
    public static final Pattern TRAFFIC_TAGSKEY_PATTERN = Pattern.compile("^[a-zA-Z_:][a-zA-Z0-9_:]*$");

    // node_exporter shell 脚本中 替换路径的名称
    public static final String NODEEXPORTER_SHELLREPLACE_PATH = "target_path";

    // 测试邮件发送标题
    public static final String TESTEMAIL_SEND_TITLE = "Test";
    
    // reset操作处理的表信息数据
    public static final String RESET_TABLE_INFOS = "reset_table_infos";
    
    // 图表类型为line bar stackArea
    public static final String CHART_DEFAULT_PARAMkEY = "threshold";
 
    // 图表类型为alertList
    public static final String CHART_ALERTLIST_TYPE = "alertList";
}
