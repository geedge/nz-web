/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 50725
 Source Host           : localhost:3306
 Source Schema         : nz

 Target Server Type    : MySQL
 Target Server Version : 50725
 File Encoding         : 65001

 Date: 28/12/2020 17:31:55
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for account
-- ----------------------------
DROP TABLE IF EXISTS `account`;
CREATE TABLE `account`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `asset_id` int(10) NOT NULL COMMENT '资产id',
  `protocol` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '协议，可选值：SSH，TELNET。统一大写',
  `port` int(10) NOT NULL,
  `seq` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '全局唯一，用于导入撤销',
  `params` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '根据协议不同内容不同，json对象保存',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_asset_id`(`asset_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for alert_message
-- ----------------------------
DROP TABLE IF EXISTS `alert_message`;
CREATE TABLE `alert_message`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `severity` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '告警级别',
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
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_severity`(`severity`) USING BTREE,
  INDEX `idx_state`(`state`) USING BTREE,
  INDEX `idx_rule_id`(`rule_id`) USING BTREE,
  INDEX `idx_project_id`(`project_id`) USING BTREE,
  INDEX `idx_module_id`(`module_id`) USING BTREE,
  INDEX `idx_endpoint_id`(`endpoint_id`) USING BTREE,
  INDEX `idx_asset_id`(`asset_id`) USING BTREE,
  INDEX `idx_dc_id`(`dc_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for alert_rule
