/**
 * 1、parma_value 不能为空，默认 ：""
 * 2、sys_config表新增cortex loki配置region参数 
 */

ALTER TABLE `sys_config` 
MODIFY COLUMN `param_value` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' AFTER `param_key`,
MODIFY COLUMN `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态   0：隐藏   1：显示' AFTER `param_value`;

REPLACE INTO `sys_config` VALUES (null, 'metrics_storage_s3_region', '', 1, NULL);
REPLACE INTO `sys_config` VALUES (null, 'logs_storage_s3_region', '', 1, NULL);
