-- 初始化sql文件
-- 每次提交代码涉及数据库变更的，都需要在db 目录下新建 sql 文件保存变更的 sql 语句。
-- sql文件命名格式：yyyy-mm-dd.sql，例 2020-01-01.sql，文件头部注释说明变更缘由或变更内容


/*
 Navicat Premium Data Transfer

 Source Server         : 192.168.40.126
 Source Server Type    : MySQL
 Source Server Version : 100223
 Source Host           : 192.168.40.126:3306
 Source Schema         : nezha

 Target Server Type    : MySQL
 Target Server Version : 100223
 File Encoding         : 65001

 Date: 22/04/2020 14:22:23
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
  `auth_type` int(10) NOT NULL COMMENT '认证类型，1：密码，2：私钥证书，ssh可选1、2，telnet可选1',
  `user` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户名',
  `pwd` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '密码',
  `private_key` varchar(4096) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '私钥',
  `port` int(10) NULL DEFAULT NULL COMMENT '端口，ssh默认：22,telnet默认：23',
  `user_tip` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户名提示，telnet协议有效，用于自动登录，默认值：login',
  `password_tip` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '密码提示，telnet协议有效，用于自动登录，默认值：password',
  `relogin_password_tip` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '重新登陆提示，telnet协议有效，用于relogin自动登录\r\n\r\n默认值：password',
  `seq` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '全局唯一，用于导入撤销',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 57 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for alert_message
-- ----------------------------
DROP TABLE IF EXISTS `alert_message`;
CREATE TABLE `alert_message`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `severity` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '告警级别',
  `summary` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '告警摘要',
  `description` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '告警描述',
  `labels` varchar(2048) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '告警labels信息',
  `start_at` datetime(0) NULL DEFAULT NULL COMMENT '开始时间',
  `end_at` datetime(0) NULL DEFAULT NULL COMMENT '结束时间',
  `state` int(10) NULL DEFAULT NULL COMMENT '状态1:pending,2:Expired',
  `rule_id` int(10) NOT NULL COMMENT '告警规则ID',
  `project_id` int(10) NULL DEFAULT NULL COMMENT '关联project id',
  `module_id` int(10) NULL DEFAULT NULL COMMENT '关联module id',
  `endpoint_id` int(10) NULL DEFAULT NULL COMMENT '关联endpoint id',
  `asset_id` int(10) NULL DEFAULT NULL COMMENT '关联资产id',
  `dc_id` int(10) NULL DEFAULT NULL COMMENT '关联idc id',
  `hash_key` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'redis hash key',
  `remark` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '备注 记录手动关闭记录信息，格式\r\n\r\ntime     username\r\n\r\nmessage',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1599 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for alert_rule
-- ----------------------------
DROP TABLE IF EXISTS `alert_rule`;
CREATE TABLE `alert_rule`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `alert_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '告警名称',
  `expr` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '运算表达式',
  `last` int(11) NOT NULL COMMENT '持续时间,单位s',
  `severity` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '告警级别',
  `summary` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '告警摘要',
  `description` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '告警详细描述',
  `receiver` int(11) NULL DEFAULT NULL COMMENT '通知用户组',
  `build_in` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '0' COMMENT '1：内置，0：非内置，默认：0\r\n\r\n内置规则不允许编辑删除，系统内置一条 snmptrap 规则',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 35 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

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
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 151 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for asset_ping
-- ----------------------------
DROP TABLE IF EXISTS `asset_ping`;
CREATE TABLE `asset_ping`  (
  `host` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'ip地址',
  `status` int(1) NOT NULL COMMENT '是否可达 可达为1 不可达为0',
  `rtt` double(7, 2) NULL DEFAULT NULL COMMENT '传输的时间延迟Round-Trip Time ，单位：ms',
  `last_update` datetime(0) NULL DEFAULT NULL COMMENT '最后更新时间'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

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
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 48 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

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
  `prev` int(10) NOT NULL COMMENT '记录前一个chart.id\r\n\r\n如果前面没有内容了，记录为 0',
  `next` int(10) NOT NULL COMMENT '记录后一个chart.id\r\n\r\n如果后面没有内容了，记录为 -1',
  `param` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '用于记录不同type类型对应的param',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 554 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

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
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1305 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

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
INSERT INTO `conf_event` VALUES ('endpoint', 69);
INSERT INTO `conf_event` VALUES ('idc', 5);
INSERT INTO `conf_event` VALUES ('module', 16);
INSERT INTO `conf_event` VALUES ('rule', 0);
INSERT INTO `conf_event` VALUES ('scrape_interval', 0);
INSERT INTO `conf_event` VALUES ('server', 3);
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
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 338 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

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
  `area_id` int(10) NULL DEFAULT NULL COMMENT '关联sys_area',
  `x` int(10) NULL DEFAULT NULL COMMENT '列',
  `y` int(10) NULL DEFAULT NULL COMMENT '行',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 89 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

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
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 78 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

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
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_projectId_name`(`project_id`, `name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 58 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of module
-- ----------------------------
INSERT INTO `module` VALUES (1, 2, 'http', 'node_exporter', 9100, '', '/metrics', NULL, '1', NULL, NULL, NULL);

-- ----------------------------
-- Table structure for panel
-- ----------------------------
DROP TABLE IF EXISTS `panel`;
CREATE TABLE `panel`  (
  `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `create_by` int(10) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
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
) ENGINE = InnoDB AUTO_INCREMENT = 61 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of project
-- ----------------------------
INSERT INTO `project` VALUES (1, 'system', '系统内置project，展示系统基础信息', '1', NULL);
INSERT INTO `project` VALUES (2, 'node_exporter', '系统内置project', '1', NULL);

-- ----------------------------
-- Table structure for prom_server
-- ----------------------------
DROP TABLE IF EXISTS `prom_server`;
CREATE TABLE `prom_server`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `idc_id` int(10) NOT NULL COMMENT '所在idc',
  `host` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '部署ip',
  `port` int(10) NOT NULL COMMENT '端口 默认9090',
  `type` int(10) NOT NULL COMMENT '1:global,负责汇聚所有per-datacenter采集的数据及告警配置\r\n\r\n2:per-datacenter，负责根据exporter配置采集监测数据',
  `status` int(1) NOT NULL COMMENT '默认：0， 1：可用，0：prometheus server 不可用，-1：configagent不可用，-2：都不可用',
  `check_time` datetime(1) NULL DEFAULT NULL COMMENT '最后操作时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

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
  PRIMARY KEY (`SCHED_NAME`, `ENTRY_ID`) USING BTREE
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
  PRIMARY KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) USING BTREE
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
-- Records of qrtz_locks
-- ----------------------------
INSERT INTO `qrtz_locks` VALUES ('NeZhaScheduler', 'STATE_ACCESS');
INSERT INTO `qrtz_locks` VALUES ('NeZhaScheduler', 'TRIGGER_ACCESS');

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
-- Records of qrtz_scheduler_state
-- ----------------------------
INSERT INTO `qrtz_scheduler_state` VALUES ('NeZhaScheduler', 'www.puppet-foreman.com1576734310869', 1576743748484, 15000);

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
  INDEX `SCHED_NAME`(`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) USING BTREE,
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
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 77 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_area
-- ----------------------------
DROP TABLE IF EXISTS `sys_area`;
CREATE TABLE `sys_area`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `pid` int(10) NOT NULL DEFAULT 0 COMMENT '父id，没有为 0 ',
  `pids` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '所有父id',
  `longitude` double(11, 6) NOT NULL COMMENT '经度',
  `latitude` double(11, 6) NOT NULL COMMENT '纬度',
  `i18n` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'i18n内容，json对象保存，key为语言简称，value为对应翻译',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 98 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_area
-- ----------------------------
INSERT INTO `sys_area` VALUES (5, 'Almaty', 0, '0', 76.928611, 43.256667, '{\"cn\":\"Almaty\",\"en\":\"Almaty\"}');
INSERT INTO `sys_area` VALUES (6, 'Shymkent', 0, '0', 69.600000, 42.300000, '{\"cn\":\"Shymkent\",\"en\":\"Shymkent\"}');
INSERT INTO `sys_area` VALUES (7, 'Qaraghandy', 0, '0', 73.100000, 49.800000, '{\"cn\":\"Qaraghandy\",\"en\":\"Qaraghandy\"}');
INSERT INTO `sys_area` VALUES (8, 'Taraz', 0, '0', 71.366667, 42.900000, '{\"cn\":\"Taraz\",\"en\":\"Taraz\"}');
INSERT INTO `sys_area` VALUES (9, 'Astana', 0, '0', 71.450000, 51.166666, '{\"cn\":\"Astana\",\"en\":\"Astana\"}');
INSERT INTO `sys_area` VALUES (10, 'Pavlodar', 0, '0', 76.966666, 52.283333, '{\"cn\":\"Pavlodar\",\"en\":\"Pavlodar\"}');
INSERT INTO `sys_area` VALUES (11, 'Öskemen', 0, '0', 82.600000, 49.966666, '{\"cn\":\"Öskemen\",\"en\":\"Öskemen\"}');
INSERT INTO `sys_area` VALUES (12, 'Semey', 0, '0', 80.266694, 50.426749, '{\"cn\":\"Semey\",\"en\":\"Semey\"}');
INSERT INTO `sys_area` VALUES (13, 'Qyzylorda', 0, '0', 65.500000, 44.850000, '{\"cn\":\"Qyzylorda\",\"en\":\"Qyzylorda\"}');
INSERT INTO `sys_area` VALUES (14, 'Aqtöbe', 0, '0', 57.183333, 50.300000, '{\"cn\":\"Aqtöbe\",\"en\":\"Aqtöbe\"}');
INSERT INTO `sys_area` VALUES (15, 'Qostanay', 0, '0', 63.633333, 53.216666, '{\"cn\":\"Qostanay\",\"en\":\"Qostanay\"}');
INSERT INTO `sys_area` VALUES (16, 'Petropavl', 0, '0', 69.150000, 54.866666, '{\"cn\":\"Petropavl\",\"en\":\"Petropavl\"}');
INSERT INTO `sys_area` VALUES (17, 'Oral', 0, '0', 51.366667, 51.233333, '{\"cn\":\"Oral\",\"en\":\"Oral\"}');
INSERT INTO `sys_area` VALUES (18, 'Atyraū', 0, '0', 51.883333, 47.116667, '{\"cn\":\"Atyraū\",\"en\":\"Atyraū\"}');
INSERT INTO `sys_area` VALUES (19, 'Temirtaū', 0, '0', 72.964643, 50.054938, '{\"cn\":\"Temirtaū\",\"en\":\"Temirtaū\"}');
INSERT INTO `sys_area` VALUES (20, 'Kökshetaū', 0, '0', 69.400000, 53.283333, '{\"cn\":\"Kökshetaū\",\"en\":\"Kökshetaū\"}');
INSERT INTO `sys_area` VALUES (21, 'Ekibastuz', 0, '0', 75.322869, 51.723714, '{\"cn\":\"Ekibastuz\",\"en\":\"Ekibastuz\"}');
INSERT INTO `sys_area` VALUES (22, 'Rudnyy', 0, '0', 63.116772, 52.972903, '{\"cn\":\"Rudnyy\",\"en\":\"Rudnyy\"}');
INSERT INTO `sys_area` VALUES (23, 'Zhezqazghan', 0, '0', 67.766667, 47.783333, '{\"cn\":\"Zhezqazghan\",\"en\":\"Zhezqazghan\"}');
INSERT INTO `sys_area` VALUES (24, 'Türkistan', 0, '0', 68.222892, 43.299228, '{\"cn\":\"Türkistan\",\"en\":\"Türkistan\"}');
INSERT INTO `sys_area` VALUES (25, 'Taldyqorghan', 0, '0', 78.383333, 45.016666, '{\"cn\":\"Taldyqorghan\",\"en\":\"Taldyqorghan\"}');
INSERT INTO `sys_area` VALUES (26, 'Balkhash', 0, '0', 74.995000, 46.848056, '{\"cn\":\"Balkhash\",\"en\":\"Balkhash\"}');
INSERT INTO `sys_area` VALUES (27, 'Sarqan', 0, '0', 79.915449, 45.411259, '{\"cn\":\"Sarqan\",\"en\":\"Sarqan\"}');
INSERT INTO `sys_area` VALUES (28, 'Kentaū', 0, '0', 68.504631, 43.516721, '{\"cn\":\"Kentaū\",\"en\":\"Kentaū\"}');
INSERT INTO `sys_area` VALUES (29, 'Ridder', 0, '0', 83.512868, 50.344129, '{\"cn\":\"Ridder\",\"en\":\"Ridder\"}');
INSERT INTO `sys_area` VALUES (30, 'Arqalyq', 0, '0', 66.920267, 50.249146, '{\"cn\":\"Arqalyq\",\"en\":\"Arqalyq\"}');
INSERT INTO `sys_area` VALUES (31, 'Zyryanovsk', 0, '0', 84.273182, 49.726540, '{\"cn\":\"Zyryanovsk\",\"en\":\"Zyryanovsk\"}');
INSERT INTO `sys_area` VALUES (32, 'Zhitiqara', 0, '0', 61.198936, 52.190190, '{\"cn\":\"Zhitiqara\",\"en\":\"Zhitiqara\"}');
INSERT INTO `sys_area` VALUES (33, 'Ayagöz', 0, '0', 80.434372, 47.964473, '{\"cn\":\"Ayagöz\",\"en\":\"Ayagöz\"}');
INSERT INTO `sys_area` VALUES (34, 'Arys', 0, '0', 68.808701, 42.430155, '{\"cn\":\"Arys\",\"en\":\"Arys\"}');
INSERT INTO `sys_area` VALUES (35, 'Shū', 0, '0', 73.759192, 43.603340, '{\"cn\":\"Shū\",\"en\":\"Shū\"}');
INSERT INTO `sys_area` VALUES (36, 'Qapshaghay', 0, '0', 77.065781, 43.862107, '{\"cn\":\"Qapshaghay\",\"en\":\"Qapshaghay\"}');
INSERT INTO `sys_area` VALUES (37, 'Qarataū', 0, '0', 70.467681, 43.178687, '{\"cn\":\"Qarataū\",\"en\":\"Qarataū\"}');
INSERT INTO `sys_area` VALUES (38, 'Qulsary', 0, '0', 54.016667, 46.983333, '{\"cn\":\"Qulsary\",\"en\":\"Qulsary\"}');
INSERT INTO `sys_area` VALUES (39, 'Zharkent', 0, '0', 80.006549, 44.166600, '{\"cn\":\"Zharkent\",\"en\":\"Zharkent\"}');
INSERT INTO `sys_area` VALUES (40, 'Atbasar', 0, '0', 68.359963, 51.806522, '{\"cn\":\"Atbasar\",\"en\":\"Atbasar\"}');
INSERT INTO `sys_area` VALUES (41, 'Esik', 0, '0', 77.440348, 43.358857, '{\"cn\":\"Esik\",\"en\":\"Esik\"}');
INSERT INTO `sys_area` VALUES (42, 'Aksay', 0, '0', 52.997815, 51.168099, '{\"cn\":\"Aksay\",\"en\":\"Aksay\"}');
INSERT INTO `sys_area` VALUES (43, 'Aral', 0, '0', 61.666667, 46.800000, '{\"cn\":\"Aral\",\"en\":\"Aral\"}');
INSERT INTO `sys_area` VALUES (44, 'Beyneu', 0, '0', 55.100000, 45.183333, '{\"cn\":\"Beyneu\",\"en\":\"Beyneu\"}');
INSERT INTO `sys_area` VALUES (45, 'Balyqshy', 0, '0', 51.866667, 47.066667, '{\"cn\":\"Balyqshy\",\"en\":\"Balyqshy\"}');
INSERT INTO `sys_area` VALUES (46, 'Shīeli', 0, '0', 66.738872, 44.168817, '{\"cn\":\"Shīeli\",\"en\":\"Shīeli\"}');
INSERT INTO `sys_area` VALUES (47, 'Alga', 0, '0', 57.330543, 49.900418, '{\"cn\":\"Alga\",\"en\":\"Alga\"}');
INSERT INTO `sys_area` VALUES (48, 'Oktyabr’sk', 0, '0', 57.419142, 49.469124, '{\"cn\":\"Oktyabr’sk\",\"en\":\"Oktyabr’sk\"}');
INSERT INTO `sys_area` VALUES (49, 'Shalqar', 0, '0', 59.600000, 47.833333, '{\"cn\":\"Shalqar\",\"en\":\"Shalqar\"}');
INSERT INTO `sys_area` VALUES (50, 'Shemonaīkha', 0, '0', 81.912128, 50.628114, '{\"cn\":\"Shemonaīkha\",\"en\":\"Shemonaīkha\"}');
INSERT INTO `sys_area` VALUES (51, 'Ereymentaū', 0, '0', 73.102651, 51.623636, '{\"cn\":\"Ereymentaū\",\"en\":\"Ereymentaū\"}');
INSERT INTO `sys_area` VALUES (52, 'Khromtaū', 0, '0', 58.440034, 50.251173, '{\"cn\":\"Khromtaū\",\"en\":\"Khromtaū\"}');
INSERT INTO `sys_area` VALUES (53, 'Lengir', 0, '0', 69.885821, 42.181518, '{\"cn\":\"Lengir\",\"en\":\"Lengir\"}');
INSERT INTO `sys_area` VALUES (54, 'Makīnsk', 0, '0', 70.419112, 52.632900, '{\"cn\":\"Makīnsk\",\"en\":\"Makīnsk\"}');
INSERT INTO `sys_area` VALUES (55, 'Oytal', 0, '0', 73.228205, 42.890255, '{\"cn\":\"Oytal\",\"en\":\"Oytal\"}');
INSERT INTO `sys_area` VALUES (56, 'Qarazhal', 0, '0', 70.857056, 48.040210, '{\"cn\":\"Qarazhal\",\"en\":\"Qarazhal\"}');
INSERT INTO `sys_area` VALUES (57, 'Boralday', 0, '0', 76.857778, 43.360278, '{\"cn\":\"Boralday\",\"en\":\"Boralday\"}');
INSERT INTO `sys_area` VALUES (58, 'Üshtöbe', 0, '0', 77.980067, 45.252006, '{\"cn\":\"Üshtöbe\",\"en\":\"Üshtöbe\"}');
INSERT INTO `sys_area` VALUES (59, 'Zhosaly', 0, '0', 64.078056, 45.487778, '{\"cn\":\"Zhosaly\",\"en\":\"Zhosaly\"}');
INSERT INTO `sys_area` VALUES (60, 'Atasū', 0, '0', 71.644688, 48.686593, '{\"cn\":\"Atasū\",\"en\":\"Atasū\"}');
INSERT INTO `sys_area` VALUES (61, 'Emba', 0, '0', 58.150422, 48.829814, '{\"cn\":\"Emba\",\"en\":\"Emba\"}');
INSERT INTO `sys_area` VALUES (62, 'Zaysan', 0, '0', 84.881108, 47.467831, '{\"cn\":\"Zaysan\",\"en\":\"Zaysan\"}');
INSERT INTO `sys_area` VALUES (63, 'Derzhavīnsk', 0, '0', 66.315570, 51.099223, '{\"cn\":\"Derzhavīnsk\",\"en\":\"Derzhavīnsk\"}');
INSERT INTO `sys_area` VALUES (64, 'Uryzhar', 0, '0', 81.629393, 47.093020, '{\"cn\":\"Uryzhar\",\"en\":\"Uryzhar\"}');
INSERT INTO `sys_area` VALUES (65, 'Maqat', 0, '0', 53.316667, 47.650000, '{\"cn\":\"Maqat\",\"en\":\"Maqat\"}');
INSERT INTO `sys_area` VALUES (66, 'Esil', 0, '0', 66.408409, 51.954949, '{\"cn\":\"Esil\",\"en\":\"Esil\"}');
INSERT INTO `sys_area` VALUES (67, 'Taiynsha', 0, '0', 69.759873, 53.846646, '{\"cn\":\"Taiynsha\",\"en\":\"Taiynsha\"}');
INSERT INTO `sys_area` VALUES (68, 'Qulan', 0, '0', 72.720661, 42.910104, '{\"cn\":\"Qulan\",\"en\":\"Qulan\"}');
INSERT INTO `sys_area` VALUES (69, 'Otar', 0, '0', 75.216174, 43.538130, '{\"cn\":\"Otar\",\"en\":\"Otar\"}');
INSERT INTO `sys_area` VALUES (70, 'Būlaevo', 0, '0', 70.441549, 54.905963, '{\"cn\":\"Būlaevo\",\"en\":\"Būlaevo\"}');
INSERT INTO `sys_area` VALUES (71, 'Aqadyr', 0, '0', 72.858255, 48.260598, '{\"cn\":\"Aqadyr\",\"en\":\"Aqadyr\"}');
INSERT INTO `sys_area` VALUES (72, 'Ertis', 0, '0', 75.457750, 53.333649, '{\"cn\":\"Ertis\",\"en\":\"Ertis\"}');
INSERT INTO `sys_area` VALUES (73, 'Shar', 0, '0', 81.048515, 49.585591, '{\"cn\":\"Shar\",\"en\":\"Shar\"}');
INSERT INTO `sys_area` VALUES (74, 'Makhambet', 0, '0', 51.583333, 47.666667, '{\"cn\":\"Makhambet\",\"en\":\"Makhambet\"}');
INSERT INTO `sys_area` VALUES (75, 'Zhangaözen', 0, '0', 52.800000, 43.300000, '{\"cn\":\"Zhangaözen\",\"en\":\"Zhangaözen\"}');
INSERT INTO `sys_area` VALUES (76, 'Aqsū', 0, '0', 71.957606, 52.444223, '{\"cn\":\"Aqsū\",\"en\":\"Aqsū\"}');
INSERT INTO `sys_area` VALUES (77, 'Zhänibek', 0, '0', 46.847052, 49.422073, '{\"cn\":\"Zhänibek\",\"en\":\"Zhänibek\"}');
INSERT INTO `sys_area` VALUES (78, 'Qarqaraly', 0, '0', 75.472858, 49.412873, '{\"cn\":\"Qarqaraly\",\"en\":\"Qarqaraly\"}');
INSERT INTO `sys_area` VALUES (79, 'Qusmuryn', 0, '0', 64.619771, 52.451073, '{\"cn\":\"Qusmuryn\",\"en\":\"Qusmuryn\"}');
INSERT INTO `sys_area` VALUES (80, 'Bayghanīn', 0, '0', 55.875121, 48.689755, '{\"cn\":\"Bayghanīn\",\"en\":\"Bayghanīn\"}');
INSERT INTO `sys_area` VALUES (81, 'Osakarovka', 0, '0', 72.570891, 50.562193, '{\"cn\":\"Osakarovka\",\"en\":\"Osakarovka\"}');
INSERT INTO `sys_area` VALUES (82, 'Bestöbe', 0, '0', 73.095919, 52.497951, '{\"cn\":\"Bestöbe\",\"en\":\"Bestöbe\"}');
INSERT INTO `sys_area` VALUES (83, 'Tobyl', 0, '0', 62.581014, 52.696575, '{\"cn\":\"Tobyl\",\"en\":\"Tobyl\"}');
INSERT INTO `sys_area` VALUES (84, 'Zholymbet', 0, '0', 71.712454, 51.742113, '{\"cn\":\"Zholymbet\",\"en\":\"Zholymbet\"}');
INSERT INTO `sys_area` VALUES (85, 'Kishkeneköl', 0, '0', 72.340793, 53.635894, '{\"cn\":\"Kishkeneköl\",\"en\":\"Kishkeneköl\"}');
INSERT INTO `sys_area` VALUES (86, 'Qazaly', 0, '0', 62.107500, 45.762778, '{\"cn\":\"Qazaly\",\"en\":\"Qazaly\"}');
INSERT INTO `sys_area` VALUES (87, 'Chapaev', 0, '0', 51.166667, 50.200000, '{\"cn\":\"Chapaev\",\"en\":\"Chapaev\"}');
INSERT INTO `sys_area` VALUES (88, 'Torghay', 0, '0', 63.498056, 49.633889, '{\"cn\":\"Torghay\",\"en\":\"Torghay\"}');
INSERT INTO `sys_area` VALUES (89, 'Fort-Shevchenko', 0, '0', 50.263880, 44.506544, '{\"cn\":\"Fort-Shevchenko\",\"en\":\"Fort-Shevchenko\"}');
INSERT INTO `sys_area` VALUES (90, 'Aqtaū', 0, '0', 51.166666, 43.650000, '{\"cn\":\"Aqtaū\",\"en\":\"Aqtaū\"}');
INSERT INTO `sys_area` VALUES (91, 'Saryshagan', 0, '0', 73.619167, 46.119167, '{\"cn\":\"Saryshagan\",\"en\":\"Saryshagan\"}');
INSERT INTO `sys_area` VALUES (92, 'Shongzhy', 0, '0', 79.465819, 43.538134, '{\"cn\":\"Shongzhy\",\"en\":\"Shongzhy\"}');
INSERT INTO `sys_area` VALUES (93, 'Serebryansk', 0, '0', 83.295612, 49.684134, '{\"cn\":\"Serebryansk\",\"en\":\"Serebryansk\"}');
INSERT INTO `sys_area` VALUES (94, 'Zhaltyr', 0, '0', 69.835519, 51.628321, '{\"cn\":\"Zhaltyr\",\"en\":\"Zhaltyr\"}');
INSERT INTO `sys_area` VALUES (95, 'Sharbaqty', 0, '0', 78.155562, 52.490866, '{\"cn\":\"Sharbaqty\",\"en\":\"Sharbaqty\"}');
INSERT INTO `sys_area` VALUES (96, 'Būrylbaytal', 0, '0', 74.017766, 44.939840, '{\"cn\":\"Būrylbaytal\",\"en\":\"Būrylbaytal\"}');
INSERT INTO `sys_area` VALUES (97, 'testCity', 0, '0', 6760.000000, 5586.000000, '{\"cn\":\"测试城市\",\"en\":\"testCity\"}');

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `id` bigint(19) NOT NULL AUTO_INCREMENT,
  `param_key` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'key',
  `param_value` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT 'value',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态   0：隐藏   1：显示',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `param_key`(`param_key`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 71 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '系统配置表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES (3, 'alert_api', '192.168.40.247:8080', 1, 'test');
INSERT INTO `sys_config` VALUES (4, 'alert_path_prefix', '/nz-admin', 1, NULL);
INSERT INTO `sys_config` VALUES (5, 'alert_interval', '60', 1, NULL);
INSERT INTO `sys_config` VALUES (6, 'init_project_system', '1', 1, '内置 project：system');
INSERT INTO `sys_config` VALUES (7, 'init_module_node_exporter', '1', 1, '内置 module: node_exporter');
INSERT INTO `sys_config` VALUES (8, 'node_exporter_target_path', '/opt/nezha/node_exporter', 1, '内置 node_exporter 安装路径');
INSERT INTO `sys_config` VALUES (9, 'node_exporter_path', 'build_node_exporter', 1, '内置 node_exporter 项目中的路径');
INSERT INTO `sys_config` VALUES (10, 'node_exporter_name', 'node_exporter.tar.gz', 1, '内置 node_exporter 压缩包名称');
INSERT INTO `sys_config` VALUES (11, 'node_exporter_shell_name', 'init_exporter.sh', 1, '内置 node_exporter 初始执行脚本名称');
INSERT INTO `sys_config` VALUES (12, 'asset_ping_switch', 'on', 1, '主机连通性检查开关，on：开启，off：关闭');
INSERT INTO `sys_config` VALUES (13, 'asset_ping_command', 'fping -f {filename} -e', 1, 'fping命令');
INSERT INTO `sys_config` VALUES (14, 'confagent_port', '8989', 1, 'confagent内置端口');
INSERT INTO `sys_config` VALUES (15, 'confagent_path', '/nz-confagent', 1, 'confagent内置路径');
INSERT INTO `sys_config` VALUES (16, 'confagent_api', '192.168.40.247:8989', 1, 'confagent内置api');
INSERT INTO `sys_config` VALUES (17, 'telnet_user_tip', 'ogin:', 1, 'telnet用户名提示');
INSERT INTO `sys_config` VALUES (18, 'telnet_password_tip', 'assword:', 1, 'telnet密码提示');
INSERT INTO `sys_config` VALUES (19, 'telnet_relogin_password_tip', 'assword:', 1, 'telnet relogin密码提示');
INSERT INTO `sys_config` VALUES (20, 'max_terminal_num', '30', 1, '一个登陆用户最多允许打开窗口数量');
INSERT INTO `sys_config` VALUES (21, 'ldap_address', 'ldap://192.168.40.130:389/', 1, NULL);
INSERT INTO `sys_config` VALUES (22, 'ldap_dn', 'cn=root,dc=bifang,dc=com', 1, NULL);
INSERT INTO `sys_config` VALUES (23, 'ldap_password', '111111', 1, NULL);
INSERT INTO `sys_config` VALUES (30, 'ldap_user_filter', 'uid', 1, NULL);
INSERT INTO `sys_config` VALUES (31, 'ldap_mapping', '{\"email\":\"mail\"}', 1, NULL);
INSERT INTO `sys_config` VALUES (32, 'ldap_enable', 'on', 1, NULL);
INSERT INTO `sys_config` VALUES (33, 'geoJson', '{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{\"name\":\"Akmola Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ÔdƊĳʪèāƂNŎǀ\\\\ǔ§DȆȨC˔ŷǈƾ»ìƞ¦ƪXÄĢʈĤÉŤļÂRƌÐŁLĉǂâHƚƬјƬËjĜĚɐUűȞW¿ūǖCĠŋŘLOôŘXAĆτEUąȦu˖ĴƖǯiŗĥ¾ąȸùʒCĶȺΜ¦¾ėeǩŸήë¿ͦSà¥@ԣǛƭMŅǸd|ȊÙɱmƛ½ŧƉŗzɥ^čƂJŞ³ƍûȳAÓǃ¹ǋvĽśƿYÛ·äÏċ©ĥą­ré͕R[ȔťĂˇÇɷƑL¥ǭ}ëígšĽŵ«ÖïȽõƩ]nŀéò̧Éćþ˭Úĥ˵ÍǵÀCɰ¢RȪʏJ̯ƻȓnēſËȏBΣďÇŬüĵɡʓĊįsÙŤĝŇŴštÓġƹosÆ®ƾǧĸļƄÞȌ³²řBĞƪRȚÐĞɦŊÂPȖ\",\"@@ÒõƒkƈĊ¿Ŧǽrĭſ\"],\"encodeOffsets\":[[67591,53693],[72928,52338]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Aktobe Province\"},\"geometry\":{\"type\":\"MultiPolygon\",\"coordinates\":[[\"@@Л̉ƸƬŨLŖĴf\"],[\"@@ƖʆǎĀǬQ˄˄zŜı²^żÙÊČÀâŀƾø¥ĎfƤŏpDĔƒXƊğȢdçȦţɶāͤŒêºƺƴĈÄ¦ĨľÿÆøǂ]ɎxưŧƶÉª¼ƪOǮĈʸx¼ŚfÓŜRĬćįƴ«ĎĭҚ¿ĊğʠPƸŞdŬƘŤĝО[՞ŲĴδĺdȊNķưCŶ·­đǜĿŹ©SƇƢ¢¾ǁńġĎſƂũÀŏƃeǇ×Ņį§ŏƽȰȳď¯ĒƱʠƙƦäĢÅåė|Û˴ã̖ůȶʃĔƗƂđɗǑƝÅǇĻΫŻɳƛ̱ªrðɯǞĵ^ŗƬųĖӅQyֱ͟έXiǯ́_̡țǩƍϗϑåTćơbŏÍŭɡӵŋ੉ʁřؾŝǦsĞĿŖyŘҥĮʋĆǽŮħǆįŜÆɂCŖ²ǄmƬƇŠ~Ƣ÷Ą¦ǬaŔƍƗgíџļ@ǄȂƅyǝĆ\"]],\"encodeOffsets\":[[[60468,46905]],[[54918,50423]]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Almaty\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ʴ¤wēČěıĵhűèÏƮ\"],\"encodeOffsets\":[[78580,44354]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Almaty Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ĖŘӠ͜ɴ~ʶŀʄXτƨċŤ¬ĨÁľϾÜûĪXÚÒǼP÷ҐŦ¤ɒŬĦņûƊŞ˶ǃłO¤ïϒ˄ʩϞŖƢ­I«֚ȭɘOĜßɸÝƶƇÃŦ×ĘȟĮŋJħÖÛĎEɪŃÝKĿęǧÄ̃YĵńƛBƱā˧ȅ÷ǙPɕȉ×»|ɩšŶęŒnªąĀuĈýÁŅ¸ǃWƳňʑȒ̑ÅōĀuFơǏCǕőŜĽȿĩǵ¶ûXƷªħė]ǭǰϵxƃŤƏĄdĚɧ}ʍÌų¸š͏GƿÂ̛\\\\ǁaá¼ʭNţÙƻfƭ½űÐƕiùħ_ƅĈʉä×ȔĆŞˇøă¾ʠǘ_Ķ¤È]ĲƇĪǝòǅ^șņFǶĵĒɧĦǶ˧oİ~ÊɺAĀç̬ĨŦĤ\",\"@@ÐƭŲçĶgĲċĜxĔʳ£\"],\"encodeOffsets\":[[75839,47106],[78580,44354]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Astana\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ĮƀǾqÀťƇĉƑlÑö\"],\"encodeOffsets\":[[72928,52338]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Atyrau\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ֺÃݒÇɸ©ЬŋOĔʾUȊvǠŞҦȺʒì´ĮǨTĴΤ£UğłVŌ·ŜhȂéĜDBǀÜƤĐIOȠĴ¬π}̰ȈǞąƆzȁ@ǃѠĻîƘhƎbœ¥ǫøă}ơƈşnƫ±ǃDŕÅɁİśĨǅǾŭʌąҦĭzŗŀŕtĝ́nƑĘÓgãƖǛ\\\\īĊǽŷƱXɍ_ŝϓÕɯEÝĳǯĥЯEǦʎcǄē¨Ŕę¸źǯĺÓʅtď°ǑãȩŭÛĥjoØı¢ȳt÷ū¥ǷYēóƑc«ỚŁįí³ĉčIYŏ±û}ƃǃīí°ƿ¨ċöı\\\\ȋö°ȯn^â̤ªÜ¤çŚǡǸǅˈËAȩɾċ¬ϯ^ȥâſǅâYĀŋŦ¼Ȥ\"],\"encodeOffsets\":[[48242,49392]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"East Kazakhstan\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@΢Ĺʜ¿ǘ¨ǦȜŊÙnkȆƅ[Ç²DĒƳąŠįĖŊ¸ːƸæžȰc̚ɒͤѣ̀ϟPúĠÒʐÌȬǀCĢɜµkɐµįǵ̒zȁŐ[ʬ¾˲ÉňǼ{ƞĔKÞƬgʔðǤhȰ·ˆŹ[¯ȎǏüǍaÇƄåȀBȒıƘÛǸȷXġ̼[ĀÁǦbðãŦöµÐĮČV˜ǸÖƙƃȎ»FÙČ¥GÑĊûȴqJıƛǻeřımÁöÿŗřÿ©ñŧ³œBċ«˽CęûŷÑǁöΣyÝÔÙDǯėí_ßɗJ̗ǝǩŶȅÏzѹ˳ðȉĪʫ¨·Ɖǣ̹¡ƣŗ̏±ƫķTťñƭ\\\\ËčFÕÜIĨĭŌėȠťØÄƵƈɷÞěàɗP֙ȮJ¬ơ®ϝŕ˃ʪϑ£ðŁP˵ǄƉŝŅüūĥɑť£ȡ^ûāɉĎĠpŸƸsǔ~ÜǊÔbɂđŎǿŐĴŐǔWŲŢŢæ·ÏĀ\\\\ĔǚŇèEÂʿǌ^¶кŘøú[ǂĴâƾ¾ȎŲYÜ\"],\"encodeOffsets\":[[79562,51428]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Jambyl Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ቒP٢I׸Aੈ@ģħťè̫ ÿɹB}Éį˨pǵɨĥĶđEǵȚŅǆ]Ǟñƈĩ^ı£Ç`ĵʟǗĄ½ˈ÷ąŝØȓʊãƆćęţŭ˵pͅŪďSƣŦũśÜÑɝsͯŵÿ]ƛËǹ®ōƟI¼ǅǫcƓĖȷǻØƉmɍĄȏÅęɇmąƛƭsƷ÷çǗŐâȫźơ¾dǒċŌǩŚʇªďZĒĝhļŀļǨvɜĲŒěвÃȠ»ÊŅ͐ėƆýɬǗȘ\"],\"encodeOffsets\":[[70725,47104]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Karagandy Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ɴƜάżǈļƞÆɘǒƁĒēƘɢƬ±Ĩ̐¦ġƎȖB¢ɚجğɤ¸nĂʠV˸ȾŊzĠƾƼĺǢÎ˨ʒɞƲOĲˬäņŰŹômĔȔ̰ƼʐIQȩ¡DɯǶ¿˶ÎĦˮÙĈý̨ÊêñmĿƪ^ȾöÕðŶ¬ľhŢìîǮ~K¦ɸƒˈÈŦā\\\\ȓ͖QqêĆ®ĦČªãÐÜ¸ǀZľŜǌuǄºBÔȴƎüŝ´ƁI]ĎɦŘy̠Ýdēµũ¬ĝĹÃwīČ­ɀTƀȒ¯˶ǑLçƘÙǡÏŏÝÇƖUˊŤˀbÀŦ}ǂ®ư´þɆtŀµŌˌÂ¤ȚxƪčǔaǐïƽĆ±ZÛȍűƽ½ĳá\\\\ǁ÷ùйŗ]µˀǋFÁňçǙ[ēÐÿΈåšűšǓXĳŏȀŏĒōɁÓaÛǉ} ǓƷtoŷğɊčüĂȢ]øҏǻOÙÑĩWÛüϽĽħÂţ«ƧČσʃWʵĿɳ}ӟ͛ĕŗੇ@׷B١JቑOۡBࢿ@Ťȡ|ȋ[ڇhʱǵČșƐǗtгǀɥ¬˥ĀĝIȗĊǥĔŤ͛îËO\"],\"encodeOffsets\":[[64461,48324]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Kostanay Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@RȃþŁũʊűƗuƽÛìǑŎDKœÃ§DįŜBPʹŦBÅǏHſjáɂW[ëǃNTŇğEY˝OȕŉÁĝɥșÏƩQĝŚA´±ȋƃÝĻǨķ­ƽtÅƺpÔĢŢsňųĞÚţİtʔĉɢĶûÈūΤĐȐAƀÌźóŅů˫ãPıɝƱ˧ʑǡÍƻĹğƽŉy˷ȽʟUmāɣ·ثĠ¡əȕAĢƍ̏¥²ħɡƫȵʄ̕Ű˳ä{ÜæĘġÆƥãʟƚđƲĐ°ȯȴŐƾİ¨ņǈØƄf¿ŐƁŪčƀŃĢ½ǂơ¡TƈźªǛŀ®Ēŵ¸ƯDĸȉMşÌêʫ°ǟNÁŮͯDī|NŶɣĀŇläǒEˀƔǦA̎ǈïƈŝ^DĴŅlEĐĺTʊǲŊ¦ŜäƎéÎɒ£ƜWĺ˭Ôġèū§Ń¸ƥRÏļŤĘŊ±ƒÐÈƋTƧāªȁX˜ƘûĞƄƜƾRø×ƀlŎÉƾ[ö˪IĚėĞNŚǬɰOÒâ̰pʈÄƲBмĂɎYȤÐŬÿİuƀΞĠĪĈøŘjĀÝ\"],\"encodeOffsets\":[[67662,55952]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Kyzylorda Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ÌP͜íēţǦȘĉĞJ˦ÿɦ«дƿǘsȚƏǶċʲڈgȌ\\\\Ȣ{ţ@Çňí¾ûãùɩÇăǼıŀSċj̵eǯċĶÓŴoØėɆŝāû\\\\ŏăąÉĕƅsĥîŵԏȏċ«ࠍӛß`rȒD̺Ʌãʇδ̽ǦƵȂÏjͭƑ֩zёªङŏȧYӡдʋȐŔĭª࠽ӺŁvʌ·ĊƧă˵ϹǆМ¢ÖĲWlłǓéSĈæϘϒǪƎ̢Ȝ̂`jǰήWֲ͠zӆRŴĕŘƫĶ]ɰǝqï̲©\"],\"encodeOffsets\":[[64461,48324]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Mangystau Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@аFǰĦÞĴɰFϔÖŞɎ`ƲWǾŸĬĉǜ[äƕÔhƒė̂mŞǥŚؽصƩ@Ӂ@ַFᎭĿN̓ãƷŞǙȂƓȈDÖƟŪҽ˞խùӯƕЧ˗AŦÑŞ|ĎļňĚȬÎØ]ĞĿÐşōQǙ®ȡŝǚĕÎè˳~ʰĪĩÐŋƦ{ńŃĈ{ƌÃ̃¬ƯüSƈÖĎβŮTǬřČ@ĂÆŀ\\\\ĈįGķʐìņ}žŠŘǔŢ¼ʔNʒąƼT͆ǭWȂĨŲG˴ŝ̭ĻăùóŹșˏPÃĲwľĸŖǶPúǘŶŚmǤÊƞfɬW;ȢĹ©äƧÎěĺĩvÈƿŨȯkÕāɱƧƕǎ̙ÊJúöƈ\"],\"encodeOffsets\":[[54469,47117]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"North Kazakhstan\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ʲÂݶĺ¨¢Ș{ǮîǴiǞfĬãÆö°̲HĔƘǀµIĸȨµ©Вjƶ±îïɸ¿Ⱦł˜GǂǝƱŜơǎķý°ȋȓ_ƬȯĸüǌuA¹ȞNŰ˼ĔŜèvĹľŧµĉĲŞàĀ͖ÔĥȞǮIRÆȾ¨ÝǕȧjǵʷƆñŢœűOsŁĠ{ŜÐĺǗƁúÙĳŜIȈĸąȗŝµÇƨͥTÀέìŷfǪ½ĘΛ¥ȹDĵʑȷú½ĆĦjŘƕǰ˕ĳȥvVĆσFBąŗWPóŗKğŌǕDÀŬȝXŲɏVęiěƫÌїƙƫáGĊǁłK ÏQƋĻÁÊţʇģÃġƩWƝ¥¼ëǇƽ˓ŸȧDCȅǓ¨ƿ[ōƁMçĂʩƉĴÓcZ˞ĠFSňǄM\\\\ìɁXiâƀǐGAÆťOʺśACİÄ¨LŔōCëǒƾÜƘvʉŲŪýłQȄ\"],\"encodeOffsets\":[[67662,55952]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"Pavlodar Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@ǦĲƪtâÔƞkǸËĔėĆĪyĸƦkðĸÎɄ˦ƖCĢȈ|ԔƊ²ĊȀǘx°ÖŰĭĹşģǭCĒĻڪͻӊʭͲ΁Ϫэ˔˳Ɇ˥̙ɑȯdŽƷåˏŉ·İĕĆşƴCđÈ±Ɔ\\\\lȅÚmŉǥțǗ§ʛÀΡĺą²ƾǏðǓbƩĎșwÁ£ŋˋĿ¶Ʌs³ýƯǁ­ť~¿ʿaˉţƕVÈŐÞǢÐƗÚKè˵ǒȑ°ſȿSċ®xĬĺÄ«Ğ¶ŪcĔ̟ÞŨƊƜ¾ɲnȉÚ{ǷcNņǜƮ@Ԥß¦Ƨ¶ÈȘŞĆȇķśJĴùÚǘƂĹśÏğ|tłŲPšŔ\"],\"encodeOffsets\":[[75202,54718]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"South Kazakhstan Province\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@à_ࠎӜČ¬ԐȐŶítĦĖƆĆÊĄ[ŐĂüɅŞ×ĘųpĵÔČfǰi̶TČĿǻĲÈĄɪäú½üŇî@Èࣀ@ۢAǘȗþɫĘƅņ͏¼ÉÄȟĜбıőuɛĻǧĻĿĞgYđĐʈ©ǪřČŋcǑƢ½ȬŹ áǘŏdğíEģñ«ėƃĀſÓƋƿƳÏe˷ÑùċÇLƅŋɓÓCġȡŝƇơÅcJǻxōĝFʅĞȭŦĤĀ@ĘĕIeľč]ō¢̱±փ\\\\ċ̒×͒҅ºFΤÎm\"],\"encodeOffsets\":[[67657,43355]]}},{\"type\":\"Feature\",\"properties\":{\"name\":\"West Kazakhstan\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[\"@@áĿċ¿ÚÉ]ŻĲ±yś˃˃ǫRǍÿƕʅ̯ȇο~ĳ«PȟďJÛƣAƿěCȁêśgŋ¸ŁUVĠΣ¤ĳǧS³ĭʑëҥȹǟŝȉuʽVPēЫŌɷªݑÈֹÄZ̓îɉäʌѤĨ~ŘĜŤǷĜĸѲɼÒƞƆăĈĤĚÈîŜŴĹƪĉƀţđŚřƴqʜðƄĆƟȪã̀ƌKǐòİĒʘÄsȮ͎bɚŨǴ~òĎǢèIŀ̚üФ¨ƓǬÂ`÷Ƅ¢îļĐǒqȶîȘɽȌ¨ĪЪ\\\\ƴÇhıÈÙ̮ËĠ¯Àťè­Ɇ½ķȓŊý\"],\"encodeOffsets\":[[55853,51741]]}}],\"UTF8Encoding\":true}', 1, NULL);
INSERT INTO `sys_config` VALUES (35, 'email_enable', 'off', 1, NULL);
INSERT INTO `sys_config` VALUES (36, 'generator_path', '/opt/nezha/nz-web/generator', 1, 'generator路径');
INSERT INTO `sys_config` VALUES (37, 'email_smtp_host', 'smtp.163.com', 1, NULL);
INSERT INTO `sys_config` VALUES (38, 'email_smtp_port', '25', 1, NULL);
INSERT INTO `sys_config` VALUES (39, 'email_smtp_account', '15833449417@163.com', 1, NULL);
INSERT INTO `sys_config` VALUES (40, 'email_smtp_password', 'XORTBFYKFTFLTADZ', 1, NULL);
INSERT INTO `sys_config` VALUES (41, 'email_send_account', '15833449417@163.com', 1, NULL);
INSERT INTO `sys_config` VALUES (42, 'email_test_reciver', 'xxxxx@qq.com', 1, NULL);
INSERT INTO `sys_config` VALUES (43, 'email_ssl_flag', 'off', 1, NULL);
INSERT INTO `sys_config` VALUES (44, 'email_tls_flag', 'off', 1, NULL);
INSERT INTO `sys_config` VALUES (45, 'asset_ping_interval', '16000', 1, NULL);
INSERT INTO `sys_config` VALUES (46, 'scrape_interval', '15', 1, NULL);
INSERT INTO `sys_config` VALUES (47, 'storage_local_retention', '15', 1, NULL);
INSERT INTO `sys_config` VALUES (48, 'system_name', 'Nezha', 1, NULL);
INSERT INTO `sys_config` VALUES (49, 'ldap_ou', 'Dev', 1, NULL);
INSERT INTO `sys_config` VALUES (50, 'timezone', '8', 1, NULL);
INSERT INTO `sys_config` VALUES (51, 'default_cabinet_uszie', '40', 1, '机柜默认U位值');
INSERT INTO `sys_config` VALUES (52, 'export_pagesize', '1000', 1, 'alert_message到处时默认的每批次查询数量');
INSERT INTO `sys_config` VALUES (53, 'alert_export_header', '{\"zh\":{\"id\":\"唯一标识\",\"alertName\":\"告警规则名称\",\"datacenter\":\"机房名称\",\"project\":\"应用名称\",\"module\":\"组件名称\",\"endpoint\":\"Endpoint\",\"asset\":\"主机地址\",\"labels\":\"告警tag信息\",\"severity\":\"告警级别\",\"summary\":\"告警摘要\",\"description\":\"告警描述\",\"startAt\":\"开始时间\",\"endAt\":\"结束时间\",\"state\":\"状态\"},\"en\":{\"id\":\"ID\",\"alertName\":\"alertName\",\"datacenter\":\"datacenter\",\"project\":\"project\",\"module\":\"module\",\"endpoint\":\"endpoint\",\"asset\":\"asset\",\"labels\":\"labels\",\"severity\":\"severity\",\"summary\":\"summary\",\"description\":\"description\",\"startAt\":\"startAt\",\"endAt\":\"endAt\",\"state\":\"state\"}}', 1, 'alert_message导出表头');
INSERT INTO `sys_config` VALUES (54, 'alert_notify_interval', '50', 1, '单位：s');
INSERT INTO `sys_config` VALUES (69, 'email_template', '<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"></head><body><div style=\"padding:40px 0; height:auto; min-height:100px; text-align:center;\"><span align=\"center\" style=\"margin:0 auto; min-width:290px; max-width:750px;\">${description}</span></div></body></html>', 1, '邮件模板');
INSERT INTO `sys_config` VALUES (70, 'email_smtp_protocol', 'smtp', 1, NULL);

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
) ENGINE = InnoDB AUTO_INCREMENT = 217 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '系统字典表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_dict
-- ----------------------------
INSERT INTO `sys_dict` VALUES (3, '资产状态', 'assetState', 1, '在线', '', 0, '在线', 0, 0, '2019-11-25 14:54:43', NULL);
INSERT INTO `sys_dict` VALUES (4, '资产状态', 'assetState', 2, '下线', '', 0, '下线', 0, 0, '2019-11-25 14:54:46', NULL);
INSERT INTO `sys_dict` VALUES (5, '认证类型', 'authType', 1, 'Password', '', 0, '密码认证', 0, 0, '2019-11-28 10:45:46', NULL);
INSERT INTO `sys_dict` VALUES (6, '认证类型', 'authType', 2, 'Certificate', '', 0, '证书认证', 0, 0, '2019-11-28 10:46:59', NULL);
INSERT INTO `sys_dict` VALUES (112, '图表类型', 'chartType', 1, 'line', '', 1, '折线图', 0, 1, '2019-12-13 17:53:19', NULL);
INSERT INTO `sys_dict` VALUES (113, '图表类型', 'chartType', 2, 'bar', '', 2, '条形图', 0, 1, '2019-12-13 17:53:19', NULL);
INSERT INTO `sys_dict` VALUES (114, '图表类型', 'chartType', 3, 'table', '', 3, '表格', 0, 1, '2019-12-13 17:53:19', NULL);
INSERT INTO `sys_dict` VALUES (115, '表达式类型', 'exprType', 1, 'normal', '', 1, '简单表达式', 0, 1, '2019-12-13 17:53:19', NULL);
INSERT INTO `sys_dict` VALUES (116, '表达式类型', 'exprType', 2, 'expert', '', 2, '用户输入表达式', 0, 1, '2019-12-13 17:53:19', NULL);
INSERT INTO `sys_dict` VALUES (117, '告警周期', 'alertInterval', 0, '17', '', 0, NULL, 0, 0, '2019-12-27 15:56:35', NULL);
INSERT INTO `sys_dict` VALUES (141, '图表类型', 'chartType', 4, 'stackArea', '', 4, '堆栈面积图', 0, 1, '2019-12-13 17:53:19', NULL);
INSERT INTO `sys_dict` VALUES (212, '图表类型', 'chartType', 5, 'singleStat', '', 5, 'singleStat', 0, 1, '2020-03-31 10:57:30', NULL);
INSERT INTO `sys_dict` VALUES (213, '图表类型', 'chartType', 6, 'url', '', 6, 'url', 0, 1, '2020-03-31 10:57:33', NULL);

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
  `user_id` int(10) NULL DEFAULT NULL COMMENT '用户名',
  `operation` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户操作',
  `type` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '可选值\r\n\r\nasset\r\n\r\nendpoint\r\n\r\nproject\r\n\r\nmodule\r\n\r\nalert rule\r\n\r\nalert message\r\n\r\npanel\r\n\r\nchart\r\n\r\ndatacenter\r\n\r\ncabinet',
  `opera_id` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '记录操作类型的id',
  `method` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '请求方法',
  `params` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '请求参数',
  `time` bigint(19) NOT NULL COMMENT '执行时长(毫秒)',
  `ip` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'IP地址',
  `state` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '请求结果',
  `response` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
  `exception` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
  `create_date` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 124 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '系统日志表' ROW_FORMAT = Compact;

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
  `lang` varchar(4) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '默认语言',
  `source` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT 'system' COMMENT '用户来源\r\n可选值：\r\n\r\nsystem（系统内注册用户）\r\n\r\nldap （ldap认证用户）\r\n\r\n默认：system',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `username`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 48 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '系统用户表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', 'e1153123d7d180ceeb820d577ff119876678732a68eef4e6ffc0b1f06a01f91b', 'YzcmCZNvbXocrsz9dm8e', 'nezha@www.puppn.com', 1, '2016-11-11 11:11:11', 'zh', 'system');

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
  PRIMARY KEY (`id`) USING BTREE
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
-- Table structure for idc_traffic
-- ----------------------------
DROP TABLE IF EXISTS `idc_traffic`;
CREATE TABLE `idc_traffic`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `idc_id` int(10) NOT NULL COMMENT '	数据中心id',
  `asset_id` int(10) NOT NULL COMMENT '资产Id',
  `direction` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '流量方向 可选值 rx：接收流量\r\n\r\ntx：发送流量',
  `ifindex` int(10) NOT NULL COMMENT '端口号',
  `ifdescr` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '端口描述',
  `tags` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标签 格式为json对象',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

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
