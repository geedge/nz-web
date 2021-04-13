ALTER TABLE `snmp_credential` 
ADD COLUMN `port` int(1) NOT NULL DEFAULT 161 COMMENT '端口号 默认161' AFTER `config`;


ALTER TABLE `asset_asset` 
DROP COLUMN `ssh_port`,
DROP COLUMN `telnet_port`,
ADD COLUMN `auth_protocol_port` int(10) NULL COMMENT '认证服务端口',
ADD COLUMN `auth_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '认证方式1：用户名密码\r\n\r\n2：密钥',
ADD COLUMN `auth_username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '用户名',
ADD COLUMN `auth_pin` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '密码auth_type =1 : 记录登录密码\r\n\r\nauth_type=2 : 记录密钥密码',
ADD COLUMN `auth_pri_key` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '秘钥',
ADD COLUMN `auth_user_tip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '用户名提示 telnet协议有效',
ADD COLUMN `auth_pin_tip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '密码提示 telnet协议有效',
ADD COLUMN `snmp_credential_id` int(10) NULL COMMENT 'snmp凭证id type 开启snmp有效';


ALTER TABLE `asset_type_conf` 
DROP COLUMN `ssh`,
DROP COLUMN `telnet`,
ADD COLUMN `auth_protocol` int(1) NOT NULL DEFAULT 0 COMMENT '认证协议 默认：0 \r\n\r\n0：无\r\n\r\n1：ssh\r\n\r\n2：telnet',
ADD COLUMN `snmp_enable` int(1) NOT NULL DEFAULT 0 COMMENT '是否支持snmp 默认：0 \r\n\r\n0：不支持\r\n\r\n1：支持',
ADD COLUMN `snmp_collect` int(1) NOT NULL DEFAULT 0 COMMENT '是否开启snmp采集 默认：0 \r\n\r\n0：不开启\r\n\r\n1：开启\r\n\r\n当snmp_enable=1时有效',
ADD COLUMN `ssh_collect` int(1) NOT NULL COMMENT '是否开启ssh采集 默认：0 \r\n\r\n0：不开启\r\n\r\n1：开启\r\n\r\n当 auth_protocol=1时有效',
ADD COLUMN `ssh_collect_script` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT 

delete from sys_config where param_key='asset_asset_export_header';
INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (189, 'asset_asset_export_header', '{\r\n	\"cn\": {\r\n		\"*资产名称\": \"资产名称，唯一标识(必填) \\n示例: DELL服务器\",\r\n		\"*SN码\": \"资产序列号，唯一标识(必填) \\n示例: A0420200101\",\r\n		\"虚拟机宿主\": \"虚拟机宿主，当资产为虚拟机时必填 \\n示例: DELL物理机\",\r\n		\"*管理ip\": \"主机地址(必填),IP格式 \\n示例: 172.0.0.1\",\r\n		\"*品牌\": \"品牌(必填) \\n示例: DELL\",\r\n		\"*型号\": \"型号(必填) \\n示例: DELL-D3\",\r\n		\"*数据中心\": \"数据中心(必填) \\n示例: 北京电信中心机房\",\r\n		\"机柜\": \"机柜 \\n示例: cabinet-001\",\r\n		\"开始U位\": \"机柜开始位置 此信息一般和机柜结束位置联合使用 \\n示例: 1\",\r\n		\"结束U位\": \"机柜结束位置 此信息一般与机柜开始位置联合使用 \\n示例: 2\",\r\n		\"*资产状态\": \"资产状态 \\n示例: 在库\",\r\n		\"*资产类型\": \"资产类型 \\n示例: 服务器\",\r\n		\"购买日期\": \"购买日期 \\n示例: 2020/2/25\",\r\n		\"属性信息\": \"资产属性信息,格式: key=value 如需填写多个请使用回车换行 \\n示例: NetworkCard=eth0 \\n user=root\",\r\n		\"认证服务端口\": \"ssh/telnet端口 \\n示例: 22/23\",\r\n		\"认证方式\":\"用户名密码/密钥 \\n示例：111111\",\r\n		\"用户名\":\"用户名 \\n示例:root\",\r\n		\"密码\":\"auth_type =1 : 记录登录密码 auth_type=2 : 记录密钥密码\",\r\n		\"密钥\":\"密钥内容\",\r\n		\"用户名提示\":\"telnet协议有效\",\r\n		\"密码提示\":\"telnet协议有效\",\r\n		\"snmp凭证\":\"type 开启snmp有效\"\r\n	},\r\n	\"en\": {\r\n		\"*Asset Name\": \"Asset name, unique identifier (required) \\nExample: DELL server\",\r\n		\"*Sn\": \"Asset serial number, unique identifier (required) \\nExample: A0420200101\",\r\n		\"*Virtual machine host\": \"Virtual machine host, required when the asset is a virtual machine \\nExample: DELL physical machine\",\r\n		\"*Management ip\": \"Host address (required), IP format \\nExample: 172.0.0.1\",\r\n		\"*Brand\": \"Brand (required) \\nExample: DELL\",\r\n		\"*Model\": \"Model (required) \\nExample: DELL-D3\",\r\n		\"*DC\": \"Data Center (Required) \\nExample: Beijing Telecom Center Computer Room\",\r\n		\"Cabinet\": \"Cabinet \\nExample: cabinet-001\",\r\n		\"Cabinet Start\": \"Cabinet start position This information is generally used in conjunction with the cabinet end position \\nExample: 1\",\r\n		\"Cabinet End\": \"Cabinet end position This information is generally used in conjunction with the cabinet start position \\nExample: 2\",\r\n		\"*Asset state\": \"Asset status \\nExample: In stock\",\r\n		\"*Asset type\": \"Asset Type\\nExample: Server\",\r\n		\"Purchase date\": \"Purchase date \\nExample: 2020/2/25\",\r\n		\"Field Information\": \"Asset field information, format: key=value If you need to fill in more than one, please use carriage return and line feed \\nExample: NetworkCard=eth0 \\n user=root\",\r\n		\"Auth protocol Port\": \"ssh/telnet \\nExample: 22/23\",\r\n		\"Auth Type\":\"User name password / pri key \\nExample: 111111\",\r\n		\"Auth Username\":\"username \\nExample: root\",\r\n		\"Auth Pin\":\"auth_type =1 : pin value  auth_type=2 : pri key pin\",\r\n		\"Auth Pri Key\":\"pri key content\",\r\n		\"Auth User Tip\":\"telnet protocol valid\",\r\n		\"Auth Pin Tip\":\"telnet protocol valid\",\r\n		\"Snmp Credential\":\"snmp protocol valid\"\r\n	}\r\n}', 1, NULL);
