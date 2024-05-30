/**
 * 1. 新增 operation_log_retention 配置，操作日志保留天数, 默认：0
 */
INSERT IGNORE INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'operation_log_retention', '0', 1, '操作日志保留天数');
