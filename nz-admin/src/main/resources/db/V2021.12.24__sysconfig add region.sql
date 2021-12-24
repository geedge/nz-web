/**
 * sys_config表新增cortex loki配置region参数 
 */

INSERT INTO `sys_config` VALUES (null, 'metrics_storage_s3_region', "", 1, NULL);
INSERT INTO `sys_config` VALUES (null, 'logs_storage_s3_region', "", 1, NULL);
