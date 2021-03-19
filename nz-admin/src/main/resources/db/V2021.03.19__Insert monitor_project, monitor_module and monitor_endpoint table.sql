
/**
 * 1. insert monitor_project monitor_module monitor_endpoint tables
 * 2. update common role remark content
 * 3. sys_config 表新增 monitor_exdpoint 导入导出表头
 */
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for monitor_project
-- ----------------------------
DROP TABLE IF EXISTS `monitor_project`;
CREATE TABLE `monitor_project`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '应用名称',
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述信息',
  `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '是否内置	1:内置，0：非内置，默认：0',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for monitor_module
-- ----------------------------
DROP TABLE IF EXISTS `monitor_module`;
CREATE TABLE `monitor_module`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '组件名称	唯一索引，表内不重复',
  `project_id` int(10) NOT NULL,
  `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '可选值http  snmp',
  `endpoint_name_tmpl` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'endpoint name模板 支持 {{}} 变量替换，asset，module\r\n\r\n例：{{asset.name}}-{{module.name}}\r\n\r\n最终endpoint的name为： 192.168.40.1-mysql',
  `port` int(10) NOT NULL COMMENT '默认端口 type=snmp时，默认 161',
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `configs` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置参数	',
  `snmp_yml` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT 'snmp_exporter需要的配置部分',
  `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for monitor_endpoint
-- ----------------------------
DROP TABLE IF EXISTS `monitor_endpoint`;
CREATE TABLE `monitor_endpoint`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `module_id` int(10) NOT NULL,
  `asset_id` int(10) NOT NULL,
  `host` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'exporter地址，默认：asset.host',
  `port` int(10) NULL DEFAULT NULL COMMENT 'export端口，默认为 module.port',
  `configs` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '计算 moduleId + assetId + host + port + configs 的hash值，用于确保相同配置不重复',
  `enabled` int(4) NOT NULL COMMENT '是否启用	 可选值\r\n\r\n0：停用\r\n\r\n1：启用',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;


update sys_role set remark = 'The system built-in common user role' where name = 'common';

INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'monitor_endpoint_export_header', '{\r\n	\"cn\": {\r\n		\"*系统\": \"系统名称(必填) \\n示例: Nezha\",\r\n		\"*组件\": \"组件名称(必填) \\n示例: node_exporter\",\r\n		\"名称\": \"实例名称 \\n示例: node_exporter\",\r\n		\"资产名称\": \"资产名称(非必填) 与 主机地址 不能同时为空 \\n示例: A0420200101\",\r\n		\"端口号\": \"端口号 范围0-65535 \\n示例: 9100\",\r\n		\"配置\": \"实例配置，JSON格式， \\n示例: {\\\"key\\\":\\\"value\\\"}\",\r\n		\"主机地址\": \"主机地址（非必填） 与 资产名称 不能同时为空 \\n示例: 127.0.0.1\",\r\n		\"启用\": \"启用状态（非必填） 可选 0:停用 1:启用,为空时默认为1 \\n示例: 1\"\r\n	},\r\n	\"en\": {\r\n		\"*Project Name\": \"Project Name(Required) \\nExample: Nezha\",\r\n		\"*Module Name\": \"Module Name(Required) \\nExample: node_exporter\",\r\n		\"Name\": \"Endpoint name \\nExample: node_exporter\",\r\n		\"Asset Name\": \"Asset name(Not required) And host address cannot be empty at the same time \\n示例: A0420200101\",\r\n		\"Port\": \"Port number range 0-65535 \\nExample: 9100\",\r\n		\"Configs\": \"Endpoint Configs，JSON format， \\nExample: {\\\"key\\\":\\\"value\\\"}\",\r\n		\"Host\": \"Host address (not required) and asset name cannot be empty at the same time \\nExample: 127.0.0.1\",\r\n		\"Enabled\": \"Enable state (not required) Optional values 0: Disable 1: Enable, when empty, the default is 1 \\nExample: 1\"\r\n	}\r\n}', 1, 'monitor endpoint 导入表头');
