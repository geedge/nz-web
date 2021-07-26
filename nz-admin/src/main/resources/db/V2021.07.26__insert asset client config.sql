/**
 *  1.asset 导入导出表头更新
 *  2.asset_type_conf 增加 client_enable column
 *  3.asset_asset 增加 client_ 信息
 *  4.新增 asset_talon_status 表
 */

DELETE from `sys_config` where param_key = "asset_asset_export_header";
INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'asset_asset_export_header', '{\"cn\":{\"*资产名称\":\"资产名称，唯一标识(必填) \\n示例: DELL服务器\",\"*SN码\":\"资产序列号，唯一标识(必填) \\n示例: A0420200101\",\"虚拟机宿主\":\"虚拟机宿主，当资产为虚拟机时必填 \\n示例: DELL物理机\",\"*管理ip\":\"主机地址(必填),IP格式 \\n示例: 172.0.0.1\",\"*品牌\":\"品牌(必填) \\n示例: DELL\",\"*型号\":\"型号(必填) \\n示例: DELL-D3\",\"*数据中心\":\"数据中心(必填) \\n示例: 北京电信中心机房\",\"机柜\":\"机柜 \\n示例: cabinet-001\",\"开始U位\":\"机柜开始位置 此信息一般和机柜结束位置联合使用 \\n示例: 1\",\"结束U位\":\"机柜结束位置 此信息一般与机柜开始位置联合使用 \\n示例: 2\",\"*资产状态\":\"资产状态 \\n示例: 在库\",\"*资产类型\":\"资产类型 \\n示例: 服务器\",\"购买日期\":\"购买日期 \\n示例: 2020/2/25\",\"属性信息\":\"资产属性信息,格式: key=value 如需填写多个请使用回车换行 \\n示例: NetworkCard=eth0 \\n user=root\",\"认证服务端口\":\"认证服务端口 \\n示例: 22\",\"认证方式\":\"认证方式,可选值: 1:用户名密码; 2: 密钥 \\n示例: 1\",\"用户名\":\"用户名, \\n示例:root\",\"密码\":\"密码 ,认证方式为1，记录登录密码; 认证方式为2,记录密钥密码.  \\n示例: pass\",\"密钥\":\"登录密钥 \\n示例: ssh-rsa CFdv7Jez6K...\",\"用户名提示\":\"TELNET用户名提示 \\n示例: login:\",\"密码提示\":\"TELNET密码提示 \\n示例: pass:\",\"SNMP凭证\":\"snmp凭证名称 \\n实例: system-snmpv2\",\"Talon用户名\":\"Talon用户名 为空时默认：nezha \\n示例: admin\",\"Talon密码\":\"Talon密码 为空时默认：nezha \\n示例: yourpass\",\"Talon管理ip\":\"Talon管理ip 为空时默认：\\\"{{asset.manageIp}}\\\" \\n示例: 127.0.0.1\",\"Talon服务端口\":\"Talon服务端口 为空时默认：10092 \\n示例: 10092\"},\"en\":{\"*Asset Name\":\"Asset name, unique identifier (required) \\nExample: DELL server\",\"*Sn\":\"Asset serial number, unique identifier (required) \\nExample: A0420200101\",\"*Virtual machine host\":\"Virtual machine host, required when the asset is a virtual machine \\nExample: DELL physical machine\",\"*Management ip\":\"Host address (required), IP format \\nExample: 172.0.0.1\",\"*Brand\":\"Brand (required) \\nExample: DELL\",\"*Model\":\"Model (required) \\nExample: DELL-D3\",\"*DC\":\"Data Center (Required) \\nExample: Beijing Telecom Center Computer Room\",\"Cabinet\":\"Cabinet \\nExample: cabinet-001\",\"Cabinet Start\":\"Cabinet start position This information is generally used in conjunction with the cabinet end position \\nExample: 1\",\"Cabinet End\":\"Cabinet end position This information is generally used in conjunction with the cabinet start position \\nExample: 2\",\"*Asset state\":\"Asset status \\nExample: In stock\",\"*Asset type\":\"Asset Type\\nExample: Server\",\"Purchase date\":\"Purchase date \\nExample: 2020/2/25\",\"Field Information\":\"Asset field information, format: key=value If you need to fill in more than one, please use carriage return and line feed \\nExample: NetworkCard=eth0 \\n user=root\",\"Server port\":\"Server port \\nExample: 22\",\"Auth methed\":\"Auth methed,Optional values: 1: username and password; 2: key \\nExample: 1\",\"User name\":\"User name, \\nExample:root\",\"Password\":\"Password ,The auth method is 1, record the login password; the auth method is 2, record the key password.  \\nExample: pass\",\"PrivateKey\":\"PrivateKey \\nExample: ssh-rsa CFdv7Jez6K...\",\"User Tip\":\"TELNET username prompt \\nExample: login:\",\"Password Tip\":\"TELNET password prompt \\nExample: pass:\",\"SNMP credential\":\"SNMP credential name \\nExample: system-snmpv2\",\"Talon username\":\"Default when Talon username is empty: nezha \\nExample: nezha\",\"Talon password\":\"Default when Talon password is empty: nezha \\nExample: yourpass\",\"Talon management ip\":\"Default when Talon management ip is empty: \\\"{{asset.manageIp}}\\\" \\nExample: 127.0.0.1\",\"Talon service port\":\"When the Talon service port is empty, the default: 10092 \\nExample: 10092\"}}', 1, 'asset 导入导出表头信息');

ALTER TABLE  `asset_type_conf` ADD COLUMN  `client_enable` int(1) NOT NULL DEFAULT 0 COMMENT '默认：0  0：不开启  1：开启';

ALTER TABLE  `asset_asset`
ADD COLUMN  `client_host` varchar(64) NOT NULL DEFAULT '' COMMENT '默认：“{{asset.manageIp}}”',
ADD COLUMN  `client_port` int(5) NOT NULL DEFAULT 10092 COMMENT '默认：10092',
ADD COLUMN  `client_user` varchar(64) NOT NULL DEFAULT 'nezha' COMMENT '默认：nezha',
ADD COLUMN  `client_pin` varchar(64) NOT NULL DEFAULT 'nezha' COMMENT '默认：nezha';


-- ----------------------------
-- Table structure for asset_talon_status
-- ----------------------------
DROP TABLE IF EXISTS `asset_talon_status`;
CREATE TABLE `asset_talon_status`  (
  `asset_id` int(10) NOT NULL,
  `status` int(10) NOT NULL COMMENT '1: OK\r\n2: 连接超时\r\n3：认证失败\r\n4：未知错误',
  `msg` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '默认：“”\r\n\r\n记录 错误描述信息',
  `last_update` datetime(0) NULL DEFAULT NULL COMMENT '最后更新时间',
  PRIMARY KEY (`asset_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;