-- ----------------------------
DROP TABLE IF EXISTS `alert_rule`;
CREATE TABLE `alert_rule`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `alert_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '告警名称',
  `expr` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '运算表达式',
  `last` int(10) NOT NULL,
  `severity` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '告警级别',
  `summary` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '告警摘要',
  `description` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '告警详细描述',
  `receiver` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `build_in` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '0' COMMENT '1：内置，0：非内置，默认：0\r\n\r\n内置规则不允许编辑删除，系统内置一条 snmptrap 规则',
  `operator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '比较符号',
  `threshold` double(20, 5) NULL DEFAULT NULL COMMENT '阈值',
  `unit` int(10) NOT NULL COMMENT '单位',
  `seq` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '导入唯一标识 全局唯一，用于导入撤销',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_alert_name`(`alert_name`) USING BTREE,
  INDEX `idx_severity`(`severity`) USING BTREE,
  INDEX `idx_threshold`(`threshold`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of alert_rule
-- ----------------------------
INSERT INTO `alert_rule` VALUES (1, 'SNMP TRAP', ' ', 600, 'P1', NULL, NULL, NULL, '1', '', NULL, 0, NULL);

-- ----------------------------
-- Table structure for asset
-- ----------------------------
DROP TABLE IF EXISTS `asset`;
CREATE TABLE `asset`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sn` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '资产序列化，唯一标识',
  `model_id` int(10) NOT NULL COMMENT '产品型号',
  `idc_id` int(10) NOT NULL COMMENT '数据中心id',
  `cabinet_id` int(10) NULL DEFAULT NULL COMMENT '机柜编号',
  `cabinet_start` int(10) NULL DEFAULT NULL COMMENT '需要验证开始结束u位内没有已占用u位，且不小于 model.u_size ',
  `cabinet_end` int(10) NULL DEFAULT NULL,
  `host` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主机地址',
  `ipmi` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'ipmi配置信息',
  `state` int(10) NOT NULL COMMENT '资产状态',
  `purchase_date` date NULL DEFAULT NULL COMMENT '购买日期',
  `create_at` datetime(0) NULL DEFAULT NULL COMMENT '添加时间',
  `update_at` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `create_by` int(10) NULL DEFAULT NULL COMMENT '添加人',
  `update_by` int(10) NULL DEFAULT NULL COMMENT '最后更新人',
  `seq` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '全局唯一，用于导入撤销',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sn`(`sn`) USING BTREE,
  INDEX `idx_host`(`host`) USING BTREE,
  INDEX `idx_state`(`state`) USING BTREE,
  INDEX `idx_idc_id`(`idc_id`) USING BTREE,
  INDEX `idx_cabinet_id`(`cabinet_id`) USING BTREE,
  INDEX `idx_model_id`(`model_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

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
-- Table structure for asset_ping
-- ----------------------------
DROP TABLE IF EXISTS `asset_ping`;
CREATE TABLE `asset_ping`  (
  `host` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'ip地址',
  `status` int(1) NOT NULL COMMENT '是否可达 可达为1 不可达为0',
  `rtt` double(7, 2) NULL DEFAULT NULL COMMENT '传输的时间延迟Round-Trip Time ，单位：ms',
  `last_update` datetime(0) NULL DEFAULT NULL COMMENT '最后更新时间',
  INDEX `idx_host`(`host`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for asset_tag
-- ----------------------------
DROP TABLE IF EXISTS `asset_tag`;
CREATE TABLE `asset_tag`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `asset_id` int(10) NOT NULL COMMENT '资产id',
  `tag` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'key',
  `value` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'value',
  `seq` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '全局唯一UUID，用于导入撤销',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_asset_id`(`asset_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for cabinet
-- ----------------------------
DROP TABLE IF EXISTS `cabinet`;
CREATE TABLE `cabinet`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '机柜名称',
  `idc_id` int(10) NOT NULL COMMENT '数据中心id',
  `u_size` int(10) NULL DEFAULT NULL COMMENT 'u位容量',
  `remark` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `seq` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '全局唯一，用于导入撤销',
  `x` int(10) NULL DEFAULT NULL COMMENT '在机房的那一列',
  `y` int(10) NULL DEFAULT NULL COMMENT '在机房的那一行',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_idc_id`(`idc_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for chart
-- ----------------------------
DROP TABLE IF EXISTS `chart`;
CREATE TABLE `chart`  (
  `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `span` int(10) NOT NULL DEFAULT 12,
  `height` int(10) NOT NULL DEFAULT 100,
  `create_at` datetime(0) NOT NULL,
  `type` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'line,bar,table',
  `panel_id` int(10) UNSIGNED NOT NULL,
  `unit` int(10) NOT NULL DEFAULT 2 COMMENT '单位',
  `prev` int(10) NULL DEFAULT NULL,
  `next` int(10) NULL DEFAULT NULL,
  `param` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '用于记录不同type类型对应的param',
  `pid` int(10) NULL DEFAULT NULL COMMENT '模板id',
  `build_in` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '0' COMMENT '是否内置 1:内置，0：非内置，默认：0',
  `seq` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '导入唯一标识 全局唯一，用于导入撤销',
  `weight` int(10) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_panel_id`(`panel_id`) USING BTREE,
  INDEX `idx_type`(`type`) USING BTREE,
  INDEX `idx_pid`(`pid`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chart_element
-- ----------------------------
DROP TABLE IF EXISTS `chart_element`;
CREATE TABLE `chart_element`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `expression` varchar(4096) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'nomal: up{asset=\"192.168.40.40\",module=\"kafka\"}',
  `type` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'normal,expert',
  `chart_id` int(10) UNSIGNED NOT NULL,
  `legend` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `build_in` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '0' COMMENT '是否内置 1:内置，0：非内置，默认：0',
  `seq` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '导入唯一标识 全局唯一，用于导入撤销',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_chart_id`(`chart_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for conf_event
-- ----------------------------
DROP TABLE IF EXISTS `conf_event`;
CREATE TABLE `conf_event`  (
  `table` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `value` bigint(20) NULL DEFAULT 0,
  PRIMARY KEY (`table`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of conf_event
-- ----------------------------
INSERT INTO `conf_event` VALUES ('asset', 0);
INSERT INTO `conf_event` VALUES ('endpoint', 0);
INSERT INTO `conf_event` VALUES ('idc', 0);
INSERT INTO `conf_event` VALUES ('idc_traffic', 0);
INSERT INTO `conf_event` VALUES ('module', 0);
INSERT INTO `conf_event` VALUES ('rule', 7);
INSERT INTO `conf_event` VALUES ('scrape_interval', 0);
INSERT INTO `conf_event` VALUES ('server', 1);
INSERT INTO `conf_event` VALUES ('storage_local_retention', 0);

-- ----------------------------
-- Table structure for endpoint
-- ----------------------------
DROP TABLE IF EXISTS `endpoint`;
CREATE TABLE `endpoint`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `module_id` int(10) NOT NULL COMMENT '组件id',
  `asset_id` int(10) NOT NULL COMMENT '资产id',
  `host` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'exporter地址',
  `port` int(10) NULL DEFAULT NULL COMMENT 'export端口',
  `param` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'exporter参数,json格式,示例：{\"keys\":\"values\"}',
  `path` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '请求路径，默认 /metric',
  `seq` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '导入唯一标识 用于导入撤销',
  `labels` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '使用json对象保存{\"key\":\"value\"}',
  `enabled` int(4) NOT NULL DEFAULT 1 COMMENT '可选值\r\n\r\n0：停用\r\n\r\n1：启用',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_port`(`port`) USING BTREE,
  INDEX `idx_host`(`host`) USING BTREE,
  INDEX `idx_module_id`(`module_id`) USING BTREE,
  INDEX `idx_asset_id`(`asset_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for endpoint_state
-- ----------------------------
DROP TABLE IF EXISTS `endpoint_state`;
CREATE TABLE `endpoint_state`  (
  `id` int(11) NOT NULL,
  `endpoint_id` int(11) NOT NULL,
  `create_time` datetime(0) NULL DEFAULT NULL,
  `state` int(11) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for idc
-- ----------------------------
DROP TABLE IF EXISTS `idc`;
CREATE TABLE `idc`  (
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
-- Table structure for idc_traffic
-- ----------------------------
DROP TABLE IF EXISTS `idc_traffic`;
CREATE TABLE `idc_traffic`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `idc_id` int(10) NOT NULL COMMENT '	数据中心id',
  `direction` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '流量方向 可选值 rx：接收流量\r\n\r\ntx：发送流量',
  `ifindex` int(10) NOT NULL COMMENT '端口号',
  `ifdescr` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '端口描述',
  `tags` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标签 格式为json对象',
  `host` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'ip地址',
  `port` int(10) NOT NULL DEFAULT 161 COMMENT 'snmp端口 默认为161',
  `version` int(1) NOT NULL DEFAULT 2 COMMENT 'snmp版本 默认为2',
  `community` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'public' COMMENT 'snmp团体名 默认为public',
  `auth` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'snmp认证信息 json格式',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_idc_id`(`idc_id`) USING BTREE,
  INDEX `idx_direction`(`direction`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for link
-- ----------------------------
DROP TABLE IF EXISTS `link`;
CREATE TABLE `link`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `create_by` int(10) NULL DEFAULT NULL COMMENT '创建人id',
  `url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'url',
  `prev` int(10) NOT NULL COMMENT '前一个panel 记录前一个link.id\r\n\r\n如果前面没有内容了，记录为 0',
  `next` int(10) NOT NULL COMMENT '后一个panel 记录后一个link.id\r\n\r\n如果后面没有内容了，记录为 -1	',
  `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '1:内置，0：非内置，默认：0',
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
-- Table structure for metadata_label
-- ----------------------------
DROP TABLE IF EXISTS `metadata_label`;
CREATE TABLE `metadata_label`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `metric_id` int(10) NOT NULL COMMENT '指标ID',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'label名称',
  `value` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'lable值',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 271062 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for metadata_metric
-- ----------------------------
DROP TABLE IF EXISTS `metadata_metric`;
CREATE TABLE `metadata_metric`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `metric` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '指标名称',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '指标类型 默认：-',
  `help` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述信息',
  `unit` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '单位',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6089 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for model
-- ----------------------------
DROP TABLE IF EXISTS `model`;
CREATE TABLE `model`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '型号名称',
  `vendor` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '厂商',
  `type` int(10) NULL DEFAULT NULL COMMENT '类型',
  `remark` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `seq` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '全局唯一，用于导入撤销',
  `usize` int(10) NULL DEFAULT NULL COMMENT '大于0',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE,
  INDEX `idx_type`(`type`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for module
-- ----------------------------
DROP TABLE IF EXISTS `module`;
CREATE TABLE `module`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` int(10) NOT NULL COMMENT '系统id',
  `type` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'module类型 可选值http，snmp',
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '组件名称',
  `port` int(10) NOT NULL COMMENT 'export默认端口 type=snmp时，默认 161',
  `param` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'exporter默认参数',
  `path` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '默认请求路径，默认 /metric',
  `remark` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `build_in` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '0' COMMENT '1:内置，0：非内置，默认：0',
  `seq` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '导入唯一标识 用于导入撤销',
  `snmp_param` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT 'snmp配置参数,保存格式为json',
  `snmp_yml` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT 'snmp_exporter需要的配置部分,由web端生成保存',
  `labels` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '使用json对象保存{\"key\":\"value\"}',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_projectId_name`(`project_id`, `name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 58 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of module
-- ----------------------------
INSERT INTO `module` VALUES (1, 1, 'http', 'node_exporter', 9100, '', '/metrics', NULL, '1', NULL, NULL, NULL, NULL);

-- ----------------------------
-- Table structure for notification_script
-- ----------------------------
DROP TABLE IF EXISTS `notification_script`;
CREATE TABLE `notification_script`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '通知方式名称，唯一主键',
  `file_path` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '脚本路径，路径为 nz-web所在服务器路径',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for panel
-- ----------------------------
DROP TABLE IF EXISTS `panel`;
CREATE TABLE `panel`  (
  `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `create_by` int(10) NULL DEFAULT NULL,
  `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'dashboard' COMMENT 'panel类型 可选值：dashboard，asset，project，module，endpoint, model 默认值：dashboard',
  `link` int(10) NULL DEFAULT NULL COMMENT '关联id, 当type ！= dashboard 时不能为空',
  `build_in` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '0' COMMENT '是否内置 1:内置，0：非内置，默认：0',
  `prev` int(10) NOT NULL COMMENT '前一个panel',
  `next` int(10) NOT NULL COMMENT '后一个panel',
  `seq` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '导入唯一标识 全局唯一，用于导入撤销',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE,
  INDEX `idx_type`(`type`) USING BTREE,
  INDEX `idx_link`(`link`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 284 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for project
-- ----------------------------
DROP TABLE IF EXISTS `project`;
CREATE TABLE `project`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '应用名称',
  `remark` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '描述信息',
  `build_in` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '0' COMMENT '1:内置，0：非内置，默认：0',
  `seq` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '导入唯一标识 用于导入撤销',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of project
-- ----------------------------
INSERT INTO `project` VALUES (1, 'Common', '系统内置project，展示系统基础信息', '1', NULL);

-- ----------------------------
-- Table structure for project_topo
-- ----------------------------
DROP TABLE IF EXISTS `project_topo`;
CREATE TABLE `project_topo`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` int(10) NOT NULL COMMENT 'project  id',
  `topo` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '拓扑图配置信息',
  `update_at` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `update_by` int(10) NULL DEFAULT NULL COMMENT '更新人id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uni_projectid`(`project_id`) USING BTREE COMMENT '唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for project_topo_icon
-- ----------------------------
DROP TABLE IF EXISTS `project_topo_icon`;
CREATE TABLE `project_topo_icon`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'icon名称',
  `bytes` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'icon图片字节',
  `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '截取自图片后缀，支持jpg，jpeg，png，gif',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of project_topo_icon
-- ----------------------------
INSERT INTO `project_topo_icon` VALUES (1, 'common', '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', 'png');
INSERT INTO `project_topo_icon` VALUES (2, 'mysql', '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', 'png');
INSERT INTO `project_topo_icon` VALUES (3, 'oracle', 'iVBORw0KGgoAAAANSUhEUgAAAGAAAABqCAYAAABDA1ReAAAAAXNSR0IArs4c6QAAAERlWElmTU0AKgAAAAgAAYdpAAQAAAABAAAAGgAAAAAAA6ABAAMAAAABAAEAAKACAAQAAAABAAAAYKADAAQAAAABAAAAagAAAADjg0fvAAASkElEQVR4Ae1dWWwcR3qu6pnhcHhKvA9dpC5bEiVGpL2WbckRbG8EbJKHLBDEu5uXYIHF5mUDJNgNAjgveQmCYJHkIQcS5CFIDGwAI8AGi9jedbzrQ9ZuKEuyTFonKYkSr+F9zZA905Xvq2H39IxmhkOK5Ayp/oFmV1fX+X/1//VXdc1PKQpASinj1vBwTcyUDXFL1IuYaFCGqDcEwpbVoISsV1LVCiGDUoggmhhUQuFuPys8Sx2/0vwlKdQS3i8pIXBXeJa4C/2Md1Gh5KQw1JgURhjxY5YUYakvNVZSIsJHmpsnpZTWSnlbdkP/Nod6lSoRd0YOx4U4JqQ6KpRqU1K2obZ2XHvw7NucmtdbqowJKR4CvH6ANYBS+n2GuIlW9p3Y33wb4MTWW3KufBsCwO2Jiaro5PJpdKDLEla3VLITI/EQOuPPVfn2eSdNMOo22nsFktqDTvXUlhtXmpqaFp60D+sC4O7dyeoFFT0vlHgFTD+HRnRSrTxpY7ZXfi0RlyEZv0C7f95QIT9cDyB5A9DXP7Q/psTvgNG/CRE9i9Ed2F4M29zWYj6JYg76X9z/W0jff3W0N47mU2NOAEZGRspH56w3kOj3MdLPgvk50+dT4VORRso4GPWeIYx/O36w8W1IiZmt3xkZ2jsw1hSPxb6HyfM7Sond2TJ78XlxYEga4u/KRegfDx6smUnPkQLA4KAKTS0N/zF0+w9g9lWkJ/aen4QDMmwY4s9PtDf/MyQCxmGCHAD67oyciCnrP8H4Z+2X3n0TOCDlJ6I0+Hun9tTC5MWswT+f9w+dU5b4H9jmZXz2aLM5IMN+Ic8fP9zcK3/5+e0zoVD5uxj5lZtdrVd+CgeGzFj0ghGz1A895qcwZqseWqLR+A+NaCTSDQC2qlKvnhUOkOdLkcgrRiwW889MwzqCvenRFnEAvCbPY/FYQO/VRKNRMRGPi+rqauH375Dtmy3i5VqrwYAXMzMzwjQTazOH24yYGJ8QFZUVorwMxhDW1B5tIAcw6hcWF8X83DyUT1LbOACwKr6Ym5sTCwsLory8XJSWlgqfr8h2jTeQJ1tRlGVZIhKJaJ4ynE4pANgvmZBA8KJKIhDBYFAEAt7+m82jXHdlKRFdigoYOGJ5edk13h/PlREAdzLqrPn5eX1RGggEASnBZySPkhywsJJdii6JKK6lZX6US6qZZKrHQ6sC4M4Sx0S9CD3Gi1OE318iSjCP+yEZlI6naQInf01zCSPc1KPcNDHS8+O5m6ViTQC4cyYasIxGLDvRBlDx24D4CYhPGD6/8GEXajtTHB+uYzETV0xbLybC8Zizn/ZEXVs3AJlqtYBKYkSkbn9LA8AQCKgwXpQU3g18dPVhr1YWAUAW9Da24AWlHPa5ZnAMYcZRvWwWbSgA2RrJScm0TMf2zZTOAAiJC+cZAIr9jK1bnTxxQ1g/Ig1i7Xf4UJRSpH5Cnfg+LVg3Gci7gnGRmjKhqi0wnIOnELQlAOTTMVpemcy0fPJu5zTbWzlvZ86vtN0DoMAgegB4ABSYAwWu3pOAAgOQtxVEs6+hbpeoLA+JirKQCMCWX4hExfxCRIxPzYpFLMHTiavk5oaa9GhhYWETxQpyZm5BLJurH7kMlgREU33idAzt9cFhnq9dnUKlQVG3u0qUh4KiLMSNRQN7NMvYLlgWEdwXFqNianZeF+RuK9s0PDaZs4L6mmqUyfPB2Sk8OSMWI4/zxZ0jLwAqwPQTh/eLqorUb/a7qxMnV2g+3n0wLO49HE2xs8m4o2173PWlhLnCvDuYyJfyIu1hb3O9aN/b5MTOzi9q8JyItICBhV/bniZcjXo9kfbaeWQ5l67e0M/uts4hfjUAWhtrRUPtLqesTAGC/cQA1OyqFKePHUJH9ApIDIcnxdjENDacTFFdWa47WYJ9oMMHWgUB+az3bqa2aEkJQ1IqykrFLuQLQDo4Io8g38zsgjMSM2VuSZOiPU21OQF4ruOIbhvLimNwPBgaQ/pFPfo50sshDbWQDD/q3wgaHZ/KyOiFxdyjn3XnlADu7Rw7uNdh/u17Q2Lg4YjT5mkwjmB0nzgsEuJeLZrrazRITqKVwOzCorh975F+IpQvdR8XZVARJIJsqwId4fpDFVIaLNGAEzS2qbFut7jR/xDbBo9vEexrqXeYzxH4We8dMQ9V4yaqzPsAJTGk3G/WFx6CuqK6WQ/lHAL7Wxu07mTB84uRFObblUWgT6l+bDrS1uoAZsel37nofzQynh6d8XlPU52Op94fX+mkH/tITQAhnRh/aH+LE33n/tBjzHdeIlCYzQd3C4TICQBHpk25Jj7qS3sypS7lJL0auSewbKOH6qIOkx33eobGJsRDF2g2MO56KspLoVYSX/CoIjkyi51yqiDqSpvSxdiO550jiZMNGUYqh57nBJeJOJfsaawTLQ34BRKI6bKlbcFER5UzPjUD/W2uXMtaJXH+4Xzibpe7vatNfpnatt64BgySUGnqB6rR8cQ8uVqZWQHgHj51r01kQC6K4tMbWK+T0OxLp8ba3aLuK1XafLV3MTl/XL95Lz2p80xLg/RoZMKJ46i2LaJWAHlz4KHzjqamTdT/W0WtK2rSXd8czHNK4WqUFQBaD7zsjymrWQy26LNC03z8YwUtHl42cQLtu/NA12HHue+7q3A6AwxdxmmNsclp59Wj0XFteRFErjFuYWK3t6P5wcQmfgzaKqJlyDWFmzg35kNZAWDmRSy0KssTtj+lgahmo5BLWrhAS6cRmGo37g4KjmqarATj2KF94uqX/elJ9XMrTE0SGf3cyaM6bP8hwxlPlUdbnGYgacG16HHPMXa+zbqPhKc2xwpy61eag9mIzHd3OH00MB8Xa5youVjj6plE5tlqRkes/KG0UWWRuOLmusF98WONTVwT2OSulyYuV+3FTjkl4MFQWJt7HG1kVP/gSEa9dgArTqYh0aLhMj8bccL+EpLw3MkjOsnR9j1icmZeRFxbGU1YS1BCprFVkWmOYFVfOXVUg1NTXannKup8St4UyuKCkO1h2T3Xb2drSlHEJ4dShuZwr+bRaGIC5Kjr7sCCy6VqmOXgvmbBrQIS9TqZuxpx0UWzksS5o+PIAR22/9gmJs1OApN+0cIZHkuoHXtw2Hk5r3C/iERwnof6cltHdjqC9Ez7Xvsx9Q7w7Dkr/Z5p8cZ5Mj0dn+3dg9TCU59ySgCT3hp4JGjbc/OJHeEKlnslVCfVlWXQw4nDWiaee9H5fK0PlssytYqpKteWDSWMaoN7TvwgPgrdmo04GXPVS6J02otBSkHfnfuauZyId6Hsl7qO6cmcWwOMo8lI4E2cbLjR//iAYRtePdOZsWrOWbTe3HTymTb3oxN27zU5kWmBVQEgI6703RW0yQ9htHMypg1uE0c91Q47Yi/G7He57kzLleqzB/fpZO0om1sE9vqAExutsGxEg4AdJFhsE+co5ifRVJ2YntMg1NVUaUuOA6WkOnmyj1I0OpEd4Gz1bnS8fPejy2takXMEVWKlSxHjaMvX3Nrohq+lPI54Si/VFYGnfZ6vpK6lnvWk9eOAxwIObCSH9CqlxCC22TbOVslasNccJMU4UMh7nIpSPy8YZ572isF7w/CLP4NsZrcbn3YmbVb/wXPy3njtha7PDaHe8EDYLE5nKJfMB8/Je8esfe+TK504n/cW5gPvh9oZeLZhUVLcwNHLN7760q9dZZkOAHy4eHEwNK/Cf4HdrT/Edkvxr+PZ6G1CMMAi0DJ/XyHr33zxxb3OploKAHZffnbpemM8tvwncLz03bVYSHZ+757kgLYypfoHn7/kr197oWM0+SYRygiAnejixd6aeWvpdwHCN1DQy9iFzJnezve037HegAJRH4Nnb4WU/0dnz57MuuLLm6E//bh3H07zfB0ZLuAT2Dms3pJfP552jqP/4EsUfz4EX97Bp6y3X3/5+IN82JIRALqtmVwe/g4Y/UfwOHgNW1Nv7Qo2/njvXql1F+eKRTH2CnYhXoWaOoOPkl1PGyCa4UJehnfHT7Ep8H6ZaPiFrdvJv+noyG8paX0TXy5OAZi/qSlp/iebf25gUgCAijGu9w9/D/Lzp5iIG1ITyjlU9hN8x38HG2jvHTtQ7xyF6OlRgemlq3DUp17A7k2nVKID4B3bKfMH9TiY2AcFfB3bx1fxfGlXsPNqd3fSE1bfvXCzGYt9FV8+LmBQfg19T55oICOlHMNE/Jcd7c1/CxXlbHI5APTeHdoXV+LfAcJZN+OzhZHxGr6Yvw9XlJ+WBgKfHt1blzj0s5KB88X7H19vs2TsBBp/CIC2oxVtAKYNq+8DxWZlaStFyXto6wAkegAM60db7xjK/8WrL3cMUK+7eXFzcLw1appnoO3PwHx/FS9Pud9nC6Ocj3xSfOv4wRatojQA1/qHzqKQH6PCXdkyrhaPDgzCmd8lNOQqNuqu+w3xxdH9jffSG85yCM5HV67URSK+RjiPajQs2WRJ1Sgs2YgfFO1G56vQMHyCU5VIWgWmVCETnbgGEA6gHj9GI7c27d3cGNhjAtgY+mCiDQjDcasSs+AbtkghvTiAgTJm4bh1Cg5cRw0lRy1DjcB96WhJsHTk17uOTGRr6837owdiljiBnd8OtKET9byAAZTlY8JqnMJ7KaYxeH/7VHvLR7a/oHcgMtm/OeZRZqYkENV5dLwPjb6DSvuxABkA0P0+nxp4tq35ETqc/IqeqYAtisOA8H85MNwaj8s2MKZdKYuS2o62H0LbqUo33H0beDMbiSxckBev3vol3BI8v0V9darhaMMoGgcwo5joRzCqR3FMjKd7p9DpWUxeidEr1ZwhfbOQhKiMWzFsXpmm34pJ028avrg+92HFfTi1aPoV7pYVCyif4UfxpZaKQ3KkliLUUQWGUpp247d7+IaqIG2yCfVBCkUdpdJp3BYF4BLiV/L9T6/FautqoZa2vP4t6mZxVgOponOUOP0F+Tx/QVsMEkRf+wsC7/Uk5vkL2joAeHjM8xe0dfxO1oRRv2Z/QWWhUFG4EUj2YvuF+Ov8xfX6C6J3p1J8zA4BCM89zdrAp58gOmqK4uACJ9xsZC9kMr5nRhbCy4cjgiE6biqh4yZkg+3mkYsDUDE8G0VfQRH44KOTj3woJwDuAligdtwk5mGwYhkKEAJw2kTJoK8g93lNd76dGuZZV/rZ40g3eYH5uUZ6Nj7kDYC7AFbEY+O86F+ORPczNhjaeRMMLLqp2QlETyt0YUOG20ynW5uNoHUBkKliNshWV/Z7SgV9A+mL/oIC9BeE55WfEdnpiuXu+ArC7xvIcJqMvDjaN4s2DIBMDdRH0iGeFFM3UYVph02QEDptMgw4bwJYiQtxK8/0G4QNOT3fMMRdLM492FZ4fA6CDsbWAnf6dB7NMh1GavgLYlssK64P7ibCK8/4qKGdNWEArUeF6GY9wZ9NBSBbu9hRjjBBKV79VzzZitkR8YmBtSO6sj074QFQYNw8ADwACsyBAlfvSYAHQIE5UODqPQnwACgwBwpcvScBHgAF5kCBq/ckwAOgwBwocPWeBHgAFJgDBa7ekwAPgAJzoMDVexLgAVBgDhS4ek8CPAAKzIECV+9JQKEBwCmP9XmdLnDDd0L1OGAzTQn4eCd0Zlv2QapPDGn43sRxpvxOkm7LXhZno8lz8t54/cXOKzig9i00c2MOOxZnf4utVXHyXPOeLXvt5a4f+YS8gKDz6/dia/EOas8weU2es0+QhCR90HOzzowu0F/Qt3HCsiDHFpOt2Vkhreal/JdAafmb57uPOv+9IgUAu8sfXPz8GTMe/z7OcH4Dp12Ddrx3Xw8H5BKsnbcCPt9fnX/xZOI/BrmKyQiA/f7dT641GCr+BwDimzhofMKO9+6rcwDm/Rdg/H9Y0vevv/HSqbFsOXIC4M70s0uXT+L36V/HL8q/hl+5ny7EL8vd7Sm2cOKX/+oz3H9i+NXbdMiXTxvzBsBd2Ae/6m3Cv/N+HafMz/GChCRcobsTPQVhjPBbmEU/5BUIBH96/vnjI2vt9roASK+EqkqoeBfA6MKc0YUfUXSi4P07RUr06BbiPowTeDqUl8Hwy6Ey0XPu9On8/qVfOsNczxsCgKs8J9jTM1Q2Y4afUXHrWfwf6yMApw0OMtrQAVyipdjAIZPRriEMoAE4+IC/IDEAt8K3pM/4sjpQf6O7uyXzfyVyery+wKYBkKs5H8A9jPi/vjr4oa7HvxKv96lYPXqPS9IffQPDYAT+CaUsxagrwYQWBFOCABAWmQzCICjRYYV4khRL8FK1zDvywU8QfAVJtYR0uNMrsML/VJGT6CxH7Bh+tBRmOC79YfyXpjAckofFc8fGzxfAfc7/AzUP1GcAgokmAAAAAElFTkSuQmCC', 'png');
INSERT INTO `project_topo_icon` VALUES (4, 'redis', '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', 'png');

-- ----------------------------
-- Table structure for prom_server
-- ----------------------------
DROP TABLE IF EXISTS `prom_server`;
CREATE TABLE `prom_server`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `idc_id` int(10) NOT NULL COMMENT '所在idc',
  `host` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '部署ip',
  `port` int(10) NOT NULL DEFAULT 9090 COMMENT '端口 默认9090',
  `type` int(10) NOT NULL COMMENT '1:global,负责汇聚所有per-datacenter采集的数据及告警配置\r\n\r\n2:per-datacenter，负责根据exporter配置采集监测数据',
  `status` int(1) NOT NULL DEFAULT 0,
  `check_time` datetime(1) NULL DEFAULT NULL COMMENT '最后操作时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_idc_id`(`idc_id`) USING BTREE,
  INDEX `idx_type`(`type`) USING BTREE,
  INDEX `idx_port`(`port`) USING BTREE,
  INDEX `idx_host`(`host`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for qrtz_blob_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_blob_triggers`;
CREATE TABLE `qrtz_blob_triggers`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `BLOB_DATA` blob NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) USING BTREE,
  INDEX `SCHED_NAME`(`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) USING BTREE,
  CONSTRAINT `qrtz_blob_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_calendars
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_calendars`;
CREATE TABLE `qrtz_calendars`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `CALENDAR_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `CALENDAR` blob NOT NULL,
  PRIMARY KEY (`SCHED_NAME`, `CALENDAR_NAME`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_cron_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_cron_triggers`;
CREATE TABLE `qrtz_cron_triggers`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `CRON_EXPRESSION` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TIME_ZONE_ID` varchar(80) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) USING BTREE,
  CONSTRAINT `qrtz_cron_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_fired_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_fired_triggers`;
CREATE TABLE `qrtz_fired_triggers`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `ENTRY_ID` varchar(95) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `INSTANCE_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `FIRED_TIME` bigint(13) NOT NULL,
  `SCHED_TIME` bigint(13) NOT NULL,
  `PRIORITY` int(11) NOT NULL,
  `STATE` varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `JOB_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `JOB_GROUP` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `IS_NONCONCURRENT` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `REQUESTS_RECOVERY` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`, `ENTRY_ID`) USING BTREE,
  INDEX `IDX_QRTZ_FT_TRIG_INST_NAME`(`SCHED_NAME`, `INSTANCE_NAME`) USING BTREE,
  INDEX `IDX_QRTZ_FT_INST_JOB_REQ_RCVRY`(`SCHED_NAME`, `INSTANCE_NAME`, `REQUESTS_RECOVERY`) USING BTREE,
  INDEX `IDX_QRTZ_FT_J_G`(`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) USING BTREE,
  INDEX `IDX_QRTZ_FT_JG`(`SCHED_NAME`, `JOB_GROUP`) USING BTREE,
  INDEX `IDX_QRTZ_FT_T_G`(`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) USING BTREE,
  INDEX `IDX_QRTZ_FT_TG`(`SCHED_NAME`, `TRIGGER_GROUP`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_job_details
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_job_details`;
CREATE TABLE `qrtz_job_details`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `JOB_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `JOB_GROUP` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `DESCRIPTION` varchar(250) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `JOB_CLASS_NAME` varchar(250) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `IS_DURABLE` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `IS_NONCONCURRENT` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `IS_UPDATE_DATA` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `REQUESTS_RECOVERY` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `JOB_DATA` blob NULL,
  PRIMARY KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) USING BTREE,
  INDEX `IDX_QRTZ_J_REQ_RECOVERY`(`SCHED_NAME`, `REQUESTS_RECOVERY`) USING BTREE,
  INDEX `IDX_QRTZ_J_GRP`(`SCHED_NAME`, `JOB_GROUP`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_locks
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_locks`;
CREATE TABLE `qrtz_locks`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `LOCK_NAME` varchar(40) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  PRIMARY KEY (`SCHED_NAME`, `LOCK_NAME`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_paused_trigger_grps
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_paused_trigger_grps`;
CREATE TABLE `qrtz_paused_trigger_grps`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_GROUP`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_scheduler_state
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_scheduler_state`;
CREATE TABLE `qrtz_scheduler_state`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `INSTANCE_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `LAST_CHECKIN_TIME` bigint(13) NOT NULL,
  `CHECKIN_INTERVAL` bigint(13) NOT NULL,
  PRIMARY KEY (`SCHED_NAME`, `INSTANCE_NAME`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_simple_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_simple_triggers`;
CREATE TABLE `qrtz_simple_triggers`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `REPEAT_COUNT` bigint(7) NOT NULL,
  `REPEAT_INTERVAL` bigint(12) NOT NULL,
  `TIMES_TRIGGERED` bigint(10) NOT NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) USING BTREE,
  CONSTRAINT `qrtz_simple_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_simprop_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_simprop_triggers`;
CREATE TABLE `qrtz_simprop_triggers`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `STR_PROP_1` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `STR_PROP_2` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `STR_PROP_3` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `INT_PROP_1` int(11) NULL DEFAULT NULL,
  `INT_PROP_2` int(11) NULL DEFAULT NULL,
  `LONG_PROP_1` bigint(20) NULL DEFAULT NULL,
  `LONG_PROP_2` bigint(20) NULL DEFAULT NULL,
  `DEC_PROP_1` decimal(13, 4) NULL DEFAULT NULL,
  `DEC_PROP_2` decimal(13, 4) NULL DEFAULT NULL,
  `BOOL_PROP_1` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `BOOL_PROP_2` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) USING BTREE,
  CONSTRAINT `qrtz_simprop_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_triggers`;
CREATE TABLE `qrtz_triggers`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `JOB_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `JOB_GROUP` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `DESCRIPTION` varchar(250) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `NEXT_FIRE_TIME` bigint(13) NULL DEFAULT NULL,
  `PREV_FIRE_TIME` bigint(13) NULL DEFAULT NULL,
  `PRIORITY` int(11) NULL DEFAULT NULL,
  `TRIGGER_STATE` varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_TYPE` varchar(8) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `START_TIME` bigint(13) NOT NULL,
  `END_TIME` bigint(13) NULL DEFAULT NULL,
  `CALENDAR_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `MISFIRE_INSTR` smallint(2) NULL DEFAULT NULL,
  `JOB_DATA` blob NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) USING BTREE,
  INDEX `IDX_QRTZ_T_J`(`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) USING BTREE,
  INDEX `IDX_QRTZ_T_JG`(`SCHED_NAME`, `JOB_GROUP`) USING BTREE,
  INDEX `IDX_QRTZ_T_C`(`SCHED_NAME`, `CALENDAR_NAME`) USING BTREE,
  INDEX `IDX_QRTZ_T_G`(`SCHED_NAME`, `TRIGGER_GROUP`) USING BTREE,
  INDEX `IDX_QRTZ_T_STATE`(`SCHED_NAME`, `TRIGGER_STATE`) USING BTREE,
  INDEX `IDX_QRTZ_T_N_STATE`(`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`, `TRIGGER_STATE`) USING BTREE,
  INDEX `IDX_QRTZ_T_N_G_STATE`(`SCHED_NAME`, `TRIGGER_GROUP`, `TRIGGER_STATE`) USING BTREE,
  INDEX `IDX_QRTZ_T_NEXT_FIRE_TIME`(`SCHED_NAME`, `NEXT_FIRE_TIME`) USING BTREE,
  INDEX `IDX_QRTZ_T_NFT_ST`(`SCHED_NAME`, `TRIGGER_STATE`, `NEXT_FIRE_TIME`) USING BTREE,
  INDEX `IDX_QRTZ_T_NFT_MISFIRE`(`SCHED_NAME`, `MISFIRE_INSTR`, `NEXT_FIRE_TIME`) USING BTREE,
  INDEX `IDX_QRTZ_T_NFT_ST_MISFIRE`(`SCHED_NAME`, `MISFIRE_INSTR`, `NEXT_FIRE_TIME`, `TRIGGER_STATE`) USING BTREE,
  INDEX `IDX_QRTZ_T_NFT_ST_MISFIRE_GRP`(`SCHED_NAME`, `MISFIRE_INSTR`, `NEXT_FIRE_TIME`, `TRIGGER_GROUP`, `TRIGGER_STATE`) USING BTREE,
  CONSTRAINT `qrtz_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) REFERENCES `qrtz_job_details` (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 77 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_area
-- ----------------------------
DROP TABLE IF EXISTS `sys_area`;
CREATE TABLE `sys_area`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `pid` int(10) NOT NULL DEFAULT 0 COMMENT '父id，没有为 0 ',
  `pids` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '所有父id',
  `longitude` double(11, 6) NOT NULL COMMENT '经度',
  `latitude` double(11, 6) NOT NULL COMMENT '纬度',
  `i18n` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'i18n内容，json对象保存，key为语言简称，value为对应翻译',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_area
-- ----------------------------
INSERT INTO `sys_area` VALUES (1, 'Aktau', 0, '0', 73.063460, 50.223100, '{\"cn\":\"Aktau\",\"en\":\"Aktau\"}');
INSERT INTO `sys_area` VALUES (2, 'Aktubinsk', 0, '0', 57.100000, 50.170000, '{\"cn\":\"Aktubinsk\",\"en\":\"Aktubinsk\"}');
INSERT INTO `sys_area` VALUES (3, 'Almaty', 0, '0', 76.928611, 43.256667, '{\"cn\":\"Almaty\",\"en\":\"Almaty\"}');
INSERT INTO `sys_area` VALUES (4, 'Nur-Sultan', 0, '0', 71.450000, 51.166666, '{\"cn\":\"Nur-Sultan\",\"en\":\"Nur-Sultan\"}');
INSERT INTO `sys_area` VALUES (5, 'Atyrau', 0, '0', 51.883333, 47.116667, '{\"cn\":\"Atyrau\",\"en\":\"Atyrau\"}');
INSERT INTO `sys_area` VALUES (6, 'Karaganda', 0, '0', 73.100000, 49.500000, '{\"cn\":\"Karaganda\",\"en\":\"Karaganda\"}');
INSERT INTO `sys_area` VALUES (7, 'Kokshetau', 0, '0', 69.400000, 53.283333, '{\"cn\":\"Kokshetau\",\"en\":\"Kokshetau\"}');
INSERT INTO `sys_area` VALUES (8, 'Kostanay', 0, '0', 63.606160, 53.227440, '{\"cn\":\"Kostanay\",\"en\":\"Kostanay\"}');
INSERT INTO `sys_area` VALUES (9, 'Kyzylorda', 0, '0', 65.495400, 44.854740, '{\"cn\":\"Kyzylorda\",\"en\":\"Kyzylorda\"}');
INSERT INTO `sys_area` VALUES (10, 'Pavlodar', 0, '0', 76.966666, 52.283333, '{\"cn\":\"Pavlodar\",\"en\":\"Pavlodar\"}');
INSERT INTO `sys_area` VALUES (11, 'Petropavl', 0, '0', 69.150000, 54.866666, '{\"cn\":\"Petropavl\",\"en\":\"Petropavl\"}');
INSERT INTO `sys_area` VALUES (12, 'Semey', 0, '0', 80.266694, 50.426749, '{\"cn\":\"Semey\",\"en\":\"Semey\"}');
INSERT INTO `sys_area` VALUES (13, 'Shymkent', 0, '0', 69.600000, 42.300000, '{\"cn\":\"Shymkent\",\"en\":\"Shymkent\"}');
INSERT INTO `sys_area` VALUES (14, 'Taldykurgan', 0, '0', 78.383333, 45.016666, '{\"cn\":\"Taldykurgan\",\"en\":\"Taldykurgan\"}');
INSERT INTO `sys_area` VALUES (15, 'Taraz', 0, '0', 71.366667, 42.900000, '{\"cn\":\"Taraz\",\"en\":\"Taraz\"}');
INSERT INTO `sys_area` VALUES (16, 'Uralsk', 0, '0', 51.434590, 51.249110, '{\"cn\":\"Uralsk\",\"en\":\"Uralsk\"}');
INSERT INTO `sys_area` VALUES (17, 'Ust-Kamenogorsk', 0, '0', 82.600000, 49.966666, '{\"cn\":\"Öskemen\",\"en\":\"Öskemen\"}');
INSERT INTO `sys_area` VALUES (18, 'Zhezkazgan', 0, '0', 67.766667, 47.783333, '{\"cn\":\"Zhezkazgan\",\"en\":\"Zhezkazgan\"}');

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `id` bigint(19) NOT NULL AUTO_INCREMENT,
  `param_key` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'key',
  `param_value` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态   0：隐藏   1：显示',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `param_key`(`param_key`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 151 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '系统配置表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES (3, 'alert_api', '', 1, 'alert api 地址');
INSERT INTO `sys_config` VALUES (4, 'alert_path_prefix', '/nz-admin', 1, '告警推送请求前缀');
INSERT INTO `sys_config` VALUES (5, 'alert_interval', '15', 1, '告警间隔时间');
INSERT INTO `sys_config` VALUES (6, 'init_project_system', '1', 1, '内置 project：system');
INSERT INTO `sys_config` VALUES (7, 'init_module_node_exporter', '1', 1, '内置 module: node_exporter');
INSERT INTO `sys_config` VALUES (11, 'node_exporter_shell_name', 'init_exporter.sh', 1, '内置 node_exporter 初始执行脚本名称');
INSERT INTO `sys_config` VALUES (12, 'asset_ping_switch', 'on', 1, '主机连通性检查开关，on：开启，off：关闭');
INSERT INTO `sys_config` VALUES (13, 'asset_ping_command', 'fping -f {filename} -e', 1, 'fping命令');
INSERT INTO `sys_config` VALUES (14, 'confagent_port', '8989', 1, 'confagent内置端口');
INSERT INTO `sys_config` VALUES (15, 'confagent_path', '/nz-confagent', 1, 'confagent内置路径');
INSERT INTO `sys_config` VALUES (16, 'confagent_api', '192.168.40.42:8989', 1, 'confagent内置api');
INSERT INTO `sys_config` VALUES (17, 'telnet_user_tip', 'ogin:', 1, 'telnet用户名提示');
INSERT INTO `sys_config` VALUES (18, 'telnet_password_tip', 'assword:', 1, 'telnet密码提示');
INSERT INTO `sys_config` VALUES (19, 'telnet_relogin_password_tip', 'assword:', 1, 'telnet relogin密码提示');
INSERT INTO `sys_config` VALUES (20, 'max_terminal_num', '20', 1, '一个登陆用户最多允许打开窗口数量，默认20');
INSERT INTO `sys_config` VALUES (21, 'ldap_address', '', 1, NULL);
INSERT INTO `sys_config` VALUES (22, 'ldap_dn', '', 1, NULL);
INSERT INTO `sys_config` VALUES (23, 'ldap_password', '', 1, NULL);
INSERT INTO `sys_config` VALUES (30, 'ldap_user_filter', '', 1, NULL);
INSERT INTO `sys_config` VALUES (31, 'ldap_mapping', '', 1, NULL);
INSERT INTO `sys_config` VALUES (32, 'ldap_enable', 'off', 1, NULL);
INSERT INTO `sys_config` VALUES (33, 'geoJson', '{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{\"name\":\"Akmola Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ÔdƊĳʪèāƂNŎǀ\\\\ǔ§DȆȨC˔ŷǈƾ»ìƞ¦ƪXÄĢʈĤÉŤļÂRƌÐŁLĉǂâHƚƬјƬËjĜĚɐUűȞW¿ūǖCĠŋŘLOôŘXAĆτEUąȦu˖ĴƖǯiŗĥ¾ąȸùʒCĶȺΜ¦¾ėeǩŸήë¿ͦSà¥@ԣǛƭMŅǸd|ȊÙɱmƛ½ŧƉŗzɥ^čƂJŞ³ƍûȳAÓǃ¹ǋvĽśƿYÛ·äÏċ©ĥą­ré͕R[ȔťĂˇÇɷƑL¥ǭ}ëígšĽŵ«ÖïȽõƩ]nŀéò̧Éćþ˭Úĥ˵ÍǵÀCɰ¢RȪʏJ̯ƻȓnēſËȏBΣďÇŬüĵɡʓĊįsÙŤĝŇŴštÓġƹosÆ®ƾǧĸļƄÞȌ³²řBĞƪRȚÐĞɦŊÂPȖ\",\"@@ÒõƒkƈĊ¿Ŧǽrĭſ\"],\"encodeOffsets\":[[67591,53693],[72928,52338]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Aktobe Province\"},\"geometry\":{\"type\":\"MultiPolygon\",\"coordinates\":[[\"@@Л̉ƸƬŨLŖĴf\"],[\"@@ƖʆǎĀǬQ˄˄zŜı²^żÙÊČÀâŀƾø¥ĎfƤŏpDĔƒXƊğȢdçȦţɶāͤŒêºƺƴĈÄ¦ĨľÿÆøǂ]ɎxưŧƶÉª¼ƪOǮĈʸx¼ŚfÓŜRĬćįƴ«ĎĭҚ¿ĊğʠPƸŞdŬƘŤĝО[՞ŲĴδĺdȊNķưCŶ·­đǜĿŹ©SƇƢ¢¾ǁńġĎſƂũÀŏƃeǇ×Ņį§ŏƽȰȳď¯ĒƱʠƙƦäĢÅåė|Û˴ã̖ůȶʃĔƗƂđɗǑƝÅǇĻΫŻɳƛ̱ªrðɯǞĵ^ŗƬųĖӅQyֱ͟έXiǯ́_̡țǩƍϗϑåTćơbŏÍŭɡӵŋ੉ʁřؾŝǦsĞĿŖyŘҥĮʋĆǽŮħǆįŜÆɂCŖ²ǄmƬƇŠ~Ƣ÷Ą¦ǬaŔƍƗgíџļ@ǄȂƅyǝĆ\"]],\"encodeOffsets\":[[[60468,46905]],[[54918,50423]]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Almaty\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ʴ¤wēČěıĵhűèÏƮ\"],\"encodeOffsets\":[[78580,44354]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Almaty Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ĖŘӠ͜ɴ~ʶŀʄXτƨċŤ¬ĨÁľϾÜûĪXÚÒǼP÷ҐŦ¤ɒŬĦņûƊŞ˶ǃłO¤ïϒ˄ʩϞŖƢ­I«֚ȭɘOĜßɸÝƶƇÃŦ×ĘȟĮŋJħÖÛĎEɪŃÝKĿęǧÄ̃YĵńƛBƱā˧ȅ÷ǙPɕȉ×»|ɩšŶęŒnªąĀuĈýÁŅ¸ǃWƳňʑȒ̑ÅōĀuFơǏCǕőŜĽȿĩǵ¶ûXƷªħė]ǭǰϵxƃŤƏĄdĚɧ}ʍÌų¸š͏GƿÂ̛\\\\ǁaá¼ʭNţÙƻfƭ½űÐƕiùħ_ƅĈʉä×ȔĆŞˇøă¾ʠǘ_Ķ¤È]ĲƇĪǝòǅ^șņFǶĵĒɧĦǶ˧oİ~ÊɺAĀç̬ĨŦĤ\",\"@@ÐƭŲçĶgĲċĜxĔʳ£\"],\"encodeOffsets\":[[75839,47106],[78580,44354]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Astana\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ĮƀǾqÀťƇĉƑlÑö\"],\"encodeOffsets\":[[72928,52338]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Atyrau\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ֺÃݒÇɸ©ЬŋOĔʾUȊvǠŞҦȺʒì´ĮǨTĴΤ£UğłVŌ·ŜhȂéĜDBǀÜƤĐIOȠĴ¬π}̰ȈǞąƆzȁ@ǃѠĻîƘhƎbœ¥ǫøă}ơƈşnƫ±ǃDŕÅɁİśĨǅǾŭʌąҦĭzŗŀŕtĝ́nƑĘÓgãƖǛ\\\\īĊǽŷƱXɍ_ŝϓÕɯEÝĳǯĥЯEǦʎcǄē¨Ŕę¸źǯĺÓʅtď°ǑãȩŭÛĥjoØı¢ȳt÷ū¥ǷYēóƑc«ỚŁįí³ĉčIYŏ±û}ƃǃīí°ƿ¨ċöı\\\\ȋö°ȯn^â̤ªÜ¤çŚǡǸǅˈËAȩɾċ¬ϯ^ȥâſǅâYĀŋŦ¼Ȥ\"],\"encodeOffsets\":[[48242,49392]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"East Kazakhstan\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@΢Ĺʜ¿ǘ¨ǦȜŊÙnkȆƅ[Ç²DĒƳąŠįĖŊ¸ːƸæžȰc̚ɒͤѣ̀ϟPúĠÒʐÌȬǀCĢɜµkɐµįǵ̒zȁŐ[ʬ¾˲ÉňǼ{ƞĔKÞƬgʔðǤhȰ·ˆŹ[¯ȎǏüǍaÇƄåȀBȒıƘÛǸȷXġ̼[ĀÁǦbðãŦöµÐĮČV˜ǸÖƙƃȎ»FÙČ¥GÑĊûȴqJıƛǻeřımÁöÿŗřÿ©ñŧ³œBċ«˽CęûŷÑǁöΣyÝÔÙDǯėí_ßɗJ̗ǝǩŶȅÏzѹ˳ðȉĪʫ¨·Ɖǣ̹¡ƣŗ̏±ƫķTťñƭ\\\\ËčFÕÜIĨĭŌėȠťØÄƵƈɷÞěàɗP֙ȮJ¬ơ®ϝŕ˃ʪϑ£ðŁP˵ǄƉŝŅüūĥɑť£ȡ^ûāɉĎĠpŸƸsǔ~ÜǊÔbɂđŎǿŐĴŐǔWŲŢŢæ·ÏĀ\\\\ĔǚŇèEÂʿǌ^¶кŘøú[ǂĴâƾ¾ȎŲYÜ\"],\"encodeOffsets\":[[79562,51428]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Jambyl Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ቒP٢I׸Aੈ@ģħťè̫ ÿɹB}Éį˨pǵɨĥĶđEǵȚŅǆ]Ǟñƈĩ^ı£Ç`ĵʟǗĄ½ˈ÷ąŝØȓʊãƆćęţŭ˵pͅŪďSƣŦũśÜÑɝsͯŵÿ]ƛËǹ®ōƟI¼ǅǫcƓĖȷǻØƉmɍĄȏÅęɇmąƛƭsƷ÷çǗŐâȫźơ¾dǒċŌǩŚʇªďZĒĝhļŀļǨvɜĲŒěвÃȠ»ÊŅ͐ėƆýɬǗȘ\"],\"encodeOffsets\":[[70725,47104]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Karagandy Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ɴƜάżǈļƞÆɘǒƁĒēƘɢƬ±Ĩ̐¦ġƎȖB¢ɚجğɤ¸nĂʠV˸ȾŊzĠƾƼĺǢÎ˨ʒɞƲOĲˬäņŰŹômĔȔ̰ƼʐIQȩ¡DɯǶ¿˶ÎĦˮÙĈý̨ÊêñmĿƪ^ȾöÕðŶ¬ľhŢìîǮ~K¦ɸƒˈÈŦā\\\\ȓ͖QqêĆ®ĦČªãÐÜ¸ǀZľŜǌuǄºBÔȴƎüŝ´ƁI]ĎɦŘy̠Ýdēµũ¬ĝĹÃwīČ­ɀTƀȒ¯˶ǑLçƘÙǡÏŏÝÇƖUˊŤˀbÀŦ}ǂ®ư´þɆtŀµŌˌÂ¤ȚxƪčǔaǐïƽĆ±ZÛȍűƽ½ĳá\\\\ǁ÷ùйŗ]µˀǋFÁňçǙ[ēÐÿΈåšűšǓXĳŏȀŏĒōɁÓaÛǉ} ǓƷtoŷğɊčüĂȢ]øҏǻOÙÑĩWÛüϽĽħÂţ«ƧČσʃWʵĿɳ}ӟ͛ĕŗੇ@׷B١JቑOۡBࢿ@Ťȡ|ȋ[ڇhʱǵČșƐǗtгǀɥ¬˥ĀĝIȗĊǥĔŤ͛îËO\"],\"encodeOffsets\":[[64461,48324]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Kostanay Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@RȃþŁũʊűƗuƽÛìǑŎDKœÃ§DįŜBPʹŦBÅǏHſjáɂW[ëǃNTŇğEY˝OȕŉÁĝɥșÏƩQĝŚA´±ȋƃÝĻǨķ­ƽtÅƺpÔĢŢsňųĞÚţİtʔĉɢĶûÈūΤĐȐAƀÌźóŅů˫ãPıɝƱ˧ʑǡÍƻĹğƽŉy˷ȽʟUmāɣ·ثĠ¡əȕAĢƍ̏¥²ħɡƫȵʄ̕Ű˳ä{ÜæĘġÆƥãʟƚđƲĐ°ȯȴŐƾİ¨ņǈØƄf¿ŐƁŪčƀŃĢ½ǂơ¡TƈźªǛŀ®Ēŵ¸ƯDĸȉMşÌêʫ°ǟNÁŮͯDī|NŶɣĀŇläǒEˀƔǦA̎ǈïƈŝ^DĴŅlEĐĺTʊǲŊ¦ŜäƎéÎɒ£ƜWĺ˭Ôġèū§Ń¸ƥRÏļŤĘŊ±ƒÐÈƋTƧāªȁX˜ƘûĞƄƜƾRø×ƀlŎÉƾ[ö˪IĚėĞNŚǬɰOÒâ̰pʈÄƲBмĂɎYȤÐŬÿİuƀΞĠĪĈøŘjĀÝ\"],\"encodeOffsets\":[[67662,55952]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Kyzylorda Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ÌP͜íēţǦȘĉĞJ˦ÿɦ«дƿǘsȚƏǶċʲڈgȌ\\\\Ȣ{ţ@Çňí¾ûãùɩÇăǼıŀSċj̵eǯċĶÓŴoØėɆŝāû\\\\ŏăąÉĕƅsĥîŵԏȏċ«ࠍӛß`rȒD̺Ʌãʇδ̽ǦƵȂÏjͭƑ֩zёªङŏȧYӡдʋȐŔĭª࠽ӺŁvʌ·ĊƧă˵ϹǆМ¢ÖĲWlłǓéSĈæϘϒǪƎ̢Ȝ̂`jǰήWֲ͠zӆRŴĕŘƫĶ]ɰǝqï̲©\"],\"encodeOffsets\":[[64461,48324]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Mangystau Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@аFǰĦÞĴɰFϔÖŞɎ`ƲWǾŸĬĉǜ[äƕÔhƒė̂mŞǥŚؽصƩ@Ӂ@ַFᎭĿN̓ãƷŞǙȂƓȈDÖƟŪҽ˞խùӯƕЧ˗AŦÑŞ|ĎļňĚȬÎØ]ĞĿÐşōQǙ®ȡŝǚĕÎè˳~ʰĪĩÐŋƦ{ńŃĈ{ƌÃ̃¬ƯüSƈÖĎβŮTǬřČ@ĂÆŀ\\\\ĈįGķʐìņ}žŠŘǔŢ¼ʔNʒąƼT͆ǭWȂĨŲG˴ŝ̭ĻăùóŹșˏPÃĲwľĸŖǶPúǘŶŚmǤÊƞfɬW;ȢĹ©äƧÎěĺĩvÈƿŨȯkÕāɱƧƕǎ̙ÊJúöƈ\"],\"encodeOffsets\":[[54469,47117]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"North Kazakhstan\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ʲÂݶĺ¨¢Ș{ǮîǴiǞfĬãÆö°̲HĔƘǀµIĸȨµ©Вjƶ±îïɸ¿Ⱦł˜GǂǝƱŜơǎķý°ȋȓ_ƬȯĸüǌuA¹ȞNŰ˼ĔŜèvĹľŧµĉĲŞàĀ͖ÔĥȞǮIRÆȾ¨ÝǕȧjǵʷƆñŢœűOsŁĠ{ŜÐĺǗƁúÙĳŜIȈĸąȗŝµÇƨͥTÀέìŷfǪ½ĘΛ¥ȹDĵʑȷú½ĆĦjŘƕǰ˕ĳȥvVĆσFBąŗWPóŗKğŌǕDÀŬȝXŲɏVęiěƫÌїƙƫáGĊǁłK ÏQƋĻÁÊţʇģÃġƩWƝ¥¼ëǇƽ˓ŸȧDCȅǓ¨ƿ[ōƁMçĂʩƉĴÓcZ˞ĠFSňǄM\\\\ìɁXiâƀǐGAÆťOʺśACİÄ¨LŔōCëǒƾÜƘvʉŲŪýłQȄ\"],\"encodeOffsets\":[[67662,55952]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Pavlodar Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ǦĲƪtâÔƞkǸËĔėĆĪyĸƦkðĸÎɄ˦ƖCĢȈ|ԔƊ²ĊȀǘx°ÖŰĭĹşģǭCĒĻڪͻӊʭͲ΁Ϫэ˔˳Ɇ˥̙ɑȯdŽƷåˏŉ·İĕĆşƴCđÈ±Ɔ\\\\lȅÚmŉǥțǗ§ʛÀΡĺą²ƾǏðǓbƩĎșwÁ£ŋˋĿ¶Ʌs³ýƯǁ­ť~¿ʿaˉţƕVÈŐÞǢÐƗÚKè˵ǒȑ°ſȿSċ®xĬĺÄ«Ğ¶ŪcĔ̟ÞŨƊƜ¾ɲnȉÚ{ǷcNņǜƮ@Ԥß¦Ƨ¶ÈȘŞĆȇķśJĴùÚǘƂĹśÏğ|tłŲPšŔ\"],\"encodeOffsets\":[[75202,54718]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"South Kazakhstan Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@à_ࠎӜČ¬ԐȐŶítĦĖƆĆÊĄ[ŐĂüɅŞ×ĘųpĵÔČfǰi̶TČĿǻĲÈĄɪäú½üŇî@Èࣀ@ۢAǘȗþɫĘƅņ͏¼ÉÄȟĜбıőuɛĻǧĻĿĞgYđĐʈ©ǪřČŋcǑƢ½ȬŹ áǘŏdğíEģñ«ėƃĀſÓƋƿƳÏe˷ÑùċÇLƅŋɓÓCġȡŝƇơÅcJǻxōĝFʅĞȭŦĤĀ@ĘĕIeľč]ō¢̱±փ\\\\ċ̒×͒҅ºFΤÎm\"],\"encodeOffsets\":[[67657,43355]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"West Kazakhstan\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@áĿċ¿ÚÉ]ŻĲ±yś˃˃ǫRǍÿƕʅ̯ȇο~ĳ«PȟďJÛƣAƿěCȁêśgŋ¸ŁUVĠΣ¤ĳǧS³ĭʑëҥȹǟŝȉuʽVPēЫŌɷªݑÈֹÄZ̓îɉäʌѤĨ~ŘĜŤǷĜĸѲɼÒƞƆăĈĤĚÈîŜŴĹƪĉƀţđŚřƴqʜðƄĆƟȪã̀ƌKǐòİĒʘÄsȮ͎bɚŨǴ~òĎǢèIŀ̚üФ¨ƓǬÂ`÷Ƅ¢îļĐǒqȶîȘɽȌ¨ĪЪ\\\\ƴÇhıÈÙ̮ËĠ¯Àťè­Ɇ½ķȓŊý\"],\"encodeOffsets\":[[55853,51741]]}}],\"UTF8Encoding\":true}', 1, NULL);
INSERT INTO `sys_config` VALUES (35, 'email_enable', 'off', 1, NULL);
INSERT INTO `sys_config` VALUES (36, 'generator_path', '/opt/nezha/nz-web/generator', 1, 'generator路径');
INSERT INTO `sys_config` VALUES (37, 'email_smtp_host', '', 1, NULL);
INSERT INTO `sys_config` VALUES (38, 'email_smtp_port', '', 1, NULL);
INSERT INTO `sys_config` VALUES (39, 'email_smtp_account', '', 1, NULL);
INSERT INTO `sys_config` VALUES (40, 'email_smtp_password', '', 1, NULL);
INSERT INTO `sys_config` VALUES (41, 'email_send_account', '', 1, NULL);
INSERT INTO `sys_config` VALUES (42, 'email_test_reciver', '', 1, NULL);
INSERT INTO `sys_config` VALUES (43, 'email_ssl_flag', 'off', 1, NULL);
INSERT INTO `sys_config` VALUES (44, 'email_tls_flag', 'off', 1, NULL);
INSERT INTO `sys_config` VALUES (45, 'asset_ping_interval', '1600', 1, NULL);
INSERT INTO `sys_config` VALUES (46, 'scrape_interval', '15', 1, NULL);
INSERT INTO `sys_config` VALUES (47, 'storage_local_retention', '15', 1, NULL);
INSERT INTO `sys_config` VALUES (48, 'system_name', 'One-stop monitoring system', 1, NULL);
INSERT INTO `sys_config` VALUES (49, 'ldap_ou', '', 1, NULL);
INSERT INTO `sys_config` VALUES (50, 'timezone', '8', 1, NULL);
INSERT INTO `sys_config` VALUES (52, 'export_pagesize', '1000', 1, 'alert_message到处时默认的每批次查询数量');
INSERT INTO `sys_config` VALUES (54, 'alert_notify_interval', '300', 1, '单位：s');
INSERT INTO `sys_config` VALUES (69, 'email_template', '<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"></head><body><div style=\"padding:40px 0; height:auto; min-height:100px; text-align:center;\"><span align=\"center\" style=\"margin:0 auto; min-width:290px; max-width:750px;\">${alertMessage.description}</span></div></body></html>', 1, '邮件模板');
INSERT INTO `sys_config` VALUES (70, 'email_smtp_protocol', 'smtp', 1, NULL);
INSERT INTO `sys_config` VALUES (71, 'test_email_send_template', '<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"></head><body><div style=\"padding:40px 0; height:auto; min-height:100px; text-align:center;\"><span align=\"center\" style=\"margin:0 auto; min-width:290px; max-width:750px;\">Nezha test sending email.</span></div></body></html>', 1, NULL);
INSERT INTO `sys_config` VALUES (72, 'reset_table_infos', '[\"Delete from account\",\"Delete from alert_rule where build_in !=1 \",\"Delete from asset\",\"Delete from cabinet\",\"Delete from chart\",\"Delete from chart_element\",\"Delete from endpoint\",\"Delete from idc\",\"Delete from maintenance_record\",\"Delete from model\",\"Delete from module where build_in !=1 \",\"Delete from panel\",\"Delete from project where build_in !=1 \",\"Delete from prom_server\",\"Delete from snmp_mib\",\"Delete from sys_dict where type IN (\'assetType\',\'vendor\')\",\"Delete from idc_traffic\",\"Delete from sys_user where username!=\'admin\'\"]', 1, '清空项目数据信息');
INSERT INTO `sys_config` VALUES (75, 'model_chart_tpl', '{\"title\":\"{{asset.sn}}\",\"type\":\"assetInfo\",\"span\":12,\"height\":400,\"unit\":2}', 1, NULL);
INSERT INTO `sys_config` VALUES (107, 'default_cabinet_usize', '40', 1, '机柜默认U位值');
INSERT INTO `sys_config` VALUES (110, 'node_exporter_name', 'node-exporter-1.0.0-1.x86_64.rpm', 1, '内置 node_exporter 压缩包名称');
INSERT INTO `sys_config` VALUES (111, 'node_exporter_path', './build_node_exporter', 1, '内置 node_exporter 项目中的路径');
INSERT INTO `sys_config` VALUES (112, 'node_exporter_target_path', '/tmp/nezha', 1, '内置node_exporter安装路径');
INSERT INTO `sys_config` VALUES (114, 'alert_export_header', '{\"cn\":{\"ID\":\"ID\",\"告警名称\":\"告警名称\",\"数据中心\":\"数据中心\",\"应用名称\":\"应用名称\",\"组件名称\":\"组件名称\",\"Endpoint\":\"Endpoint\",\"主机地址\":\"主机地址\",\"告警tag信息\":\"告警tag信息\",\"告警级别\":\"告警级别\",\"告警摘要\":\"告警摘要\",\"告警描述\":\"告警描述\",\"开始时间\":\"开始时间\",\"结束时间\":\"结束时间\",\"状态\":\"状态\"},\"en\":{\"ID\":\"ID\",\"Alert Name\":\"Alert Name\",\"Datacenter\":\"Datacenter\",\"Project\":\"Project\",\"Module\":\"Module\",\"Endpoint\":\"Endpoint\",\"Asset\":\"Asset\",\"Labels\":\"Labels\",\"Severity\":\"Severity\",\"Summary\":\"Summary\",\"Description\":\"Description\",\"StartAt\":\"StartAt\",\"EndAt\":\"EndAt\",\"State\":\"State\"}}', 1, 'alert_message导出表头');
INSERT INTO `sys_config` VALUES (118, 'map_center_config', '{\"longitude\":51.16,\"latitude\":71.45,\"zoom\":4,\"minZoom\":3,\"maxZoom\":7}', 1, '地图配置');
INSERT INTO `sys_config` VALUES (132, 'i18n_mapping', '{\"assetState\":{\"en\":{\"1\":\"In stock\",\"2\":\"Not in stock\",\"3\":\"Suspended\"},\"cn\":{\"1\":\"在库\",\"2\":\"出库\",\"3\":\"停用\"}},\"chartType\":{\"en\":{\"line\":\"Line Chart\",\"bar\":\"Histogram\",\"table\":\"Table\",\"stackArea\":\"Stack Area\",\"singleStat\":\"SingleStat\",\"url\":\"URL\",\"alertList\":\"Alert list\",\"text\":\"Text\"},\"cn\":{\"line\":\"曲线图\",\"bar\":\"柱状图\",\"table\":\"表格\",\"stackArea\":\"堆叠区域图\",\"singleStat\":\"SingleStat\",\"url\":\"URL\",\"alertList\":\"告警列表\",\"text\":\"文本\"}}}', 1, NULL);
INSERT INTO `sys_config` VALUES (133, 'asset_export_header', '{\"cn\":{\"*SN码\":\"资产序列号，唯一标识(必填) \\n示例: A0420200101\",\"*资产类型\":\"设备类型（必填） \\n示例: server\",\"*厂商\":\"厂商（必填） \\n示例: Cisco\",\"*型号\":\"型号(必填) \\n示例: CISCO-UCS-C220\",\"*数据中心\":\"数据中心(必填) \\n示例: 北京电信中心机房\",\"机柜\":\"机柜 \\n示例: cabinet-001\",\"开始U位\":\"机柜开始位置 此信息一般和机柜结束位置联合使用 \\n示例: 1\",\"结束U位\":\"机柜结束位置 此信息一般与机柜开始位置联合使用 \\n示例: 2\",\"*主机地址\":\"主机地址(必填),IP格式 \\n示例: 172.0.0.1\",\"状态\":\"资产状态，可选: 在库、出库、停用 \\n示例: 在库\",\"购买日期\":\"购买日期 \\n示例: 2020/2/25\",\"SSH账号端口\":\"SSH连接端口 \\n示例: 22\",\"SSH账号登录名\":\"SSH账号登录名 \\n示例: root\",\"SSH登录认证方式\":\"SSH登录认证方式 密码登录:password,密钥登录:key \\n示例: password\",\"SSH登录密码\":\"SSH登录密码 认证方式为password时登录密码 \\n示例: xxxxx\",\"SSH登录密钥\":\"SSH登录密钥 \\n示例: ssh-rsa CFdv7Jez6K...\",\"SSH密钥密码\":\"SSH密钥密码 \\n示例: xxxxx\",\"TELNET账号端口\":\"TELNET账号端口 \\n示例: 23\",\"TELNET账号登录名\":\"TELNET账号登录名 \\n示例: root\",\"TELNET用户名提示\":\"TELNET用户名提示 \\n示例: user\",\"TELNET登录密码\":\"TELNET登录密码 \\n示例: xxxxx\",\"TELNET密码提示\":\"TELNET密码提示 \\n示例: pass\",\"TELNET重启密码提示\":\"TELNET重启密码提示 \\n示例: pass\",\"SNMP账号端口\":\"SNMP账号端口 \\n示例: 161\",\"SNMP团体名\":\"SNMP团体名 \\n示例: public\",\"SNMP上下文名称\":\"SNMP上下文名称 \\n示例: 80002b8046h\",\"SNMP协议版本\":\"SNMP协议版本 支持V2和V3 \\n示例: 2\",\"SNMP用户名\":\"SNMPV3版本时,登录用户名 \\n示例: root\",\"SNMP安全级别\":\"SNMP安全级别 noAuthNoPriv(不认证不加密) authNoPriv(认证不加密) authPriv(认证且加密) \\n示例: noAuthNoPriv\",\"SNMP用户密码\":\"SNMP用户密码, 根据安全级别选择填写 \\n示例: xxxxx\",\"SNMP认证协议\":\"SNMP用户认证协议 MD5、SHA \\n示例: MD5\",\"SNMP加密协议\":\"SNMP加密协议 AES、DES \\n示例: AES\",\"SNMP加密密码\":\"SNMP加密密码 \\n示例: xxxxx\",\"资产标签\":\"资产标签,tag信息,格式: key=value 如需填写多个请使用回车换行 \\n示例: isOpen=true \\n isHighPerformance=true\",\"Exporter\":\"是否安装内置node_exporter，1：安装，0：不安装 \\n示例：1\"},\"en\":{\"*Sn\":\"Asset serial number, unique identifier (required) \\nExample: A0420200101\",\"*Type\":\"Asset type (required) \\nExample: server\",\"*Vendor\":\"Vendor (required) \\nExample: Cisco\",\"*Model\":\"Model (required) \\nExample: CISCO-UCS-C220\",\"*IDC\":\"Data Center (Required) \\nExample: Beijing Telecom Center Computer Room\",\"Cabinet\":\"Cabinet \\nExample: cabinet-001\",\"Cabinet Start\":\"Cabinet start position This information is generally used in conjunction with the cabinet end position \\nExample: 1\",\"Cabinet End\":\"Cabinet end position This information is generally used in conjunction with the cabinet start position \\nExample: 2\",\"*Host\":\"Host address (required), IP format \\nExample: 172.0.0.1\",\"State\":\"Asset status, optional: In stock, Not in stock or Suspended \\nExample: In stock\",\"PurchaseDate\":\"Purchase date \\nExample: 2020/2/25\",\"SSH Account Port\":\"SSH account connection port \\nExample: 22\",\"SSH UserName\":\"SSH account login name \\nExample: root\",\"Methed\":\"SSH login authentication method, Password login: password, key login: key \\nExample: password\",\"SSH Password\":\"SSH login password The login password when the authentication method is password \\nExample: xxxxx\",\"PrivateKey\":\"SSH login key file \\nExample: ssh-rsa CFdv7Jez6K...\",\"Key Password\":\"SSH key password \\nExample: xxxxx\",\"TELNET Port\":\"TELNET account port \\nExample: 23\",\"TELNET Username\":\"TELNET account login name \\nExample: root\",\"User Tip\":\"TELNET username prompt \\nExample: user\",\"TELNET Password\":\"TELNET login password \\nExample: xxxxx\",\"Password Tip\":\"TELNET password prompt \\nExample: pass\",\"Relogin Tip\":\"TELNET relogin password prompt \\nExample: pass\",\"SNMP port\":\"SNMP account port \\nExample: 161\",\"Community\":\"SNMP community name \\nExample: public\",\"Context Name\":\"SNMP context name \\nExample: 80002b8046h\",\"Version\":\"SNMP protocol version supports V2 and V3 \\nExample: 2\",\"SNMP User\":\"In SNMPV3 version, login user name\\nExample: root\",\"SecurityLevel\":\"SNMP security level noAuthNoPriv (no authentication and no encryption) authNoPriv (authentication and no encryption) authPriv (authentication and encryption) \\nExample: noAuthNoPriv\",\"SNMP Password\":\"SNMP user password, fill in according to the security level \\nExample: xxxxx\",\"Auth Protocol\":\"SNMP auth protocol MD5, SHA \\nExample: MD5\",\"Priv Protocol\":\"SNMP priv protocol AES, DES \\nExample: AES\",\"Priv Password\":\"SNMP priv password \\nExample: xxxxx\",\"Tags\":\"Asset tag information, format: key=value If you need to fill in more than one, please use carriage return and line feed \\nExample: isOpen=true \\n isHighPerformance=true\",\"Exporter\":\"Whether to install the built-in node_exporter, 1: install, 0: not install \\nExample: 1\"}}', 1, 'asset导出表头');
INSERT INTO `sys_config` VALUES (135, 'rule_export_header', '{\"cn\":{\"*告警规则名称\":\"告警规则名称(必填) \\n示例: cpuAlert\",\"*运算表达式\":\"运算表达式(必填) \\n示例: 100-(avg(irate(node_cpu_seconds_total{mode=\'idle\'}[5m])) by(instance)* 100)\",\"*比较符号\":\"比较符号(必填) 可选: >,<,<=,>=,==,!= \\n示例: >\",\"*阈值\":\"阈值(必填) \\n示例: 10\",\"*单位\":\"单位(必填) 可选: \\nMisc: none,short,percent(0-100),percent(0.0-1.0),local format\\nData: bits,bytes,kilobytes,megabytes,gigabytes,terabytes,petabytes\\nDataRate: packets/sec,bits/sec,bytes/sec,kilobytes/sec,kilobits/sec,megabytes/sec,megabits/sec,gigabytes/sec,gigabits/sec,terabytes/sec,terabits/sec,petabytes/sec,petabits/sec\\nTime: Hertz(1/s),nanoseconds(ns),microseconds(us),milliseconds(ms),seconds(s),minutes(m),hours(h),days(d),YYYY-MM-DD HH:mm:ss,MM/DD/YYYY h:mm:ss a \\n示例: short\",\"*持续时间\":\"持续时间(必填),默认单位（秒） \\n示例:60\",\"*告警级别\":\"告警级别(必填) 可选 P1、P2、P3 \\n示例: P1\",\"*告警摘要\":\"告警摘要(必填) \\n示例: cpu告警\",\"*告警详细描述\":\"告警详细描述(必填) \\n示例: cpu使用率超过10%的告警规则\",\"告警通知人\":\"告警通知人，多个使用逗号分隔 \\n示例: admin / admin,root\"},\"en\":{\"*AlertName\":\"Alert rule name (required) \\nExample: cpuAlert\",\"*Expr\":\"Operation expression (required) \\nExample: 100-(avg(irate(node_cpu_seconds_total{mode=\'idle\')[5m])) by(instance)* 100)\",\"*Operator\":\"Operation symbol (required) Optional: >,<,<=,>=,==,!= \\nExample:>\",\"*Threshold\":\"Threshold (required) \\nExample: 10\",\"*Unit\":\"Unit (required) Optional: \\nMisc: none,short,percent(0-100),percent(0.0-1.0),local format\\nData: bits,bytes,kilobytes,megabytes,gigabytes,terabytes,petabytes\\nDataRate: packets/sec,bits/sec,bytes/sec,kilobytes/sec,kilobits/sec,megabytes/sec,megabits/sec,gigabytes/sec,gigabits/sec,terabytes/sec,terabits/sec,petabytes/sec,petabits/sec\\nTime: Hertz(1/s),nanoseconds(ns),microseconds(us),milliseconds(ms),seconds(s),minutes(m),hours(h),days(d),YYYY-MM-DD HH:mm:ss,MM/DD/YYYY h:mm:ss a \\nExample:short\",\"*Last\":\"Last (required), the default unit (seconds) \\nExample: 60\",\"*Severity\":\"Alert severity (required) optional P1, P2, P3 \\nExample: P1\",\"*Summary\":\"Alert summary \\nExample: Cpu warning\",\"*Description\":\"Alert description \\nExample: Alarm rule for cpu usage exceeding 10%\",\"Receiver\":\"Alert receiver, separated by commas \\nExample: admin / admin,root\"}}', 1, NULL);
INSERT INTO `sys_config` VALUES (136, 'chart_export_header', '{\"cn\":{\"*面板\":\"面板名称(必填) \\n示例: 系统面板\",\"*图表名称\":\"图表名称(必填) \\n示例: CPU使用率\",\"*图表类型\":\"图表类型(必填) 可选: 曲线图,柱状图,表格,堆叠区域图,SingleStat,URL,告警列表,文本 \\n示例: 曲线图\",\"*宽度\":\"宽度(必填) 可选：1-12 \\n示例: 10\",\"*高度\":\"高度(必填),默认单位（px/像素） \\n示例:400\",\"单位\":\"单位(必填) 可选: \\nMisc: none,short,percent(0-100),percent(0.0-1.0),local format\\nData: bits,bytes,kilobytes,megabytes,gigabytes,terabytes,petabytes\\nDataRate: packets/sec,bits/sec,bytes/sec,kilobytes/sec,kilobits/sec,megabytes/sec,megabits/sec,gigabytes/sec,gigabits/sec,terabytes/sec,terabits/sec,petabytes/sec,petabits/sec\\nTime: Hertz(1/s),nanoseconds(ns),microseconds(us),milliseconds(ms),seconds(s),minutes(m),hours(h),days(d),YYYY-MM-DD HH:mm:ss,MM/DD/YYYY h:mm:ss a \\n示例: short\",\"*参数\":\"单位 根据type不同，内容不同，格式为JSON格式 \\n示例: {\\\"statistics\\\":\\\"average\\\"}\",\"元素\":\"当type=line，bar，stackArea时，包含 elements key，值为对象数组。\\n 示例:[{\\\"expression\\\":\\\"sum(up)\\\",\\\"legend\\\":\\\"test\\\"}]\"},\"en\":{\"*Panel Name\":\"Panel(Required) \\nExample: System panel\",\"*Chart Title\":\"Chart title(Required) \\nExample: CPU usage\",\"*Chart Type\":\"Chart type(Required) Optional: Line Chart,Histogram,Table,Stack Area,SingleStat,URL,Alert list,Text \\nExample: Line Chart\",\"*Width\":\"width(Required) Optional：1-12 \\nExample: 10\",\"*Height\":\"height(Required),Default unit（px/Pixel） \\nExample:400\",\"Unit\":\"unit(Required) Optional: \\nMisc: none,short,percent(0-100),percent(0.0-1.0),local format\\nData: bits,bytes,kilobytes,megabytes,gigabytes,terabytes,petabytes\\nDataRate: packets/sec,bits/sec,bytes/sec,kilobytes/sec,kilobits/sec,megabytes/sec,megabits/sec,gigabytes/sec,gigabits/sec,terabytes/sec,terabits/sec,petabytes/sec,petabits/sec\\nTime: Hertz(1/s),nanoseconds(ns),microseconds(us),milliseconds(ms),seconds(s),minutes(m),hours(h),days(d),YYYY-MM-DD HH:mm:ss,MM/DD/YYYY h:mm:ss a \\nExample: short\",\"*Parameter\":\"Parameters match type，The format is JSON format \\nExample: {\\\"statistics\\\":\\\"average\\\"}\",\"Elements\":\"When the type is line, table, stackArea, singleStat, histogram, it contains elements key and the value is an object array.\\nExample:[{\\\"expression\\\":\\\"sum(up)\\\",\\\"legend\\\":\\\"test\\\"}]\"}}', 1, NULL);
INSERT INTO `sys_config` VALUES (137, 'asset_chart_export_header', '{\"cn\":{\"*图表名称\":\"图表名称(必填) \\n示例: CPU使用率\",\"*图表类型\":\"图表类型(必填) 可选: 曲线图,柱状图,表格,堆叠区域图,SingleStat,URL,告警列表,文本 \\n示例: 曲线图\",\"*宽度\":\"宽度(必填) 可选：1-12 \\n示例: 10\",\"*高度\":\"高度(必填),默认单位（px/像素） \\n示例:400\",\"单位\":\"单位(必填) 可选: \\nMisc: none,short,percent(0-100),percent(0.0-1.0),local format\\nData: bits,bytes,kilobytes,megabytes,gigabytes,terabytes,petabytes\\nDataRate: packets/sec,bits/sec,bytes/sec,kilobytes/sec,kilobits/sec,megabytes/sec,megabits/sec,gigabytes/sec,gigabits/sec,terabytes/sec,terabits/sec,petabytes/sec,petabits/sec\\nTime: Hertz(1/s),nanoseconds(ns),microseconds(us),milliseconds(ms),seconds(s),minutes(m),hours(h),days(d),YYYY-MM-DD HH:mm:ss,MM/DD/YYYY h:mm:ss a \\n示例: short\",\"*参数\":\"单位 根据type不同，内容不同，格式为JSON格式 \\n示例: {\\\"statistics\\\":\\\"average\\\"}\",\"元素\":\"当type=line，bar，stackArea时，包含 elements key，值为对象数组。\\n 示例:[{\\\"expression\\\":\\\"sum(up)\\\",\\\"legend\\\":\\\"test\\\"}]\"},\"en\":{\"*Chart Title\":\"Chart title(Required) \\nExample: CPU usage\",\"*Chart Type\":\"Chart type(Required) Optional: Line Chart,Histogram,Table,Stack Area,SingleStat,URL,Alert list,Text \\nExample: Line Chart\",\"*Width\":\"width(Required) Optional：1-12 \\nExample: 10\",\"*Height\":\"height(Required),Default unit（px/Pixel） \\nExample:400\",\"Unit\":\"unit(Required) Optional: \\nMisc: none,short,percent(0-100),percent(0.0-1.0),local format\\nData: bits,bytes,kilobytes,megabytes,gigabytes,terabytes,petabytes\\nDataRate: packets/sec,bits/sec,bytes/sec,kilobytes/sec,kilobits/sec,megabytes/sec,megabits/sec,gigabytes/sec,gigabits/sec,terabytes/sec,terabits/sec,petabytes/sec,petabits/sec\\nTime: Hertz(1/s),nanoseconds(ns),microseconds(us),milliseconds(ms),seconds(s),minutes(m),hours(h),days(d),YYYY-MM-DD HH:mm:ss,MM/DD/YYYY h:mm:ss a \\nExample: short\",\"*Parameter\":\"Parameters match type，The format is JSON format \\nExample: {\\\"statistics\\\":\\\"average\\\"}\",\"Elements\":\"When the type is line, table, stackArea, singleStat, histogram, it contains elements key and the value is an object array.\\nExample:[{\\\"expression\\\":\\\"sum(up)\\\",\\\"legend\\\":\\\"test\\\"}]\"}}', 1, NULL);
INSERT INTO `sys_config` VALUES (138, 'model_chart_export_header', '{\"cn\":{\"*图表名称\":\"图表名称(必填)支持{{asset.sn}},{{asset.host}},{{asset.tag.[key]}},{{asset.tag.key}}取值替换 \\n示例: CPU使用率\",\"*图表类型\":\"图表类型(必填) 可选: 曲线图,柱状图,表格,堆叠区域图,SingleStat,URL,告警列表,文本 \\n示例: 曲线图\",\"*宽度\":\"宽度(必填) 可选：1-12 \\n示例: 10\",\"*高度\":\"高度(必填),默认单位（px/像素） \\n示例:400\",\"单位\":\"单位(必填) 可选: \\nMisc: none,short,percent(0-100),percent(0.0-1.0),local format\\nData: bits,bytes,kilobytes,megabytes,gigabytes,terabytes,petabytes\\nDataRate: packets/sec,bits/sec,bytes/sec,kilobytes/sec,kilobits/sec,megabytes/sec,megabits/sec,gigabytes/sec,gigabits/sec,terabytes/sec,terabits/sec,petabytes/sec,petabits/sec\\nTime: Hertz(1/s),nanoseconds(ns),microseconds(us),milliseconds(ms),seconds(s),minutes(m),hours(h),days(d),YYYY-MM-DD HH:mm:ss,MM/DD/YYYY h:mm:ss a \\n示例: short\",\"*参数\":\"单位 根据type不同，内容不同，格式为JSON格式 ,支持{{asset.sn}},{{asset.host}},{{asset.tag.[key]}},{{asset.tag.key}}取值替换  \\n示例: {\\\"statistics\\\":\\\"average\\\"}\",\"元素\":\"当type=line，bar，stackArea时，包含 elements key，值为对象数组，其中 expression 支持{{asset.sn}},{{asset.host}},{{asset.tag.[key]}},{{asset.tag.key}}取值替换 \\n 示例:[{\\\"expression\\\":\\\"sum(up)\\\",\\\"legend\\\":\\\"test\\\"}]\"},\"en\":{\"*Chart Title\":\"Chart title(Required) Support {{asset.sn}},{{asset.host}},{{asset.tag.[key]}},{{asset.tag.key}} value replacement \\nExample: CPU usage\",\"*Chart Type\":\"Chart type(Required) Optional: Line Chart,Histogram,Table,Stack Area,SingleStat,URL,Alert list,Text \\nExample: Line Chart\",\"*Width\":\"width(Required) Optional：1-12 \\nExample: 10\",\"*Height\":\"height(Required),Default unit（px/Pixel） \\nExample:400\",\"Unit\":\"unit(Required) Optional: \\nMisc: none,short,percent(0-100),percent(0.0-1.0),local format\\nData: bits,bytes,kilobytes,megabytes,gigabytes,terabytes,petabytes\\nDataRate: packets/sec,bits/sec,bytes/sec,kilobytes/sec,kilobits/sec,megabytes/sec,megabits/sec,gigabytes/sec,gigabits/sec,terabytes/sec,terabits/sec,petabytes/sec,petabits/sec\\nTime: Hertz(1/s),nanoseconds(ns),microseconds(us),milliseconds(ms),seconds(s),minutes(m),hours(h),days(d),YYYY-MM-DD HH:mm:ss,MM/DD/YYYY h:mm:ss a \\nExample: short\",\"*Parameter\":\"Parameters match type，The format is JSON format,Support {{asset.sn}},{{asset.host}},{{asset.tag.[key]}},{{asset.tag.key}} value replacement \\nExample: {\\\"statistics\\\":\\\"average\\\"}\",\"Elements\":\"When the type is line, table, stackArea, singleStat, histogram, it contains elements key and the value is an object array.The expression support {{asset.sn}},{{asset.host}},{{asset.tag.[key]}},{{asset.tag.key}} value replacement \\nExample:[{\\\"expression\\\":\\\"sum(up)\\\",\\\"legend\\\":\\\"test\\\"}]\"}}', 1, NULL);
INSERT INTO `sys_config` VALUES (139, 'system_version', '20.11', 1, 'nezha系统版本');
INSERT INTO `sys_config` VALUES (140, 'endpoint_alert_rule', '{\"alertName\": \"0\",\"for\": \"30s\",\"expr\": \"up{endpoint!=\'\'}>-1\"}', 1, NULL);
INSERT INTO `sys_config` VALUES (141, 'endpoint_alert_push', '1', 1, NULL);
INSERT INTO `sys_config` VALUES (142, 'query_max_series', '50', 1, '限制 prometheus query_range 接口 result 数组大小 区间范围[-1 - 1000] -1表示不限制');
INSERT INTO `sys_config` VALUES (143, 'export_max_line', '10000', 1, '告警导出最大行数');
INSERT INTO `sys_config` VALUES (144, 'endpoint_query_interval', '30', 1, 'endpoint 状态采集定时任务周期 单位 s 秒，默认和 prometheus scrape_interval 保持一致');
INSERT INTO `sys_config` VALUES (145, 'scrape_timeout', '10', 1, NULL);
INSERT INTO `sys_config` VALUES (146, 'endpoint_export_header', '{\"cn\":{\"*系统\":\"系统名称(必填) \\n示例: Nezha\",\"*组件\":\"组件名称(必填) \\n示例: node_exporter\",\"*组件类型\":\"组件类型(必填) 可选: HTTP、SNMP 当type = SNMP时，组件必须存在 \\n示例: HTTP\",\"资产序列号\":\"资产序列号(非必填) 与 主机地址 不能同时为空 \\n示例: A0420200101\",\"端口号\":\"端口号 范围0-65535 \\n示例: 9100\",\"参数\":\"端点查询参数，JSON格式， \\n示例: {\\\"key\\\":\\\"value\\\"}\",\"标签\":\"标签，JSON格式， \\n示例: {\\\"key\\\":\\\"value\\\"}\",\"*路径\":\"路径（必填） 请求路径，\\n示例: /metrics\",\"主机地址\":\"主机地址（非必填） 与 资产序列号 不能同时为空 \\n示例: 127.0.0.1\",\"启用\":\"启用状态（非必填） 可选 0:停用 1:启用,为空时默认为1 \\n示例: 1\"},\"en\":{\"*Project Name\":\"Project Name(Required) \\nExample: Nezha\",\"*Module Name\":\"Module Name(Required) \\nExample: node_exporter\",\"*Module Type\":\"Module type(Required) Optional: HTTP or SNMP，When type = SNMP, the component must exist \\nExample: HTTP\",\"Sn\":\"Asset serial number(Not required) And host address cannot be empty at the same time \\nExample: A0420200101\",\"Port\":\"Port number range 0-65535 \\nExample: 9100\",\"Parameter\":\"Endpoint query parameters, JSON format  \\nExample: {\\\"key\\\":\\\"value\\\"}\",\"Labels\":\"Endpoint lebel, JSON format  \\nExample: {\\\"key\\\":\\\"value\\\"}\",\"*Path\":\"Path (required) Request path \\nExample: /metrics\",\"Host\":\"Host address (not required) and asset serial number cannot be empty at the same time \\nExample: 127.0.0.1\",\"Enabled\":\"Enable state (not required) Optional values 0: Disable 1: Enable, when empty, the default is 1 \\nExample: 1\"}}', 1, NULL);
INSERT INTO `sys_config` VALUES (147, 'ha_mode', '2', 1, '1：Nezha，mariadb，redis分别部署 ,2：nezha，mariadb，redis共同部署在一台设备');
INSERT INTO `sys_config` VALUES (148, 'ha_vip', '192.168.40.42', 1, '当 ha_mode = 2时有效，记录nz-web对外提供的 vip');
INSERT INTO `sys_config` VALUES (149, 'develop_admin', 'admin', 1, NULL);
INSERT INTO `sys_config` VALUES (150, 'ldap_query_timeout', '3000', 1, 'ldap template 查询超时时间，单位 milliseconds 毫秒');

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
) ENGINE = InnoDB AUTO_INCREMENT = 221 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '系统字典表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_dict
-- ----------------------------
INSERT INTO `sys_dict` VALUES (3, '资产状态', 'assetState', 1, '在线', '', 0, '在线', 0, 1, '2019-11-25 14:54:43', NULL);
INSERT INTO `sys_dict` VALUES (4, '资产状态', 'assetState', 2, '下线', '', 0, '下线', 0, 1, '2019-11-25 14:54:46', NULL);
INSERT INTO `sys_dict` VALUES (5, '认证类型', 'authType', 1, 'Password', '', 0, '密码认证', 0, 1, '2019-11-28 10:45:46', NULL);
INSERT INTO `sys_dict` VALUES (6, '认证类型', 'authType', 2, 'Certificate', '', 0, '证书认证', 0, 1, '2019-11-28 10:46:59', NULL);
INSERT INTO `sys_dict` VALUES (112, '图表类型', 'chartType', 1, 'line', '', 1, '折线图', 0, 1, '2019-12-13 17:53:19', NULL);
INSERT INTO `sys_dict` VALUES (113, '图表类型', 'chartType', 2, 'bar', '', 2, '条形图', 0, 1, '2019-12-13 17:53:19', NULL);
INSERT INTO `sys_dict` VALUES (114, '图表类型', 'chartType', 3, 'table', '', 3, '表格', 0, 1, '2019-12-13 17:53:19', NULL);
INSERT INTO `sys_dict` VALUES (115, '表达式类型', 'exprType', 1, 'normal', '', 1, '简单表达式', 0, 1, '2019-12-13 17:53:19', NULL);
INSERT INTO `sys_dict` VALUES (116, '表达式类型', 'exprType', 2, 'expert', '', 2, '用户输入表达式', 0, 1, '2019-12-13 17:53:19', NULL);
INSERT INTO `sys_dict` VALUES (141, '图表类型', 'chartType', 4, 'stackArea', '', 4, '堆栈面积图', 0, 1, '2019-12-13 17:53:19', NULL);
INSERT INTO `sys_dict` VALUES (212, '图表类型', 'chartType', 5, 'singleStat', '', 5, 'singleStat', 0, 1, '2020-03-31 10:57:30', NULL);
INSERT INTO `sys_dict` VALUES (213, '图表类型', 'chartType', 6, 'url', '', 6, 'url', 0, 1, '2020-03-31 10:57:33', NULL);
INSERT INTO `sys_dict` VALUES (217, '图表类型', 'chartType', 7, 'alertList', '', 0, '告警', 0, 1, '2020-05-27 17:50:33', NULL);
INSERT INTO `sys_dict` VALUES (218, '图表类型', 'chartType', 8, 'text', '', 8, '告警', 0, 1, '2020-05-27 17:50:33', NULL);
INSERT INTO `sys_dict` VALUES (220, '资产状态', 'assetState', 3, '停用', '', 0, '停用', 0, 0, '2020-11-09 14:19:49', NULL);

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
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_type`(`type`) USING BTREE,
  INDEX `idx_operation`(`operation`) USING BTREE,
  INDEX `idx_time`(`time`) USING BTREE,
  INDEX `idx_ip`(`ip`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 124 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '系统日志表' ROW_FORMAT = Compact;

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
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2111 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, 'dashboard', 'dashboard', 'dashboard.title', 0, 'sys:config:list,project:list,idc:taffic:list,idc:list,overview:stat,project:topo:list,module:stat,project:topo:icon:info', 1, '', 1);
INSERT INTO `sys_menu` VALUES (2, 'overview', 'overview', 'dashboard.overview.title', 1, 'sys:config:list,project:list,idc:taffic:list,idc:list,overview:stat,project:topo:list,module:stat,project:topo:icon:info', 1, '/overview', 1);
INSERT INTO `sys_menu` VALUES (3, 'panel', 'panel', 'dashboard.panel.title', 1, 'panel:list,chart:list,panel:modify,chart:modify,chart:weights', 1, '/panel', 2);
INSERT INTO `sys_menu` VALUES (4, 'explore', 'explore', 'dashboard.metricPreview.title', 1, 'panel:list', 1, '/explore', 3);
INSERT INTO `sys_menu` VALUES (5, 'asset', 'asset', 'overall.asset', 0, 'asset:tag:key,asset:list,sys:user:list,asset:filter,model:list,asset:info', 1, '/asset', 2);
INSERT INTO `sys_menu` VALUES (6, 'project', 'project', 'overall.project', 0, 'module:list,sys:user:list,project:list,project:info,project:topo:icon:list,module:stat,project:topo:icon:info,endpoint:list,project:topo:list,asset:info,asset:list,alert:message:list,idc:list,alert:rule:list,panel:list', 1, '/project', 3);
INSERT INTO `sys_menu` VALUES (7, 'alert', 'alert', 'alert.alert', 0, 'idc:list,alert:message:list,asset:list,module:list,project:list', 1, '', 4);
INSERT INTO `sys_menu` VALUES (8, 'alertList', 'alertList', 'alert.alertList', 7, 'idc:list,alert:message:list,asset:list,module:list,project:list,asset:info,endpoint:list,alert:rule:list', 1, '/alertList', 1);
INSERT INTO `sys_menu` VALUES (9, 'alertConfig', 'alertConfig', 'alert.alertConfig', 7, 'alert:rule:list,asset:list,alert:rule:stat,alert:rule:stat', 1, '/alertConfig', 2);
INSERT INTO `sys_menu` VALUES (10, 'settings', 'settings', 'overall.config', 0, 'alert:script:list,sys:user:list', 1, '', 5);
INSERT INTO `sys_menu` VALUES (11, 'account', 'account', 'config.account.account', 10, 'alert:script:list,sys:user:list', 1, '/account', 1);
INSERT INTO `sys_menu` VALUES (12, 'role', 'role', 'config.roles.roles', 10, 'sys:role:list,sys:role:menu', 1, '/roles', 2);
INSERT INTO `sys_menu` VALUES (13, 'promServer', 'promServer', 'config.promServer.promServerList', 10, 'idc:list,promserver:list', 1, '/promServer', 3);
INSERT INTO `sys_menu` VALUES (14, 'dc', 'dc', 'config.dc.dc', 10, 'idc:list,sys:user:list,cabinet:list', 1, '/dc', 4);
INSERT INTO `sys_menu` VALUES (15, 'model', 'model', 'config.model.model', 10, 'model:list', 1, '/model', 5);
INSERT INTO `sys_menu` VALUES (16, 'mib', 'mib', 'config.mib.mib', 10, 'sys:mib:list', 1, '/mib', 6);
INSERT INTO `sys_menu` VALUES (17, 'system', 'system', 'config.system.system', 10, 'sys:config:list,sys:link:list,alert:script:list,sys:link:modify', 1, '/system', 7);
INSERT INTO `sys_menu` VALUES (18, 'terminalLog', 'terminalLog', 'config.terminallog.terminallog', 10, 'terminal:log:list', 1, '/terminalLog', 8);
INSERT INTO `sys_menu` VALUES (19, 'operationLog', 'operationLog', 'config.operationlog.operationlog', 10, 'sys:log:list', 1, '/operationLog', 9);
INSERT INTO `sys_menu` VALUES (20, 'about', 'about', 'overall.about', 10, 'sys:about:info', 1, '/about', 10);
INSERT INTO `sys_menu` VALUES (21, 'header', 'header', 'buttons.header.header', 0, NULL, 1, '', 0);
INSERT INTO `sys_menu` VALUES (301, 'panel_toAdd', 'panel_toAdd', 'buttons.panel.toAdd', 3, NULL, 2, '', 10);
INSERT INTO `sys_menu` VALUES (302, 'panel_toEdit', 'panel_toEdit', 'buttons.panel.toEdit', 3, NULL, 2, '', 15);
INSERT INTO `sys_menu` VALUES (303, 'panel_delete', 'panel_delete', 'buttons.panel.delete', 3, 'panel:delete', 2, '', 20);
INSERT INTO `sys_menu` VALUES (304, 'panel_save', 'panel_save', 'buttons.panel.save', 3, 'panel:save,panel:update', 2, '', 25);
INSERT INTO `sys_menu` VALUES (305, 'panel_chart_toAdd', 'panel_chart_toAdd', 'buttons.panel.chart.toAdd', 3, 'module:list', 2, '', 30);
INSERT INTO `sys_menu` VALUES (306, 'panel_chart_import', 'panel_chart_import', 'buttons.panel.chart.import', 3, 'chart:import,chart:import:template,chart:cancel:import', 2, '', 35);
INSERT INTO `sys_menu` VALUES (307, 'panel_chart_export', 'panel_chart_export', 'buttons.panel.chart.export', 3, 'chart:export', 2, '', 40);
INSERT INTO `sys_menu` VALUES (308, 'panel_chart_toEdit', 'panel_chart_toEdit', 'buttons.panel.chart.toEdit', 3, 'module:list', 2, '', 45);
INSERT INTO `sys_menu` VALUES (309, 'panel_chart_save', 'panel_chart_save', 'buttons.panel.chart.save', 3, 'chart:save,chart:update', 2, '', 50);
INSERT INTO `sys_menu` VALUES (310, 'panel_chart_delete', 'panel_chart_delete', 'buttons.panel.chart.delete', 3, 'chart:delete', 2, '', 55);
INSERT INTO `sys_menu` VALUES (311, 'panel_chart_duplicate', 'panel_chart_duplicate', 'buttons.panel.chart.duplicate', 3, 'chart:duplicate', 2, '', 60);
INSERT INTO `sys_menu` VALUES (312, 'panel_view', 'panel_view', 'buttons.panel.view', 3, 'panel:list,chart:list,panel:modify,chart:modify,chart:weights', 2, '', 5);
INSERT INTO `sys_menu` VALUES (401, 'explore_chart_toAdd', 'explore_chart_toAdd', 'buttons.explore.toAdd', 4, 'module:list', 2, '', 10);
INSERT INTO `sys_menu` VALUES (402, 'explore_chart_save', 'explore_chart_save', 'buttons.explore.save', 4, 'chart:save', 2, '', 15);
INSERT INTO `sys_menu` VALUES (403, 'explore_view', 'explore_view', 'buttons.explore.view', 4, NULL, 2, '', 5);
INSERT INTO `sys_menu` VALUES (501, 'asset_toAdd', 'asset_toAdd', 'buttons.asset.toAdd', 5, 'sys:user:list,sys:dict:all,idc:list,asset:tag:key,model:list', 2, '', 10);
INSERT INTO `sys_menu` VALUES (502, 'asset_toEdit', 'asset_toEdit', 'buttons.asset.toEdit', 5, 'sys:user:list,sys:dict:all,idc:list,asset:tag:key,cabinet:list,cabinet:u:info,model:list', 2, '', 15);
INSERT INTO `sys_menu` VALUES (503, 'asset_toBatchEdit', 'asset_toBatchEdit', 'buttons.asset.toBatchEdit', 5, 'sys:user:list', 2, '', 20);
INSERT INTO `sys_menu` VALUES (504, 'asset_delete', 'asset_delete', 'buttons.asset.delete', 5, 'asset:delete', 2, '', 25);
INSERT INTO `sys_menu` VALUES (505, 'asset_save', 'asset_save', 'buttons.asset.save', 5, 'asset:save,asset:update,asset:batch:update', 2, '', 30);
INSERT INTO `sys_menu` VALUES (506, 'asset_import', 'asset_import', 'buttons.asset.import', 5, 'asset:import,asset:import:template,asset:cancel:import', 2, '', 35);
INSERT INTO `sys_menu` VALUES (507, 'asset_export', 'asset_export', 'buttons.asset.export', 5, 'asset:export', 2, '', 40);
INSERT INTO `sys_menu` VALUES (508, 'asset_connect', 'asset_connect', 'buttons.asset.connect', 5, NULL, 2, '', 45);
INSERT INTO `sys_menu` VALUES (509, 'asset_endpoint_view', 'asset_endpoint_view', 'buttons.asset.endpoint.view', 5, 'endpoint:list,model:list', 2, '', 50);
INSERT INTO `sys_menu` VALUES (510, 'asset_endpoint_toAdd', 'asset_endpoint_toAdd', 'buttons.asset.endpoint.toAdd', 5, 'module:list,asset:list,idc:list,project:list,model:list,sys:dict:all', 2, '', 55);
INSERT INTO `sys_menu` VALUES (511, 'asset_endpoint_toEdit', 'asset_endpoint_toEdit', 'buttons.asset.endpoint.toEdit', 5, 'asset:list,project:list,module:list', 2, '', 60);
INSERT INTO `sys_menu` VALUES (512, 'asset_endpoint_delete', 'asset_endpoint_delete', 'buttons.asset.endpoint.delete', 5, 'endpoint:delete', 2, '', 65);
INSERT INTO `sys_menu` VALUES (513, 'asset_endpoint_save', 'asset_endpoint_save', 'buttons.asset.endpoint.save', 5, 'endpoint:save,endpoint:update', 2, '', 70);
INSERT INTO `sys_menu` VALUES (514, 'asset_endpoint_import', 'asset_endpoint_import', 'buttons.asset.endpoint.import', 5, 'endpoint:import,endpoint:import:template,endpoint:cancel:import', 2, '', 75);
INSERT INTO `sys_menu` VALUES (515, 'asset_endpoint_export', 'asset_endpoint_export', 'buttons.asset.endpoint.export', 5, 'endpoint:export', 2, '', 80);
INSERT INTO `sys_menu` VALUES (516, 'asset_alerts_view', 'asset_alerts_view', 'buttons.asset.alerts.view', 5, 'idc:list,alert:message:list,model:list', 2, '', 85);
INSERT INTO `sys_menu` VALUES (517, 'asset_alerts_delete', 'asset_alerts_delete', 'buttons.asset.alerts.delete', 5, 'alert:message:update', 2, '', 90);
INSERT INTO `sys_menu` VALUES (518, 'asset_alerts_export', 'asset_alerts_export', 'buttons.asset.alerts.export', 5, 'alert:message:export', 2, '', 95);
INSERT INTO `sys_menu` VALUES (519, 'asset_chart_toAdd', 'asset_chart_toAdd', 'buttons.asset.chart.toAdd', 5, 'module:list', 2, '', 100);
INSERT INTO `sys_menu` VALUES (520, 'asset_chart_import', 'asset_chart_import', 'buttons.asset.chart.import', 5, 'chart:import,chart:import:template,chart:cancel:import', 2, '', 105);
INSERT INTO `sys_menu` VALUES (521, 'asset_chart_export', 'asset_chart_export', 'buttons.asset.chart.export', 5, 'chart:export', 2, '', 110);
INSERT INTO `sys_menu` VALUES (522, 'asset_chart_toEdit', 'asset_chart_toEdit', 'buttons.asset.chart.toEdit', 5, 'module:list', 2, '', 115);
INSERT INTO `sys_menu` VALUES (523, 'asset_chart_save', 'asset_chart_save', 'buttons.asset.chart.save', 5, 'chart:save,chart:update', 2, '', 120);
INSERT INTO `sys_menu` VALUES (524, 'asset_chart_delete', 'asset_chart_delete', 'buttons.asset.chart.delete', 5, 'chart:delete', 2, '', 125);
INSERT INTO `sys_menu` VALUES (525, 'asset_chart_duplicate', 'asset_chart_duplicate', 'buttons.asset.chart.duplicate', 5, 'chart:duplicate', 2, '', 130);
INSERT INTO `sys_menu` VALUES (526, 'asset_chart_sync', 'asset_chart_sync', 'buttons.asset.chart.sync', 5, 'model:sync:chart,panel:list', 2, '', 135);
INSERT INTO `sys_menu` VALUES (527, 'asset_vendor_toAdd', 'asset_vendor_toAdd', 'buttons.asset.vendor.toAdd', 5, 'sys:dict:all', 2, '', 140);
INSERT INTO `sys_menu` VALUES (528, 'asset_vendor_save', 'asset_vendor_save', 'buttons.asset.vendor.save', 5, 'model:save,sys:dict:save', 2, '', 145);
INSERT INTO `sys_menu` VALUES (529, 'asset_view', 'asset_view', 'buttons.asset.view', 5, 'asset:tag:key,asset:list,sys:user:list,asset:filter,model:list,asset:info', 2, '', 5);
INSERT INTO `sys_menu` VALUES (601, 'project_toEdit', 'project_toEdit', 'buttons.project.toEdit', 6, NULL, 2, '', 10);
INSERT INTO `sys_menu` VALUES (602, 'project_save', 'project_save', 'buttons.project.save', 6, 'project:save,project:update', 2, '', 15);
INSERT INTO `sys_menu` VALUES (603, 'project_delete', 'project_delete', 'buttons.project.delete', 6, 'project:delete', 2, '', 20);
INSERT INTO `sys_menu` VALUES (604, 'project_topo_edit', 'project_topo_edit', 'buttons.project.topo.edit', 6, 'project:topo:icon:save', 2, '', 25);
INSERT INTO `sys_menu` VALUES (605, 'project_topo_save', 'project_topo_save', 'buttons.project.topo.save', 6, 'project:topo:update', 2, '', 30);
INSERT INTO `sys_menu` VALUES (606, 'project_module_toEdit', 'project_module_toEdit', 'buttons.project.module.toEdit', 6, 'sys:mib:tree', 2, '', 40);
INSERT INTO `sys_menu` VALUES (607, 'project_module_save', 'project_module_save', 'buttons.project.module.save', 6, 'module:save,module:update', 2, '', 45);
INSERT INTO `sys_menu` VALUES (608, 'project_module_delete', 'project_module_delete', 'buttons.project.module.delete', 6, 'module:delete', 2, '', 50);
INSERT INTO `sys_menu` VALUES (609, 'project_endpoint_toAdd', 'project_endpoint_toAdd', 'buttons.project.endpoint.toAdd', 6, 'asset:list,idc:list,model:list,sys:dict:all', 2, '', 55);
INSERT INTO `sys_menu` VALUES (610, 'project_endpoint_toEdit', 'project_endpoint_toEdit', 'buttons.project.endpoint.toEdit', 6, 'asset:list', 2, '', 60);
INSERT INTO `sys_menu` VALUES (611, 'project_endpoint_delete', 'project_endpoint_delete', 'buttons.project.endpoint.delete', 6, 'endpoint:delete', 2, '', 65);
INSERT INTO `sys_menu` VALUES (612, 'project_endpoint_save', 'project_endpoint_save', 'buttons.project.endpoint.save', 6, 'endpoint:save,endpoint:update', 2, '', 70);
INSERT INTO `sys_menu` VALUES (613, 'project_endpoint_import', 'project_endpoint_import', 'buttons.project.endpoint.import', 6, 'endpoint:import,endpoint:import:template,endpoint:cancel:import', 2, '', 75);
INSERT INTO `sys_menu` VALUES (614, 'project_endpoint_export', 'project_endpoint_export', 'buttons.project.endpoint.export', 6, 'endpoint:export', 2, '', 80);
INSERT INTO `sys_menu` VALUES (615, 'project_endpoint_alerts_view', 'project_endpoint_alerts_view', 'buttons.project.endpoint.alerts.view', 6, 'asset:list,idc:list,alert:message:list', 2, '', 85);
INSERT INTO `sys_menu` VALUES (616, 'project_endpoint_alerts_delete', 'project_endpoint_alerts_delete', 'buttons.project.endpoint.alerts.delete', 6, 'alert:message:update', 2, '', 90);
INSERT INTO `sys_menu` VALUES (617, 'project_endpoint_alerts_export', 'project_endpoint_alerts_export', 'buttons.project.endpoint.alerts.export', 6, 'alert:message:export', 2, '', 95);
INSERT INTO `sys_menu` VALUES (618, 'project_endpoint_query_chart_view', 'project_endpoint_query_chart_view', 'buttons.project.endpoint.query.view', 6, 'asset:list,panel:list', 2, '', 100);
INSERT INTO `sys_menu` VALUES (619, 'project_endpoint_query_chart_toAdd', 'project_endpoint_query_chart_toAdd', 'buttons.project.endpoint.query.toAdd', 6, 'module:list', 2, '', 105);
INSERT INTO `sys_menu` VALUES (620, 'project_endpoint_query_chart_save', 'project_endpoint_query_chart_save', 'buttons.project.endpoint.query.save', 6, 'chart:save,chart:update', 2, '', 110);
INSERT INTO `sys_menu` VALUES (621, 'project_toAdd', 'project_toAdd', 'buttons.header.add.project', 6, NULL, 2, '', 5);
INSERT INTO `sys_menu` VALUES (622, 'project_module_toAdd', 'project_module_toAdd', 'buttons.header.add.module', 6, NULL, 2, '', 35);
INSERT INTO `sys_menu` VALUES (623, 'project_view', 'project_view', 'buttons.project.view', 6, 'module:list,sys:user:list,project:list,project:info,project:topo:icon:list,module:stat,project:topo:icon:info,endpoint:list,project:topo:list,asset:info,asset:list,alert:message:list,idc:list,alert:rule:list,panel:list', 2, '', 3);
INSERT INTO `sys_menu` VALUES (801, 'message_delete', 'message_delete', 'buttons.message.delete', 8, 'alert:message:update', 2, '', 10);
INSERT INTO `sys_menu` VALUES (802, 'message_export', 'message_export', 'buttons.message.export', 8, 'alert:message:export', 2, '', 15);
INSERT INTO `sys_menu` VALUES (803, 'message_view', 'message_view', 'buttons.message.view', 8, 'idc:list,alert:message:list,asset:list,module:list,project:list,asset:info,endpoint:list,alert:rule:list', 2, '', 5);
INSERT INTO `sys_menu` VALUES (901, 'rule_toAdd', 'rule_toAdd', 'buttons.rule.toAdd', 9, 'sys:user:list', 2, '', 10);
INSERT INTO `sys_menu` VALUES (902, 'rule_toEdit', 'rule_toEdit', 'buttons.rule.toEdit', 9, 'sys:user:list', 2, '', 15);
INSERT INTO `sys_menu` VALUES (903, 'rule_delete', 'rule_delete', 'buttons.rule.delete', 9, 'alert:rule:delete', 2, '', 20);
INSERT INTO `sys_menu` VALUES (904, 'rule_save', 'rule_save', 'buttons.rule.save', 9, 'alert:rule:save,alert:rule:update', 2, '', 25);
INSERT INTO `sys_menu` VALUES (905, 'rule_alerts_view', 'rule_alerts_view', 'buttons.rule.alerts.view', 9, 'idc:list,alert:message:list,asset:list,asset:info,endpoint:list,alert:rule:stat', 2, '', 30);
INSERT INTO `sys_menu` VALUES (906, 'rule_alerts_delete', 'rule_alerts_delete', 'buttons.rule.alerts.delete', 9, 'alert:message:update', 2, '', 35);
INSERT INTO `sys_menu` VALUES (907, 'rule_alerts_export', 'rule_alerts_export', 'buttons.rule.alerts.export', 9, 'alert:message:export', 2, '', 40);
INSERT INTO `sys_menu` VALUES (908, 'rule_import', 'rule_import', 'overall.importExcel', 9, 'alert:rule:import,alert:rule:cancel:import,alert:rule:template', 2, '', 45);
INSERT INTO `sys_menu` VALUES (909, 'rule_export', 'rule_export', 'overall.exportExcel', 9, 'alert:rule:export', 2, '', 50);
INSERT INTO `sys_menu` VALUES (910, 'rule_view', 'rule_view', 'buttons.rule.view', 9, 'alert:rule:list,asset:list,alert:rule:stat', 2, '', 5);
INSERT INTO `sys_menu` VALUES (1101, 'account_toAdd', 'account_toAdd', 'buttons.account.toAdd', 11, 'sys:role:list', 2, '', 10);
INSERT INTO `sys_menu` VALUES (1102, 'account_toEdit', 'account_toEdit', 'buttons.account.toEdit', 11, 'sys:role:list', 2, '', 15);
INSERT INTO `sys_menu` VALUES (1103, 'account_delete', 'account_delete', 'buttons.account.delete', 11, 'sys:user:delete', 2, '', 20);
INSERT INTO `sys_menu` VALUES (1104, 'account_save', 'account_save', 'buttons.account.save', 11, 'sys:user:save,sys:user:update', 2, '', 25);
INSERT INTO `sys_menu` VALUES (1105, 'account_view', 'account_view', 'buttons.account.view', 11, 'alert:script:list,sys:user:list', 2, '', 5);
INSERT INTO `sys_menu` VALUES (1201, 'role_toAdd', 'role_toAdd', 'buttons.role.toAdd', 12, 'sys:menu:list', 2, '', 10);
INSERT INTO `sys_menu` VALUES (1202, 'role_toEdit', 'role_toEdit', 'buttons.role.toEdit', 12, 'sys:role:menu', 2, '', 15);
INSERT INTO `sys_menu` VALUES (1203, 'role_delete', 'role_delete', 'buttons.role.delete', 12, 'sys:role:delete', 2, '', 20);
INSERT INTO `sys_menu` VALUES (1204, 'role_save', 'role_save', 'buttons.role.save', 12, 'sys:role:save,sys:role:update', 2, '', 25);
INSERT INTO `sys_menu` VALUES (1205, 'role_view', 'role_view', 'buttons.role.view', 12, 'sys:role:list,sys:role:menu', 2, '', 5);
INSERT INTO `sys_menu` VALUES (1301, 'prom_toAdd', 'prom_toAdd', 'buttons.prom.toAdd', 13, NULL, 2, '', 10);
INSERT INTO `sys_menu` VALUES (1302, 'prom_toEdit', 'prom_toEdit', 'buttons.prom.toEdit', 13, NULL, 2, '', 15);
INSERT INTO `sys_menu` VALUES (1303, 'prom_delete', 'prom_delete', 'buttons.prom.delete', 13, 'promserver:delete', 2, '', 20);
INSERT INTO `sys_menu` VALUES (1304, 'prom_save', 'prom_save', 'buttons.prom.save', 13, 'promserver:save,promserver:update', 2, '', 25);
INSERT INTO `sys_menu` VALUES (1305, 'prom_view', 'prom_view', 'buttons.prom.view', 13, 'idc:list,promserver:list', 2, '', 5);
INSERT INTO `sys_menu` VALUES (1401, 'dc_toAdd', 'dc_toAdd', 'buttons.dc.toAdd', 14, NULL, 2, '', 7);
INSERT INTO `sys_menu` VALUES (1402, 'dc_toEdit', 'dc_toEdit', 'buttons.dc.toEdit', 14, NULL, 2, '', 10);
INSERT INTO `sys_menu` VALUES (1403, 'dc_delete', 'dc_delete', 'buttons.dc.delete', 14, 'idc:delete', 2, '', 15);
INSERT INTO `sys_menu` VALUES (1404, 'dc_save', 'dc_save', 'buttons.dc.save', 14, 'idc:save,idc:update', 2, '', 20);
INSERT INTO `sys_menu` VALUES (1405, 'dc_trafficSetting', 'dc_trafficSetting', 'buttons.dc.trafficSetting', 14, 'asset:list,idc:taffic:list,sys:mib:browser', 2, '', 25);
INSERT INTO `sys_menu` VALUES (1406, 'dc_trafficSetting_save', 'dc_trafficSetting_save', 'buttons.dc.trafficSettingSave', 14, 'idc:taffic:update', 2, '', 30);
INSERT INTO `sys_menu` VALUES (1407, 'dc_cabinet_toAdd', 'dc_cabinet_toAdd', 'buttons.dc.cabinet.toAdd', 14, NULL, 2, '', 45);
INSERT INTO `sys_menu` VALUES (1408, 'dc_cabinet_toEdit', 'dc_cabinet_toEdit', 'buttons.dc.cabinet.toEdit', 14, NULL, 2, '', 50);
INSERT INTO `sys_menu` VALUES (1409, 'dc_cabinet_delete', 'dc_cabinet_delete', 'buttons.dc.cabinet.delete', 14, 'cabinet:delete', 2, '', 55);
INSERT INTO `sys_menu` VALUES (1410, 'dc_cabinet_save', 'dc_cabinet_save', 'buttons.dc.cabinet.save', 14, 'cabinet:save,cabinet:update', 2, '', 60);
INSERT INTO `sys_menu` VALUES (1411, 'dc_view', 'dc_view', 'buttons.dc.view', 14, 'idc:list,sys:user:list,cabinet:list', 2, '', 5);
INSERT INTO `sys_menu` VALUES (1412, 'dc_cabinet_view', 'dc_cabinet_view', 'buttons.dc.cabinet.view', 14, 'cabinet:list', 2, '', 40);
INSERT INTO `sys_menu` VALUES (1501, 'model_toAdd', 'model_toAdd', 'buttons.model.toAdd', 15, 'sys:dict:all', 2, '', 10);
INSERT INTO `sys_menu` VALUES (1502, 'model_toEdit', 'model_toEdit', 'buttons.model.toEdit', 15, 'sys:dict:all', 2, '', 15);
INSERT INTO `sys_menu` VALUES (1503, 'model_delete', 'model_delete', 'buttons.model.delete', 15, 'model:delete', 2, '', 20);
INSERT INTO `sys_menu` VALUES (1504, 'model_save', 'model_save', 'buttons.model.save', 15, 'model:save,model:update,sys:dict:save', 2, '', 25);
INSERT INTO `sys_menu` VALUES (1505, 'model_chart_view', 'model_chart_view', 'buttons.model.chart.view', 15, 'panel:list,chart:list,module:list', 2, '', 30);
INSERT INTO `sys_menu` VALUES (1506, 'model_chart_toAdd', 'model_chart_toAdd', 'buttons.model.chart.toAdd', 15, 'module:list', 2, '', 35);
INSERT INTO `sys_menu` VALUES (1507, 'model_chart_import', 'model_chart_import', 'buttons.model.chart.import', 15, 'chart:import,chart:import:template,chart:cancel:import', 2, '', 40);
INSERT INTO `sys_menu` VALUES (1508, 'model_chart_export', 'model_chart_export', 'buttons.model.chart.export', 15, 'chart:export', 2, '', 45);
INSERT INTO `sys_menu` VALUES (1509, 'model_chart_toEdit', 'model_chart_toEdit', 'buttons.model.chart.toEdit', 15, 'module:list', 2, '', 50);
INSERT INTO `sys_menu` VALUES (1510, 'model_chart_save', 'model_chart_save', 'buttons.model.chart.save', 15, 'chart:save,chart:update', 2, '', 55);
INSERT INTO `sys_menu` VALUES (1511, 'model_chart_delete', 'model_chart_delete', 'buttons.model.chart.delete', 15, 'chart:delete', 2, '', 60);
INSERT INTO `sys_menu` VALUES (1512, 'model_chart_duplicate', 'model_chart_duplicate', 'buttons.model.chart.duplicate', 15, 'chart:duplicate', 2, '', 65);
INSERT INTO `sys_menu` VALUES (1513, 'model_chart_sync', 'model_chart_sync', 'buttons.model.chart.sync', 15, 'model:sync:chart', 2, '', 70);
INSERT INTO `sys_menu` VALUES (1514, 'model_view', 'model_view', 'buttons.model.view', 15, 'model:list', 2, '', 5);
INSERT INTO `sys_menu` VALUES (1601, 'snmp_file_download', 'snmp_file_download', 'buttons.snmp.download', 16, 'sys:mib:download', 2, '', 10);
INSERT INTO `sys_menu` VALUES (1602, 'snmp_file_toAdd', 'snmp_file_toAdd', 'buttons.snmp.toAdd', 16, 'model:list', 2, '', 15);
INSERT INTO `sys_menu` VALUES (1603, 'snmp_file_toEdit', 'snmp_file_toEdit', 'buttons.snmp.toEdit', 16, 'model:list', 2, '', 20);
INSERT INTO `sys_menu` VALUES (1604, 'snmp_file_delete', 'snmp_file_delete', 'buttons.snmp.delete', 16, 'sys:mib:delete', 2, '', 25);
INSERT INTO `sys_menu` VALUES (1605, 'snmp_file_save', 'snmp_file_save', 'buttons.snmp.save', 16, 'sys:mib:update,sys:mib:save', 2, '', 30);
INSERT INTO `sys_menu` VALUES (1606, 'snmp_browser_export', 'snmp_browser_export', 'buttons.snmp.browserExport', 16, 'sys:mib:tree,model:list,asset:list,sys:mib:browser', 2, '', 40);
INSERT INTO `sys_menu` VALUES (1607, 'snmp_file_view', 'snmp_file_view', 'buttons.snmp.view', 16, 'sys:mib:list', 2, '', 5);
INSERT INTO `sys_menu` VALUES (1608, 'snmp_browser_view', 'snmp_browser_view', 'buttons.snmp.browserView', 16, 'sys:mib:tree,model:list,asset:list', 2, '', 35);
INSERT INTO `sys_menu` VALUES (1701, 'system_basic_save', 'system_basic_save', 'buttons.system.basic', 17, 'sys:config:update', 2, '', 0);
INSERT INTO `sys_menu` VALUES (1702, 'system_email_save', 'system_email_save', 'buttons.system.email', 17, 'sys:config:update', 2, '', 0);
INSERT INTO `sys_menu` VALUES (1703, 'system_terminal_save', 'system_terminal_save', 'buttons.system.terminal', 17, 'sys:config:update', 2, '', 0);
INSERT INTO `sys_menu` VALUES (1704, 'system_ldap_save', 'system_ldap_save', 'buttons.system.ldap', 17, 'sys:config:update', 2, '', 0);
INSERT INTO `sys_menu` VALUES (1705, 'system_notification_add', 'system_notification_add', 'buttons.system.notification.add', 17, 'alert:script:save', 2, '', 0);
INSERT INTO `sys_menu` VALUES (1706, 'system_notification_toEdit', 'system_notification_toEdit', 'buttons.system.notification.toEdit', 17, NULL, 2, '', 0);
INSERT INTO `sys_menu` VALUES (1707, 'system_notification_save', 'system_notification_save', 'buttons.system.notification.save', 17, 'alert:script:save,alert:script:update', 2, '', 0);
INSERT INTO `sys_menu` VALUES (1708, 'system_notification_delete', 'system_notification_delete', 'buttons.system.notification.delete', 17, 'alert:script:delete', 2, '', 0);
INSERT INTO `sys_menu` VALUES (1709, 'system_link_add', 'system_link_add', 'buttons.system.link.add', 17, 'sys:link:save', 2, '', 0);
INSERT INTO `sys_menu` VALUES (1710, 'system_link_toEdit', 'system_link_toEdit', 'buttons.system.link.toEdit', 17, NULL, 2, '', 0);
INSERT INTO `sys_menu` VALUES (1711, 'system_link_save', 'system_link_save', 'buttons.system.link.save', 17, 'sys:link:save,sys:link:update', 2, '', 0);
INSERT INTO `sys_menu` VALUES (1712, 'system_link_delete', 'system_link_delete', 'buttons.system.link.delete', 17, 'sys:link:delete', 2, '', 0);
INSERT INTO `sys_menu` VALUES (1713, 'system_reset_reset', 'system_reset_reset', 'buttons.system.reset', 17, 'sys:config:reset', 2, '', 0);
INSERT INTO `sys_menu` VALUES (2101, 'header_link', 'header_link', 'buttons.header.link', 21, 'sys:link:list', 2, '', 0);
INSERT INTO `sys_menu` VALUES (2102, 'header_terminal', 'header_terminal', 'buttons.header.terminal', 21, NULL, 2, '', 0);
INSERT INTO `sys_menu` VALUES (2103, 'header_terminal_upload', 'header_terminal_upload', 'buttons.header.terminalUpload', 21, 'terminal:upload', 2, '', 0);
INSERT INTO `sys_menu` VALUES (2104, 'header_terminal_download', 'header_terminal_download', 'buttons.header.terminalDownload', 21, 'terminal:download', 2, '', 0);
INSERT INTO `sys_menu` VALUES (2105, 'header_add', 'header_add', 'buttons.header.add.view', 21, NULL, 2, '', 0);
INSERT INTO `sys_menu` VALUES (2106, 'header_add_project', 'header_add_project', 'buttons.header.add.project', 21, NULL, 2, '', 0);
INSERT INTO `sys_menu` VALUES (2107, 'header_add_module', 'header_add_module', 'buttons.header.add.module', 21, 'project:list,sys:mib:tree', 2, '', 0);
INSERT INTO `sys_menu` VALUES (2108, 'header_add_endpoint', 'header_add_endpoint', 'buttons.header.add.endpoint', 21, 'module:list,asset:list,idc:list,model:list,sys:dict:all,project:list,module:list', 2, '', 0);
INSERT INTO `sys_menu` VALUES (2109, 'header_add_rule', 'header_add_rule', 'buttons.header.add.rule', 21, 'sys:user:list', 2, '', 0);
INSERT INTO `sys_menu` VALUES (2110, 'header_add_asset', 'header_add_asset', 'buttons.header.add.asset', 21, 'sys:user:list,sys:dict:all,idc:list,asset:tag:key,model:list', 2, '', 0);

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
INSERT INTO `sys_role` VALUES (2, 'common', 'common', 'The system build-in common user role', 1);

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `role_id` int(10) NOT NULL,
  `menu_id` int(10) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4684 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (4505, 1, 512);
INSERT INTO `sys_role_menu` VALUES (4506, 1, 513);
INSERT INTO `sys_role_menu` VALUES (4507, 1, 2);
INSERT INTO `sys_role_menu` VALUES (4508, 1, 514);
INSERT INTO `sys_role_menu` VALUES (4509, 1, 515);
INSERT INTO `sys_role_menu` VALUES (4510, 1, 516);
INSERT INTO `sys_role_menu` VALUES (4511, 1, 517);
INSERT INTO `sys_role_menu` VALUES (4512, 1, 518);
INSERT INTO `sys_role_menu` VALUES (4513, 1, 519);
INSERT INTO `sys_role_menu` VALUES (4514, 1, 520);
INSERT INTO `sys_role_menu` VALUES (4515, 1, 521);
INSERT INTO `sys_role_menu` VALUES (4516, 1, 522);
INSERT INTO `sys_role_menu` VALUES (4517, 1, 523);
INSERT INTO `sys_role_menu` VALUES (4518, 1, 524);
INSERT INTO `sys_role_menu` VALUES (4519, 1, 525);
INSERT INTO `sys_role_menu` VALUES (4520, 1, 526);
INSERT INTO `sys_role_menu` VALUES (4521, 1, 527);
INSERT INTO `sys_role_menu` VALUES (4522, 1, 528);
INSERT INTO `sys_role_menu` VALUES (4523, 1, 529);
INSERT INTO `sys_role_menu` VALUES (4524, 1, 18);
INSERT INTO `sys_role_menu` VALUES (4525, 1, 19);
INSERT INTO `sys_role_menu` VALUES (4526, 1, 20);
INSERT INTO `sys_role_menu` VALUES (4527, 1, 1301);
INSERT INTO `sys_role_menu` VALUES (4528, 1, 1302);
INSERT INTO `sys_role_menu` VALUES (4529, 1, 1303);
INSERT INTO `sys_role_menu` VALUES (4530, 1, 1304);
INSERT INTO `sys_role_menu` VALUES (4531, 1, 1305);
INSERT INTO `sys_role_menu` VALUES (4532, 1, 801);
INSERT INTO `sys_role_menu` VALUES (4533, 1, 802);
INSERT INTO `sys_role_menu` VALUES (4534, 1, 803);
INSERT INTO `sys_role_menu` VALUES (4535, 1, 301);
INSERT INTO `sys_role_menu` VALUES (4536, 1, 302);
INSERT INTO `sys_role_menu` VALUES (4537, 1, 303);
INSERT INTO `sys_role_menu` VALUES (4538, 1, 304);
INSERT INTO `sys_role_menu` VALUES (4539, 1, 305);
INSERT INTO `sys_role_menu` VALUES (4540, 1, 306);
INSERT INTO `sys_role_menu` VALUES (4541, 1, 307);
INSERT INTO `sys_role_menu` VALUES (4542, 1, 308);
INSERT INTO `sys_role_menu` VALUES (4543, 1, 2101);
INSERT INTO `sys_role_menu` VALUES (4544, 1, 309);
INSERT INTO `sys_role_menu` VALUES (4545, 1, 2102);
INSERT INTO `sys_role_menu` VALUES (4546, 1, 310);
INSERT INTO `sys_role_menu` VALUES (4547, 1, 2103);
INSERT INTO `sys_role_menu` VALUES (4548, 1, 311);
INSERT INTO `sys_role_menu` VALUES (4549, 1, 2104);
INSERT INTO `sys_role_menu` VALUES (4550, 1, 312);
INSERT INTO `sys_role_menu` VALUES (4551, 1, 2105);
INSERT INTO `sys_role_menu` VALUES (4552, 1, 2106);
INSERT INTO `sys_role_menu` VALUES (4553, 1, 2107);
INSERT INTO `sys_role_menu` VALUES (4554, 1, 2108);
INSERT INTO `sys_role_menu` VALUES (4555, 1, 2109);
INSERT INTO `sys_role_menu` VALUES (4556, 1, 2110);
INSERT INTO `sys_role_menu` VALUES (4557, 1, 1601);
INSERT INTO `sys_role_menu` VALUES (4558, 1, 1602);
INSERT INTO `sys_role_menu` VALUES (4559, 1, 1603);
INSERT INTO `sys_role_menu` VALUES (4560, 1, 1604);
INSERT INTO `sys_role_menu` VALUES (4561, 1, 1605);
INSERT INTO `sys_role_menu` VALUES (4562, 1, 1606);
INSERT INTO `sys_role_menu` VALUES (4563, 1, 1607);
INSERT INTO `sys_role_menu` VALUES (4564, 1, 1608);
INSERT INTO `sys_role_menu` VALUES (4565, 1, 1101);
INSERT INTO `sys_role_menu` VALUES (4566, 1, 1102);
INSERT INTO `sys_role_menu` VALUES (4567, 1, 1103);
INSERT INTO `sys_role_menu` VALUES (4568, 1, 1104);
INSERT INTO `sys_role_menu` VALUES (4569, 1, 1105);
INSERT INTO `sys_role_menu` VALUES (4570, 1, 601);
INSERT INTO `sys_role_menu` VALUES (4571, 1, 602);
INSERT INTO `sys_role_menu` VALUES (4572, 1, 603);
INSERT INTO `sys_role_menu` VALUES (4573, 1, 604);
INSERT INTO `sys_role_menu` VALUES (4574, 1, 605);
INSERT INTO `sys_role_menu` VALUES (4575, 1, 606);
INSERT INTO `sys_role_menu` VALUES (4576, 1, 607);
INSERT INTO `sys_role_menu` VALUES (4577, 1, 608);
INSERT INTO `sys_role_menu` VALUES (4578, 1, 609);
INSERT INTO `sys_role_menu` VALUES (4579, 1, 610);
INSERT INTO `sys_role_menu` VALUES (4580, 1, 611);
INSERT INTO `sys_role_menu` VALUES (4581, 1, 612);
INSERT INTO `sys_role_menu` VALUES (4582, 1, 613);
INSERT INTO `sys_role_menu` VALUES (4583, 1, 614);
INSERT INTO `sys_role_menu` VALUES (4584, 1, 615);
INSERT INTO `sys_role_menu` VALUES (4585, 1, 616);
INSERT INTO `sys_role_menu` VALUES (4586, 1, 617);
INSERT INTO `sys_role_menu` VALUES (4587, 1, 618);
INSERT INTO `sys_role_menu` VALUES (4588, 1, 619);
INSERT INTO `sys_role_menu` VALUES (4589, 1, 620);
INSERT INTO `sys_role_menu` VALUES (4590, 1, 621);
INSERT INTO `sys_role_menu` VALUES (4591, 1, 622);
INSERT INTO `sys_role_menu` VALUES (4592, 1, 623);
INSERT INTO `sys_role_menu` VALUES (4593, 1, 1401);
INSERT INTO `sys_role_menu` VALUES (4594, 1, 1402);
INSERT INTO `sys_role_menu` VALUES (4595, 1, 1403);
INSERT INTO `sys_role_menu` VALUES (4596, 1, 1404);
INSERT INTO `sys_role_menu` VALUES (4597, 1, 1405);
INSERT INTO `sys_role_menu` VALUES (4598, 1, 1406);
INSERT INTO `sys_role_menu` VALUES (4599, 1, 1407);
INSERT INTO `sys_role_menu` VALUES (4600, 1, 1408);
INSERT INTO `sys_role_menu` VALUES (4601, 1, 1409);
INSERT INTO `sys_role_menu` VALUES (4602, 1, 1410);
INSERT INTO `sys_role_menu` VALUES (4603, 1, 1411);
INSERT INTO `sys_role_menu` VALUES (4604, 1, 1412);
INSERT INTO `sys_role_menu` VALUES (4605, 1, 901);
INSERT INTO `sys_role_menu` VALUES (4606, 1, 902);
INSERT INTO `sys_role_menu` VALUES (4607, 1, 903);
INSERT INTO `sys_role_menu` VALUES (4608, 1, 904);
INSERT INTO `sys_role_menu` VALUES (4609, 1, 905);
INSERT INTO `sys_role_menu` VALUES (4610, 1, 906);
INSERT INTO `sys_role_menu` VALUES (4611, 1, 907);
INSERT INTO `sys_role_menu` VALUES (4612, 1, 908);
INSERT INTO `sys_role_menu` VALUES (4613, 1, 909);
INSERT INTO `sys_role_menu` VALUES (4614, 1, 910);
INSERT INTO `sys_role_menu` VALUES (4615, 1, 401);
INSERT INTO `sys_role_menu` VALUES (4616, 1, 402);
INSERT INTO `sys_role_menu` VALUES (4617, 1, 403);
INSERT INTO `sys_role_menu` VALUES (4618, 1, 1701);
INSERT INTO `sys_role_menu` VALUES (4619, 1, 1702);
INSERT INTO `sys_role_menu` VALUES (4620, 1, 1703);
INSERT INTO `sys_role_menu` VALUES (4621, 1, 1704);
INSERT INTO `sys_role_menu` VALUES (4622, 1, 1705);
INSERT INTO `sys_role_menu` VALUES (4623, 1, 1706);
INSERT INTO `sys_role_menu` VALUES (4624, 1, 1707);
INSERT INTO `sys_role_menu` VALUES (4625, 1, 1708);
INSERT INTO `sys_role_menu` VALUES (4626, 1, 1709);
INSERT INTO `sys_role_menu` VALUES (4627, 1, 1710);
INSERT INTO `sys_role_menu` VALUES (4628, 1, 1711);
INSERT INTO `sys_role_menu` VALUES (4629, 1, 1712);
INSERT INTO `sys_role_menu` VALUES (4630, 1, 1201);
INSERT INTO `sys_role_menu` VALUES (4631, 1, 1713);
INSERT INTO `sys_role_menu` VALUES (4632, 1, 1202);
INSERT INTO `sys_role_menu` VALUES (4633, 1, 1203);
INSERT INTO `sys_role_menu` VALUES (4634, 1, 1204);
INSERT INTO `sys_role_menu` VALUES (4635, 1, 1205);
INSERT INTO `sys_role_menu` VALUES (4636, 1, 1501);
INSERT INTO `sys_role_menu` VALUES (4637, 1, 1502);
INSERT INTO `sys_role_menu` VALUES (4638, 1, 1503);
INSERT INTO `sys_role_menu` VALUES (4639, 1, 1504);
INSERT INTO `sys_role_menu` VALUES (4640, 1, 1505);
INSERT INTO `sys_role_menu` VALUES (4641, 1, 1506);
INSERT INTO `sys_role_menu` VALUES (4642, 1, 1507);
INSERT INTO `sys_role_menu` VALUES (4643, 1, 1508);
INSERT INTO `sys_role_menu` VALUES (4644, 1, 1509);
INSERT INTO `sys_role_menu` VALUES (4645, 1, 1510);
INSERT INTO `sys_role_menu` VALUES (4646, 1, 1511);
INSERT INTO `sys_role_menu` VALUES (4647, 1, 1512);
INSERT INTO `sys_role_menu` VALUES (4648, 1, 1513);
INSERT INTO `sys_role_menu` VALUES (4649, 1, 1514);
INSERT INTO `sys_role_menu` VALUES (4650, 1, 501);
INSERT INTO `sys_role_menu` VALUES (4651, 1, 502);
INSERT INTO `sys_role_menu` VALUES (4652, 1, 503);
INSERT INTO `sys_role_menu` VALUES (4653, 1, 504);
INSERT INTO `sys_role_menu` VALUES (4654, 1, 505);
INSERT INTO `sys_role_menu` VALUES (4655, 1, 506);
INSERT INTO `sys_role_menu` VALUES (4656, 1, 507);
INSERT INTO `sys_role_menu` VALUES (4657, 1, 508);
INSERT INTO `sys_role_menu` VALUES (4658, 1, 509);
INSERT INTO `sys_role_menu` VALUES (4659, 1, 510);
INSERT INTO `sys_role_menu` VALUES (4660, 1, 511);
INSERT INTO `sys_role_menu` VALUES (4661, 2, 1505);
INSERT INTO `sys_role_menu` VALUES (4662, 2, 1601);
INSERT INTO `sys_role_menu` VALUES (4663, 2, 2);
INSERT INTO `sys_role_menu` VALUES (4664, 2, 803);
INSERT INTO `sys_role_menu` VALUES (4665, 2, 1411);
INSERT INTO `sys_role_menu` VALUES (4666, 2, 516);
INSERT INTO `sys_role_menu` VALUES (4667, 2, 1412);
INSERT INTO `sys_role_menu` VALUES (4668, 2, 1606);
INSERT INTO `sys_role_menu` VALUES (4669, 2, 615);
INSERT INTO `sys_role_menu` VALUES (4670, 2, 1607);
INSERT INTO `sys_role_menu` VALUES (4671, 2, 1608);
INSERT INTO `sys_role_menu` VALUES (4672, 2, 905);
INSERT INTO `sys_role_menu` VALUES (4673, 2, 618);
INSERT INTO `sys_role_menu` VALUES (4674, 2, 1514);
INSERT INTO `sys_role_menu` VALUES (4675, 2, 910);
INSERT INTO `sys_role_menu` VALUES (4676, 2, 623);
INSERT INTO `sys_role_menu` VALUES (4677, 2, 529);
INSERT INTO `sys_role_menu` VALUES (4678, 2, 403);
INSERT INTO `sys_role_menu` VALUES (4679, 2, 20);
INSERT INTO `sys_role_menu` VALUES (4680, 2, 2101);
INSERT INTO `sys_role_menu` VALUES (4681, 2, 312);
INSERT INTO `sys_role_menu` VALUES (4682, 2, 1305);
INSERT INTO `sys_role_menu` VALUES (4683, 2, 509);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `user_id` int(10) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '密码',
  `salt` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '盐',
  `email` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态  0：禁用   1：正常',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `lang` varchar(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'en',
  `source` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'system',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `username`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 48 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '系统用户表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', 'e1153123d7d180ceeb820d577ff119876678732a68eef4e6ffc0b1f06a01f91b', 'YzcmCZNvbXocrsz9dm8e', 'nezha@www.puppn.com', 1, '2016-11-11 11:11:11', 'zh', 'system');

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
-- Table structure for terminal_log
-- ----------------------------
DROP TABLE IF EXISTS `terminal_log`;
CREATE TABLE `terminal_log`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `host` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '登录主机',
  `port` int(10) NULL DEFAULT NULL COMMENT '端口',
  `uuid` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'uuid',
  `protocol` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '可选值：SSH，TELNET。统一大写',
  `auth_type` int(10) NULL DEFAULT NULL COMMENT '认证类型，1：密码，2：私钥证书\r\n\r\nssh可选值：1、2\r\n\r\ntelnet可选值：1',
  `user` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '登录主机用户名',
  `cmd` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户输入命令',
  `user_id` int(10) NOT NULL COMMENT 'nezha系统内用户id',
  `time` datetime(0) NOT NULL COMMENT '时间',
  `status` int(1) NOT NULL COMMENT '状态：0：发送失败（没有通过ssh或telnet发送成功）\r\n\r\n1：发送成功',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_host`(`host`) USING BTREE,
  INDEX `idx_port`(`port`) USING BTREE,
  INDEX `idx_protocol`(`protocol`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3236 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_group
-- ----------------------------
DROP TABLE IF EXISTS `user_group`;
CREATE TABLE `user_group`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户组名',
  `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '用户组描述',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for user_group_rel
-- ----------------------------
DROP TABLE IF EXISTS `user_group_rel`;
CREATE TABLE `user_group_rel`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int(10) NOT NULL COMMENT '用户id',
  `group_id` int(10) NOT NULL COMMENT '用户组id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for user_notification
-- ----------------------------
DROP TABLE IF EXISTS `user_notification`;
CREATE TABLE `user_notification`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `user_id` int(10) NOT NULL COMMENT '用户id',
  `script_id` int(10) NOT NULL COMMENT '脚本id',
  `account` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '通知账号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Triggers structure for table alert_rule
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_alert_rule_insert`;
delimiter ;;
CREATE TRIGGER `tri_alert_rule_insert` AFTER INSERT ON `alert_rule` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'rule';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table alert_rule
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_alert_rule_update`;
delimiter ;;
CREATE TRIGGER `tri_alert_rule_update` AFTER UPDATE ON `alert_rule` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'rule';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table alert_rule
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_alert_rule_delete`;
delimiter ;;
CREATE TRIGGER `tri_alert_rule_delete` AFTER DELETE ON `alert_rule` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'rule';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table asset
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_asset_insert`;
delimiter ;;
CREATE TRIGGER `tri_asset_insert` AFTER INSERT ON `asset` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'asset';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table asset
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_asset_update`;
delimiter ;;
CREATE TRIGGER `tri_asset_update` AFTER UPDATE ON `asset` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'asset';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table asset
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_asset_delete`;
delimiter ;;
CREATE TRIGGER `tri_asset_delete` AFTER DELETE ON `asset` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'asset';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table asset_tag
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_asset_insert_copy1`;
delimiter ;;
CREATE TRIGGER `tri_asset_insert_copy1` AFTER INSERT ON `asset_tag` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'asset';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table asset_tag
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_asset_update_copy1`;
delimiter ;;
CREATE TRIGGER `tri_asset_update_copy1` AFTER UPDATE ON `asset_tag` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'asset';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table asset_tag
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_asset_delete_copy1`;
delimiter ;;
CREATE TRIGGER `tri_asset_delete_copy1` AFTER DELETE ON `asset_tag` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'asset';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table endpoint
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_endpoint_insert`;
delimiter ;;
CREATE TRIGGER `tri_endpoint_insert` AFTER INSERT ON `endpoint` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'endpoint';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table endpoint
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_endpoint_update`;
delimiter ;;
CREATE TRIGGER `tri_endpoint_update` AFTER UPDATE ON `endpoint` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'endpoint';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table endpoint
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_endpoint_delete`;
delimiter ;;
CREATE TRIGGER `tri_endpoint_delete` AFTER DELETE ON `endpoint` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'endpoint';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table idc
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_idc_insert`;
delimiter ;;
CREATE TRIGGER `tri_idc_insert` AFTER INSERT ON `idc` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'idc';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table idc
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_idc_update`;
delimiter ;;
CREATE TRIGGER `tri_idc_update` AFTER UPDATE ON `idc` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'idc';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table idc
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_idc_delete`;
delimiter ;;
CREATE TRIGGER `tri_idc_delete` AFTER DELETE ON `idc` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'idc';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table idc_traffic
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_idc_traffic_insert`;
delimiter ;;
CREATE TRIGGER `tri_idc_traffic_insert` AFTER INSERT ON `idc_traffic` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'idc_traffic';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table idc_traffic
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_idc_traffic_update`;
delimiter ;;
CREATE TRIGGER `tri_idc_traffic_update` AFTER UPDATE ON `idc_traffic` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'idc_traffic';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table idc_traffic
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_idc_traffic_delete`;
delimiter ;;
CREATE TRIGGER `tri_idc_traffic_delete` AFTER DELETE ON `idc_traffic` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'idc_traffic';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table module
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_module_insert`;
delimiter ;;
CREATE TRIGGER `tri_module_insert` AFTER INSERT ON `module` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'module';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table module
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_module_update`;
delimiter ;;
CREATE TRIGGER `tri_module_update` AFTER UPDATE ON `module` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'module';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table module
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_module_delete`;
delimiter ;;
CREATE TRIGGER `tri_module_delete` AFTER DELETE ON `module` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'module';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table prom_server
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_prom_server_insert`;
delimiter ;;
CREATE TRIGGER `tri_prom_server_insert` AFTER INSERT ON `prom_server` FOR EACH ROW begin
   update conf_event t set t.value = t.value +1 where t.table = 'server';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table prom_server
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_prom_server_update`;
delimiter ;;
CREATE TRIGGER `tri_prom_server_update` AFTER UPDATE ON `prom_server` FOR EACH ROW begin
 -- 只是 check_time 变化，不更新 conf_event表数据
 if NEW.host != OLD.host or NEW.port !=OLD.port or NEW.type != OLD.type or NEW.status != OLD.status or NEW.idc_id != OLD.idc_id THEN
   update conf_event t set t.value = t.value +1 where t.table = 'server';
 end if;
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table prom_server
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_prom_server_delete`;
delimiter ;;
CREATE TRIGGER `tri_prom_server_delete` AFTER DELETE ON `prom_server` FOR EACH ROW begin
   update conf_event t set t.value = t.value +1 where t.table = 'server';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table sys_config
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_scrape_interval_update`;
delimiter ;;
CREATE TRIGGER `tri_scrape_interval_update` AFTER UPDATE ON `sys_config` FOR EACH ROW begin
  if NEW.param_key='scrape_interval' then
	  update conf_event t set t.value = t.value +1 where t.table = 'scrape_interval';
	end if;
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table sys_config
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_storage_local_retention_update`;
delimiter ;;
CREATE TRIGGER `tri_storage_local_retention_update` AFTER UPDATE ON `sys_config` FOR EACH ROW begin
  if NEW.param_key='storage_local_retention' then
	  update conf_event t set t.value = t.value +1 where t.table = 'storage_local_retention';
	end if;
end
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;