/**
*  1. endpoint 导入导出表头变更
*  2. endpoint 表结构变更
*  3. endpoint state 表结构变更
*  4. 新增 endpoint config表
*/


DELETE from `sys_config` where param_key = "monitor_endpoint_export_header";
INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'monitor_endpoint_export_header', '{\"cn\":{\"*系统\":\"系统名称(必填) \\n示例: Nezha\",\"*组件\":\"组件名称(必填) \\n示例: node_exporter\",\"名称\":\"实例名称 \\n示例: node_exporter\",\"资产名称\":\"资产名称(非必填) 与 主机地址 不能同时为空 \\n示例: A0420200101\",\"配置\":\"实例配置，JSON格式， \\n示例: {\\\"key\\\":\\\"value\\\"}\"},\"en\":{\"*Project Name\":\"Project Name(Required) \\nExample: Nezha\",\"*Module Name\":\"Module Name(Required) \\nExample: node_exporter\",\"Name\":\"Endpoint name \\nExample: node_exporter\",\"Asset Name\":\"Asset name(Not required) And host address cannot be empty at the same time \\n示例: A0420200101\",\"Configs\":\"Endpoint Configs，JSON format， \\nExample: {\\\"key\\\":\\\"value\\\"}\"}}', 1, 'monitor endpoint 导入表头');


ALTER TABLE `monitor_endpoint` DROP COLUMN `configs`;
ALTER TABLE `monitor_endpoint` DROP COLUMN `hash`;
ALTER TABLE `monitor_endpoint` DROP COLUMN `enabled`;


ALTER TABLE `monitor_endpoint_state` DROP PRIMARY KEY;
ALTER TABLE `monitor_endpoint_state` ADD COLUMN `config_id` int(10) NOT NULL AFTER `endpoint_id`;
ALTER TABLE `monitor_endpoint_state` ADD COLUMN `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'metrics，logs' AFTER `config_id`;
ALTER TABLE `monitor_endpoint_state` ADD COLUMN `ver` int(10) NOT NULL AFTER `state`;


CREATE TABLE `monitor_endpoint_config`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `endpoint_id` int(10) NOT NULL,
  `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '可选值：\r\n\r\nmetrics，logs，trace（预留）',
  `config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `version` int(10) NOT NULL DEFAULT 1 COMMENT '默认：1\r\n\r\nupdate +1',
  `enable` int(1) NOT NULL DEFAULT 1 COMMENT '默认：1\r\n\r\n1：开启\r\n\r\n0：关闭\r\n\r\n-1: 未配置',
  `hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '计算 endpointId+ type + config 的hash值，用于确保相同配置不重复',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;
