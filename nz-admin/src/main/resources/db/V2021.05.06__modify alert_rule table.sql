/**
 * 1. alert_rule 新增 method 列
 * 2. alert_rule 删除 severity 列
 * 3. alert rule 导入导出模板变更
 * 4. 新增 asset_chart_tpl/endpoint_chart_tpl asset endpoint chart 模板
 * 5. 新增 alert_message_active/alert_message_history/alert_message_silence 表
 */

ALTER TABLE `alert_rule` ADD COLUMN `method` VARCHAR(256) COMMENT 'receiver 不为空时，必填';

ALTER TABLE `alert_rule` DROP COLUMN `severity`;

DELETE from sys_config where param_key = "rule_export_header";
INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'rule_export_header', '{\"cn\":{\"*告警规则名称\":\"告警规则名称(必填) \\n示例: cpuAlert\",\"*运算表达式\":\"运算表达式(必填) \\n示例: 100-(avg(irate(node_cpu_seconds_total{mode=\'idle\'}[5m])) by(instance)* 100)\",\"*比较符号\":\"比较符号(必填) 可选: >,<,<=,>=,==,!= \\n示例: >\",\"*阈值\":\"阈值(必填) \\n示例: 10\",\"*单位\":\"单位(必填) 可选: \\nMisc: none,short,percent(0-100),percent(0.0-1.0),local format\\nData: bits,bytes,kilobytes,megabytes,gigabytes,terabytes,petabytes\\nDataRate: packets/sec,bits/sec,bytes/sec,kilobytes/sec,kilobits/sec,megabytes/sec,megabits/sec,gigabytes/sec,gigabits/sec,terabytes/sec,terabits/sec,petabytes/sec,petabits/sec\\nTime: Hertz(1/s),nanoseconds(ns),microseconds(us),milliseconds(ms),seconds(s),minutes(m),hours(h),days(d),YYYY-MM-DD HH:mm:ss,MM/DD/YYYY h:mm:ss a \\n示例: short\",\"*持续时间\":\"持续时间(必填),默认单位（秒） \\n示例:60\",\"*告警级别\":\"告警级别(必填) 可选 P1、P2、P3 \\n示例: P1\",\"*告警摘要\":\"告警摘要(必填) \\n示例: cpu告警\",\"告警详细描述\":\"告警详细描述 \\n示例: cpu使用率超过10%的告警规则\",\"告警通知人\":\"告警通知人，多个使用逗号分隔 \\n示例: admin / admin,root\",\"通知方式\":\"告警通知方式，告警通知人不为空时，必填.多个使用逗号分隔 \\n示例: email / email,telephone\"},\"en\":{\"*AlertName\":\"Alert rule name (required) \\nExample: cpuAlert\",\"*Expr\":\"Operation expression (required) \\nExample: 100-(avg(irate(node_cpu_seconds_total{mode=\'idle\')[5m])) by(instance)* 100)\",\"*Operator\":\"Operation symbol (required) Optional: >,<,<=,>=,==,!= \\nExample:>\",\"*Threshold\":\"Threshold (required) \\nExample: 10\",\"*Unit\":\"Unit (required) Optional: \\nMisc: none,short,percent(0-100),percent(0.0-1.0),local format\\nData: bits,bytes,kilobytes,megabytes,gigabytes,terabytes,petabytes\\nDataRate: packets/sec,bits/sec,bytes/sec,kilobytes/sec,kilobits/sec,megabytes/sec,megabits/sec,gigabytes/sec,gigabits/sec,terabytes/sec,terabits/sec,petabytes/sec,petabits/sec\\nTime: Hertz(1/s),nanoseconds(ns),microseconds(us),milliseconds(ms),seconds(s),minutes(m),hours(h),days(d),YYYY-MM-DD HH:mm:ss,MM/DD/YYYY h:mm:ss a \\nExample:short\",\"*Last\":\"Last (required), the default unit (seconds) \\nExample: 60\",\"*Severity\":\"Alert severity (required) optional P1, P2, P3 \\nExample: P1\",\"*Summary\":\"Alert summary \\nExample: Cpu warning\",\"Description\":\"Alert description \\nExample: Alarm rule for cpu usage exceeding 10%\",\"Receiver\":\"Alert receiver, separated by commas \\nExample: admin / admin,root\",\"Notification method\":\"Alarm notification method. When the receiver is not empty, it is required. Multiple use commas to separate \\nExample: email / email,telephone\"}}', 1, NULL);

DELETE from sys_config where param_key = "asset_chart_tpl";
DELETE from sys_config where param_key = "endpoint_chart_tpl";

INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'asset_chart_tpl', '{\"name\":\"{{asset.sn}}\",\"type\":\"assetInfo\",\"span\":12,\"height\":400,\"unit\":2}', 1, NULL);
INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'endpoint_chart_tpl', '{\"name\":\"{{endpoint.name}}\",\"type\":\"endpointInfo\",\"span\":12,\"height\":400,\"unit\":2}', 1, NULL);


/*
 Navicat Premium Data Transfer

 Source Server         : 192.168.40.42
 Source Server Type    : MySQL
 Source Server Version : 100214
 Source Host           : 192.168.40.42:3306
 Source Schema         : nz

 Target Server Type    : MySQL
 Target Server Version : 100214
 File Encoding         : 65001

 Date: 07/05/2021 15:22:30
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
) ENGINE = InnoDB AUTO_INCREMENT = 20210430071700002 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 20210430064300002 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
