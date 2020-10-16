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
     * 排序方式
     */
    public static final String ORDER = "orderBy";
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
     * 记录endpoint和proserver 对应关系
     */
    public static final String REDIS_KEY_ENDPOINT_PROM = "endpoint_prom";

    /**
     * 告警消息邮件通知
     */
    public static final String REDIS_KEY_EMAIL_ALERT_MESSAGE= "alert_message_list";

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

    // module http 类型默认 path
    public static final String MODULE_HTTP_DEFAULT_PATH = "/metrics";

    public static final Integer SNMP_DEFAULT_PORT = 161;

    // snmp set value 可选类型集合
    public static final List<String> SNMP_SETVLAUE_TYPE;

    // idc trafficSetting direction 流量方向 可选值 rx(接收） tx（发送）
    public static final List<String> TRAFFIC_DIRECTIONS;

    // 图表 singleStat statistics可选值列表
    public static final List<String> CHART_SINGLESTAT_STATISTICS;

    // alert rule 比较符号
    public static final List<String> RULE_OPERATORS;

    // snmp 采集指标相关 oid 信息
    public static final Map<String,String> SNMP_OID_MAP ;

    public static final Map<String,String> SNMP_IFENTRY_MAP;

    // ssh 采集相关服务器信息指令
    public static final Map<String,String> SSH_SHELL_MAP;

    // alert rule unit
    public static final Map<String,Integer> ALERT_RULE_UNIT_MAP;

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
		
		ALERTMESSAGE_STATE = new ArrayList<Integer>();
		ALERTMESSAGE_STATE.add(1);
		ALERTMESSAGE_STATE.add(2);

        SPECIAL_SYS_DICT = new ArrayList<String>();
        SPECIAL_SYS_DICT.add(Constant.DICT_ASSET_MODEL_TYPE);
        SPECIAL_SYS_DICT.add(Constant.DICT_ASSET_MODEL_VENDOR);

        SYSCONFIG_TYPEWITHKEYS_MAP = new HashMap<>();

        SYSCONFIG_TYPEWITHKEYS_MAP.put("basic", Arrays.asList(new String[]{"alert_api", "asset_ping_switch", "asset_ping_interval", "node_exporter_target_path", "scrape_interval", "storage_local_retention","system_name", "timezone", "default_cabinet_usize"}));
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

        RULE_OPERATORS = Arrays.asList(new String[]{"==", "!=", ">", "<", ">=", "<="});


        SNMP_OID_MAP = new HashMap<>();
        SNMP_OID_MAP.put("sysName", "1.3.6.1.2.1.1.5");
        SNMP_OID_MAP.put("sysLocation", "1.3.6.1.2.1.1.6");
        SNMP_OID_MAP.put("sysContact", "1.3.6.1.2.1.1.4");
        SNMP_OID_MAP.put("sysServices", "1.3.6.1.2.1.1.7");
        SNMP_OID_MAP.put("sysUpTime", "1.3.6.1.2.1.1.3");
        SNMP_OID_MAP.put("ifNumber", "1.3.6.1.2.1.2.1");
        SNMP_OID_MAP.put("ifEntry", "1.3.6.1.2.1.2.2.1");


        SNMP_IFENTRY_MAP = new HashMap<>();
        SNMP_IFENTRY_MAP.put("1.3.6.1.2.1.2.2.1.1", "ifIndex");
        SNMP_IFENTRY_MAP.put("1.3.6.1.2.1.2.2.1.2", "ifDescr");
        SNMP_IFENTRY_MAP.put("1.3.6.1.2.1.2.2.1.3", "ifType");
        SNMP_IFENTRY_MAP.put("1.3.6.1.2.1.2.2.1.4", "ifMtu");
        SNMP_IFENTRY_MAP.put("1.3.6.1.2.1.2.2.1.5", "ifSpeed");
        SNMP_IFENTRY_MAP.put("1.3.6.1.2.1.2.2.1.6", "ifPhysAddress");
        SNMP_IFENTRY_MAP.put("1.3.6.1.2.1.2.2.1.7", "ifAdminStatus");
        SNMP_IFENTRY_MAP.put("1.3.6.1.2.1.2.2.1.8", "ifOperStatus");
        SNMP_IFENTRY_MAP.put("1.3.6.1.2.1.2.2.1.9", "ifLastChange");

        SSH_SHELL_MAP = new HashMap<>();
        SSH_SHELL_MAP.put("hostname", "hostname");
        SSH_SHELL_MAP.put("cpu", "cat /proc/cpuinfo| grep \"physical id\"| sort| uniq| wc -l;echo `cat /proc/cpuinfo|grep \"model name\" | uniq -d|awk -F:  '{print $2}'` \\* `cat /proc/cpuinfo| grep \"processor\"| wc -l`");
        SSH_SHELL_MAP.put("memory", "free -m  | grep Mem | awk -F ' ' '{printf (\"%.3f\\n\",$2/1000)}'");
        SSH_SHELL_MAP.put("disk", "lsblk|grep disk|awk '{print $1,$4}';cat /proc/scsi/scsi|grep Vendor|awk '{print $2,$4}'|grep -v DVD");
        SSH_SHELL_MAP.put("core", "cat /proc/version");
        //   		put("os","cat /etc/redhat-release");
        SSH_SHELL_MAP.put("uptime", "uptime -s");


        ALERT_RULE_UNIT_MAP = new HashMap<>();
        ALERT_RULE_UNIT_MAP.put("none", 1);
        ALERT_RULE_UNIT_MAP.put("short", 2);
        ALERT_RULE_UNIT_MAP.put("percent(0-100)", 3);
        ALERT_RULE_UNIT_MAP.put("percent(0.0-1.0)", 4);
        ALERT_RULE_UNIT_MAP.put("local format", 5);
        ALERT_RULE_UNIT_MAP.put("bits", 6);
        ALERT_RULE_UNIT_MAP.put("bytes", 7);
        ALERT_RULE_UNIT_MAP.put("kilobytes", 8);
        ALERT_RULE_UNIT_MAP.put("megabytes", 9);
        ALERT_RULE_UNIT_MAP.put("gigabytes", 10);
        ALERT_RULE_UNIT_MAP.put("terabytes", 11);
        ALERT_RULE_UNIT_MAP.put("petabytes", 12);
        ALERT_RULE_UNIT_MAP.put("packets/sec", 13);
        ALERT_RULE_UNIT_MAP.put("bits/sec", 14);
        ALERT_RULE_UNIT_MAP.put("bytes/sec", 15);
        ALERT_RULE_UNIT_MAP.put("kilobytes/sec", 16);
        ALERT_RULE_UNIT_MAP.put("kilobits/sec", 17);
        ALERT_RULE_UNIT_MAP.put("megabytes/sec", 18);
        ALERT_RULE_UNIT_MAP.put("megabits/sec", 19);
        ALERT_RULE_UNIT_MAP.put("gigabytes/sec", 20);
        ALERT_RULE_UNIT_MAP.put("gigabits/sec", 21);
        ALERT_RULE_UNIT_MAP.put("terabytes/sec", 22);
        ALERT_RULE_UNIT_MAP.put("terabits/sec", 23);
        ALERT_RULE_UNIT_MAP.put("petabytes/sec", 24);
        ALERT_RULE_UNIT_MAP.put("petabits/sec", 25);
        ALERT_RULE_UNIT_MAP.put("Hertz(1/s)", 26);
        ALERT_RULE_UNIT_MAP.put("nanoseconds(ns)", 27);
        ALERT_RULE_UNIT_MAP.put("nanoseconds", 27);
        ALERT_RULE_UNIT_MAP.put("ns", 27);
        ALERT_RULE_UNIT_MAP.put("microseconds(us)", 28);
        ALERT_RULE_UNIT_MAP.put("microseconds", 28);
        ALERT_RULE_UNIT_MAP.put("us", 28);
        ALERT_RULE_UNIT_MAP.put("milliseconds(ms)", 29);
        ALERT_RULE_UNIT_MAP.put("milliseconds", 29);
        ALERT_RULE_UNIT_MAP.put("ms", 29);
        ALERT_RULE_UNIT_MAP.put("seconds(s)", 30);
        ALERT_RULE_UNIT_MAP.put("seconds", 30);
        ALERT_RULE_UNIT_MAP.put("s", 30);
        ALERT_RULE_UNIT_MAP.put("minutes(m)", 31);
        ALERT_RULE_UNIT_MAP.put("minutes", 31);
        ALERT_RULE_UNIT_MAP.put("m", 31);
        ALERT_RULE_UNIT_MAP.put("hours(h)", 32);
        ALERT_RULE_UNIT_MAP.put("hours", 32);
        ALERT_RULE_UNIT_MAP.put("h", 32);
        ALERT_RULE_UNIT_MAP.put("days(d)", 33);
        ALERT_RULE_UNIT_MAP.put("days", 33);
        ALERT_RULE_UNIT_MAP.put("d", 33);
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

    public static final Long GLOBAL_ID = 1L;
    
    //delFlag
    public static final String DELFLAG_TRUE = "1"; //删除
    public static final String DELFLAG_FALSE = "0"; //未删除

    public static final Integer AUTH_PASSWORD=1;//密码认证
    public static final Integer AUTH_CERTIFICATE=2;//私钥证书认证

    public static final String AUTH_TOKEN_CODE="Authorization";

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

    public enum PanelType{
        DASHBOARD("dashboard"),
        ASSET("asset"),
        PROJECT("project"),
        MODULE("module"),
        ENDPOINT("endpoint"),
        MODEL("model");

        private String value;

        PanelType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }


    public enum PromserverType{
        GLOBAL("global",1),
        PER_DATACENTER("per_datacenter",2),
        THANOS_QUERY("thanos_query",3),
        THANOS_RULE("thanos_rule",4),
        THANOS_SIDECAR("thanos_sidecar",5),
        THANOS_STORE("thanos_store",6),
        THANOS_COMPACTOR("thanos_compactor",7);

        private String name;
        private Integer value;

        PromserverType(String name,Integer value){
            this.value=value;
            this.name=name;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public static PromserverType getPromserverType(Integer value){
            for (PromserverType  server:PromserverType.values()){
                if (server.getValue().equals(value)){
                    return server;
                }
            }
            return null;
        }
    }

    // system built_in 2非内置，1内置
    public static final String SYSTEM_BUILDIN_YES = "1"; // 内置
    public static final String SYSTEM_BUILDIN_NO = "2"; // 非内置

    // nz-web 新增常量

    // 字典表中资产型号厂商type值
    public static final String DICT_ASSET_MODEL_VENDOR = "vendor";

    // 字典表中资产型号类型type值
    public static final String DICT_ASSET_MODEL_TYPE  = "assetType";

    // 字典表中资产状态 type 值
    public static final String DICT_ASSET_STATE_TYPE = "assetState";

    // 字典表中 account 认证类型的 type 值
    public static final String DICT_ACCOUNT_AUTH_TYPE = "authType";

    // 字典表中 chart 类型的 type 值
    public static final String DICT_CHART_TYPE = "chartType";

    // 字典表中 chart element 表达式类型的 type 值
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
    public static final String SYSCONFIG_KEY_DEFAULT_CABINETU = "default_cabinet_usize";

    // 测试邮件发送模板
    public static final String SYSCONFIG_KEY_TESTEMAIL_SEND_TEMPLATE = "test_email_send_template";

    // 新增model 添加chart配置
    public static final String SYSCONFIG_KEY_MODEL_CHART_TPL = "model_chart_tpl";

    // 新增project 添加chart配置
    public static final String SYSCONFIG_KEY_PROJECT_CHART_TPL = "project_chart_tpl";

    // asset 导入表头
    public static final String SYSCONFIG_KEY_ASSET_EXPORT_HEADER = "asset_export_header";

    // alert rule 导入表头
    public static final String SYSCONFIG_KEY_ALERTRULE_EXPORT_HEADER = "rule_export_header";

    // chart dashboard 类型导入表头
    public static final String SYSCONFIG_KEY_CHART_EXPORT_HEADER = "chart_export_header";

    // model 类型导入表头
    public static final String SYSCONFIG_KEY_MODEL_CHART_EXPORT_HEADER = "model_chart_export_header";

    // asset 类型导入表头
    public static final String SYSCONFIG_KEY_ASSET_CHART_EXPORT_HEADER = "asset_chart_export_header";

    // endpoint 导入表头
    public static final String SYSCONFIG_KEY_ENDPOINT_EXPORT_HEADER = "endpoint_export_header";

    // asset ping 命令中文件名称 会替换成文件的路径
    public static final String ASSET_PING_COMMAND_FILENAME = "{filename}";

    // 告警级别 高
    public static final String ALERT_SEVERITY_HIGH = "P1";

    // 告警级别 中
    public static final String ALERT_SEVERITY_MEDIUM = "P2";

    // 告警级别 低
    public static final String ALERT_SEVERITY_LOW = "P3";

    // account protocol ssh协议
    public static final String ACCOUNT_SSH_PROTOCOL = "SSH";

    // account protocol telnet协议
    public static final String ACCOUNT_TELNET_PROTOCOL = "TELNET";

    // account protocol snmp协议
    public static final String ACCOUNT_SNMP_PROTOCOL = "SNMP";

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

    // panel chart link 链表结构 首位 prev 值
    public static final Integer START_PREV_VALUE = 0;

    // panel chart link 链表结构 末尾 next 值
    public static final Integer END_NEXT_VALUE = -1;

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

    // IDC traffic中tags key正则校验表达式     注：同 prometheus 标签正则
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

    public static final String CHART_ELEMENT_EXPERT_TYPE = "expert";

    // 国际化对应
    public static final String I18N_MAPPING = "i18n_mapping";
}
