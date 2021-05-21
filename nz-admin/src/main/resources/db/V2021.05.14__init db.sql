/*
 Navicat Premium Data Transfer

 Source Server         : 192.168.40.42
 Source Server Type    : MySQL
 Source Server Version : 100214
 Source Host           : 192.168.40.42:3306
 Source Schema         : nz-v2.0

 Target Server Type    : MySQL
 Target Server Version : 100214
 File Encoding         : 65001

 Date: 21/05/2021 21:50:17
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for alert_message_active
-- ----------------------------
DROP TABLE IF EXISTS `alert_message_active`;
CREATE TABLE `alert_message_active`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `summary` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '告警摘要',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '告警描述',
  `labels` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '告警labels信息',
  `start_at` datetime(0) NULL DEFAULT NULL COMMENT '开始时间',
  `end_at` datetime(0) NULL DEFAULT NULL COMMENT '结束时间',
  `state` int(10) NULL DEFAULT NULL COMMENT '状态1:pending,2:Expired',
  `rule_id` int(10) NOT NULL COMMENT '告警规则ID',
  `project_id` int(10) NULL DEFAULT NULL COMMENT '关联project id',
  `module_id` int(10) NULL DEFAULT NULL COMMENT '关联module id',
  `endpoint_id` int(10) NULL DEFAULT NULL COMMENT '关联endpoint id',
  `asset_id` int(10) NULL DEFAULT NULL COMMENT '关联资产id',
  `dc_id` int(10) NULL DEFAULT NULL COMMENT '关联idc id',
  `hash_key` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT 'redis hash key',
  `remark` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '备注 记录手动关闭记录信息，格式\r\n\r\ntime     username\r\n\r\nmessage',
  `severity_id` int(10) NULL DEFAULT NULL COMMENT '告警级别id,关联ALERT_SEVERITY_CONF',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_state`(`state`) USING BTREE,
  INDEX `idx_rule_id`(`rule_id`) USING BTREE,
  INDEX `idx_project_id`(`project_id`) USING BTREE,
  INDEX `idx_module_id`(`module_id`) USING BTREE,
  INDEX `idx_endpoint_id`(`endpoint_id`) USING BTREE,
  INDEX `idx_asset_id`(`asset_id`) USING BTREE,
  INDEX `idx_dc_id`(`dc_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for alert_message_history
-- ----------------------------
DROP TABLE IF EXISTS `alert_message_history`;
CREATE TABLE `alert_message_history`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `summary` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '告警摘要',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '告警描述',
  `labels` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '告警labels信息',
  `start_at` datetime(0) NULL DEFAULT NULL COMMENT '开始时间',
  `end_at` datetime(0) NULL DEFAULT NULL COMMENT '结束时间',
  `state` int(10) NULL DEFAULT NULL COMMENT '状态1:pending,2:Expired',
  `rule_id` int(10) NOT NULL COMMENT '告警规则ID',
  `project_id` int(10) NULL DEFAULT NULL COMMENT '关联project id',
  `module_id` int(10) NULL DEFAULT NULL COMMENT '关联module id',
  `endpoint_id` int(10) NULL DEFAULT NULL COMMENT '关联endpoint id',
  `asset_id` int(10) NULL DEFAULT NULL COMMENT '关联资产id',
  `dc_id` int(10) NULL DEFAULT NULL COMMENT '关联idc id',
  `hash_key` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT 'redis hash key',
  `remark` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '备注 记录手动关闭记录信息，格式\r\n\r\ntime     username\r\n\r\nmessage',
  `severity_id` int(10) NULL DEFAULT NULL COMMENT '告警级别id,关联ALERT_SEVERITY_CONF',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_state`(`state`) USING BTREE,
  INDEX `idx_rule_id`(`rule_id`) USING BTREE,
  INDEX `idx_project_id`(`project_id`) USING BTREE,
  INDEX `idx_module_id`(`module_id`) USING BTREE,
  INDEX `idx_endpoint_id`(`endpoint_id`) USING BTREE,
  INDEX `idx_asset_id`(`asset_id`) USING BTREE,
  INDEX `idx_dc_id`(`dc_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for alert_message_silence
-- ----------------------------
DROP TABLE IF EXISTS `alert_message_silence`;
CREATE TABLE `alert_message_silence`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `summary` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '告警摘要',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '告警描述',
  `labels` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '告警labels信息',
  `start_at` datetime(0) NULL DEFAULT NULL COMMENT '开始时间',
  `end_at` datetime(0) NULL DEFAULT NULL COMMENT '结束时间',
  `state` int(10) NULL DEFAULT NULL COMMENT '状态1:pending,2:Expired',
  `rule_id` int(10) NOT NULL COMMENT '告警规则ID',
  `project_id` int(10) NULL DEFAULT NULL COMMENT '关联project id',
  `module_id` int(10) NULL DEFAULT NULL COMMENT '关联module id',
  `endpoint_id` int(10) NULL DEFAULT NULL COMMENT '关联endpoint id',
  `asset_id` int(10) NULL DEFAULT NULL COMMENT '关联资产id',
  `dc_id` int(10) NULL DEFAULT NULL COMMENT '关联idc id',
  `hash_key` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT 'redis hash key',
  `remark` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '备注 记录手动关闭记录信息，格式\r\n\r\ntime     username\r\n\r\nmessage',
  `severity_id` int(10) NULL DEFAULT NULL COMMENT '告警级别id,关联ALERT_SEVERITY_CONF',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_state`(`state`) USING BTREE,
  INDEX `idx_rule_id`(`rule_id`) USING BTREE,
  INDEX `idx_project_id`(`project_id`) USING BTREE,
  INDEX `idx_module_id`(`module_id`) USING BTREE,
  INDEX `idx_endpoint_id`(`endpoint_id`) USING BTREE,
  INDEX `idx_asset_id`(`asset_id`) USING BTREE,
  INDEX `idx_dc_id`(`dc_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for alert_notify_log
-- ----------------------------
DROP TABLE IF EXISTS `alert_notify_log`;
CREATE TABLE `alert_notify_log`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `message_id` bigint(20) NOT NULL COMMENT '关联 alert_message.id',
  `user_id` int(10) NOT NULL COMMENT '关联 sys_user.id',
  `method_id` int(10) NOT NULL COMMENT '关联ALERT_NOTIFICATION_METHOD.id',
  `message_state` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息状态1: active2: expired',
  `state` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '通知状态0:失败1:成功',
  `error_msg` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '发送错误信息',
  `time` datetime(0) NOT NULL COMMENT '发送时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_message_id`(`message_id`) USING BTREE,
  INDEX `idx_state`(`state`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for alert_notify_method
-- ----------------------------
DROP TABLE IF EXISTS `alert_notify_method`;
CREATE TABLE `alert_notify_method`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '通知方式名称',
  `file_path` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '除内置外，必填脚本路径，nz-web所在服务器路径',
  `account` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '可选值为user表属性id，name，username，email，mobile',
  `state` int(1) NULL DEFAULT NULL COMMENT '状态0：停用1：可用',
  `build_in` int(1) NOT NULL COMMENT '可选值：0：非内置1：内置默认：0系统内置一条 email 通知',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of alert_notify_method
-- ----------------------------
INSERT INTO `alert_notify_method` VALUES (1, 'emai', '', 'email', 0, 1);

-- ----------------------------
-- Table structure for alert_rule
-- ----------------------------
DROP TABLE IF EXISTS `alert_rule`;
CREATE TABLE `alert_rule`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '告警名称',
  `expr` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '运算表达式',
  `last` int(10) NOT NULL,
  `summary` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '告警摘要',
  `description` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '告警详细描述',
  `receiver` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `build_in` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '0' COMMENT '1：内置，0：非内置，默认：0\r\n\r\n内置规则不允许编辑删除，系统内置一条 snmptrap 规则',
  `operator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '比较符号',
  `threshold` double(20, 5) NULL DEFAULT NULL COMMENT '阈值',
  `unit` int(10) NOT NULL COMMENT '单位',
  `seq` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '导入唯一标识 全局唯一，用于导入撤销',
  `severity_id` int(10) NULL DEFAULT NULL COMMENT '告警级别id,关联ALERT_SEVERITY_CONF',
  `method` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'receiver 不为空时，必填',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE,
  INDEX `idx_severity_id`(`severity_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of alert_rule
-- ----------------------------
INSERT INTO `alert_rule` VALUES (1, 'SNMP TRAP', ' ', 600, NULL, NULL, NULL, '1', '', NULL, 0, NULL, 1, NULL);

-- ----------------------------
-- Table structure for alert_severity_conf
-- ----------------------------
DROP TABLE IF EXISTS `alert_severity_conf`;
CREATE TABLE `alert_severity_conf`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `color` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `weight` int(3) NOT NULL COMMENT '1-100 顺序取值不重复,值越小告警级别越高',
  `remark` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of alert_severity_conf
-- ----------------------------
INSERT INTO `alert_severity_conf` VALUES (1, 'P1', '#eb1010', 1, '高级告警');
INSERT INTO `alert_severity_conf` VALUES (2, 'P2', '#f89984', 2, '中级告警');
INSERT INTO `alert_severity_conf` VALUES (3, 'P3', '#f7ba78', 3, '低级告警');

-- ----------------------------
-- Table structure for alert_silence_conf
-- ----------------------------
DROP TABLE IF EXISTS `alert_silence_conf`;
CREATE TABLE `alert_silence_conf`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `matchers` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `remark` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '记录告警静音原因\r\n\r\n默认：“”',
  `start_at` datetime(0) NOT NULL COMMENT '开始时间',
  `end_at` datetime(0) NOT NULL COMMENT '结束时间',
  `utime` datetime(0) NOT NULL,
  `state` int(1) NOT NULL COMMENT '可选值：\r\n\r\n1:active\r\n\r\n2:pending\r\n\r\n3:expired',
  `seq` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '全局唯一，用于导入撤销\r\n\r\n默认：“”',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE,
  INDEX `idx_matchers`(`matchers`(768)) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for alert_silence_message_rel
-- ----------------------------
DROP TABLE IF EXISTS `alert_silence_message_rel`;
CREATE TABLE `alert_silence_message_rel`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `silence_conf_id` int(10) NOT NULL,
  `message_id` bigint(20) NOT NULL,
  `hash_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for asset_asset
-- ----------------------------
DROP TABLE IF EXISTS `asset_asset`;
CREATE TABLE `asset_asset`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '不重复，唯一索引',
  `number` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '资产编号',
  `sn` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '资产序列号',
  `pid` int(10) NOT NULL DEFAULT -1 COMMENT '默认：-1\r\n\r\n只有type vm = 1可以且必须填写此字段，且 pid.type vmh = 1',
  `type_id` int(10) NOT NULL,
  `state_id` int(10) NOT NULL,
  `brand_id` int(10) NOT NULL,
  `model_id` int(10) NOT NULL,
  `dc_id` int(10) NOT NULL,
  `cabinet_id` int(10) NULL DEFAULT NULL,
  `cabinet_start` int(10) NULL DEFAULT NULL,
  `cabinet_end` int(10) NULL DEFAULT NULL,
  `manage_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '管理ip',
  `purchase_date` date NULL DEFAULT NULL COMMENT '购买日期',
  `create_at` datetime(0) NULL DEFAULT NULL COMMENT '添加时间',
  `update_at` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `create_by` int(10) NULL DEFAULT NULL COMMENT '添加人，与user表关联',
  `update_by` int(10) NULL DEFAULT NULL COMMENT '最后更新人',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `auth_protocol_port` int(10) NULL DEFAULT NULL COMMENT '认证服务端口',
  `auth_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '认证方式1：用户名密码\r\n\r\n2：密钥',
  `auth_username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户名',
  `auth_pin` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '密码auth_type =1 : 记录登录密码\r\n\r\nauth_type=2 : 记录密钥密码',
  `auth_pri_key` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '秘钥',
  `auth_user_tip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户名提示 telnet协议有效',
  `auth_pin_tip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '密码提示 telnet协议有效',
  `snmp_credential_id` int(10) NULL DEFAULT NULL COMMENT 'snmp凭证id type 开启snmp有效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE,
  INDEX `idx_manage_ip`(`manage_ip`) USING BTREE,
  INDEX `idx_sn`(`sn`) USING BTREE,
  INDEX `idx_type_id`(`type_id`) USING BTREE,
  INDEX `idx_state_id`(`state_id`) USING BTREE,
  INDEX `idx_brand_id`(`brand_id`) USING BTREE,
  INDEX `idx_model_id`(`model_id`) USING BTREE,
  INDEX `idx_dc_id`(`dc_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for asset_brand
-- ----------------------------
DROP TABLE IF EXISTS `asset_brand`;
CREATE TABLE `asset_brand`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '名称',
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '备注',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for asset_feature
-- ----------------------------
DROP TABLE IF EXISTS `asset_feature`;
CREATE TABLE `asset_feature`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `asset_id` int(10) NOT NULL COMMENT '资产id',
  `item` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `source` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '采集方式，ssh/snmp',
  `update_at` datetime(0) NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_asset_id`(`asset_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for asset_field_group
-- ----------------------------
DROP TABLE IF EXISTS `asset_field_group`;
CREATE TABLE `asset_field_group`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `remark` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '1:内置，0：非内置，默认：0\r\n\r\n内置数据不允许删除',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户导入撤销',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of asset_field_group
-- ----------------------------
INSERT INTO `asset_field_group` VALUES (1, 'default', 'Default meta group', '1', '');

-- ----------------------------
-- Table structure for asset_field_meta
-- ----------------------------
DROP TABLE IF EXISTS `asset_field_meta`;
CREATE TABLE `asset_field_meta`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `meta_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '唯一不重复 正则：[\\w]+',
  `group_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '关联 asset_field_group.id',
  `search` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '默认：0 0：不可查询\r\n\r\n1：可查询',
  `display` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '默认：0 0：列表不展示\r\n\r\n1：列表展示',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'TEXT' COMMENT '属性输入类型',
  `remark` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `param` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '默认：{}',
  `build_in` int(1) NOT NULL DEFAULT 0 COMMENT '1:内置，0：非内置，默认：0',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户导入撤销',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE,
  INDEX `idx_meta_key`(`meta_key`) USING BTREE,
  INDEX `idx_group_id`(`group_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for asset_field_value
-- ----------------------------
DROP TABLE IF EXISTS `asset_field_value`;
CREATE TABLE `asset_field_value`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `asset_id` int(10) NOT NULL COMMENT '	关联 asset.id',
  `meta_id` int(10) NOT NULL COMMENT '关联asset_field_meta.id',
  `value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户导入撤销',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_asset_id`(`asset_id`) USING BTREE,
  INDEX `idx_meta_id`(`meta_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for asset_model
-- ----------------------------
DROP TABLE IF EXISTS `asset_model`;
CREATE TABLE `asset_model`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `brand_id` int(10) NOT NULL,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '型号名称',
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `usize` int(2) NOT NULL DEFAULT 1,
  `chart_ids` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'chart 模板id',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE,
  INDEX `idx_brand_id`(`brand_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for asset_ping
-- ----------------------------
DROP TABLE IF EXISTS `asset_ping`;
CREATE TABLE `asset_ping`  (
  `host` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'ip地址',
  `status` int(1) NOT NULL COMMENT '是否可达 可达为1 不可达为0',
  `rtt` double(7, 2) NULL DEFAULT NULL COMMENT '传输的时间延迟Round-Trip Time ，单位：ms',
  `last_update` datetime(0) NULL DEFAULT NULL COMMENT '最后更新时间',
  `asset_id` int(10) NOT NULL,
  INDEX `idx_host`(`host`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_asset_id`(`asset_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for asset_state_conf
-- ----------------------------
DROP TABLE IF EXISTS `asset_state_conf`;
CREATE TABLE `asset_state_conf`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '型号名称',
  `ping` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '0：关闭\r\n\r\n1：开启',
  `monitor` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '0：关闭\r\n\r\n1：开启',
  `alert` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '0：关闭\r\n\r\n1：开启',
  `remark` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '默认“”',
  `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '是否内置1:内置，0：非内置，默认：0\r\n\r\n内置数据不允许删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of asset_state_conf
-- ----------------------------
INSERT INTO `asset_state_conf` VALUES (1, 'In use', '1', '1', '1', '', '1');
INSERT INTO `asset_state_conf` VALUES (2, 'Not in storage', '0', '0', '0', '', '1');
INSERT INTO `asset_state_conf` VALUES (3, 'Idle', '1', '0', '1', '', '1');
INSERT INTO `asset_state_conf` VALUES (4, 'Repair', '1', '1', '0', '', '1');
INSERT INTO `asset_state_conf` VALUES (5, 'Scrapped', '1', '0', '0', '', '1');

-- ----------------------------
-- Table structure for asset_type_conf
-- ----------------------------
DROP TABLE IF EXISTS `asset_type_conf`;
CREATE TABLE `asset_type_conf`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `pid` int(10) NOT NULL DEFAULT 0 COMMENT '父id 默认为0',
  `vm` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '是否为虚拟机 0:不是虚拟机\r\n\r\n1:是虚拟机',
  `vmh` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '是否为虚拟机宿主 0:不是宿主机，不支持挂载 虚拟机\r\n\r\n1:是宿主机，支持挂载虚拟机',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `remark` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '1:内置，0：非内置，默认：0\r\n\r\n内置数据不允许删除',
  `auth_protocol` int(1) NOT NULL DEFAULT 0 COMMENT '认证协议 默认：0 \r\n\r\n0：无\r\n\r\n1：ssh\r\n\r\n2：telnet',
  `snmp_enable` int(1) NOT NULL DEFAULT 0 COMMENT '是否支持snmp 默认：0 \r\n\r\n0：不支持\r\n\r\n1：支持',
  `snmp_collect` int(1) NOT NULL DEFAULT 0 COMMENT '是否开启snmp采集 默认：0 \r\n\r\n0：不开启\r\n\r\n1：开启\r\n\r\n当snmp_enable=1时有效',
  `ssh_collect` int(1) NOT NULL COMMENT '是否开启ssh采集 默认：0 \r\n\r\n0：不开启\r\n\r\n1：开启\r\n\r\n当 auth_protocol=1时有效',
  `ssh_collect_script` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of asset_type_conf
-- ----------------------------
INSERT INTO `asset_type_conf` VALUES (1, 0, '1', '1', 'Network devices', 'Network devices', '1', 2, 1, 1, 1, '');
INSERT INTO `asset_type_conf` VALUES (2, 0, '0', '0', 'Router', '', '1', 2, 1, 0, 0, '');
INSERT INTO `asset_type_conf` VALUES (3, 0, '0', '0', 'Firewall', '', '1', 2, 1, 0, 0, '');
INSERT INTO `asset_type_conf` VALUES (4, 0, '0', '0', 'Switch', '', '1', 2, 1, 0, 0, '');
INSERT INTO `asset_type_conf` VALUES (5, 0, '0', '1', 'Computer', '', '1', 1, 0, 0, 0, '');
INSERT INTO `asset_type_conf` VALUES (6, 0, '0', '1', 'Server', '', '1', 1, 0, 0, 0, '');
INSERT INTO `asset_type_conf` VALUES (7, 0, '1', '0', 'Virtual machine', '', '1', 1, 0, 0, 0, '');
INSERT INTO `asset_type_conf` VALUES (8, 0, '0', '0', 'ADC', '', '1', 1, 1, 0, 0, '');

-- ----------------------------
-- Table structure for cabinet
-- ----------------------------
DROP TABLE IF EXISTS `cabinet`;
CREATE TABLE `cabinet`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '机柜名称',
  `dc_id` int(11) NOT NULL COMMENT '数据中心id',
  `u_size` int(10) NULL DEFAULT NULL COMMENT 'u位容量',
  `remark` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `seq` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '全局唯一，用于导入撤销',
  `x` int(10) NULL DEFAULT NULL COMMENT '在机房的那一列',
  `y` int(10) NULL DEFAULT NULL COMMENT '在机房的那一行',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE,
  INDEX `idx_dc_id`(`dc_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for dc
-- ----------------------------
DROP TABLE IF EXISTS `dc`;
CREATE TABLE `dc`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '名称',
  `location` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '位置',
  `tel` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '联系电话',
  `principal` int(10) NULL DEFAULT NULL COMMENT '机房负责人',
  `seq` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '全局唯一，用于导入撤销',
  `x` int(10) NULL DEFAULT NULL COMMENT '列',
  `y` int(10) NULL DEFAULT NULL COMMENT '行',
  `longitude` double(8, 5) NULL DEFAULT NULL COMMENT '经度 -180,180',
  `latitude` double(8, 5) NULL DEFAULT NULL COMMENT '纬度 -90，90',
  `state` varchar(8) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT 'ON' COMMENT '可选值：ON,OFF 默认：ON',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for flyway_schema_history
-- ----------------------------
DROP TABLE IF EXISTS `flyway_schema_history`;
CREATE TABLE `flyway_schema_history`  (
  `installed_rank` int(11) NOT NULL,
  `version` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `description` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `type` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `script` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `checksum` int(11) NULL DEFAULT NULL,
  `installed_by` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `installed_on` timestamp(0) NOT NULL DEFAULT current_timestamp(),
  `execution_time` int(11) NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`) USING BTREE,
  INDEX `flyway_schema_history_s_idx`(`success`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for link
-- ----------------------------
DROP TABLE IF EXISTS `link`;
CREATE TABLE `link`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `create_by` int(10) NULL DEFAULT NULL COMMENT '创建人id',
  `url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'url',
  `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '1:内置，0：非内置，默认：0',
  `weight` int(10) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for maintenance_record
-- ----------------------------
DROP TABLE IF EXISTS `maintenance_record`;
CREATE TABLE `maintenance_record`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `asset_id` int(10) NULL DEFAULT NULL COMMENT '资产id',
  `content` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '维修内容',
  `create_at` datetime(0) NULL DEFAULT NULL COMMENT '添加时间',
  `create_by` int(10) NULL DEFAULT NULL COMMENT '添加人',
  `update_at` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `update_by` int(10) NULL DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for monitor_endpoint
-- ----------------------------
DROP TABLE IF EXISTS `monitor_endpoint`;
CREATE TABLE `monitor_endpoint`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `module_id` int(10) NOT NULL,
  `asset_id` int(10) NOT NULL,
  `configs` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '计算 moduleId + assetId + host + port + configs 的hash值，用于确保相同配置不重复',
  `enabled` int(4) NOT NULL COMMENT '是否启用	 可选值\r\n\r\n0：停用\r\n\r\n1：启用',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE,
  INDEX `idx_module_id`(`module_id`) USING BTREE,
  INDEX `idx_asset_id`(`asset_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for monitor_endpoint_state
-- ----------------------------
DROP TABLE IF EXISTS `monitor_endpoint_state`;
CREATE TABLE `monitor_endpoint_state`  (
  `endpoint_id` int(10) NOT NULL,
  `state` int(4) NOT NULL COMMENT ' ',
  `time` datetime(0) NOT NULL,
  PRIMARY KEY (`endpoint_id`) USING BTREE,
  INDEX `idx_state`(`state`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for monitor_event_log
-- ----------------------------
DROP TABLE IF EXISTS `monitor_event_log`;
CREATE TABLE `monitor_event_log`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `event` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '唯一不重复\r\n\r\n包含值：cmd，prometheus，rule，snmp，blackbox',
  `counter` int(10) NOT NULL COMMENT '描述信息，update  +1',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of monitor_event_log
-- ----------------------------
INSERT INTO `monitor_event_log` VALUES (1, 'blackbox', 0);
INSERT INTO `monitor_event_log` VALUES (2, 'prometheus', 0);
INSERT INTO `monitor_event_log` VALUES (3, 'rule', 0);
INSERT INTO `monitor_event_log` VALUES (4, 'cmd', 0);
INSERT INTO `monitor_event_log` VALUES (5, 'snmp', 0);

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
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `configs` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置参数	',
  `snmp_yml` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT 'snmp_exporter需要的配置部分',
  `chart_ids` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'chart 模板id',
  `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE,
  INDEX `idx_type`(`type`) USING BTREE,
  INDEX `idx_project_id`(`project_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

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
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of monitor_project
-- ----------------------------
INSERT INTO `monitor_project` VALUES (1, 'Common', NULL, '1', NULL);

-- ----------------------------
-- Table structure for project_topo
-- ----------------------------
DROP TABLE IF EXISTS `project_topo`;
CREATE TABLE `project_topo`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` int(10) NOT NULL COMMENT 'project  id',
  `topo` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '拓扑图配置信息',
  `update_at` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `update_by` int(10) NULL DEFAULT NULL COMMENT '更新人id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uni_projectid`(`project_id`) USING BTREE COMMENT '唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for project_topo_icon
-- ----------------------------
DROP TABLE IF EXISTS `project_topo_icon`;
CREATE TABLE `project_topo_icon`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'icon名称 为空时使用 文件名',
  `bytes` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'icon图片字节',
  `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '截取自图片后缀，支持jpg，jpeg，png，gif',
  `unit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'icon组',
  `width` int(10) NULL DEFAULT -1 COMMENT '图片宽度  默认：-1 未知',
  `height` int(10) NULL DEFAULT -1 COMMENT '图片高度	默认：-1 未知',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of project_topo_icon
-- ----------------------------
INSERT INTO `project_topo_icon` VALUES (1, 'common', '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', 'png', '', -1, -1);
INSERT INTO `project_topo_icon` VALUES (2, 'mysql', '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', 'png', '', -1, -1);
INSERT INTO `project_topo_icon` VALUES (3, 'oracle', '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', 'png', '', -1, -1);
INSERT INTO `project_topo_icon` VALUES (4, 'redis', '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', 'png', '', -1, -1);

-- ----------------------------
-- Table structure for prom_server
-- ----------------------------
DROP TABLE IF EXISTS `prom_server`;
CREATE TABLE `prom_server`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `dc_id` int(11) NOT NULL COMMENT '数据中心id',
  `host` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '部署ip',
  `port` int(10) NOT NULL DEFAULT 9090 COMMENT '端口 默认9090',
  `type` int(10) NOT NULL COMMENT '1:global,负责汇聚所有per-datacenter采集的数据及告警配置\r\n\r\n2:per-datacenter，负责根据exporter配置采集监测数据',
  `status` int(1) NOT NULL DEFAULT 0,
  `check_time` datetime(1) NULL DEFAULT NULL COMMENT '最后操作时间',
  `status_info` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `token` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_type`(`type`) USING BTREE,
  INDEX `idx_port`(`port`) USING BTREE,
  INDEX `idx_host`(`host`) USING BTREE,
  INDEX `idx_dc_id`(`dc_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for snmp_credential
-- ----------------------------
DROP TABLE IF EXISTS `snmp_credential`;
CREATE TABLE `snmp_credential`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `remark` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述信息',
  `type` int(1) NOT NULL COMMENT '1,2,3 -- > V1,V2,V3 创建人id',
  `config` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '具体配置',
  `port` int(1) NOT NULL DEFAULT 161 COMMENT '端口号 默认161',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for snmp_mib
-- ----------------------------
DROP TABLE IF EXISTS `snmp_mib`;
CREATE TABLE `snmp_mib`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'mib文件名称，默认去掉后缀',
  `file_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '二进制文件名称',
  `content` mediumblob NOT NULL COMMENT 'mib二进制文件',
  `models` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '为空时：所有型号适用\r\n\r\n不为空时：可关联多个型号，用逗号分隔，如：1,2,3',
  `remark` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '备注',
  `update_by` int(10) NULL DEFAULT NULL COMMENT '更新人',
  `update_at` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `tree` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'MIB文件解析tree',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_api_key
-- ----------------------------
DROP TABLE IF EXISTS `sys_api_key`;
CREATE TABLE `sys_api_key`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称 唯一不重复',
  `token` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密钥 唯一不重复',
  `role_id` int(10) NOT NULL COMMENT '角色id',
  `create_by` int(10) NOT NULL COMMENT '创建用户',
  `create_at` datetime(0) NOT NULL COMMENT '创建时间',
  `expire_at` datetime(0) NULL DEFAULT NULL COMMENT '失效时间 为空不失效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE,
  INDEX `idx_role_id`(`role_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `id` bigint(19) NOT NULL AUTO_INCREMENT,
  `param_key` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'key',
  `param_value` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态   0：隐藏   1：显示',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `param_key`(`param_key`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 229 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '系统配置表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES (3, 'alert_api', '', 1, 'alert api 地址');
INSERT INTO `sys_config` VALUES (4, 'alert_path_prefix', '', 1, '告警推送请求前缀');
INSERT INTO `sys_config` VALUES (5, 'alert_interval', '15', 1, '告警间隔时间');
INSERT INTO `sys_config` VALUES (14, 'confagent_port', '10090', 1, 'confagent内置端口');
INSERT INTO `sys_config` VALUES (15, 'confagent_path', '/nz-agent', 1, 'confagent内置路径');
INSERT INTO `sys_config` VALUES (17, 'telnet_user_tip', 'ogin:', 1, 'telnet用户名提示');
INSERT INTO `sys_config` VALUES (18, 'telnet_password_tip', 'assword:', 1, 'telnet密码提示');
INSERT INTO `sys_config` VALUES (19, 'telnet_relogin_password_tip', 'assword:', 1, 'telnet relogin密码提示');
INSERT INTO `sys_config` VALUES (21, 'ldap_address', '', 1, NULL);
INSERT INTO `sys_config` VALUES (22, 'ldap_dn', '', 1, NULL);
INSERT INTO `sys_config` VALUES (23, 'ldap_password', '', 1, NULL);
INSERT INTO `sys_config` VALUES (30, 'ldap_user_filter', '', 1, NULL);
INSERT INTO `sys_config` VALUES (31, 'ldap_mapping', '', 1, NULL);
INSERT INTO `sys_config` VALUES (32, 'ldap_enable', 'off', 1, NULL);
INSERT INTO `sys_config` VALUES (33, 'geoJson', '{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{\"name\":\"Akmola Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ÔdƊĳʪèāƂNŎǀ\\\\ǔ§DȆȨC˔ŷǈƾ»ìƞ¦ƪXÄĢʈĤÉŤļÂRƌÐŁLĉǂâHƚƬјƬËjĜĚɐUűȞW¿ūǖCĠŋŘLOôŘXAĆτEUąȦu˖ĴƖǯiŗĥ¾ąȸùʒCĶȺΜ¦¾ėeǩŸήë¿ͦSà¥@ԣǛƭMŅǸd|ȊÙɱmƛ½ŧƉŗzɥ^čƂJŞ³ƍûȳAÓǃ¹ǋvĽśƿYÛ·äÏċ©ĥą­ré͕R[ȔťĂˇÇɷƑL¥ǭ}ëígšĽŵ«ÖïȽõƩ]nŀéò̧Éćþ˭Úĥ˵ÍǵÀCɰ¢RȪʏJ̯ƻȓnēſËȏBΣďÇŬüĵɡʓĊįsÙŤĝŇŴštÓġƹosÆ®ƾǧĸļƄÞȌ³²řBĞƪRȚÐĞɦŊÂPȖ\",\"@@ÒõƒkƈĊ¿Ŧǽrĭſ\"],\"encodeOffsets\":[[67591,53693],[72928,52338]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Aktobe Province\"},\"geometry\":{\"type\":\"MultiPolygon\",\"coordinates\":[[\"@@Л̉ƸƬŨLŖĴf\"],[\"@@ƖʆǎĀǬQ˄˄zŜı²^żÙÊČÀâŀƾø¥ĎfƤŏpDĔƒXƊğȢdçȦţɶāͤŒêºƺƴĈÄ¦ĨľÿÆøǂ]ɎxưŧƶÉª¼ƪOǮĈʸx¼ŚfÓŜRĬćįƴ«ĎĭҚ¿ĊğʠPƸŞdŬƘŤĝО[՞ŲĴδĺdȊNķưCŶ·­đǜĿŹ©SƇƢ¢¾ǁńġĎſƂũÀŏƃeǇ×Ņį§ŏƽȰȳď¯ĒƱʠƙƦäĢÅåė|Û˴ã̖ůȶʃĔƗƂđɗǑƝÅǇĻΫŻɳƛ̱ªrðɯǞĵ^ŗƬųĖӅQyֱ͟έXiǯ́_̡țǩƍϗϑåTćơbŏÍŭɡӵŋ੉ʁřؾŝǦsĞĿŖyŘҥĮʋĆǽŮħǆįŜÆɂCŖ²ǄmƬƇŠ~Ƣ÷Ą¦ǬaŔƍƗgíџļ@ǄȂƅyǝĆ\"]],\"encodeOffsets\":[[[60468,46905]],[[54918,50423]]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Almaty\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ʴ¤wēČěıĵhűèÏƮ\"],\"encodeOffsets\":[[78580,44354]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Almaty Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ĖŘӠ͜ɴ~ʶŀʄXτƨċŤ¬ĨÁľϾÜûĪXÚÒǼP÷ҐŦ¤ɒŬĦņûƊŞ˶ǃłO¤ïϒ˄ʩϞŖƢ­I«֚ȭɘOĜßɸÝƶƇÃŦ×ĘȟĮŋJħÖÛĎEɪŃÝKĿęǧÄ̃YĵńƛBƱā˧ȅ÷ǙPɕȉ×»|ɩšŶęŒnªąĀuĈýÁŅ¸ǃWƳňʑȒ̑ÅōĀuFơǏCǕőŜĽȿĩǵ¶ûXƷªħė]ǭǰϵxƃŤƏĄdĚɧ}ʍÌų¸š͏GƿÂ̛\\\\ǁaá¼ʭNţÙƻfƭ½űÐƕiùħ_ƅĈʉä×ȔĆŞˇøă¾ʠǘ_Ķ¤È]ĲƇĪǝòǅ^șņFǶĵĒɧĦǶ˧oİ~ÊɺAĀç̬ĨŦĤ\",\"@@ÐƭŲçĶgĲċĜxĔʳ£\"],\"encodeOffsets\":[[75839,47106],[78580,44354]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Astana\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ĮƀǾqÀťƇĉƑlÑö\"],\"encodeOffsets\":[[72928,52338]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Atyrau\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ֺÃݒÇɸ©ЬŋOĔʾUȊvǠŞҦȺʒì´ĮǨTĴΤ£UğłVŌ·ŜhȂéĜDBǀÜƤĐIOȠĴ¬π}̰ȈǞąƆzȁ@ǃѠĻîƘhƎbœ¥ǫøă}ơƈşnƫ±ǃDŕÅɁİśĨǅǾŭʌąҦĭzŗŀŕtĝ́nƑĘÓgãƖǛ\\\\īĊǽŷƱXɍ_ŝϓÕɯEÝĳǯĥЯEǦʎcǄē¨Ŕę¸źǯĺÓʅtď°ǑãȩŭÛĥjoØı¢ȳt÷ū¥ǷYēóƑc«ỚŁįí³ĉčIYŏ±û}ƃǃīí°ƿ¨ċöı\\\\ȋö°ȯn^â̤ªÜ¤çŚǡǸǅˈËAȩɾċ¬ϯ^ȥâſǅâYĀŋŦ¼Ȥ\"],\"encodeOffsets\":[[48242,49392]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"East Kazakhstan\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@΢Ĺʜ¿ǘ¨ǦȜŊÙnkȆƅ[Ç²DĒƳąŠįĖŊ¸ːƸæžȰc̚ɒͤѣ̀ϟPúĠÒʐÌȬǀCĢɜµkɐµįǵ̒zȁŐ[ʬ¾˲ÉňǼ{ƞĔKÞƬgʔðǤhȰ·ˆŹ[¯ȎǏüǍaÇƄåȀBȒıƘÛǸȷXġ̼[ĀÁǦbðãŦöµÐĮČV˜ǸÖƙƃȎ»FÙČ¥GÑĊûȴqJıƛǻeřımÁöÿŗřÿ©ñŧ³œBċ«˽CęûŷÑǁöΣyÝÔÙDǯėí_ßɗJ̗ǝǩŶȅÏzѹ˳ðȉĪʫ¨·Ɖǣ̹¡ƣŗ̏±ƫķTťñƭ\\\\ËčFÕÜIĨĭŌėȠťØÄƵƈɷÞěàɗP֙ȮJ¬ơ®ϝŕ˃ʪϑ£ðŁP˵ǄƉŝŅüūĥɑť£ȡ^ûāɉĎĠpŸƸsǔ~ÜǊÔbɂđŎǿŐĴŐǔWŲŢŢæ·ÏĀ\\\\ĔǚŇèEÂʿǌ^¶кŘøú[ǂĴâƾ¾ȎŲYÜ\"],\"encodeOffsets\":[[79562,51428]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Jambyl Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ቒP٢I׸Aੈ@ģħťè̫ ÿɹB}Éį˨pǵɨĥĶđEǵȚŅǆ]Ǟñƈĩ^ı£Ç`ĵʟǗĄ½ˈ÷ąŝØȓʊãƆćęţŭ˵pͅŪďSƣŦũśÜÑɝsͯŵÿ]ƛËǹ®ōƟI¼ǅǫcƓĖȷǻØƉmɍĄȏÅęɇmąƛƭsƷ÷çǗŐâȫźơ¾dǒċŌǩŚʇªďZĒĝhļŀļǨvɜĲŒěвÃȠ»ÊŅ͐ėƆýɬǗȘ\"],\"encodeOffsets\":[[70725,47104]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Karagandy Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ɴƜάżǈļƞÆɘǒƁĒēƘɢƬ±Ĩ̐¦ġƎȖB¢ɚجğɤ¸nĂʠV˸ȾŊzĠƾƼĺǢÎ˨ʒɞƲOĲˬäņŰŹômĔȔ̰ƼʐIQȩ¡DɯǶ¿˶ÎĦˮÙĈý̨ÊêñmĿƪ^ȾöÕðŶ¬ľhŢìîǮ~K¦ɸƒˈÈŦā\\\\ȓ͖QqêĆ®ĦČªãÐÜ¸ǀZľŜǌuǄºBÔȴƎüŝ´ƁI]ĎɦŘy̠Ýdēµũ¬ĝĹÃwīČ­ɀTƀȒ¯˶ǑLçƘÙǡÏŏÝÇƖUˊŤˀbÀŦ}ǂ®ư´þɆtŀµŌˌÂ¤ȚxƪčǔaǐïƽĆ±ZÛȍűƽ½ĳá\\\\ǁ÷ùйŗ]µˀǋFÁňçǙ[ēÐÿΈåšűšǓXĳŏȀŏĒōɁÓaÛǉ} ǓƷtoŷğɊčüĂȢ]øҏǻOÙÑĩWÛüϽĽħÂţ«ƧČσʃWʵĿɳ}ӟ͛ĕŗੇ@׷B١JቑOۡBࢿ@Ťȡ|ȋ[ڇhʱǵČșƐǗtгǀɥ¬˥ĀĝIȗĊǥĔŤ͛îËO\"],\"encodeOffsets\":[[64461,48324]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Kostanay Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@RȃþŁũʊűƗuƽÛìǑŎDKœÃ§DįŜBPʹŦBÅǏHſjáɂW[ëǃNTŇğEY˝OȕŉÁĝɥșÏƩQĝŚA´±ȋƃÝĻǨķ­ƽtÅƺpÔĢŢsňųĞÚţİtʔĉɢĶûÈūΤĐȐAƀÌźóŅů˫ãPıɝƱ˧ʑǡÍƻĹğƽŉy˷ȽʟUmāɣ·ثĠ¡əȕAĢƍ̏¥²ħɡƫȵʄ̕Ű˳ä{ÜæĘġÆƥãʟƚđƲĐ°ȯȴŐƾİ¨ņǈØƄf¿ŐƁŪčƀŃĢ½ǂơ¡TƈźªǛŀ®Ēŵ¸ƯDĸȉMşÌêʫ°ǟNÁŮͯDī|NŶɣĀŇläǒEˀƔǦA̎ǈïƈŝ^DĴŅlEĐĺTʊǲŊ¦ŜäƎéÎɒ£ƜWĺ˭Ôġèū§Ń¸ƥRÏļŤĘŊ±ƒÐÈƋTƧāªȁX˜ƘûĞƄƜƾRø×ƀlŎÉƾ[ö˪IĚėĞNŚǬɰOÒâ̰pʈÄƲBмĂɎYȤÐŬÿİuƀΞĠĪĈøŘjĀÝ\"],\"encodeOffsets\":[[67662,55952]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Kyzylorda Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ÌP͜íēţǦȘĉĞJ˦ÿɦ«дƿǘsȚƏǶċʲڈgȌ\\\\Ȣ{ţ@Çňí¾ûãùɩÇăǼıŀSċj̵eǯċĶÓŴoØėɆŝāû\\\\ŏăąÉĕƅsĥîŵԏȏċ«ࠍӛß`rȒD̺Ʌãʇδ̽ǦƵȂÏjͭƑ֩zёªङŏȧYӡдʋȐŔĭª࠽ӺŁvʌ·ĊƧă˵ϹǆМ¢ÖĲWlłǓéSĈæϘϒǪƎ̢Ȝ̂`jǰήWֲ͠zӆRŴĕŘƫĶ]ɰǝqï̲©\"],\"encodeOffsets\":[[64461,48324]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Mangystau Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@аFǰĦÞĴɰFϔÖŞɎ`ƲWǾŸĬĉǜ[äƕÔhƒė̂mŞǥŚؽصƩ@Ӂ@ַFᎭĿN̓ãƷŞǙȂƓȈDÖƟŪҽ˞խùӯƕЧ˗AŦÑŞ|ĎļňĚȬÎØ]ĞĿÐşōQǙ®ȡŝǚĕÎè˳~ʰĪĩÐŋƦ{ńŃĈ{ƌÃ̃¬ƯüSƈÖĎβŮTǬřČ@ĂÆŀ\\\\ĈįGķʐìņ}žŠŘǔŢ¼ʔNʒąƼT͆ǭWȂĨŲG˴ŝ̭ĻăùóŹșˏPÃĲwľĸŖǶPúǘŶŚmǤÊƞfɬW;ȢĹ©äƧÎěĺĩvÈƿŨȯkÕāɱƧƕǎ̙ÊJúöƈ\"],\"encodeOffsets\":[[54469,47117]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"North Kazakhstan\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ʲÂݶĺ¨¢Ș{ǮîǴiǞfĬãÆö°̲HĔƘǀµIĸȨµ©Вjƶ±îïɸ¿Ⱦł˜GǂǝƱŜơǎķý°ȋȓ_ƬȯĸüǌuA¹ȞNŰ˼ĔŜèvĹľŧµĉĲŞàĀ͖ÔĥȞǮIRÆȾ¨ÝǕȧjǵʷƆñŢœűOsŁĠ{ŜÐĺǗƁúÙĳŜIȈĸąȗŝµÇƨͥTÀέìŷfǪ½ĘΛ¥ȹDĵʑȷú½ĆĦjŘƕǰ˕ĳȥvVĆσFBąŗWPóŗKğŌǕDÀŬȝXŲɏVęiěƫÌїƙƫáGĊǁłK ÏQƋĻÁÊţʇģÃġƩWƝ¥¼ëǇƽ˓ŸȧDCȅǓ¨ƿ[ōƁMçĂʩƉĴÓcZ˞ĠFSňǄM\\\\ìɁXiâƀǐGAÆťOʺśACİÄ¨LŔōCëǒƾÜƘvʉŲŪýłQȄ\"],\"encodeOffsets\":[[67662,55952]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Pavlodar Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ǦĲƪtâÔƞkǸËĔėĆĪyĸƦkðĸÎɄ˦ƖCĢȈ|ԔƊ²ĊȀǘx°ÖŰĭĹşģǭCĒĻڪͻӊʭͲ΁Ϫэ˔˳Ɇ˥̙ɑȯdŽƷåˏŉ·İĕĆşƴCđÈ±Ɔ\\\\lȅÚmŉǥțǗ§ʛÀΡĺą²ƾǏðǓbƩĎșwÁ£ŋˋĿ¶Ʌs³ýƯǁ­ť~¿ʿaˉţƕVÈŐÞǢÐƗÚKè˵ǒȑ°ſȿSċ®xĬĺÄ«Ğ¶ŪcĔ̟ÞŨƊƜ¾ɲnȉÚ{ǷcNņǜƮ@Ԥß¦Ƨ¶ÈȘŞĆȇķśJĴùÚǘƂĹśÏğ|tłŲPšŔ\"],\"encodeOffsets\":[[75202,54718]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"South Kazakhstan Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@à_ࠎӜČ¬ԐȐŶítĦĖƆĆÊĄ[ŐĂüɅŞ×ĘųpĵÔČfǰi̶TČĿǻĲÈĄɪäú½üŇî@Èࣀ@ۢAǘȗþɫĘƅņ͏¼ÉÄȟĜбıőuɛĻǧĻĿĞgYđĐʈ©ǪřČŋcǑƢ½ȬŹ áǘŏdğíEģñ«ėƃĀſÓƋƿƳÏe˷ÑùċÇLƅŋɓÓCġȡŝƇơÅcJǻxōĝFʅĞȭŦĤĀ@ĘĕIeľč]ō¢̱±փ\\\\ċ̒×͒҅ºFΤÎm\"],\"encodeOffsets\":[[67657,43355]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"West Kazakhstan\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@áĿċ¿ÚÉ]ŻĲ±yś˃˃ǫRǍÿƕʅ̯ȇο~ĳ«PȟďJÛƣAƿěCȁêśgŋ¸ŁUVĠΣ¤ĳǧS³ĭʑëҥȹǟŝȉuʽVPēЫŌɷªݑÈֹÄZ̓îɉäʌѤĨ~ŘĜŤǷĜĸѲɼÒƞƆăĈĤĚÈîŜŴĹƪĉƀţđŚřƴqʜðƄĆƟȪã̀ƌKǐòİĒʘÄsȮ͎bɚŨǴ~òĎǢèIŀ̚üФ¨ƓǬÂ`÷Ƅ¢îļĐǒqȶîȘɽȌ¨ĪЪ\\\\ƴÇhıÈÙ̮ËĠ¯Àťè­Ɇ½ķȓŊý\"],\"encodeOffsets\":[[55853,51741]]}}],\"UTF8Encoding\":true}', 1, NULL);
INSERT INTO `sys_config` VALUES (36, 'generator_path', '/opt/nezha/nz-web/generator', 1, 'generator路径');
INSERT INTO `sys_config` VALUES (45, 'asset_ping_interval', '300', 1, NULL);
INSERT INTO `sys_config` VALUES (48, 'system_name', 'One-stop monitoring system', 1, NULL);
INSERT INTO `sys_config` VALUES (49, 'ldap_ou', '', 1, NULL);
INSERT INTO `sys_config` VALUES (50, 'timezone', 'Asia/Shanghai', 1, NULL);
INSERT INTO `sys_config` VALUES (52, 'export_pagesize', '1000', 1, 'alert_message到处时默认的每批次查询数量');
INSERT INTO `sys_config` VALUES (54, 'alert_notify_interval', '300', 1, '单位：s');
INSERT INTO `sys_config` VALUES (69, 'email_template', '<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"></head><body><div style=\"padding:40px 0; height:auto; min-height:100px; text-align:center;\"><span align=\"center\" style=\"margin:0 auto; min-width:290px; max-width:750px;\">${alertMessage.description}</span></div></body></html>', 1, '邮件模板');
INSERT INTO `sys_config` VALUES (71, 'test_email_send_template', '<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"></head><body><div style=\"padding:40px 0; height:auto; min-height:100px; text-align:center;\"><span align=\"center\" style=\"margin:0 auto; min-width:290px; max-width:750px;\">Nezha test sending email.</span></div></body></html>', 1, NULL);
INSERT INTO `sys_config` VALUES (72, 'reset_table_infos', '[\"Delete from alert_rule where build_in !=1 \",\"Delete from asset_asset\",\"Delete from cabinet\",\"Delete from visual_chart\",\"Delete from visual_chart_element\",\"Delete from monitor_endpoint\",\"Delete from monitor_endpoint_state\",\"Delete from dc\",\"Delete from maintenance_record\",\"Delete from asset_ping\",\"Delete from asset_model\",\"Delete from asset_brand\",\"Delete from asset_feature\",\"Delete from asset_field_value\",\"Delete from asset_field_group where build_in !=1 \",\"Delete from asset_field_meta where build_in !=1 \",\"Delete from monitor_module where build_in !=1 \",\"Delete from viausl_panel where build_in !=1 \",\"Delete from monitor_project where build_in !=1 \",\"Delete from project_topo\",\"Delete from prom_server\",\"Delete from snmp_credential\",\"Delete from sys_api_key\",\"Delete from snmp_mib\",\"Delete from terminal_cmd\",\"Delete from terminal_record\",\"Delete from terminal_session\",\"Delete from sys_log\",\"Delete from sys_user where username!=\'admin\'\",\"Delete from alert_notify_log\",\"Delete from alert_notify_method where build_in !=1 \",\"Delete from alert_silence_conf\",\"Delete from alert_silence_message_rel\",\"Delete from link where build_in !=1 \",\"Delete from visual_chart where build_in !=1 \",\"Delete from visual_chart_element where build_in !=1 \",\"Delete from visual_expression_tmpl where build_in !=1 \"]', 1, '清空项目数据信息');
INSERT INTO `sys_config` VALUES (107, 'default_cabinet_usize', '40', 1, '机柜默认U位值');
INSERT INTO `sys_config` VALUES (114, 'alert_export_header', '{\"cn\":{\"ID\":\"ID\",\"告警名称\":\"告警名称\",\"数据中心\":\"数据中心\",\"应用名称\":\"应用名称\",\"组件名称\":\"组件名称\",\"Endpoint\":\"Endpoint\",\"主机地址\":\"主机地址\",\"告警tag信息\":\"告警tag信息\",\"告警级别\":\"告警级别\",\"告警摘要\":\"告警摘要\",\"告警描述\":\"告警描述\",\"开始时间\":\"开始时间\",\"结束时间\":\"结束时间\",\"状态\":\"状态\"},\"en\":{\"ID\":\"ID\",\"Alert Name\":\"Alert Name\",\"Datacenter\":\"Datacenter\",\"Project\":\"Project\",\"Module\":\"Module\",\"Endpoint\":\"Endpoint\",\"Asset\":\"Asset\",\"Labels\":\"Labels\",\"Severity\":\"Severity\",\"Summary\":\"Summary\",\"Description\":\"Description\",\"StartAt\":\"StartAt\",\"EndAt\":\"EndAt\",\"State\":\"State\"}}', 1, 'alert_message导出表头');
INSERT INTO `sys_config` VALUES (118, 'map_center_config', '{\"longitude\":71.45,\"latitude\":51.16,\"zoom\":4,\"minZoom\":3,\"maxZoom\":7}', 1, '地图配置');
INSERT INTO `sys_config` VALUES (139, 'system_version', '21.04', 1, 'nezha系统版本');
INSERT INTO `sys_config` VALUES (142, 'query_max_series', '50', 1, '限制 prometheus query_range 接口 result 数组大小 区间范围[-1 - 1000] -1表示不限制');
INSERT INTO `sys_config` VALUES (143, 'export_max_line', '10000', 1, '告警导出最大行数');
INSERT INTO `sys_config` VALUES (149, 'develop_admin', 'admin', 1, NULL);
INSERT INTO `sys_config` VALUES (150, 'ldap_query_timeout', '3000', 1, 'ldap template 查询超时时间，单位 milliseconds 毫秒');
INSERT INTO `sys_config` VALUES (153, 'unsaved_change', 'on', 1, '未保存提示开关');
INSERT INTO `sys_config` VALUES (154, 'expretmpl_export_header', '{\"cn\":{\"*名称\":\"模板名称(必填) \\n示例: CPU利用率模板\",\"组名\":\"组名 \\n示例: base，默认值: default\",\"*表达式\":\"表达式内容(必填) \\n示例: node_cpu_usage(asset=\\\"{{asset.host}}\\\",device=\\\"{{device}}\\\")\",\"描述\":\"描述 \\n示例: node_export CPU利用率模板\"},\"en\":{\"*Name\":\"Template name (required) \\nExample: CPU utilization template\",\"Group Name\":\"Group Name \\nExample: base, Default value: default\",\"*Expression\":\"Expression content (required) Optional: node_cpu_usage(asset=\\\"{{asset.host}}\\\",device=\\\"{{device}}\\\")\",\"Remark\":\"Remark \\nExample: node_export CPU utilization template\"}}', 1, '表达式导入导出模板表头信息');
INSERT INTO `sys_config` VALUES (157, 'confagent_name', '{\"centos\":\"nz-agent-21.04-Release.x86_64.rpm\",\"ubuntu\":\"nz-agent-21.04-Release.x86_64.rpm\"}', 1, 'confagent 下载文件名称');
INSERT INTO `sys_config` VALUES (189, 'asset_asset_export_header', '{\"cn\":{\"*资产名称\":\"资产名称，唯一标识(必填) \\n示例: DELL服务器\",\"*SN码\":\"资产序列号，唯一标识(必填) \\n示例: A0420200101\",\"虚拟机宿主\":\"虚拟机宿主，当资产为虚拟机时必填 \\n示例: DELL物理机\",\"*管理ip\":\"主机地址(必填),IP格式 \\n示例: 172.0.0.1\",\"*品牌\":\"品牌(必填) \\n示例: DELL\",\"*型号\":\"型号(必填) \\n示例: DELL-D3\",\"*数据中心\":\"数据中心(必填) \\n示例: 北京电信中心机房\",\"机柜\":\"机柜 \\n示例: cabinet-001\",\"开始U位\":\"机柜开始位置 此信息一般和机柜结束位置联合使用 \\n示例: 1\",\"结束U位\":\"机柜结束位置 此信息一般与机柜开始位置联合使用 \\n示例: 2\",\"*资产状态\":\"资产状态 \\n示例: 在库\",\"*资产类型\":\"资产类型 \\n示例: 服务器\",\"购买日期\":\"购买日期 \\n示例: 2020/2/25\",\"属性信息\":\"资产属性信息,格式: key=value 如需填写多个请使用回车换行 \\n示例: NetworkCard=eth0 \\n user=root\",\"认证服务端口\":\"认证服务端口 \\n示例: 22\",\"认证方式\":\"认证方式,可选值: 1:用户名密码; 2: 密钥 \\n示例: 1\",\"用户名\":\"用户名, \\n示例:root\",\"密码\":\"密码 ,认证方式为1，记录登录密码; 认证方式为2,记录密钥密码.  \\n示例: pass\",\"密钥\":\"登录密钥 \\n示例: ssh-rsa CFdv7Jez6K...\",\"用户名提示\":\"TELNET用户名提示 \\n示例: login:\",\"密码提示\":\"TELNET密码提示 \\n示例: pass:\",\"SNMP凭证\":\"snmp凭证名称 \\n实例: system-snmpv2\"},\"en\":{\"*Asset Name\":\"Asset name, unique identifier (required) \\nExample: DELL server\",\"*Sn\":\"Asset serial number, unique identifier (required) \\nExample: A0420200101\",\"*Virtual machine host\":\"Virtual machine host, required when the asset is a virtual machine \\nExample: DELL physical machine\",\"*Management ip\":\"Host address (required), IP format \\nExample: 172.0.0.1\",\"*Brand\":\"Brand (required) \\nExample: DELL\",\"*Model\":\"Model (required) \\nExample: DELL-D3\",\"*DC\":\"Data Center (Required) \\nExample: Beijing Telecom Center Computer Room\",\"Cabinet\":\"Cabinet \\nExample: cabinet-001\",\"Cabinet Start\":\"Cabinet start position This information is generally used in conjunction with the cabinet end position \\nExample: 1\",\"Cabinet End\":\"Cabinet end position This information is generally used in conjunction with the cabinet start position \\nExample: 2\",\"*Asset state\":\"Asset status \\nExample: In stock\",\"*Asset type\":\"Asset Type\\nExample: Server\",\"Purchase date\":\"Purchase date \\nExample: 2020/2/25\",\"Field Information\":\"Asset field information, format: key=value If you need to fill in more than one, please use carriage return and line feed \\nExample: NetworkCard=eth0 \\n user=root\",\"Server port\":\"Server port \\nExample: 22\",\"Auth methed\":\"Auth methed,Optional values: 1: username and password; 2: key \\nExample: 1\",\"User name\":\"User name, \\nExample:root\",\"Password\":\"Password ,The auth method is 1, record the login password; the auth method is 2, record the key password.  \\nExample: pass\",\"PrivateKey\":\"PrivateKey \\nExample: ssh-rsa CFdv7Jez6K...\",\"User Tip\":\"TELNET username prompt \\nExample: login:\",\"Password Tip\":\"TELNET password prompt \\nExample: pass:\",\"SNMP credential\":\"SNMP credential name \\nExample: system-snmpv2\"}}', 1, NULL);
INSERT INTO `sys_config` VALUES (190, 'prom_cmd_tmpl', 'OPTION=\" --config.file=\'/opt/nezha/nz-agent/prometheus/prometheus.yml\' --web.listen-address=\'0.0.0.0:10091\' --web.enable-admin-api --storage.tsdb.path=\'/data/prometheusData\'  --web.enable-lifecycle --storage.tsdb.retention.time=${storageDays}d \"', 1, 'prometheus 启动参数模板');
INSERT INTO `sys_config` VALUES (192, 'prom_rule_tmpl', '{\r\n	\"groups\": [{\r\n		\"name\": \"alertRule\",\r\n		\"rules\": [\r\n            <#if endpointAlert>\r\n            {\r\n				\"alert\": \"0\",\r\n				\"expr\": \"up{endpoint!=\'\'}>-1\",\r\n				\"for\": \"30s\",\r\n				\"annotations\": {\r\n					\"summary\": \"{{$value}}\"\r\n				}\r\n			}, \r\n             </#if>\r\n             <#list ruleList as rule>\r\n            {\r\n				\"alert\":  ${rule.id?c},\r\n				\"expr\": \"(${rule.expr})${rule.operator}${rule.threshold}\",\r\n				\"for\": \"${rule.last}s\",\r\n				\"labels\": {\r\n					\"severity\": \"${rule.severity_name}\",\r\n					\"severity_id\": ${rule.severity_id?c}\r\n				},\r\n				\"annotations\": {\r\n					\"summary\": <#if rule.summary?default(\"\")?trim?length gt 1>\r\n                                \"${rule.summary}\",\r\n                      <#else>\"\",</#if>\r\n					\"description\": <#if rule.description?default(\"\")?trim?length gt 1>\r\n                                \"${rule.description}\"\r\n                      <#else>\"\"</#if>\r\n					\r\n				}\r\n			}\r\n			<#sep>,</#sep>\r\n		</#list>]\r\n	}]\r\n}', 1, 'prometheus rule文件模板');
INSERT INTO `sys_config` VALUES (194, 'monitor_endpoint_export_header', '{\"cn\":{\"*系统\":\"系统名称(必填) \\n示例: Nezha\",\"*组件\":\"组件名称(必填) \\n示例: node_exporter\",\"名称\":\"实例名称 \\n示例: node_exporter\",\"*资产名称\":\"资产名称(必填) \\n示例: A0420200101\",\"配置\":\"实例配置，JSON格式， \\n示例: {\\\"key\\\":\\\"value\\\"}\",\"启用\":\"启用状态（非必填） 可选 0:停用 1:启用,为空时默认为1 \\n示例: 1\"},\"en\":{\"*Project Name\":\"Project Name(Required) \\nExample: Nezha\",\"*Module Name\":\"Module Name(Required) \\nExample: node_exporter\",\"Name\":\"Endpoint name \\nExample: node_exporter\",\"*Asset Name\":\"Asset name(Required) \\nExample: A0420200101\",\"Configs\":\"Endpoint Configs，JSON format， \\nExample: {\\\"key\\\":\\\"value\\\"}\",\"Enabled\":\"Enable state (not required) Optional values 0: Disable 1: Enable, when empty, the default is 1 \\nExample: 1\"}}', 1, 'monitor endpoint 导入表头');
INSERT INTO `sys_config` VALUES (195, 'system_logo', '', 1, '系统 logo');
INSERT INTO `sys_config` VALUES (196, 'default_scrape_interval', '60', 1, '默认60s');
INSERT INTO `sys_config` VALUES (197, 'default_scrape_timeout', '30', 1, '默认30s');
INSERT INTO `sys_config` VALUES (198, 'terminal_timeout', '30', 1, '默认：30，单位：minute');
INSERT INTO `sys_config` VALUES (199, 'terminal_telnet_user_tip', 'ogin:', 1, '默认：ogin:');
INSERT INTO `sys_config` VALUES (200, 'terminal_telnet_pin_tip', 'assword:', 1, '默认：assword:');
INSERT INTO `sys_config` VALUES (201, 'terminal_record_local_retention', '365', 1, '默认：365，单位：天,record数据保留天数');
INSERT INTO `sys_config` VALUES (202, 'email_enable', 'off', 1, '是否启用邮件');
INSERT INTO `sys_config` VALUES (203, 'email_host', '', 1, '');
INSERT INTO `sys_config` VALUES (204, 'email_port', '25', 1, '');
INSERT INTO `sys_config` VALUES (205, 'email_timeout', '10', 1, '默认：10');
INSERT INTO `sys_config` VALUES (206, 'email_auth_account', '', 1, '');
INSERT INTO `sys_config` VALUES (207, 'email_auth_password', '', 1, '');
INSERT INTO `sys_config` VALUES (208, 'email_send_account', '', 1, '');
INSERT INTO `sys_config` VALUES (209, 'email_test_account', '', 1, '');
INSERT INTO `sys_config` VALUES (210, 'email_security_type', 'NONE', 1, 'NONE SSL TLS');
INSERT INTO `sys_config` VALUES (211, 'session_timeout', '30', 1, '默认：30，不小于 15 单位：minute');
INSERT INTO `sys_config` VALUES (215, 'asset_ping_from', '2', 1, '1: global\r\n\r\n2: per-datacenter\r\n\r\n默认：1');
INSERT INTO `sys_config` VALUES (216, 'prometheus_federation_enabled', '1', 1, '配置prometheus是否开启联邦模式，1：开启，0：关闭（界面禁用 添加选择 per-datacenter agent，所有endpoint 配置信息 hash 分布到 global prometheus）');
INSERT INTO `sys_config` VALUES (217, 'prom_yml_tmpl', '{\r\n    \"global\": {\r\n        \"scrape_interval\": \"${scrape_interval}s\",\r\n        \"scrape_timeout\": 	<#if scrape_timeout??> \r\n								\"${scrape_timeout}s\",\r\n						    <#else>\r\n								\"10s\",\r\n							</#if>\r\n        \"evaluation_interval\": \"${scrape_interval}s\"\r\n    },\r\n	\r\n	<#-- alert 配置 -->\r\n	<#if isAlert>\r\n	\"alerting\": {\r\n		\"alertmanagers\": [{\r\n			\"path_prefix\": \"${path_prefix}\",\r\n			\"static_configs\": [{\r\n				\"targets\": [\"${alert_url}\"]\r\n			}]\r\n		}],\r\n		\"alert_relabel_configs\": [{\r\n			\"regex\": \"instance\",\r\n			\"action\": \"labeldrop\"\r\n		}, {\r\n			\"regex\": \"replica\",\r\n			\"action\": \"labeldrop\"\r\n		}]\r\n	},\r\n	\"rule_files\": [\"rule.yml\"],\r\n	</#if>\r\n	\r\n	\r\n	\r\n	<#-- per-datacenter 配置 -->\r\n	<#if !isGlobal>\r\n    \"scrape_configs\": [\r\n		<#list endpointList as endpoint>\r\n			{\r\n				\"job_name\": ${endpoint.id?c},\r\n				\"metrics_path\": <#if endpoint.config.metrics_path?default(\"\")?trim?length gt 0>\r\n									\"${endpoint.config.metrics_path}\",\r\n								<#else>\r\n									<#if endpoint.module.type == \"http\">\r\n										\"/metrics\",\r\n									<#else>\r\n										\"/snmp\",\r\n									</#if>\r\n								</#if>\r\n				\"scrape_interval\": 	<#if endpoint.config.scrape_interval?default(\"\")?trim?length gt 0>\r\n										\"${endpoint.config.scrape_interval}s\",\r\n									<#else>\r\n										\"${scrape_interval}s\",\r\n									</#if>\r\n				\"scrape_timeout\": 	<#if endpoint.config.scrape_timeout?default(\"\")?trim?length gt 0>\r\n										\"${endpoint.config.scrape_timeout}s\",\r\n									<#else>\r\n										<#if scrape_timeout??> \r\n											\"${scrape_timeout}s\",\r\n										<#else>\r\n											\"10s\",\r\n										</#if>\r\n									</#if>\r\n\r\n\r\n\r\n				<#if endpoint.config.basic_auth??&& (endpoint.config.basic_auth?size >0)>\r\n					<#if endpoint.config.basic_auth.username?default(\"\")?trim?length gt 0 && endpoint.config.basic_auth.password?default(\"\")?trim?length gt 0>\r\n					\"basic_auth\":{\r\n						\"username\": \"${endpoint.config.basic_auth.username}\",\r\n						\"password\": \"${endpoint.config.basic_auth.password}\"\r\n						},\r\n					</#if>\r\n				</#if>\r\n				\r\n				\r\n				<#if endpoint.config.bearer_token??&&endpoint.config.bearer_token?trim?length gt 0>\r\n				\"authorization\":{\r\n						\"type\":\"Bearer\",\r\n						\"credentials\":\"${endpoint.config.bearer_token}\"\r\n					},\r\n				</#if>\r\n\r\n				\r\n				<#-- params 不为空且类型为 http ，添加 params -->\r\n				<#if endpoint.config.params??&& (endpoint.config.params?size >0) && endpoint.module.type == \"http\">\r\n				\"params\":{\r\n					<#list endpoint.config.params?keys as itemKey>\r\n					\"${itemKey}\":[\r\n						<#list endpoint.config.params[itemKey] as pojo>\r\n							\"${pojo}\"\r\n							<#sep>,</#sep>\r\n						</#list>\r\n						]\r\n						<#sep>,</#sep>\r\n					</#list>\r\n				},\r\n				<#elseif endpoint.module.type == \"snmp\">\r\n				<#-- 类型是 snmp ，组织 param 数据 ， 原来params弃用，因为 snmp_exporter 不支持其他参数 -->\r\n			   \"params\":{\r\n					\"target\":[ \"${endpoint.config.host}:${endpoint.config.port?c}\" ],\r\n					\"module\":[ \"${endpoint.module.name}\" ]\r\n				},\r\n				</#if>\r\n				\r\n				\"static_configs\": [{\r\n					\"targets\": [<#if endpoint.module.type == \"http\">\r\n									\"${endpoint.config.host}:${endpoint.config.port?c}\"\r\n								<#else>\r\n									\"127.0.0.1:19116\"\r\n								</#if>\r\n								],\r\n					\"labels\": {\r\n						\"datacenter\": 	\"${endpoint.dc.name}\",\r\n                        \"datacenter_id\":${endpoint.dc.id?c},\r\n						\r\n						\"project\": 		\"${endpoint.project.name}\",\r\n						\"project_id\":   ${endpoint.project.id?c},\r\n						\r\n						\"asset\": 		\"${endpoint.asset.name}\",\r\n						\"asset_id\":     ${endpoint.asset.id?c},\r\n						\r\n						\"module\": 		\"${endpoint.module.name}\",\r\n						\"module_id\":    ${endpoint.module.id?c},\r\n						\r\n						\"endpoint\":     \"${endpoint.name}\",\r\n						\"endpoint_id\":  ${endpoint.id?c},\r\n						\r\n						\"nz_agent_id\":  ${endpoint.promserver.id?c}\r\n												\r\n										\r\n										<#if endpoint.config.labels??&& (endpoint.config.labels?size >0)>\r\n											,\r\n										</#if>\r\n										<#-- 补充用户自定义 Labels -->\r\n										<#if endpoint.config.labels??&& (endpoint.config.labels?size >0)>\r\n											<#list endpoint.config.labels?keys as key>\r\n												<#if key != \"datacenter\" && key != \"datacenter_id\" && key != \"project\" && key != \"project_id\" && key != \"asset\" && key != \"asset_id\" && key != \"module\" && key != \"module_id\" && key != \"endpoint\" && key != \"endpoint_id\">\r\n													\"${key}\":\"${endpoint.config.labels[key]}\"<#sep>,</#sep>\r\n												</#if>\r\n											 </#list>\r\n										</#if>\r\n					}\r\n				}],\r\n				\"relabel_configs\": [\r\n					{\r\n						\"regex\": \"instance\",\r\n						\"action\": \"labeldrop\"\r\n					},\r\n					{\r\n						\"regex\": \"job\",\r\n						\"action\": \"labeldrop\"\r\n					}\r\n				],\r\n				\"metric_relabel_configs\": [\r\n					{\r\n						\"regex\": \"instance\",\r\n						\"action\": \"labeldrop\"\r\n					}\r\n				]\r\n			}\r\n			<#sep>,</#sep>\r\n		</#list>\r\n		\r\n		<#-- ping 配置 -->\r\n		\r\n		<#if endpointList??&&(endpointList?size>0)>\r\n		,\r\n		</#if>\r\n		\r\n		<#list assetList as asset>\r\n		{\r\n			\"job_name\": 	\"asset_ping_${asset.id?c}\",\r\n			\"metrics_path\": \"/probe\",\r\n			\"scrape_interval\": 	<#if asset.assetPingInterval?default(\"\")?trim?length gt 0>\r\n									\"${asset.assetPingInterval}s\",\r\n								<#else>\r\n									\"${scrape_interval}s\",\r\n								</#if>\r\n			\"scrape_timeout\": 	<#if asset.scrape_timeout?default(\"\")?trim?length gt 0>\r\n									\"${asset.scrape_timeout}s\",\r\n								<#else>\r\n									<#if scrape_timeout??> \r\n										\"${scrape_timeout}s\",\r\n									<#else>\r\n										\"10s\",\r\n									</#if>\r\n								</#if>\r\n            \"params\":	{\r\n				\"target\":[ \"${asset.manageIp}\" ],\r\n				\"module\":[ \"ping\" ]\r\n			},\r\n			\"static_configs\": [{\r\n				\"targets\": [\"127.0.0.1:19115\"],\r\n				\"labels\": {\r\n					\"datacenter\": 	\"${asset.dc.name}\",\r\n					\"host\": 	  	\"${asset.manageIp}\",\r\n					\"module\":       \"ping\",\r\n					\"source_ip\":    \"${asset.promserver.host}\",\r\n					\"nz_agent_id\":  ${asset.promserver.id?c}\r\n				}\r\n			}],\r\n			\"relabel_configs\": [\r\n					{\r\n						\"regex\": \"instance\",\r\n						\"action\": \"labeldrop\"\r\n					}\r\n				],\r\n			\"metric_relabel_configs\": [\r\n				{\r\n					\"regex\": \"instance\",\r\n					\"action\": \"labeldrop\"\r\n				}\r\n			]\r\n		}\r\n		<#sep>,</#sep>\r\n		</#list>\r\n    ]\r\n	<#else>\r\n	<#-- global 配置 -->\r\n	\"scrape_configs\": [\r\n		<#list jobList as jobPojo>\r\n			{\r\n			   \"job_name\": \"${jobPojo.job_name}\",\r\n				<#if jobPojo.isFederate> \r\n				\"honor_labels\": ${jobPojo.honor_labels},\r\n				\"params\": {\r\n					\"match[]\": [\r\n						<#-- 开启联邦 -->\r\n					    <#if jobPojo.fedeEnabled>\r\n							<#if jobPojo.scrapePing> \r\n								\"{job=~\\\"asset_ping_.+\\\",nz_agent_id != \\\"${jobPojo.excludeAgentId}\\\"}\"\r\n							<#else>\r\n								\"{__name__=~\\\".+\\\",nz_agent_id != \\\"${jobPojo.excludeAgentId}\\\"}\"\r\n							</#if>\r\n						<#else>\r\n							<#-- 联邦关闭，抓取所有指标即可 -->\r\n							\"{__name__=~\\\".+\\\",nz_agent_id != \\\"${jobPojo.excludeAgentId}\\\"}\"\r\n						</#if>\r\n					]\r\n				},\r\n				\"authorization\":{\r\n					\"type\":\"Bearer\",\r\n					\"credentials\":\"${jobPojo.token}\"\r\n				},\r\n				</#if>\r\n				\"metrics_path\": <#if jobPojo.metrics_path?default(\"\")?trim?length gt 0>\r\n									\"${jobPojo.metrics_path}\",\r\n								<#else>\r\n									\"/metrics\",\r\n								</#if>\r\n				\"static_configs\":[\r\n					<#list jobPojo.staticConfigList as staticConf >\r\n						{\r\n						  \"targets\": [\r\n								<#list staticConf.targets as targetConf>\r\n										\"${targetConf}\"<#sep>,</#sep>\r\n								</#list>\r\n							]\r\n						<#if staticConf.labels ??&& (staticConf.labels?size >0)>\r\n						   ,\r\n						   \"labels\": {\r\n								<#list staticConf.labels?keys as itemKey>\r\n									\"${itemKey}\":<#if staticConf.labels[itemKey]?default(\"\")?trim?length gt 0>\r\n													\"${staticConf.labels[itemKey]}\"\r\n												  <#else>\"\"</#if><#sep>,</#sep>\r\n								</#list>\r\n							} \r\n						</#if>\r\n						}<#sep>,</#sep>\r\n					</#list>\r\n					],\r\n				\"relabel_configs\": [\r\n					{\r\n						\"regex\": \"job\",\r\n						\"action\": \"labeldrop\"\r\n					}\r\n					<#if jobPojo.instanceRelabel> \r\n					,\r\n					{\r\n						\"regex\": \"instance\",\r\n						\"action\": \"labeldrop\"\r\n					}\r\n					</#if>\r\n				],\r\n				\"metric_relabel_configs\": [\r\n					{\r\n						\"regex\": \"instance\",\r\n						\"action\": \"labeldrop\"\r\n					}\r\n				]\r\n			}<#sep>,</#sep>\r\n		</#list>\r\n		\r\n		<#-- ping 配置 -->\r\n		\r\n		<#if jobList??&&(jobList?size>0)>\r\n		,\r\n		</#if>\r\n		\r\n		<#list assetList as asset>\r\n		{\r\n			\"job_name\": 	\"asset_ping_${asset.id?c}\",\r\n			\"metrics_path\": \"/probe\",\r\n			\"scrape_interval\": 	<#if asset.assetPingInterval?default(\"\")?trim?length gt 0>\r\n									\"${asset.assetPingInterval}s\",\r\n								<#else>\r\n									\"${scrape_interval}s\",\r\n								</#if>\r\n			\"scrape_timeout\": 	<#if asset.scrape_timeout?default(\"\")?trim?length gt 0>\r\n									\"${asset.scrape_timeout}s\",\r\n								<#else>\r\n									<#if scrape_timeout??> \r\n										\"${scrape_timeout}s\",\r\n									<#else>\r\n										\"10s\",\r\n									</#if>\r\n								</#if>\r\n            \"params\":	{\r\n				\"target\":[ \"${asset.manageIp}\" ],\r\n				\"module\":[ \"ping\" ]\r\n			},\r\n			\"static_configs\": [{\r\n				\"targets\": [\"127.0.0.1:19115\"],\r\n				\"labels\": {\r\n					\"datacenter\": 	\"${asset.dc.name}\",\r\n					\"host\": 	  	\"${asset.manageIp}\",\r\n					\"module\":       \"ping\",\r\n					\"source_ip\":    \"${asset.promserver.host}\",\r\n					\"nz_agent_id\":  ${asset.promserver.id?c}\r\n				}\r\n			}],\r\n			\"relabel_configs\": [\r\n					{\r\n						\"regex\": \"instance\",\r\n						\"action\": \"labeldrop\"\r\n					}\r\n				],\r\n			\"metric_relabel_configs\": [\r\n				{\r\n					\"regex\": \"instance\",\r\n					\"action\": \"labeldrop\"\r\n				}\r\n			]\r\n		}\r\n		<#sep>,</#sep>\r\n		</#list>\r\n		\r\n		\r\n		<#if ( jobList??&&(jobList?size>0)) ||  assetList??&&(assetList?size>0)>\r\n		,\r\n		</#if>\r\n		\r\n		<#list endpointList as endpoint>\r\n			{\r\n				\"job_name\": ${endpoint.id?c},\r\n				\"metrics_path\": <#if endpoint.config.metrics_path?default(\"\")?trim?length gt 0>\r\n									\"${endpoint.config.metrics_path}\",\r\n								<#else>\r\n									\"/metrics\",\r\n								</#if>\r\n				\"scrape_interval\": 	<#if endpoint.config.scrape_interval?default(\"\")?trim?length gt 0>\r\n										\"${endpoint.config.scrape_interval}s\",\r\n									<#else>\r\n										\"${scrape_interval}s\",\r\n									</#if>\r\n				\"scrape_timeout\": 	<#if endpoint.config.scrape_timeout?default(\"\")?trim?length gt 0>\r\n										\"${endpoint.config.scrape_timeout}s\",\r\n									<#else>\r\n										<#if scrape_timeout??> \r\n											\"${scrape_timeout}s\",\r\n										<#else>\r\n											\"10s\",\r\n										</#if>\r\n									</#if>\r\n\r\n\r\n\r\n				<#if endpoint.config.basic_auth??&& (endpoint.config.basic_auth?size >0)>\r\n					<#if endpoint.config.basic_auth.username?default(\"\")?trim?length gt 0 && endpoint.config.basic_auth.password?default(\"\")?trim?length gt 0>\r\n					\"basic_auth\":{\r\n						\"username\": \"${endpoint.config.basic_auth.username}\",\r\n						\"password\": \"${endpoint.config.basic_auth.password}\"\r\n						},\r\n					</#if>\r\n				</#if>\r\n				\r\n				\r\n				<#if endpoint.config.bearer_token??&&endpoint.config.bearer_token?trim?length gt 0>\r\n				\"authorization\":{\r\n						\"type\":\"Bearer\",\r\n						\"credentials\":\"${endpoint.config.bearer_token}\"\r\n					},\r\n				</#if>\r\n\r\n				\r\n				<#-- params 不为空且类型为 http ，添加 params -->\r\n				<#if endpoint.config.params??&& (endpoint.config.params?size >0) && endpoint.module.type == \"http\">\r\n				\"params\":{\r\n					<#list endpoint.config.params?keys as itemKey>\r\n					\"${itemKey}\":[\r\n						<#list endpoint.config.params[itemKey] as pojo>\r\n							\"${pojo}\"\r\n							<#sep>,</#sep>\r\n						</#list>\r\n						]\r\n						<#sep>,</#sep>\r\n					</#list>\r\n				},\r\n				<#elseif endpoint.module.type == \"snmp\">\r\n				<#-- 类型是 snmp ，组织 param 数据 ， 原来params弃用，因为 snmp_exporter 不支持其他参数 -->\r\n			   \"params\":{\r\n					\"target\":[ \"${endpoint.config.host}:${endpoint.config.port?c}\" ],\r\n					\"module\":[ \"${endpoint.module.name}\" ]\r\n				},\r\n				</#if>\r\n				\r\n				\"static_configs\": [{\r\n					\"targets\": [<#if endpoint.module.type == \"http\">\r\n									\"${endpoint.config.host}:${endpoint.config.port?c}\"\r\n								<#else>\r\n									\"127.0.0.1:19116\"\r\n								</#if>\r\n								],\r\n					\"labels\": {\r\n						\"datacenter\": 	\"${endpoint.dc.name}\",\r\n                        \"datacenter_id\":${endpoint.dc.id?c},\r\n						\r\n						\"project\": 		\"${endpoint.project.name}\",\r\n						\"project_id\":   ${endpoint.project.id?c},\r\n						\r\n						\"asset\": 		\"${endpoint.asset.name}\",\r\n						\"asset_id\":     ${endpoint.asset.id?c},\r\n						\r\n						\"module\": 		\"${endpoint.module.name}\",\r\n						\"module_id\":    ${endpoint.module.id?c},\r\n						\r\n						\"endpoint\":     \"${endpoint.name}\",\r\n						\"endpoint_id\":  ${endpoint.id?c},\r\n						\r\n						\"nz_agent_id\":  ${endpoint.promserver.id?c}\r\n												\r\n										\r\n										<#if endpoint.config.labels??&& (endpoint.config.labels?size >0)>\r\n											,\r\n										</#if>\r\n										<#-- 补充用户自定义 Labels -->\r\n										<#if endpoint.config.labels??&& (endpoint.config.labels?size >0)>\r\n											<#list endpoint.config.labels?keys as key>\r\n												<#if key != \"datacenter\" && key != \"datacenter_id\" && key != \"project\" && key != \"project_id\" && key != \"asset\" && key != \"asset_id\" && key != \"module\" && key != \"module_id\" && key != \"endpoint\" && key != \"endpoint_id\">\r\n													\"${key}\":\"${endpoint.config.labels[key]}\"<#sep>,</#sep>\r\n												</#if>\r\n											 </#list>\r\n										</#if>\r\n					}\r\n				}],\r\n				\"relabel_configs\": [\r\n					{\r\n						\"regex\": \"instance\",\r\n						\"action\": \"labeldrop\"\r\n					},\r\n					{\r\n						\"regex\": \"job\",\r\n						\"action\": \"labeldrop\"\r\n					}\r\n				],\r\n				\"metric_relabel_configs\": [\r\n					{\r\n						\"regex\": \"instance\",\r\n						\"action\": \"labeldrop\"\r\n					}\r\n				]\r\n			}\r\n			<#sep>,</#sep>\r\n		</#list>\r\n	\r\n	]\r\n	</#if>\r\n}', 1, 'prometheus config文件模板');
INSERT INTO `sys_config` VALUES (218, 'license', '<?xml version=\"1.0\" encoding=\"UTF-8\"?><license><product><maximumAssets>2000</maximumAssets><name>NEZHA</name><version>base</version></product><endDate>2021-06-03T14:52:43Z</endDate><signature><digestValue>3403d697d45c594a0d147d556d94ad087272830c</digestValue><signatureValue>35af0e65b3b6d6474db62359a42adf685413144e3ff57f3551f33c54614ed8eb4d705efd25ca873a73af08912242b74d3272cef149ac0b39ac4247e7263f9b395c1892aea7dce0d941ea14ee901f052192ce555c4c438510b31d6fef30b109125a42b19fbcf3190482adab2ffc64d01c31f3dd020a712fc97138c3c52df41f0116c56543faddba8a7393581dee612fec9646ca4edb6f46bf2894eeaff1436feb126ce7155dd8b5514038320b297fab40ca4ed0402eb71951e8b00185fe7fe7f0f0ba13de3d4dc9416a5762214bb8f2bfa7ec688bd119aeaa2a4c4153383b64a463892d63301070c295404538b54f8de7d36ca7fbf40011b9e057cbb1d4a15147</signatureValue></signature><contract>def@gz.com</contract><by>abc</by><genDate>2021-04-16T13:52:38Z</genDate><comment>test license</comment><tokens/><licenseId>111</licenseId><startDate>2021-03-03T14:52:43Z</startDate><customer>zhangsan</customer></license>', 1, '3403d697d45c594a0d147d556d94ad087272830c');
INSERT INTO `sys_config` VALUES (219, 'license_token', 'abbbb', 1, NULL);
INSERT INTO `sys_config` VALUES (220, 'rule_export_header', '{\"cn\":{\"*告警规则名称\":\"告警规则名称(必填) \\n示例: cpuAlert\",\"*运算表达式\":\"运算表达式(必填) \\n示例: 100-(avg(irate(node_cpu_seconds_total{mode=\'idle\'}[5m])) by(instance)* 100)\",\"*比较符号\":\"比较符号(必填) 可选: >,<,<=,>=,==,!= \\n示例: >\",\"*阈值\":\"阈值(必填) \\n示例: 10\",\"*单位\":\"单位(必填) 可选: \\nMisc: none,short,percent(0-100),percent(0.0-1.0),local format\\nData: bits,bytes,kilobytes,megabytes,gigabytes,terabytes,petabytes\\nDataRate: packets/sec,bits/sec,bytes/sec,kilobytes/sec,kilobits/sec,megabytes/sec,megabits/sec,gigabytes/sec,gigabits/sec,terabytes/sec,terabits/sec,petabytes/sec,petabits/sec\\nTime: Hertz(1/s),nanoseconds(ns),microseconds(us),milliseconds(ms),seconds(s),minutes(m),hours(h),days(d),YYYY-MM-DD HH:mm:ss,MM/DD/YYYY h:mm:ss a \\n示例: short\",\"*持续时间\":\"持续时间(必填),默认单位（秒） \\n示例:60\",\"*告警级别\":\"告警级别(必填) 可选 P1、P2、P3 \\n示例: P1\",\"*告警摘要\":\"告警摘要(必填) \\n示例: cpu告警\",\"告警详细描述\":\"告警详细描述 \\n示例: cpu使用率超过10%的告警规则\",\"告警通知人\":\"告警通知人，多个使用逗号分隔 \\n示例: admin / admin,root\",\"通知方式\":\"告警通知方式，告警通知人不为空时，必填.多个使用逗号分隔 \\n示例: email / email,telephone\"},\"en\":{\"*AlertName\":\"Alert rule name (required) \\nExample: cpuAlert\",\"*Expr\":\"Operation expression (required) \\nExample: 100-(avg(irate(node_cpu_seconds_total{mode=\'idle\')[5m])) by(instance)* 100)\",\"*Operator\":\"Operation symbol (required) Optional: >,<,<=,>=,==,!= \\nExample:>\",\"*Threshold\":\"Threshold (required) \\nExample: 10\",\"*Unit\":\"Unit (required) Optional: \\nMisc: none,short,percent(0-100),percent(0.0-1.0),local format\\nData: bits,bytes,kilobytes,megabytes,gigabytes,terabytes,petabytes\\nDataRate: packets/sec,bits/sec,bytes/sec,kilobytes/sec,kilobits/sec,megabytes/sec,megabits/sec,gigabytes/sec,gigabits/sec,terabytes/sec,terabits/sec,petabytes/sec,petabits/sec\\nTime: Hertz(1/s),nanoseconds(ns),microseconds(us),milliseconds(ms),seconds(s),minutes(m),hours(h),days(d),YYYY-MM-DD HH:mm:ss,MM/DD/YYYY h:mm:ss a \\nExample:short\",\"*Last\":\"Last (required), the default unit (seconds) \\nExample: 60\",\"*Severity\":\"Alert severity (required) optional P1, P2, P3 \\nExample: P1\",\"*Summary\":\"Alert summary \\nExample: Cpu warning\",\"Description\":\"Alert description \\nExample: Alarm rule for cpu usage exceeding 10%\",\"Receiver\":\"Alert receiver, separated by commas \\nExample: admin / admin,root\",\"Notification method\":\"Alarm notification method. When the receiver is not empty, it is required. Multiple use commas to separate \\nExample: email / email,telephone\"}}', 1, 'alert rule 导入导出表头');
INSERT INTO `sys_config` VALUES (221, 'asset_chart_tpl', '{\"name\":\"{{asset.sn}}\",\"type\":\"assetInfo\",\"span\":12,\"height\":4,\"unit\":2}', 1, 'asset添加时，默认chart配置');
INSERT INTO `sys_config` VALUES (222, 'endpoint_chart_tpl', '{\"name\":\"{{endpoint.name}}\",\"type\":\"endpointInfo\",\"span\":12,\"height\":4,\"unit\":2}', 1, 'endpoint添加时，默认chart配置');
INSERT INTO `sys_config` VALUES (223, 'ldap_timeout', '30', 1, '超时时间秒');
INSERT INTO `sys_config` VALUES (224, 'storage_local_retention', '15', 1, 'prometheus 本地数据保存天数 单位 d');
INSERT INTO `sys_config` VALUES (225, 'snmp_trap_listen_port', '162', 1, NULL);
INSERT INTO `sys_config` VALUES (226, 'i18n_mapping', '{\"chartType\":{\"en\":{\"line\":\"Line Chart\",\"bar\":\"Histogram\",\"table\":\"Table\",\"stackArea\":\"Stack Area\",\"singleStat\":\"SingleStat\",\"url\":\"URL\",\"alertList\":\"Alert list\",\"text\":\"Text\",\"group\":\"Group\",\"pie\":\"Pie\"},\"cn\":{\"line\":\"曲线图\",\"bar\":\"柱状图\",\"table\":\"表格\",\"stackArea\":\"堆叠区域图\",\"singleStat\":\"SingleStat\",\"url\":\"URL\",\"alertList\":\"告警列表\",\"text\":\"文本\",\"group\":\"图表组\",\"pie\":\"饼图\"}}}', 1, NULL);
INSERT INTO `sys_config` VALUES (227, 'chart_export_header', '{\"cn\":{\"*面板\":\"面板名称(必填) \\n示例: 系统面板/主面板  （使用 / 作为字符分隔，表示 父子节点关系）\",\"图表组名\":\"组名 \\n示例: default\",\"*图表名称\":\"图表名称(必填) \\n示例: CPU使用率\",\"*图表类型\":\"图表类型(必填) 可选: 曲线图,柱状图,表格,堆叠区域图,SingleStat,URL,告警列表,文本,图表组 \\n示例: 曲线图\",\"*宽度\":\"宽度(必填) 可选：1-12 \\n示例: 10\",\"*高度\":\"高度(必填),可选：1-12 \\n示例: 10\",\"单位\":\"单位(必填) 可选: \\nMisc: none,short,percent(0-100),percent(0.0-1.0),local format\\nData: bits,bytes,kilobytes,megabytes,gigabytes,terabytes,petabytes\\nDataRate: packets/sec,bits/sec,bytes/sec,kilobytes/sec,kilobits/sec,megabytes/sec,megabits/sec,gigabytes/sec,gigabits/sec,terabytes/sec,terabits/sec,petabytes/sec,petabits/sec\\nTime: Hertz(1/s),nanoseconds(ns),microseconds(us),milliseconds(ms),seconds(s),minutes(m),hours(h),days(d),YYYY-MM-DD HH:mm:ss,MM/DD/YYYY h:mm:ss a \\n示例: short\",\"*参数\":\"单位 根据type不同，内容不同，格式为JSON格式 \\n示例: {\\\"statistics\\\":\\\"average\\\"}\",\"元素\":\"当type=line，bar，stackArea时，包含 elements key，值为对象数组。\\n 示例:[{\\\"expression\\\":\\\"sum(up)\\\",\\\"legend\\\":\\\"test\\\"}]\"},\"en\":{\"*Panel Name\":\"Panel(Required) \\nExample: System panel/Main panel (use / as a character separation to indicate the relationship between parent and child nodes)\",\"Chart Group\":\"Group name \\nExample: default\",\"*Chart Title\":\"Chart title(Required) \\nExample: CPU usage\",\"*Chart Type\":\"Chart type(Required) Optional: Line Chart,Histogram,Table,Stack Area,SingleStat,URL,Alert list,Text,Group \\nExample: Line Chart\",\"*Width\":\"width(Required) Optional：1-12 \\nExample: 10\",\"*Height\":\"height(Required) Optional：1-12 \\nExample: 10\",\"Unit\":\"unit(Required) Optional: \\nMisc: none,short,percent(0-100),percent(0.0-1.0),local format\\nData: bits,bytes,kilobytes,megabytes,gigabytes,terabytes,petabytes\\nDataRate: packets/sec,bits/sec,bytes/sec,kilobytes/sec,kilobits/sec,megabytes/sec,megabits/sec,gigabytes/sec,gigabits/sec,terabytes/sec,terabits/sec,petabytes/sec,petabits/sec\\nTime: Hertz(1/s),nanoseconds(ns),microseconds(us),milliseconds(ms),seconds(s),minutes(m),hours(h),days(d),YYYY-MM-DD HH:mm:ss,MM/DD/YYYY h:mm:ss a \\nExample: short\",\"*Parameter\":\"Parameters match type，The format is JSON format \\nExample: {\\\"statistics\\\":\\\"average\\\"}\",\"Elements\":\"When the type is line, table, stackArea, singleStat, histogram, it contains elements key and the value is an object array.\\nExample:[{\\\"expression\\\":\\\"sum(up)\\\",\\\"legend\\\":\\\"test\\\"}]\"}}', 1, NULL);
INSERT INTO `sys_config` VALUES (228, 'asset_chart_export_header', '{\"cn\":{\"图表组名\":\"组名 \\n示例: default\",\"*图表名称\":\"图表名称(必填) \\n示例: CPU使用率\",\"*图表类型\":\"图表类型(必填) 可选: 曲线图,柱状图,表格,堆叠区域图,SingleStat,URL,告警列表,文本,图表组 \\n示例: 曲线图\",\"*宽度\":\"宽度(必填) 可选：1-12 \\n示例: 10\",\"*高度\":\"高度(必填),可选：1-12 \\n示例: 10\",\"单位\":\"单位(必填) 可选: \\nMisc: none,short,percent(0-100),percent(0.0-1.0),local format\\nData: bits,bytes,kilobytes,megabytes,gigabytes,terabytes,petabytes\\nDataRate: packets/sec,bits/sec,bytes/sec,kilobytes/sec,kilobits/sec,megabytes/sec,megabits/sec,gigabytes/sec,gigabits/sec,terabytes/sec,terabits/sec,petabytes/sec,petabits/sec\\nTime: Hertz(1/s),nanoseconds(ns),microseconds(us),milliseconds(ms),seconds(s),minutes(m),hours(h),days(d),YYYY-MM-DD HH:mm:ss,MM/DD/YYYY h:mm:ss a \\n示例: short\",\"*参数\":\"单位 根据type不同，内容不同，格式为JSON格式 \\n示例: {\\\"statistics\\\":\\\"average\\\"}\",\"元素\":\"当type=line，bar，stackArea时，包含 elements key，值为对象数组。\\n 示例:[{\\\"expression\\\":\\\"sum(up)\\\",\\\"legend\\\":\\\"test\\\"}]\"},\"en\":{\"Chart Group\":\"Group name \\nExample: default\",\"*Chart Title\":\"Chart title(Required) \\nExample: CPU usage\",\"*Chart Type\":\"Chart type(Required) Optional: Line Chart,Histogram,Table,Stack Area,SingleStat,URL,Alert list,Text,Group \\nExample: Line Chart\",\"*Width\":\"width(Required) Optional：1-12 \\nExample: 10\",\"*Height\":\"height(Required),Optional：1-12 \\nExample: 10\",\"Unit\":\"unit(Required) Optional: \\nMisc: none,short,percent(0-100),percent(0.0-1.0),local format\\nData: bits,bytes,kilobytes,megabytes,gigabytes,terabytes,petabytes\\nDataRate: packets/sec,bits/sec,bytes/sec,kilobytes/sec,kilobits/sec,megabytes/sec,megabits/sec,gigabytes/sec,gigabits/sec,terabytes/sec,terabits/sec,petabytes/sec,petabits/sec\\nTime: Hertz(1/s),nanoseconds(ns),microseconds(us),milliseconds(ms),seconds(s),minutes(m),hours(h),days(d),YYYY-MM-DD HH:mm:ss,MM/DD/YYYY h:mm:ss a \\nExample: short\",\"*Parameter\":\"Parameters match type，The format is JSON format \\nExample: {\\\"statistics\\\":\\\"average\\\"}\",\"Elements\":\"When the type is line, table, stackArea, singleStat, histogram, it contains elements key and the value is an object array.\\nExample:[{\\\"expression\\\":\\\"sum(up)\\\",\\\"legend\\\":\\\"test\\\"}]\"}}', 1, NULL);

-- ----------------------------
-- Table structure for sys_dict
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict`  (
  `id` bigint(19) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '字典名称',
  `type` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '字典类型',
  `code` int(11) NOT NULL COMMENT '字典码',
  `value` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '字典值',
  `i18n_code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT 'i18n code',
  `order_num` int(11) NOT NULL DEFAULT 0 COMMENT '排序',
  `remark` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `del_flag` tinyint(4) NOT NULL DEFAULT 0 COMMENT '删除标记  1：删除，0：未删除',
  `operator` bigint(19) NOT NULL COMMENT '操作人',
  `op_time` datetime(0) NOT NULL COMMENT '操作时间',
  `seq` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '全局唯一，用于导入撤销',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `type`(`type`, `code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '系统字典表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_dict
-- ----------------------------
INSERT INTO `sys_dict` VALUES (1, '图表类型', 'chartType', 1, 'line', '', 1, '折线图', 0, 1, '2019-12-13 17:53:19', NULL);
INSERT INTO `sys_dict` VALUES (2, '图表类型', 'chartType', 2, 'bar', '', 2, '条形图', 0, 1, '2019-12-13 17:53:19', NULL);
INSERT INTO `sys_dict` VALUES (3, '图表类型', 'chartType', 3, 'table', '', 3, '表格', 0, 1, '2019-12-13 17:53:19', NULL);
INSERT INTO `sys_dict` VALUES (4, '图表类型', 'chartType', 4, 'stackArea', '', 4, '堆栈面积图', 0, 1, '2019-12-13 17:53:19', NULL);
INSERT INTO `sys_dict` VALUES (5, '图表类型', 'chartType', 5, 'singleStat', '', 5, 'singleStat', 0, 1, '2020-03-31 10:57:30', NULL);
INSERT INTO `sys_dict` VALUES (6, '图表类型', 'chartType', 6, 'url', '', 6, 'url', 0, 1, '2020-03-31 10:57:33', NULL);
INSERT INTO `sys_dict` VALUES (7, '图表类型', 'chartType', 7, 'alertList', '', 0, '告警', 0, 1, '2020-05-27 17:50:33', NULL);
INSERT INTO `sys_dict` VALUES (8, '图表类型', 'chartType', 8, 'text', '', 8, '告警', 0, 1, '2020-05-27 17:50:33', NULL);
INSERT INTO `sys_dict` VALUES (9, '图表类型', 'chartType', 9, 'pie', '', 9, '饼图', 0, 1, '2021-02-05 10:57:33', NULL);
INSERT INTO `sys_dict` VALUES (10, '图表类型', 'chartType', 10, 'group', '', 10, '组', 0, 1, '2021-04-06 14:30:18', NULL);
INSERT INTO `sys_dict` VALUES (11, '表达式类型', 'exprType', 1, 'normal', '', 1, '简单表达式', 0, 1, '2019-12-13 17:53:19', NULL);
INSERT INTO `sys_dict` VALUES (12, '表达式类型', 'exprType', 2, 'expert', '', 2, '用户输入表达式', 0, 1, '2019-12-13 17:53:19', NULL);

-- ----------------------------
-- Table structure for sys_i18n
-- ----------------------------
DROP TABLE IF EXISTS `sys_i18n`;
CREATE TABLE `sys_i18n`  (
  `id` bigint(19) NOT NULL COMMENT '主键',
  `code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'i18n code',
  `lang` varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '语言',
  `value` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '翻译值',
  `remark` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '备注信息',
  `operator` bigint(19) NOT NULL COMMENT '操作人',
  `op_time` datetime(0) NOT NULL COMMENT '操作时间',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '0正常，1删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = 'i18n信息表' ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for sys_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `user_id` int(10) NOT NULL DEFAULT -1,
  `operation` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '用户操作',
  `type` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '可选值\r\n\r\nasset\r\n\r\nendpoint\r\n\r\nproject\r\n\r\nmodule\r\n\r\nalert rule\r\n\r\nalert message\r\n\r\npanel\r\n\r\nchart\r\n\r\ndatacenter\r\n\r\ncabinet',
  `opera_id` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1',
  `method` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '请求方法',
  `params` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '请求参数',
  `time` bigint(19) NULL DEFAULT NULL COMMENT '执行时长(毫秒)',
  `ip` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT 'IP地址',
  `state` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '请求结果',
  `response` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
  `exception` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
  `create_date` datetime(0) NOT NULL COMMENT '创建时间',
  `api_key_id` int(10) NULL DEFAULT NULL COMMENT '关联 sys_api_key.id',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_type`(`type`) USING BTREE,
  INDEX `idx_operation`(`operation`) USING BTREE,
  INDEX `idx_time`(`time`) USING BTREE,
  INDEX `idx_ip`(`ip`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '系统日志表' ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '唯一主键，前端根据此字段判断是否显示按钮或菜单',
  `i18n` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `parent_id` int(10) NOT NULL,
  `perms` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '多个用逗号分隔，后台根据此字段判断用户是否有权限访问接口',
  `type` int(1) NOT NULL COMMENT '1：菜单 2：按钮',
  `route` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `order_num` int(10) NOT NULL COMMENT '默认： 1',
  `icon` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT 'nz-icon nz-alert-add',
  `required` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '必要权限，如编辑按钮需要 勾选查看权限\r\n填写内容为 sys_menu.id，多个逗号分隔\r\n1,2',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2191 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, 'dashboard', 'dashboard', 'dashboard.title', 0, '', 1, '', 1, 'nz-icon nz-icon-menu-dashboard', '');
INSERT INTO `sys_menu` VALUES (2, 'overview', 'overview', 'dashboard.overview.title', 1, '', 1, '/overview', 1, '', '');
INSERT INTO `sys_menu` VALUES (3, 'panel', 'panel', 'dashboard.panel.title', 1, '', 1, '/panel', 2, '', '');
INSERT INTO `sys_menu` VALUES (4, 'explore', 'explore', 'dashboard.metricPreview.title', 1, '', 1, '/explore', 3, '', '');
INSERT INTO `sys_menu` VALUES (5, 'asset', 'asset', 'overall.asset', 2147, '', 1, '/asset', 1, '', '');
INSERT INTO `sys_menu` VALUES (6, 'monitor', 'monitor', 'overall.monitor', 0, '', 1, '', 3, 'nz-icon nz-icon-menu-project', '');
INSERT INTO `sys_menu` VALUES (7, 'alert', 'alert', 'alert.alert', 0, '', 1, '', 4, 'nz-icon nz-icon-menu-alert', '');
INSERT INTO `sys_menu` VALUES (8, 'alertMessage', 'alertMessage', 'alert.alertMessage', 7, '', 1, '/alertMessage', 1, '', '');
INSERT INTO `sys_menu` VALUES (9, 'alertRule', 'alertRule', 'alert.alertRule', 7, '', 1, '/alertRule', 2, '', '');
INSERT INTO `sys_menu` VALUES (10, 'settings', 'settings', 'overall.config', 0, '', 1, '', 5, 'nz-icon nz-icon-menu-setting', '');
INSERT INTO `sys_menu` VALUES (11, 'User', 'user', 'config.user.user', 2149, '', 1, '/user', 1, '', '');
INSERT INTO `sys_menu` VALUES (12, 'role', 'role', 'config.roles.roles', 2149, '', 1, '/roles', 2, '', '');
INSERT INTO `sys_menu` VALUES (13, 'Agent', 'agent', 'config.agent.agent.agent', 10, '', 1, '/agent', 3, '', '');
INSERT INTO `sys_menu` VALUES (14, 'dc', 'dc', 'config.dc.dc', 10, '', 1, '/dc', 1, '', '');
INSERT INTO `sys_menu` VALUES (15, 'model', 'model', 'config.model.model', 2148, '', 1, '/model', 1, '', '');
INSERT INTO `sys_menu` VALUES (16, 'SNMP', 'snmp', 'SNMP', 10, '', 1, '/snmp', 6, '', '');
INSERT INTO `sys_menu` VALUES (17, 'system', 'system', 'config.system.system', 10, '', 1, '/system', 7, '', '');
INSERT INTO `sys_menu` VALUES (18, 'Terminal log', 'terminalLog', 'config.terminallog.terminallog', 2149, '', 1, '/terminalLog', 12, '', '');
INSERT INTO `sys_menu` VALUES (19, 'operationLog', 'operationLog', 'config.operationlog.operationlog', 2149, '', 1, '/operationLog', 9, '', '');
INSERT INTO `sys_menu` VALUES (20, 'about', 'about', 'overall.about', 0, '', 1, '/about', 99, 'nz-icon nz-icon-about', '');
INSERT INTO `sys_menu` VALUES (301, 'Add', 'panel_add', 'buttons.add', 3, 'visual:panel:save', 2, '', 10, '', '');
INSERT INTO `sys_menu` VALUES (302, 'Edit panel', 'panel_edit', 'buttons.edit', 3, 'visual:panel:update', 2, '', 15, '', '');
INSERT INTO `sys_menu` VALUES (303, 'Delete panel', 'panel_delete', 'buttons.delete', 3, 'visual:panel:delete', 2, '', 20, '', '');
INSERT INTO `sys_menu` VALUES (305, 'Add chart', 'panel_chart_add', 'buttons.panel.addChart', 3, 'visual:chart:save', 2, '', 30, '', '');
INSERT INTO `sys_menu` VALUES (308, 'Edit chart', 'panel_chart_edit', 'buttons.panel.editChart', 3, 'visual:chart:update', 2, '', 45, '', '');
INSERT INTO `sys_menu` VALUES (310, 'Delete chart', 'panel_chart_delete', 'buttons.panel.deleteChart', 3, 'visual:chart:delete', 2, '', 55, '', '');
INSERT INTO `sys_menu` VALUES (312, 'panel_view', 'panel_view', 'buttons.view', 3, '', 2, '', 5, '', '');
INSERT INTO `sys_menu` VALUES (501, 'Add asset', 'asset_add', 'buttons.add', 5, 'asset:save', 2, '', 10, '', '');
INSERT INTO `sys_menu` VALUES (502, 'Edit asset', 'asset_edit', 'buttons.edit', 5, 'asset:update', 2, '', 15, '', '');
INSERT INTO `sys_menu` VALUES (504, 'asset_delete', 'asset_delete', 'buttons.delete', 5, 'asset:delete', 2, '', 25, '', '');
INSERT INTO `sys_menu` VALUES (508, 'asset_connect', 'asset_connect', 'buttons.asset.connect', 5, '', 2, '', 45, '', '');
INSERT INTO `sys_menu` VALUES (529, 'asset_view', 'asset_view', 'buttons.asset.view', 5, '', 2, '', 5, '', '');
INSERT INTO `sys_menu` VALUES (601, 'project_edit', 'project_edit', 'buttons.edit', 2144, 'project:update', 2, '', 15, '', '');
INSERT INTO `sys_menu` VALUES (603, 'project_delete', 'project_delete', 'buttons.delete', 2144, 'project:delete', 2, '', 20, '', '');
INSERT INTO `sys_menu` VALUES (606, 'monitor_module_edit', 'monitor_module_edit', 'buttons.edit', 2145, 'module:update', 2, '', 15, '', '');
INSERT INTO `sys_menu` VALUES (608, 'monitor_module_delete', 'monitor_module_delete', 'buttons.delete', 2145, 'module:delete', 2, '', 20, '', '');
INSERT INTO `sys_menu` VALUES (609, 'monitor_endpoint_add', 'monitor_endpoint_add', 'buttons.add', 2146, 'endpoint:save', 2, '', 10, '', '');
INSERT INTO `sys_menu` VALUES (610, 'monitor_endpoint_edit', 'monitor_endpoint_edit', 'buttons.edit', 2146, 'endpoint:update', 2, '', 15, '', '');
INSERT INTO `sys_menu` VALUES (611, 'monitor_endpoint_delete', 'monitor_endpoint_delete', 'buttons.delete', 2146, 'endpoint:delete', 2, '', 20, '', '');
INSERT INTO `sys_menu` VALUES (622, 'monitor_module_add', 'monitor_module_add', 'buttons.add', 2145, 'module:save', 2, '', 10, '', '');
INSERT INTO `sys_menu` VALUES (623, 'project_view', 'project_view', 'buttons.view', 2144, '', 2, '', 5, '', '');
INSERT INTO `sys_menu` VALUES (801, 'Expire message', 'alertMessage_expired', 'buttons.expired', 8, '', 2, '', 10, '', '');
INSERT INTO `sys_menu` VALUES (803, 'View message', 'alertMessage_view', 'buttons.view', 8, '', 2, '', 5, '', '');
INSERT INTO `sys_menu` VALUES (901, 'Add rule', 'alertRule_add', 'buttons.add', 9, 'alert:rule:save', 2, '', 10, '', '');
INSERT INTO `sys_menu` VALUES (902, 'Edit rule', 'alertRule_edit', 'buttons.edit', 9, 'alert:rule:update', 2, '', 15, '', '');
INSERT INTO `sys_menu` VALUES (903, 'Delete rule', 'alertRule_delete', 'buttons.delete', 9, 'alert:rule:delete', 2, '', 20, '', '');
INSERT INTO `sys_menu` VALUES (910, 'View rule', 'alertRule_view', 'buttons.view', 9, '', 2, '', 5, '', '');
INSERT INTO `sys_menu` VALUES (1101, 'Add user', 'user_add', 'buttons.add', 11, 'sys:user:save', 2, '', 10, '', '');
INSERT INTO `sys_menu` VALUES (1102, 'Edit user', 'user_edit', 'buttons.edit', 11, 'sys:user:update', 2, '', 15, '', '');
INSERT INTO `sys_menu` VALUES (1103, 'Delete user', 'user_delete', 'buttons.delete', 11, 'sys:user:delete', 2, '', 20, '', '');
INSERT INTO `sys_menu` VALUES (1105, 'View user', 'user_view', 'buttons.view', 11, '', 2, '', 5, '', '');
INSERT INTO `sys_menu` VALUES (1201, 'Add role', 'role_add', 'buttons.add', 12, 'sys:role:save', 2, '', 10, '', '');
INSERT INTO `sys_menu` VALUES (1202, 'Edit role', 'role_edit', 'buttons.edit', 12, 'sys:role:update', 2, '', 15, '', '');
INSERT INTO `sys_menu` VALUES (1203, 'Delete role', 'role_delete', 'buttons.delete', 12, 'sys:role:delete', 2, '', 20, '', '');
INSERT INTO `sys_menu` VALUES (1205, 'View role', 'role_view', 'buttons.view', 12, '', 2, '', 5, '', '');
INSERT INTO `sys_menu` VALUES (1301, 'Add agent', 'agent_add', 'buttons.add', 13, 'promserver:save', 2, '', 10, '', '');
INSERT INTO `sys_menu` VALUES (1302, 'Edit agent', 'agent_edit', 'buttons.edit', 13, 'promserver:update', 2, '', 15, '', '');
INSERT INTO `sys_menu` VALUES (1303, 'Delete agent', 'agent_delete', 'buttons.delete', 13, 'promserver:delete', 2, '', 20, '', '');
INSERT INTO `sys_menu` VALUES (1305, 'View agent', 'agent_view', 'buttons.view', 13, '', 2, '', 5, '', '');
INSERT INTO `sys_menu` VALUES (1401, 'Add data center', 'dc_add', 'buttons.add', 14, 'dc:save,cabinet:save', 2, '', 10, '', '');
INSERT INTO `sys_menu` VALUES (1402, 'Edit data center', 'dc_edit', 'buttons.edit', 14, 'dc:update,cabinet:update', 2, '', 15, '', '');
INSERT INTO `sys_menu` VALUES (1403, 'Delete data center', 'dc_delete', 'buttons.delete', 14, 'dc:delete,cabinet:delete', 2, '', 20, '', '');
INSERT INTO `sys_menu` VALUES (1411, 'View data center', 'dc_view', 'buttons.view', 14, '', 2, '', 5, '', '');
INSERT INTO `sys_menu` VALUES (1501, 'Add model', 'model_add', 'buttons.add', 15, 'asset:model:save,asset:brand:save', 2, '', 10, '', '');
INSERT INTO `sys_menu` VALUES (1502, 'Edit model', 'model_edit', 'buttons.edit', 15, 'asset:model:update,asset:brand:update', 2, '', 15, '', '');
INSERT INTO `sys_menu` VALUES (1503, 'Delete model', 'model_delete', 'buttons.delete', 15, 'asset:model:delete,asset:brand:delete', 2, '', 20, '', '');
INSERT INTO `sys_menu` VALUES (1514, 'View model', 'model_view', 'buttons.view', 15, '', 2, '', 5, '', '');
INSERT INTO `sys_menu` VALUES (1602, 'Add MIB file', 'snmp_mibFile_add', 'buttons.snmp.add', 16, 'sys:mib:save', 2, '', 5, '', '');
INSERT INTO `sys_menu` VALUES (1603, 'Edit MIB file', 'snmp_mibFile_edit', 'buttons.snmp.edit', 16, 'sys:mib:update', 2, '', 15, '', '');
INSERT INTO `sys_menu` VALUES (1604, 'Delete MIB file', 'snmp_mibFile_delete', 'buttons.snmp.delete', 16, 'sys:mib:delete', 2, '', 20, '', '');
INSERT INTO `sys_menu` VALUES (1607, 'View MIB file', 'snmp_mibFile_view', 'buttons.snmp.view', 16, '', 2, '', 1, '', '');
INSERT INTO `sys_menu` VALUES (1701, '基础设置', 'system_basic_save', 'buttons.system.basic', 17, 'sys:config:update', 2, '', 1, '', '');
INSERT INTO `sys_menu` VALUES (1702, 'system_email_save', 'system_email_save', 'buttons.system.email', 17, 'sys:config:update', 2, '', 0, '', '');
INSERT INTO `sys_menu` VALUES (1703, 'system_terminal_save', 'system_terminal_save', 'buttons.system.terminal', 17, 'sys:config:update', 2, '', 0, '', '');
INSERT INTO `sys_menu` VALUES (1704, 'system_ldap_save', 'system_ldap_save', 'buttons.system.ldap', 17, 'sys:config:update', 2, '', 0, '', '');
INSERT INTO `sys_menu` VALUES (1705, 'system_notification_add', 'system_notification_add', 'buttons.add', 2159, 'alert:notify:save', 2, '', 10, '', '');
INSERT INTO `sys_menu` VALUES (1706, 'system_notification_edit', 'system_notification_edit', 'buttons.edit', 2159, 'alert:notify:update', 2, '', 15, '', '');
INSERT INTO `sys_menu` VALUES (1707, 'system_notification_view', 'system_notification_view', 'buttons.view', 2159, '', 2, '', 5, '', '');
INSERT INTO `sys_menu` VALUES (1708, 'system_notification_delete', 'system_notification_delete', 'buttons.delete', 2159, 'alert:notify:delete', 2, '', 20, '', '');
INSERT INTO `sys_menu` VALUES (1709, 'system_link_view', 'system_link_view', 'buttons.view', 2160, '', 2, '', 5, '', '');
INSERT INTO `sys_menu` VALUES (1710, 'system_link_edit', 'system_link_edit', 'buttons.edit', 2160, 'sys:link:update', 2, '', 15, '', '');
INSERT INTO `sys_menu` VALUES (1711, 'system_link_add', 'system_link_add', 'buttons.add', 2160, 'sys:link:save', 2, '', 10, '', '');
INSERT INTO `sys_menu` VALUES (1712, 'system_link_delete', 'system_link_delete', 'buttons.delete', 2160, 'sys:link:delete', 2, '', 20, '', '');
INSERT INTO `sys_menu` VALUES (2114, 'alertSilence', 'alertSilence_list', 'alert.silence.silence', 7, '', 1, '/alertSilence', 3, '', '');
INSERT INTO `sys_menu` VALUES (2115, 'View alert silence', 'alertSilence_view', 'buttons.view', 2114, '', 2, '', 3, '', '');
INSERT INTO `sys_menu` VALUES (2116, 'Add alert silence', 'alertSilence_add', 'buttons.add', 2114, 'alert:silence:save', 2, '', 5, '', '');
INSERT INTO `sys_menu` VALUES (2117, 'Edit alert silence', 'alertSilence_edit', 'buttons.edit', 2114, 'alert:silence:update', 2, '', 6, '', '');
INSERT INTO `sys_menu` VALUES (2118, 'expressionTemplate', 'exprTemp', 'config.exprTemp.exprTemp', 2180, '', 3, '/exprTemp', 6, '', '');
INSERT INTO `sys_menu` VALUES (2120, 'Edit expression template', 'expressionTemplate_edit', 'buttons.edit', 2118, 'expression:tmpl:update', 2, '', 15, '', '');
INSERT INTO `sys_menu` VALUES (2121, 'Delete expression template', 'expressionTemplate_delete', 'buttons.delete', 2118, 'expression:tmpl:delete', 2, '', 20, '', '');
INSERT INTO `sys_menu` VALUES (2122, 'View expression template', 'expressionTemplate_view', 'buttons.view', 2118, '', 2, '', 1, '', '');
INSERT INTO `sys_menu` VALUES (2123, 'Add expression template', 'expressionTemplate_add', 'buttons.add', 2118, 'expression:tmpl:save', 2, '', 10, '', '');
INSERT INTO `sys_menu` VALUES (2127, 'asset type to add', 'assetType_toAdd', 'buttons.assetType.toAdd', 2125, 'asset:type:save', 2, '', 1, '', '');
INSERT INTO `sys_menu` VALUES (2128, 'Asset type view', 'assetType_view', 'buttons.assetType.view', 2125, '', 2, '', 2, '', '');
INSERT INTO `sys_menu` VALUES (2129, 'Asset type to edit', 'assetType_toEdit', 'buttons.assetType.toEdit', 2125, 'asset:type:update', 2, '', 3, '', '');
INSERT INTO `sys_menu` VALUES (2130, 'Asset type delete', 'assetType_delete', 'buttons.assetType.delete', 2125, 'asset:type:delete', 2, '', 4, '', '');
INSERT INTO `sys_menu` VALUES (2131, 'Asset state view', 'assetState_view', 'buttons.assetState.view', 2126, '', 2, '', 1, '', '');
INSERT INTO `sys_menu` VALUES (2132, 'Asset state to add', 'assetState_toAdd', 'buttons.assetState.toAdd', 2126, 'asset:state:save', 2, '', 2, '', '');
INSERT INTO `sys_menu` VALUES (2133, 'Asset state to edit', 'assetState_toEdit', 'buttons.assetState.toEdit', 2126, 'asset:state:update', 2, '', 3, '', '');
INSERT INTO `sys_menu` VALUES (2134, 'Asset state delete', 'assetState_delete', 'buttons.assetState.delete', 2126, 'asset:state:delete', 2, '', 4, '', '');
INSERT INTO `sys_menu` VALUES (2136, 'Asset label', 'assetLabel', 'config.assetLabel.assetLabel', 2148, '', 1, '/assetLabel', 2, '', '');
INSERT INTO `sys_menu` VALUES (2137, 'Add asset label', 'asset_label_add', 'buttons.add', 2136, 'field:meta:save,field:group:save', 2, '', 1, '', '');
INSERT INTO `sys_menu` VALUES (2138, 'Edit asset label', 'asset_label_edit', 'buttons.edit', 2136, 'field:meta:update,field:group:update', 2, '', 5, '', '');
INSERT INTO `sys_menu` VALUES (2139, 'Delete asset label', 'asset_label_delete', 'buttons.delete', 2136, 'field:meta:delete,field:group:delete', 2, '', 10, '', '');
INSERT INTO `sys_menu` VALUES (2144, 'project', 'project', 'overall.project', 6, '', 1, '/monitor/project', 1, '', '');
INSERT INTO `sys_menu` VALUES (2145, 'module', 'module', 'project.module.module', 6, '', 1, '/monitor/module', 5, '', '');
INSERT INTO `sys_menu` VALUES (2146, 'endpoint', 'endpoint', 'project.endpoint.endpoint', 6, '', 1, '/monitor/endpoint', 10, '', '');
INSERT INTO `sys_menu` VALUES (2147, 'Assets', 'assets', 'overall.asset', 0, '', 1, '', 2, 'nz-icon nz-icon-menu-assets', '');
INSERT INTO `sys_menu` VALUES (2148, 'Configuration', 'configuration', 'overall.configuration', 2147, '', 1, '', 2, '', '');
INSERT INTO `sys_menu` VALUES (2149, 'Administration', 'administration', 'overall.administration', 0, '', 1, '', 50, 'nz-icon nz-icon-personal-circle', '');
INSERT INTO `sys_menu` VALUES (2150, 'Kill terminal', 'terminal_kill', 'buttons.terminalLog.kill', 18, 'terminal:kill', 2, '', 1, '', '');
INSERT INTO `sys_menu` VALUES (2151, 'Monitor terminal', 'terminal_monitor', 'buttons.terminalLog.monitor', 18, '', 2, '', 5, '', '');
INSERT INTO `sys_menu` VALUES (2153, 'MIB browser', 'mibBrowser', 'buttons.snmp.mibBrowser', 16, '', 2, '', 20, '', '');
INSERT INTO `sys_menu` VALUES (2155, 'View credential', 'snmp_credential_view', 'buttons.credential.view', 16, '', 2, '', 25, '', '');
INSERT INTO `sys_menu` VALUES (2156, 'Add credential', 'credential_add', 'buttons.credential.add', 16, 'snmp:credential:save', 2, '', 30, '', '');
INSERT INTO `sys_menu` VALUES (2157, 'Edit credential', 'credential_edit', 'buttons.credential.edit', 16, 'snmp:credential:update', 2, '', 35, '', '');
INSERT INTO `sys_menu` VALUES (2158, 'Delete credential', 'credential_delete', 'buttons.credential.delete', 16, 'snmp:credential:delete', 2, '', 40, '', '');
INSERT INTO `sys_menu` VALUES (2159, 'Notification', 'notification', 'system.notification.notification', 17, '', 3, '', 1, '', '');
INSERT INTO `sys_menu` VALUES (2160, 'Link', 'link', 'config.system.link.link', 17, '', 3, '', 10, '', '');
INSERT INTO `sys_menu` VALUES (2161, 'Add project', 'project_add', 'buttons.add', 2144, 'project:save', 2, '', 10, '', '');
INSERT INTO `sys_menu` VALUES (2162, 'View module', 'monitor_module_view', 'buttons.view', 2145, '', 2, '', 5, '', '');
INSERT INTO `sys_menu` VALUES (2163, 'Expire', 'alertSilence_expire', 'buttons.expire', 2114, 'alert:silence:delete', 2, '', 20, '', '');
INSERT INTO `sys_menu` VALUES (2164, 'Browser', 'snmp_mib_browser', 'config.mib.mibBrowser', 16, '', 3, '/mib/browser', 2, '', '');
INSERT INTO `sys_menu` VALUES (2165, 'Mib file', 'snmp_mib_file', 'config.mib.mib', 16, '', 3, '/mib/file', 1, '', '');
INSERT INTO `sys_menu` VALUES (2166, 'Credentials', 'snmp_mib_credentials', 'config.mib.credentials', 16, '', 3, '/mib/credentials', 3, '', '');
INSERT INTO `sys_menu` VALUES (2169, 'projectTopoIconList', 'topo_icon_list', 'project.topo.icon.list', 2144, '', 2, '', 1, '', '');
INSERT INTO `sys_menu` VALUES (2170, 'projectTopoIconDelete', 'topo_icon_delete', 'project.topo.icon.delete', 2144, 'project:topo:icon:delete', 2, '', 1, '', '');
INSERT INTO `sys_menu` VALUES (2171, 'project:topo:icon:save', 'topo_icon_save', 'project.topo.icon.save', 2144, 'project:topo:icon:save', 2, '', 1, '', '');
INSERT INTO `sys_menu` VALUES (2172, 'projectTopoIconInfo', 'topo_icon_info', 'project.topo.icon.info', 2144, '', 2, '', 1, '', '');
INSERT INTO `sys_menu` VALUES (2173, 'projectTopoList', 'topo_list', 'project.topo.list', 2144, '', 2, '', 1, '', '');
INSERT INTO `sys_menu` VALUES (2174, 'projectTopoUpdate', 'project_topo_update', 'project.topo.update', 2144, 'project:topo:update', 2, '', 1, '', '');
INSERT INTO `sys_menu` VALUES (2176, 'endpointList', 'endpoint_list', 'peoject.endpoint.list', 2146, '', 2, '', 1, '', '');
INSERT INTO `sys_menu` VALUES (2180, 'template', 'template', 'overall.template', 10, '', 1, '/template', 4, '', '');
INSERT INTO `sys_menu` VALUES (2181, 'exprTemp', 'config.exprTemp', 'overall.exprTemp', 2180, '', 3, '', 1, '', '');
INSERT INTO `sys_menu` VALUES (2182, 'bbuttons.system.reset', 'system_reset_reset', 'system.reset.reset', 17, 'sys:config:reset', 2, '', 1, '', '');
INSERT INTO `sys_menu` VALUES (2185, 'addChartTemp', 'addChartTemp', 'dashboard.addChartTemp', 3, 'visual:chart:save', 2, '', 1, '', '');
INSERT INTO `sys_menu` VALUES (2187, 'apiKey', 'apiKey', 'system.apikey.apiKey', 17, '', 3, '', 1, '', '');
INSERT INTO `sys_menu` VALUES (2188, 'buttons.system.apikey.add', 'system_apiKey_add', 'buttons.system.apikey.add', 17, 'sys:apiKey:save', 2, '', 1, '', '');
INSERT INTO `sys_menu` VALUES (2189, 'buttons.system.apiKey.edit', 'system_apiKey_edit', 'buttons.system.apiKey.edit', 17, 'sys:apiKey:save', 2, '', 2, '', '');
INSERT INTO `sys_menu` VALUES (2190, 'buttons.system.apiKey.delete', 'system_apiKey_delete', 'buttons.system.apiKey.delete', 17, 'sys:apiKey:delete', 2, '', 3, '', '');

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `i18n` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `build_in` int(1) NOT NULL DEFAULT 0 COMMENT '可选值：\r\n\r\n0：非内置\r\n\r\n1：内置',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, 'admin', 'admin', 'The system built-in administrator role', 1);
INSERT INTO `sys_role` VALUES (2, 'common', 'common', 'The system built-in common user role', 1);

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `role_id` int(10) NOT NULL,
  `menu_id` int(10) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 256 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (1, 1, 1);
INSERT INTO `sys_role_menu` VALUES (2, 1, 2);
INSERT INTO `sys_role_menu` VALUES (3, 1, 3);
INSERT INTO `sys_role_menu` VALUES (4, 1, 4);
INSERT INTO `sys_role_menu` VALUES (5, 1, 5);
INSERT INTO `sys_role_menu` VALUES (6, 1, 6);
INSERT INTO `sys_role_menu` VALUES (7, 1, 7);
INSERT INTO `sys_role_menu` VALUES (8, 1, 8);
INSERT INTO `sys_role_menu` VALUES (9, 1, 9);
INSERT INTO `sys_role_menu` VALUES (10, 1, 10);
INSERT INTO `sys_role_menu` VALUES (11, 1, 11);
INSERT INTO `sys_role_menu` VALUES (12, 1, 12);
INSERT INTO `sys_role_menu` VALUES (13, 1, 13);
INSERT INTO `sys_role_menu` VALUES (14, 1, 14);
INSERT INTO `sys_role_menu` VALUES (15, 1, 15);
INSERT INTO `sys_role_menu` VALUES (16, 1, 16);
INSERT INTO `sys_role_menu` VALUES (17, 1, 17);
INSERT INTO `sys_role_menu` VALUES (18, 1, 18);
INSERT INTO `sys_role_menu` VALUES (19, 1, 19);
INSERT INTO `sys_role_menu` VALUES (20, 1, 20);
INSERT INTO `sys_role_menu` VALUES (21, 1, 301);
INSERT INTO `sys_role_menu` VALUES (22, 1, 302);
INSERT INTO `sys_role_menu` VALUES (23, 1, 303);
INSERT INTO `sys_role_menu` VALUES (24, 1, 305);
INSERT INTO `sys_role_menu` VALUES (25, 1, 308);
INSERT INTO `sys_role_menu` VALUES (26, 1, 310);
INSERT INTO `sys_role_menu` VALUES (27, 1, 312);
INSERT INTO `sys_role_menu` VALUES (28, 1, 501);
INSERT INTO `sys_role_menu` VALUES (29, 1, 502);
INSERT INTO `sys_role_menu` VALUES (30, 1, 504);
INSERT INTO `sys_role_menu` VALUES (31, 1, 508);
INSERT INTO `sys_role_menu` VALUES (32, 1, 529);
INSERT INTO `sys_role_menu` VALUES (33, 1, 601);
INSERT INTO `sys_role_menu` VALUES (34, 1, 603);
INSERT INTO `sys_role_menu` VALUES (35, 1, 606);
INSERT INTO `sys_role_menu` VALUES (36, 1, 608);
INSERT INTO `sys_role_menu` VALUES (37, 1, 609);
INSERT INTO `sys_role_menu` VALUES (38, 1, 610);
INSERT INTO `sys_role_menu` VALUES (39, 1, 611);
INSERT INTO `sys_role_menu` VALUES (40, 1, 622);
INSERT INTO `sys_role_menu` VALUES (41, 1, 623);
INSERT INTO `sys_role_menu` VALUES (42, 1, 801);
INSERT INTO `sys_role_menu` VALUES (43, 1, 803);
INSERT INTO `sys_role_menu` VALUES (44, 1, 901);
INSERT INTO `sys_role_menu` VALUES (45, 1, 902);
INSERT INTO `sys_role_menu` VALUES (46, 1, 903);
INSERT INTO `sys_role_menu` VALUES (47, 1, 910);
INSERT INTO `sys_role_menu` VALUES (48, 1, 1101);
INSERT INTO `sys_role_menu` VALUES (49, 1, 1102);
INSERT INTO `sys_role_menu` VALUES (50, 1, 1103);
INSERT INTO `sys_role_menu` VALUES (51, 1, 1105);
INSERT INTO `sys_role_menu` VALUES (52, 1, 1201);
INSERT INTO `sys_role_menu` VALUES (53, 1, 1202);
INSERT INTO `sys_role_menu` VALUES (54, 1, 1203);
INSERT INTO `sys_role_menu` VALUES (55, 1, 1205);
INSERT INTO `sys_role_menu` VALUES (56, 1, 1301);
INSERT INTO `sys_role_menu` VALUES (57, 1, 1302);
INSERT INTO `sys_role_menu` VALUES (58, 1, 1303);
INSERT INTO `sys_role_menu` VALUES (59, 1, 1305);
INSERT INTO `sys_role_menu` VALUES (60, 1, 1401);
INSERT INTO `sys_role_menu` VALUES (61, 1, 1402);
INSERT INTO `sys_role_menu` VALUES (62, 1, 1403);
INSERT INTO `sys_role_menu` VALUES (63, 1, 1411);
INSERT INTO `sys_role_menu` VALUES (64, 1, 1501);
INSERT INTO `sys_role_menu` VALUES (65, 1, 1502);
INSERT INTO `sys_role_menu` VALUES (66, 1, 1503);
INSERT INTO `sys_role_menu` VALUES (67, 1, 1514);
INSERT INTO `sys_role_menu` VALUES (68, 1, 1602);
INSERT INTO `sys_role_menu` VALUES (69, 1, 1603);
INSERT INTO `sys_role_menu` VALUES (70, 1, 1604);
INSERT INTO `sys_role_menu` VALUES (71, 1, 1607);
INSERT INTO `sys_role_menu` VALUES (72, 1, 1701);
INSERT INTO `sys_role_menu` VALUES (73, 1, 1702);
INSERT INTO `sys_role_menu` VALUES (74, 1, 1703);
INSERT INTO `sys_role_menu` VALUES (75, 1, 1704);
INSERT INTO `sys_role_menu` VALUES (76, 1, 1705);
INSERT INTO `sys_role_menu` VALUES (77, 1, 1706);
INSERT INTO `sys_role_menu` VALUES (78, 1, 1707);
INSERT INTO `sys_role_menu` VALUES (79, 1, 1708);
INSERT INTO `sys_role_menu` VALUES (80, 1, 1709);
INSERT INTO `sys_role_menu` VALUES (81, 1, 1710);
INSERT INTO `sys_role_menu` VALUES (82, 1, 1711);
INSERT INTO `sys_role_menu` VALUES (83, 1, 1712);
INSERT INTO `sys_role_menu` VALUES (84, 1, 2114);
INSERT INTO `sys_role_menu` VALUES (85, 1, 2115);
INSERT INTO `sys_role_menu` VALUES (86, 1, 2116);
INSERT INTO `sys_role_menu` VALUES (87, 1, 2117);
INSERT INTO `sys_role_menu` VALUES (88, 1, 2118);
INSERT INTO `sys_role_menu` VALUES (89, 1, 2120);
INSERT INTO `sys_role_menu` VALUES (90, 1, 2121);
INSERT INTO `sys_role_menu` VALUES (91, 1, 2122);
INSERT INTO `sys_role_menu` VALUES (92, 1, 2123);
INSERT INTO `sys_role_menu` VALUES (93, 1, 2127);
INSERT INTO `sys_role_menu` VALUES (94, 1, 2128);
INSERT INTO `sys_role_menu` VALUES (95, 1, 2129);
INSERT INTO `sys_role_menu` VALUES (96, 1, 2130);
INSERT INTO `sys_role_menu` VALUES (97, 1, 2131);
INSERT INTO `sys_role_menu` VALUES (98, 1, 2132);
INSERT INTO `sys_role_menu` VALUES (99, 1, 2133);
INSERT INTO `sys_role_menu` VALUES (100, 1, 2134);
INSERT INTO `sys_role_menu` VALUES (101, 1, 2136);
INSERT INTO `sys_role_menu` VALUES (102, 1, 2137);
INSERT INTO `sys_role_menu` VALUES (103, 1, 2138);
INSERT INTO `sys_role_menu` VALUES (104, 1, 2139);
INSERT INTO `sys_role_menu` VALUES (105, 1, 2144);
INSERT INTO `sys_role_menu` VALUES (106, 1, 2145);
INSERT INTO `sys_role_menu` VALUES (107, 1, 2146);
INSERT INTO `sys_role_menu` VALUES (108, 1, 2147);
INSERT INTO `sys_role_menu` VALUES (109, 1, 2148);
INSERT INTO `sys_role_menu` VALUES (110, 1, 2149);
INSERT INTO `sys_role_menu` VALUES (111, 1, 2150);
INSERT INTO `sys_role_menu` VALUES (112, 1, 2151);
INSERT INTO `sys_role_menu` VALUES (113, 1, 2153);
INSERT INTO `sys_role_menu` VALUES (114, 1, 2155);
INSERT INTO `sys_role_menu` VALUES (115, 1, 2156);
INSERT INTO `sys_role_menu` VALUES (116, 1, 2157);
INSERT INTO `sys_role_menu` VALUES (117, 1, 2158);
INSERT INTO `sys_role_menu` VALUES (118, 1, 2159);
INSERT INTO `sys_role_menu` VALUES (119, 1, 2160);
INSERT INTO `sys_role_menu` VALUES (120, 1, 2161);
INSERT INTO `sys_role_menu` VALUES (121, 1, 2162);
INSERT INTO `sys_role_menu` VALUES (122, 1, 2163);
INSERT INTO `sys_role_menu` VALUES (123, 1, 2164);
INSERT INTO `sys_role_menu` VALUES (124, 1, 2165);
INSERT INTO `sys_role_menu` VALUES (125, 1, 2166);
INSERT INTO `sys_role_menu` VALUES (126, 1, 2169);
INSERT INTO `sys_role_menu` VALUES (127, 1, 2170);
INSERT INTO `sys_role_menu` VALUES (128, 1, 2171);
INSERT INTO `sys_role_menu` VALUES (129, 1, 2172);
INSERT INTO `sys_role_menu` VALUES (130, 1, 2173);
INSERT INTO `sys_role_menu` VALUES (131, 1, 2174);
INSERT INTO `sys_role_menu` VALUES (132, 1, 2176);
INSERT INTO `sys_role_menu` VALUES (133, 1, 2180);
INSERT INTO `sys_role_menu` VALUES (134, 1, 2181);
INSERT INTO `sys_role_menu` VALUES (135, 1, 2182);
INSERT INTO `sys_role_menu` VALUES (136, 1, 2185);
INSERT INTO `sys_role_menu` VALUES (137, 1, 2187);
INSERT INTO `sys_role_menu` VALUES (138, 1, 2188);
INSERT INTO `sys_role_menu` VALUES (139, 1, 2189);
INSERT INTO `sys_role_menu` VALUES (140, 1, 2190);

-- ----------------------------
-- Table structure for sys_timezone
-- ----------------------------
DROP TABLE IF EXISTS `sys_timezone`;
CREATE TABLE `sys_timezone`  (
  `id` int(4) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'timezone id',
  `offset` int(10) NOT NULL COMMENT '单位：milliseconds',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 247 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_timezone
-- ----------------------------
INSERT INTO `sys_timezone` VALUES (1, 'Africa/Abidjan', 0);
INSERT INTO `sys_timezone` VALUES (2, 'Africa/Accra', 0);
INSERT INTO `sys_timezone` VALUES (3, 'Africa/Algiers', 3600000);
INSERT INTO `sys_timezone` VALUES (4, 'Africa/Bissau', 0);
INSERT INTO `sys_timezone` VALUES (5, 'Africa/Cairo', 7200000);
INSERT INTO `sys_timezone` VALUES (6, 'Africa/Casablanca', 0);
INSERT INTO `sys_timezone` VALUES (7, 'Africa/Ceuta', 3600000);
INSERT INTO `sys_timezone` VALUES (8, 'Africa/El_Aaiun', 0);
INSERT INTO `sys_timezone` VALUES (9, 'Africa/Johannesburg', 7200000);
INSERT INTO `sys_timezone` VALUES (10, 'Africa/Juba', 10800000);
INSERT INTO `sys_timezone` VALUES (11, 'Africa/Khartoum', 10800000);
INSERT INTO `sys_timezone` VALUES (12, 'Africa/Lagos', 3600000);
INSERT INTO `sys_timezone` VALUES (13, 'Africa/Maputo', 7200000);
INSERT INTO `sys_timezone` VALUES (14, 'Africa/Monrovia', 0);
INSERT INTO `sys_timezone` VALUES (15, 'Africa/Nairobi', 10800000);
INSERT INTO `sys_timezone` VALUES (16, 'Africa/Ndjamena', 3600000);
INSERT INTO `sys_timezone` VALUES (17, 'Africa/Sao_Tome', 0);
INSERT INTO `sys_timezone` VALUES (18, 'Africa/Tripoli', 7200000);
INSERT INTO `sys_timezone` VALUES (19, 'Africa/Tunis', 3600000);
INSERT INTO `sys_timezone` VALUES (20, 'Africa/Windhoek', 3600000);
INSERT INTO `sys_timezone` VALUES (21, 'Antarctica/Casey', 28800000);
INSERT INTO `sys_timezone` VALUES (22, 'Antarctica/Davis', 25200000);
INSERT INTO `sys_timezone` VALUES (23, 'Antarctica/DumontDUrville', 36000000);
INSERT INTO `sys_timezone` VALUES (24, 'Antarctica/Macquarie', 39600000);
INSERT INTO `sys_timezone` VALUES (25, 'Antarctica/Mawson', 18000000);
INSERT INTO `sys_timezone` VALUES (26, 'Antarctica/Palmer', -14400000);
INSERT INTO `sys_timezone` VALUES (27, 'Antarctica/Rothera', -10800000);
INSERT INTO `sys_timezone` VALUES (28, 'Antarctica/Syowa', 10800000);
INSERT INTO `sys_timezone` VALUES (29, 'Antarctica/Troll', 0);
INSERT INTO `sys_timezone` VALUES (30, 'Antarctica/Vostok', 21600000);
INSERT INTO `sys_timezone` VALUES (31, 'Asia/Almaty', 21600000);
INSERT INTO `sys_timezone` VALUES (32, 'Asia/Amman', 7200000);
INSERT INTO `sys_timezone` VALUES (33, 'Asia/Anadyr', 43200000);
INSERT INTO `sys_timezone` VALUES (34, 'Asia/Aqtau', 18000000);
INSERT INTO `sys_timezone` VALUES (35, 'Asia/Aqtobe', 18000000);
INSERT INTO `sys_timezone` VALUES (36, 'Asia/Ashgabat', 18000000);
INSERT INTO `sys_timezone` VALUES (37, 'Asia/Atyrau', 0);
INSERT INTO `sys_timezone` VALUES (38, 'Asia/Baghdad', 10800000);
INSERT INTO `sys_timezone` VALUES (39, 'Asia/Baku', 14400000);
INSERT INTO `sys_timezone` VALUES (40, 'Asia/Bangkok', 25200000);
INSERT INTO `sys_timezone` VALUES (41, 'Asia/Barnaul', 25200000);
INSERT INTO `sys_timezone` VALUES (42, 'Asia/Beirut', 7200000);
INSERT INTO `sys_timezone` VALUES (43, 'Asia/Bishkek', 21600000);
INSERT INTO `sys_timezone` VALUES (44, 'Asia/Brunei', 28800000);
INSERT INTO `sys_timezone` VALUES (45, 'Asia/Chita', 32400000);
INSERT INTO `sys_timezone` VALUES (46, 'Asia/Choibalsan', 28800000);
INSERT INTO `sys_timezone` VALUES (47, 'Asia/Colombo', 19800000);
INSERT INTO `sys_timezone` VALUES (48, 'Asia/Damascus', 7200000);
INSERT INTO `sys_timezone` VALUES (49, 'Asia/Dhaka', 21600000);
INSERT INTO `sys_timezone` VALUES (50, 'Asia/Dili', 32400000);
INSERT INTO `sys_timezone` VALUES (51, 'Asia/Dubai', 14400000);
INSERT INTO `sys_timezone` VALUES (52, 'Asia/Dushanbe', 18000000);
INSERT INTO `sys_timezone` VALUES (53, 'Asia/Famagusta', 0);
INSERT INTO `sys_timezone` VALUES (54, 'Asia/Gaza', 7200000);
INSERT INTO `sys_timezone` VALUES (55, 'Asia/Hebron', 7200000);
INSERT INTO `sys_timezone` VALUES (56, 'Asia/Ho_Chi_Minh', 25200000);
INSERT INTO `sys_timezone` VALUES (57, 'Asia/Hong_Kong', 28800000);
INSERT INTO `sys_timezone` VALUES (58, 'Asia/Hovd', 25200000);
INSERT INTO `sys_timezone` VALUES (59, 'Asia/Irkutsk', 28800000);
INSERT INTO `sys_timezone` VALUES (60, 'Asia/Jakarta', 25200000);
INSERT INTO `sys_timezone` VALUES (61, 'Asia/Jayapura', 32400000);
INSERT INTO `sys_timezone` VALUES (62, 'Asia/Jerusalem', 7200000);
INSERT INTO `sys_timezone` VALUES (63, 'Asia/Kabul', 16200000);
INSERT INTO `sys_timezone` VALUES (64, 'Asia/Kamchatka', 43200000);
INSERT INTO `sys_timezone` VALUES (65, 'Asia/Karachi', 18000000);
INSERT INTO `sys_timezone` VALUES (66, 'Asia/Kathmandu', 20700000);
INSERT INTO `sys_timezone` VALUES (67, 'Asia/Khandyga', 32400000);
INSERT INTO `sys_timezone` VALUES (68, 'Asia/Kolkata', 19800000);
INSERT INTO `sys_timezone` VALUES (69, 'Asia/Krasnoyarsk', 25200000);
INSERT INTO `sys_timezone` VALUES (70, 'Asia/Kuala_Lumpur', 28800000);
INSERT INTO `sys_timezone` VALUES (71, 'Asia/Kuching', 28800000);
INSERT INTO `sys_timezone` VALUES (72, 'Asia/Macau', 28800000);
INSERT INTO `sys_timezone` VALUES (73, 'Asia/Magadan', 39600000);
INSERT INTO `sys_timezone` VALUES (74, 'Asia/Makassar', 28800000);
INSERT INTO `sys_timezone` VALUES (75, 'Asia/Manila', 28800000);
INSERT INTO `sys_timezone` VALUES (76, 'Asia/Nicosia', 7200000);
INSERT INTO `sys_timezone` VALUES (77, 'Asia/Novokuznetsk', 25200000);
INSERT INTO `sys_timezone` VALUES (78, 'Asia/Novosibirsk', 21600000);
INSERT INTO `sys_timezone` VALUES (79, 'Asia/Omsk', 21600000);
INSERT INTO `sys_timezone` VALUES (80, 'Asia/Oral', 18000000);
INSERT INTO `sys_timezone` VALUES (81, 'Asia/Pontianak', 25200000);
INSERT INTO `sys_timezone` VALUES (82, 'Asia/Pyongyang', 30600000);
INSERT INTO `sys_timezone` VALUES (83, 'Asia/Qatar', 10800000);
INSERT INTO `sys_timezone` VALUES (84, 'Asia/Qostanay', 0);
INSERT INTO `sys_timezone` VALUES (85, 'Asia/Qyzylorda', 21600000);
INSERT INTO `sys_timezone` VALUES (86, 'Asia/Riyadh', 10800000);
INSERT INTO `sys_timezone` VALUES (87, 'Asia/Sakhalin', 39600000);
INSERT INTO `sys_timezone` VALUES (88, 'Asia/Samarkand', 18000000);
INSERT INTO `sys_timezone` VALUES (89, 'Asia/Seoul', 32400000);
INSERT INTO `sys_timezone` VALUES (90, 'Asia/Shanghai', 28800000);
INSERT INTO `sys_timezone` VALUES (91, 'Asia/Singapore', 28800000);
INSERT INTO `sys_timezone` VALUES (92, 'Asia/Srednekolymsk', 39600000);
INSERT INTO `sys_timezone` VALUES (93, 'Asia/Taipei', 28800000);
INSERT INTO `sys_timezone` VALUES (94, 'Asia/Tashkent', 18000000);
INSERT INTO `sys_timezone` VALUES (95, 'Asia/Tbilisi', 14400000);
INSERT INTO `sys_timezone` VALUES (96, 'Asia/Tehran', 12600000);
INSERT INTO `sys_timezone` VALUES (97, 'Asia/Thimphu', 21600000);
INSERT INTO `sys_timezone` VALUES (98, 'Asia/Tokyo', 32400000);
INSERT INTO `sys_timezone` VALUES (99, 'Asia/Tomsk', 25200000);
INSERT INTO `sys_timezone` VALUES (100, 'Asia/Ulaanbaatar', 28800000);
INSERT INTO `sys_timezone` VALUES (101, 'Asia/Urumqi', 21600000);
INSERT INTO `sys_timezone` VALUES (102, 'Asia/Ust-Nera', 36000000);
INSERT INTO `sys_timezone` VALUES (103, 'Asia/Vladivostok', 36000000);
INSERT INTO `sys_timezone` VALUES (104, 'Asia/Yakutsk', 32400000);
INSERT INTO `sys_timezone` VALUES (105, 'Asia/Yangon', 0);
INSERT INTO `sys_timezone` VALUES (106, 'Asia/Yekaterinburg', 18000000);
INSERT INTO `sys_timezone` VALUES (107, 'Asia/Yerevan', 14400000);
INSERT INTO `sys_timezone` VALUES (108, 'Atlantic/Azores', -3600000);
INSERT INTO `sys_timezone` VALUES (109, 'Atlantic/Bermuda', -14400000);
INSERT INTO `sys_timezone` VALUES (110, 'Atlantic/Canary', 0);
INSERT INTO `sys_timezone` VALUES (111, 'Atlantic/Cape_Verde', -3600000);
INSERT INTO `sys_timezone` VALUES (112, 'Atlantic/Faroe', 0);
INSERT INTO `sys_timezone` VALUES (113, 'Atlantic/Madeira', 0);
INSERT INTO `sys_timezone` VALUES (114, 'Atlantic/Reykjavik', 0);
INSERT INTO `sys_timezone` VALUES (115, 'Atlantic/South_Georgia', -7200000);
INSERT INTO `sys_timezone` VALUES (116, 'Atlantic/Stanley', -10800000);
INSERT INTO `sys_timezone` VALUES (117, 'Australia/Adelaide', 34200000);
INSERT INTO `sys_timezone` VALUES (118, 'Australia/Brisbane', 36000000);
INSERT INTO `sys_timezone` VALUES (119, 'Australia/Broken_Hill', 34200000);
INSERT INTO `sys_timezone` VALUES (120, 'Australia/Darwin', 34200000);
INSERT INTO `sys_timezone` VALUES (121, 'Australia/Eucla', 31500000);
INSERT INTO `sys_timezone` VALUES (122, 'Australia/Hobart', 36000000);
INSERT INTO `sys_timezone` VALUES (123, 'Australia/Lindeman', 36000000);
INSERT INTO `sys_timezone` VALUES (124, 'Australia/Lord_Howe', 37800000);
INSERT INTO `sys_timezone` VALUES (125, 'Australia/Melbourne', 36000000);
INSERT INTO `sys_timezone` VALUES (126, 'Australia/Perth', 28800000);
INSERT INTO `sys_timezone` VALUES (127, 'Australia/Sydney', 36000000);
INSERT INTO `sys_timezone` VALUES (128, 'Etc/GMT', 0);
INSERT INTO `sys_timezone` VALUES (129, 'Etc/GMT+1', -3600000);
INSERT INTO `sys_timezone` VALUES (130, 'Etc/GMT+10', -36000000);
INSERT INTO `sys_timezone` VALUES (131, 'Etc/GMT+11', -39600000);
INSERT INTO `sys_timezone` VALUES (132, 'Etc/GMT+12', -43200000);
INSERT INTO `sys_timezone` VALUES (133, 'Etc/GMT+2', -7200000);
INSERT INTO `sys_timezone` VALUES (134, 'Etc/GMT+3', -10800000);
INSERT INTO `sys_timezone` VALUES (135, 'Etc/GMT+4', -14400000);
INSERT INTO `sys_timezone` VALUES (136, 'Etc/GMT+5', -18000000);
INSERT INTO `sys_timezone` VALUES (137, 'Etc/GMT+6', -21600000);
INSERT INTO `sys_timezone` VALUES (138, 'Etc/GMT+7', -25200000);
INSERT INTO `sys_timezone` VALUES (139, 'Etc/GMT+8', -28800000);
INSERT INTO `sys_timezone` VALUES (140, 'Etc/GMT+9', -32400000);
INSERT INTO `sys_timezone` VALUES (141, 'Etc/GMT-1', 3600000);
INSERT INTO `sys_timezone` VALUES (142, 'Etc/GMT-10', 36000000);
INSERT INTO `sys_timezone` VALUES (143, 'Etc/GMT-11', 39600000);
INSERT INTO `sys_timezone` VALUES (144, 'Etc/GMT-12', 43200000);
INSERT INTO `sys_timezone` VALUES (145, 'Etc/GMT-13', 46800000);
INSERT INTO `sys_timezone` VALUES (146, 'Etc/GMT-14', 50400000);
INSERT INTO `sys_timezone` VALUES (147, 'Etc/GMT-2', 7200000);
INSERT INTO `sys_timezone` VALUES (148, 'Etc/GMT-3', 10800000);
INSERT INTO `sys_timezone` VALUES (149, 'Etc/GMT-4', 14400000);
INSERT INTO `sys_timezone` VALUES (150, 'Etc/GMT-5', 18000000);
INSERT INTO `sys_timezone` VALUES (151, 'Etc/GMT-6', 21600000);
INSERT INTO `sys_timezone` VALUES (152, 'Etc/GMT-7', 25200000);
INSERT INTO `sys_timezone` VALUES (153, 'Etc/GMT-8', 28800000);
INSERT INTO `sys_timezone` VALUES (154, 'Etc/GMT-9', 32400000);
INSERT INTO `sys_timezone` VALUES (155, 'Etc/UTC', 0);
INSERT INTO `sys_timezone` VALUES (156, 'Europe/Amsterdam', 3600000);
INSERT INTO `sys_timezone` VALUES (157, 'Europe/Andorra', 3600000);
INSERT INTO `sys_timezone` VALUES (158, 'Europe/Astrakhan', 14400000);
INSERT INTO `sys_timezone` VALUES (159, 'Europe/Athens', 7200000);
INSERT INTO `sys_timezone` VALUES (160, 'Europe/Belgrade', 3600000);
INSERT INTO `sys_timezone` VALUES (161, 'Europe/Berlin', 3600000);
INSERT INTO `sys_timezone` VALUES (162, 'Europe/Brussels', 3600000);
INSERT INTO `sys_timezone` VALUES (163, 'Europe/Bucharest', 7200000);
INSERT INTO `sys_timezone` VALUES (164, 'Europe/Budapest', 3600000);
INSERT INTO `sys_timezone` VALUES (165, 'Europe/Chisinau', 7200000);
INSERT INTO `sys_timezone` VALUES (166, 'Europe/Copenhagen', 3600000);
INSERT INTO `sys_timezone` VALUES (167, 'Europe/Dublin', 0);
INSERT INTO `sys_timezone` VALUES (168, 'Europe/Gibraltar', 3600000);
INSERT INTO `sys_timezone` VALUES (169, 'Europe/Helsinki', 7200000);
INSERT INTO `sys_timezone` VALUES (170, 'Europe/Istanbul', 7200000);
INSERT INTO `sys_timezone` VALUES (171, 'Europe/Kaliningrad', 7200000);
INSERT INTO `sys_timezone` VALUES (172, 'Europe/Kiev', 7200000);
INSERT INTO `sys_timezone` VALUES (173, 'Europe/Kirov', 10800000);
INSERT INTO `sys_timezone` VALUES (174, 'Europe/Lisbon', 0);
INSERT INTO `sys_timezone` VALUES (175, 'Europe/London', 0);
INSERT INTO `sys_timezone` VALUES (176, 'Europe/Luxembourg', 3600000);
INSERT INTO `sys_timezone` VALUES (177, 'Europe/Madrid', 3600000);
INSERT INTO `sys_timezone` VALUES (178, 'Europe/Malta', 3600000);
INSERT INTO `sys_timezone` VALUES (179, 'Europe/Minsk', 10800000);
INSERT INTO `sys_timezone` VALUES (180, 'Europe/Monaco', 3600000);
INSERT INTO `sys_timezone` VALUES (181, 'Europe/Moscow', 10800000);
INSERT INTO `sys_timezone` VALUES (182, 'Europe/Oslo', 3600000);
INSERT INTO `sys_timezone` VALUES (183, 'Europe/Paris', 3600000);
INSERT INTO `sys_timezone` VALUES (184, 'Europe/Prague', 3600000);
INSERT INTO `sys_timezone` VALUES (185, 'Europe/Riga', 7200000);
INSERT INTO `sys_timezone` VALUES (186, 'Europe/Rome', 3600000);
INSERT INTO `sys_timezone` VALUES (187, 'Europe/Samara', 14400000);
INSERT INTO `sys_timezone` VALUES (188, 'Europe/Saratov', 0);
INSERT INTO `sys_timezone` VALUES (189, 'Europe/Simferopol', 10800000);
INSERT INTO `sys_timezone` VALUES (190, 'Europe/Sofia', 7200000);
INSERT INTO `sys_timezone` VALUES (191, 'Europe/Stockholm', 3600000);
INSERT INTO `sys_timezone` VALUES (192, 'Europe/Tallinn', 7200000);
INSERT INTO `sys_timezone` VALUES (193, 'Europe/Tirane', 3600000);
INSERT INTO `sys_timezone` VALUES (194, 'Europe/Ulyanovsk', 14400000);
INSERT INTO `sys_timezone` VALUES (195, 'Europe/Uzhgorod', 7200000);
INSERT INTO `sys_timezone` VALUES (196, 'Europe/Vienna', 3600000);
INSERT INTO `sys_timezone` VALUES (197, 'Europe/Vilnius', 7200000);
INSERT INTO `sys_timezone` VALUES (198, 'Europe/Volgograd', 10800000);
INSERT INTO `sys_timezone` VALUES (199, 'Europe/Warsaw', 3600000);
INSERT INTO `sys_timezone` VALUES (200, 'Europe/Zaporozhye', 7200000);
INSERT INTO `sys_timezone` VALUES (201, 'Europe/Zurich', 3600000);
INSERT INTO `sys_timezone` VALUES (202, 'Factory', 0);
INSERT INTO `sys_timezone` VALUES (203, 'Indian/Chagos', 21600000);
INSERT INTO `sys_timezone` VALUES (204, 'Indian/Christmas', 25200000);
INSERT INTO `sys_timezone` VALUES (205, 'Indian/Cocos', 23400000);
INSERT INTO `sys_timezone` VALUES (206, 'Indian/Kerguelen', 18000000);
INSERT INTO `sys_timezone` VALUES (207, 'Indian/Mahe', 14400000);
INSERT INTO `sys_timezone` VALUES (208, 'Indian/Maldives', 18000000);
INSERT INTO `sys_timezone` VALUES (209, 'Indian/Mauritius', 14400000);
INSERT INTO `sys_timezone` VALUES (210, 'Indian/Reunion', 14400000);
INSERT INTO `sys_timezone` VALUES (211, 'Pacific/Apia', 46800000);
INSERT INTO `sys_timezone` VALUES (212, 'Pacific/Auckland', 43200000);
INSERT INTO `sys_timezone` VALUES (213, 'Pacific/Bougainville', 39600000);
INSERT INTO `sys_timezone` VALUES (214, 'Pacific/Chatham', 45900000);
INSERT INTO `sys_timezone` VALUES (215, 'Pacific/Chuuk', 36000000);
INSERT INTO `sys_timezone` VALUES (216, 'Pacific/Easter', -21600000);
INSERT INTO `sys_timezone` VALUES (217, 'Pacific/Efate', 39600000);
INSERT INTO `sys_timezone` VALUES (218, 'Pacific/Enderbury', 46800000);
INSERT INTO `sys_timezone` VALUES (219, 'Pacific/Fakaofo', 46800000);
INSERT INTO `sys_timezone` VALUES (220, 'Pacific/Fiji', 43200000);
INSERT INTO `sys_timezone` VALUES (221, 'Pacific/Funafuti', 43200000);
INSERT INTO `sys_timezone` VALUES (222, 'Pacific/Galapagos', -21600000);
INSERT INTO `sys_timezone` VALUES (223, 'Pacific/Gambier', -32400000);
INSERT INTO `sys_timezone` VALUES (224, 'Pacific/Guadalcanal', 39600000);
INSERT INTO `sys_timezone` VALUES (225, 'Pacific/Guam', 36000000);
INSERT INTO `sys_timezone` VALUES (226, 'Pacific/Honolulu', -36000000);
INSERT INTO `sys_timezone` VALUES (227, 'Pacific/Kiritimati', 50400000);
INSERT INTO `sys_timezone` VALUES (228, 'Pacific/Kosrae', 39600000);
INSERT INTO `sys_timezone` VALUES (229, 'Pacific/Kwajalein', 43200000);
INSERT INTO `sys_timezone` VALUES (230, 'Pacific/Majuro', 43200000);
INSERT INTO `sys_timezone` VALUES (231, 'Pacific/Marquesas', -34200000);
INSERT INTO `sys_timezone` VALUES (232, 'Pacific/Nauru', 43200000);
INSERT INTO `sys_timezone` VALUES (233, 'Pacific/Niue', -39600000);
INSERT INTO `sys_timezone` VALUES (234, 'Pacific/Norfolk', 39600000);
INSERT INTO `sys_timezone` VALUES (235, 'Pacific/Noumea', 39600000);
INSERT INTO `sys_timezone` VALUES (236, 'Pacific/Pago_Pago', -39600000);
INSERT INTO `sys_timezone` VALUES (237, 'Pacific/Palau', 32400000);
INSERT INTO `sys_timezone` VALUES (238, 'Pacific/Pitcairn', -28800000);
INSERT INTO `sys_timezone` VALUES (239, 'Pacific/Pohnpei', 39600000);
INSERT INTO `sys_timezone` VALUES (240, 'Pacific/Port_Moresby', 36000000);
INSERT INTO `sys_timezone` VALUES (241, 'Pacific/Rarotonga', -36000000);
INSERT INTO `sys_timezone` VALUES (242, 'Pacific/Tahiti', -36000000);
INSERT INTO `sys_timezone` VALUES (243, 'Pacific/Tarawa', 43200000);
INSERT INTO `sys_timezone` VALUES (244, 'Pacific/Tongatapu', 46800000);
INSERT INTO `sys_timezone` VALUES (245, 'Pacific/Wake', 43200000);
INSERT INTO `sys_timezone` VALUES (246, 'Pacific/Wallis', 43200000);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '用户名',
  `pin` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '密码',
  `salt` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '盐',
  `email` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态  0：禁用   1：正常',
  `create_at` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `lang` varchar(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'en',
  `source` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'system',
  `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户姓名\r\n\r\n(用于系统内展示用户名称)',
  `last_login_time` datetime(0) NULL DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后登录ip',
  `mobile` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `create_by` int(10) NULL DEFAULT NULL COMMENT '添加人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '系统用户表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', 'e1153123d7d180ceeb820d577ff119876678732a68eef4e6ffc0b1f06a01f91b', 'YzcmCZNvbXocrsz9dm8e', 'nezha@www.puppn.com', 1, '2016-11-11 11:11:11', 'zh', 'system', 'admin', NULL, NULL, NULL, NULL);

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `user_id` int(10) NOT NULL,
  `role_id` int(10) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 1, 1);

-- ----------------------------
-- Table structure for terminal_cmd
-- ----------------------------
DROP TABLE IF EXISTS `terminal_cmd`;
CREATE TABLE `terminal_cmd`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '会话id',
  `cmd` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户输入命令',
  `time` int(10) NULL DEFAULT NULL COMMENT '单位：ms\r\n\r\n记录的是 session开始后的时间偏移量',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_uuid`(`uuid`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for terminal_record
-- ----------------------------
DROP TABLE IF EXISTS `terminal_record`;
CREATE TABLE `terminal_record`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '会话ID',
  `start_time` int(10) NOT NULL COMMENT '单位：ms\r\n\r\nrecord 开始时间，记录的是会话开始后的时间偏移量',
  `end_time` int(10) NOT NULL COMMENT '单位：ms\r\n\r\nrecord 结束时间，记录的是会话开始后的时间偏移量',
  `content` mediumblob NOT NULL COMMENT '记录的是 session开始后的时间偏移量\r\n\r\n最大储存 65K，超过部分再新增一条记录',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_uuid`(`uuid`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for terminal_session
-- ----------------------------
DROP TABLE IF EXISTS `terminal_session`;
CREATE TABLE `terminal_session`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '会话id',
  `asset_id` int(10) NULL DEFAULT NULL COMMENT '关联资产id',
  `host` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '登录主机',
  `port` int(10) NULL DEFAULT NULL COMMENT '端口',
  `protocol` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '协议，可选值：SSH，TELNET。统一大写',
  `auth_type` int(10) NULL DEFAULT NULL COMMENT '认证类型，1：密码，2：私钥证书  认证类型，1：密码，2：私钥证书\r\n\r\nssh可选值：1、2\r\n\r\ntelnet可选值：1',
  `login_user` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '登录主机的用户名',
  `user_id` int(10) NOT NULL COMMENT 'nezha系统内用户id',
  `remote_addr` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '客户端ip地址',
  `start_time` datetime(0) NOT NULL COMMENT '开始时间',
  `end_time` datetime(0) NULL DEFAULT NULL COMMENT '结束时间',
  `status` int(1) NOT NULL COMMENT '0：连接中\r\n\r\n1：发送失败（没有通过ssh或telnet发送成功）\r\n\r\n2：已结束\r\n\r\n3：强制退出\r\n\r\n4：未知错误',
  `kill_user_id` int(10) NULL DEFAULT NULL COMMENT '强制关闭用户id',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_uuid`(`uuid`) USING BTREE,
  INDEX `idx_asset_id`(`asset_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_host`(`host`) USING BTREE,
  INDEX `idx_port`(`port`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for visual_chart
-- ----------------------------
DROP TABLE IF EXISTS `visual_chart`;
CREATE TABLE `visual_chart`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '图表标题',
  `panel_id` int(10) NOT NULL COMMENT '图表id，关联panel表\r\n\r\n当 panel_id =0 代表为模板',
  `group_id` int(10) NOT NULL COMMENT '关联 visual_chart.id，关联 chart  的 type 只能为 group',
  `span` int(10) NOT NULL COMMENT 'type = group，span = 12, 用户修改无效',
  `height` int(10) NOT NULL COMMENT 'type = group，height = -1，用户修改无效',
  `update_by` int(10) NOT NULL COMMENT '更新人',
  `update_at` datetime(0) NOT NULL COMMENT '更新时间',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '图表类型 line\r\nbar\r\ntable\r\nstackArea\r\nsingleStat\r\nurl\r\nassetInfo\r\nalertList\r\ngroup',
  `unit` int(10) NOT NULL DEFAULT 2 COMMENT '界面显示采用二级菜单',
  `weight` int(10) NOT NULL DEFAULT 0 COMMENT '默认为 0，用于chart排序',
  `param` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '参数 记录type的具体参数信息，数据格式为json对象\r\n\r\n当type = url时\r\n\r\n{\r\n\r\n    “url”:\"https://www.baidu.com\"\r\n\r\n}',
  `pid` int(10) NULL DEFAULT NULL COMMENT '模板id 图表通过模板创建时不能为空，便于后续模板修改同步修改',
  `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '1:内置，0：非内置，默认：0',
  `remark` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '默认：\"\"',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '导入唯一标识',
  `var_type` int(1) NULL DEFAULT NULL COMMENT '模板或使用模板创建有效，pid != null | panel_id == 0 时不能为空\r\n\r\n1: asset\r\n\r\n2: endpoint',
  `var_id` int(10) NULL DEFAULT NULL COMMENT 'pid != null 时不能为空\r\n\r\n根据var_type 分别对应 asset.id, endpoint.id，用于 表达式变量替换',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE,
  INDEX `idx_panel_id`(`panel_id`) USING BTREE,
  INDEX `idx_group_id`(`group_id`) USING BTREE,
  INDEX `idx_type`(`type`) USING BTREE,
  INDEX `idx_var_type`(`var_type`) USING BTREE,
  INDEX `idx_var_id`(`var_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for visual_chart_element
-- ----------------------------
DROP TABLE IF EXISTS `visual_chart_element`;
CREATE TABLE `visual_chart_element`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `chart_id` int(10) NOT NULL COMMENT '图表id，关联chart表',
  `expression` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'promql表达式',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'normal：简单表达式，可页面解析，如：up{asset=\"192.168.40.40\",module=\"kafka\"}\r\n\r\nexpert：用户输入表达式，解析困难，直接在页面显示',
  `legend` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '图例别名表达式',
  `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '是否内置',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '导入唯一标识 用于导入撤销',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_chart_id`(`chart_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for visual_expression_tmpl
-- ----------------------------
DROP TABLE IF EXISTS `visual_expression_tmpl`;
CREATE TABLE `visual_expression_tmpl`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板名称',
  `gname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'default' COMMENT '组名，默认：default',
  `expression` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '表达式，不能为空',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '描述',
  `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '1:内置，0：非内置，默认：0',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '用于导入撤销，默认：“”',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE,
  INDEX `idx_gname`(`gname`) USING BTREE,
  INDEX `idx_expression`(`expression`(768)) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 42 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of visual_expression_tmpl
-- ----------------------------
INSERT INTO `visual_expression_tmpl` VALUES (1, 'abs', 'function', 'abs()', 'abs(v instant-vector) returns the input vector with all sample values converted to their absolute value.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (2, 'absent', 'function', 'absent()', 'absent(v instant-vector) returns an empty vector if the vector passed to it has any elements and a 1-element vector with the value 1 if the vector passed to it has no elements.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (3, 'absent_over_time', 'function', 'absent_over_time()', 'absent_over_time(v range-vector) returns an empty vector if the range vector passed to it has any elements and a 1-element vector with the value 1 if the range vector passed to it has no elements.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (4, 'ceil', 'function', 'ceil()', 'ceil(v instant-vector) rounds the sample values of all elements in v up to the nearest integer.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (5, 'changes', 'function', 'changes()', 'For each input time series, changes(v range-vector) returns the number of times its value has changed within the provided time range as an instant vector.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (6, 'clamp', 'function', 'clamp()', 'clamp(v instant-vector, min scalar, max scalar) clamps the sample values of all elements in v to have a lower limit of min and an upper limit of max.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (7, 'clamp_max', 'function', 'clamp_max()', 'clamp_max(v instant-vector, max scalar) clamps the sample values of all elements in v to have an upper limit of max.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (8, 'clamp_min', 'function', 'clamp_min()', 'clamp_min(v instant-vector, min scalar) clamps the sample values of all elements in v to have a lower limit of min.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (9, 'day_of_month', 'function', 'day_of_month()', 'day_of_month(v=vector(time()) instant-vector) returns the day of the month for each of the given times in UTC. Returned values are from 1 to 31.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (10, 'day_of_week', 'function', 'day_of_week()', 'day_of_week(v=vector(time()) instant-vector) returns the day of the week for each of the given times in UTC. Returned values are from 0 to 6, where 0 means Sunday etc.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (11, 'days_in_month', 'function', 'days_in_month()', 'days_in_month(v=vector(time()) instant-vector) returns number of days in the month for each of the given times in UTC. Returned values are from 28 to 31.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (12, 'delta', 'function', 'delta()', 'delta(v range-vector) calculates the difference between the first and last value of each time series element in a range vector v, returning an instant vector with the given deltas and equivalent labels. The delta is extrapolated to cover the full time range as specified in the range vector selector, so that it is possible to get a non-integer result even if the sample values are all integers.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (13, 'deriv', 'function', 'deriv()', 'deriv(v range-vector) calculates the per-second derivative of the time series in a range vector v, using simple linear regression.\n\nderiv should only be used with gauges.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (14, 'exp', 'function', 'exp()', 'exp(v instant-vector) calculates the exponential function for all elements in v.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (15, 'floor', 'function', 'floor()', 'floor(v instant-vector) rounds the sample values of all elements in v down to the nearest integer.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (16, 'histogram_quantile', 'function', 'histogram_quantile()', 'histogram_quantile(φ scalar, b instant-vector) calculates the φ-quantile (0 ≤ φ ≤ 1) from the buckets b of a histogram', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (17, 'holt_winters', 'function', 'holt_winters()', 'holt_winters(v range-vector, sf scalar, tf scalar) produces a smoothed value for time series based on the range in v. The lower the smoothing factor sf, the more importance is given to old data. The higher the trend factor tf, the more trends in the data is considered. Both sf and tf must be between 0 and 1.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (18, 'hour', 'function', 'hour()', 'hour(v=vector(time()) instant-vector) returns the hour of the day for each of the given times in UTC. Returned values are from 0 to 23.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (19, 'idelta', 'function', 'idelta()', 'idelta(v range-vector) calculates the difference between the last two samples in the range vector v, returning an instant vector with the given deltas and equivalent labels.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (20, 'increase', 'function', 'increase()', 'increase(v range-vector) calculates the increase in the time series in the range vector. Breaks in monotonicity (such as counter resets due to target restarts) are automatically adjusted for. The increase is extrapolated to cover the full time range as specified in the range vector selector, so that it is possible to get a non-integer result even if a counter increases only by integer increments.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (21, 'irate', 'function', 'irate()', 'irate(v range-vector) calculates the per-second instant rate of increase of the time series in the range vector. This is based on the last two data points. Breaks in monotonicity (such as counter resets due to target restarts) are automatically adjusted for.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (22, 'label_join', 'function', 'label_join()', 'For each timeseries in v, label_join(v instant-vector, dst_label string, separator string, src_label_1 string, src_label_2 string, ...) joins all the values of all the src_labels using separator and returns the timeseries with the label dst_label containing the joined value. There can be any number of src_labels in this function.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (23, 'label_replace', 'function', 'label_replace()', 'For each timeseries in v, label_replace(v instant-vector, dst_label string, replacement string, src_label string, regex string) matches the regular expression regex against the label src_label. If it matches, then the timeseries is returned with the label dst_label replaced by the expansion of replacement. $1 is replaced with the first matching subgroup, $2 with the second etc. If the regular expression doesn\'t match then the timeseries is returned unchanged.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (24, 'ln', 'function', 'ln()', 'ln(v instant-vector) calculates the natural logarithm for all elements in v', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (25, 'log2', 'function', 'log2()', 'log2(v instant-vector) calculates the binary logarithm for all elements in v. The special cases are equivalent to those in ln.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (26, 'log10', 'function', 'log10()', 'log10(v instant-vector) calculates the decimal logarithm for all elements in v. The special cases are equivalent to those in ln.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (27, 'minute', 'function', 'minute()', 'minute(v=vector(time()) instant-vector) returns the minute of the hour for each of the given times in UTC. Returned values are from 0 to 59.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (28, 'month', 'function', 'month()', 'month(v=vector(time()) instant-vector) returns the month of the year for each of the given times in UTC. Returned values are from 1 to 12, where 1 means January etc.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (29, 'predict_linear', 'function', 'predict_linear()', 'predict_linear(v range-vector, t scalar) predicts the value of time series t seconds from now, based on the range vector v, using simple linear regression.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (30, 'rate', 'function', 'rate()', 'rate(v range-vector) calculates the per-second average rate of increase of the time series in the range vector. Breaks in monotonicity (such as counter resets due to target restarts) are automatically adjusted for. Also, the calculation extrapolates to the ends of the time range, allowing for missed scrapes or imperfect alignment of scrape cycles with the range\'s time period.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (31, 'resets', 'function', 'resets()', 'For each input time series, resets(v range-vector) returns the number of counter resets within the provided time range as an instant vector. Any decrease in the value between two consecutive samples is interpreted as a counter reset.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (32, 'round', 'function', 'round()', 'round(v instant-vector, to_nearest=1 scalar) rounds the sample values of all elements in v to the nearest integer. Ties are resolved by rounding up. The optional to_nearest argument allows specifying the nearest multiple to which the sample values should be rounded. This multiple may also be a fraction.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (33, 'scalar', 'function', 'scalar()', 'Given a single-element input vector, scalar(v instant-vector) returns the sample value of that single element as a scalar. If the input vector does not have exactly one element, scalar will return NaN.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (34, 'sgn', 'function', 'sgn()', 'sgn(v instant-vector) returns a vector with all sample values converted to their sign, defined as this: 1 if v is positive, -1 if v is negative and 0 if v is equal to zero.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (35, 'sort', 'function', 'sort()', 'sort(v instant-vector) returns vector elements sorted by their sample values, in ascending order.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (36, 'sort_desc', 'function', 'sort_desc()', 'Same as sort, but sorts in descending order.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (37, 'sqrt', 'function', 'sqrt()', 'sqrt(v instant-vector) calculates the square root of all elements in v.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (38, 'time', 'function', 'time()', 'time() returns the number of seconds since January 1, 1970 UTC. Note that this does not actually return the current time, but the time at which the expression is to be evaluated.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (39, 'timestamp', 'function', 'timestamp()', 'timestamp(v instant-vector) returns the timestamp of each of the samples of the given vector as the number of seconds since January 1, 1970 UTC.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (40, 'vector', 'function', 'vector()', 'vector(s scalar) returns the scalar s as a vector with no labels.', '1', '');
INSERT INTO `visual_expression_tmpl` VALUES (41, 'year', 'function', 'year()', 'year(v=vector(time()) instant-vector) returns the year for each of the given times in UTC.', '1', '');

-- ----------------------------
-- Table structure for visual_panel
-- ----------------------------
DROP TABLE IF EXISTS `visual_panel`;
CREATE TABLE `visual_panel`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '图表名',
  `create_by` int(10) NOT NULL COMMENT '创建人id',
  `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'dashboard' COMMENT '可选值：dashboard，asset，project，module，endpoint, model\r\n\r\n默认值：dashboard',
  `link` int(10) NULL DEFAULT NULL COMMENT '关联id',
  `pid` int(10) NOT NULL DEFAULT 0 COMMENT '父id 默认0',
  `weight` int(10) NOT NULL DEFAULT 0 COMMENT '排序 默认0',
  `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '是否内置',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '默认：“”\r\n\r\n用于导入撤销',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE,
  INDEX `idx_type`(`type`) USING BTREE,
  INDEX `idx_link`(`link`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
