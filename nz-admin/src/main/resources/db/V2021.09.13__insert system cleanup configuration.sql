/**
 * sys_config 新增 数据清理 配置信息
 */
REPLACE INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'terminal_record_local_retention', '365', 1, '默认：365，单位：天,record数据保留天数');
REPLACE INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'alert_rule_eval_log_local_retention', '30', 1, '默认：30，单位：天,record数据保留天数');
REPLACE INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'alert_rule_sched_log_local_retention', '30', 1, '默认：30，单位：天,record数据保留天数');
REPLACE INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'clean_alert_message_enable', '0', 1, '默认：0，0：不清理，1：清理');
REPLACE INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'alert_message_local_retention', '365', 1, '默认：365，单位：天,record数据保留天数');
