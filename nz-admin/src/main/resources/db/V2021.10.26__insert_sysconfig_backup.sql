
/**
 * 2021年10月26日
 * sys_config 增加backup相关配置数据
 */

INSERT INTO `sys_config`(`param_key`, `param_value`, `status`, `remark`) VALUES ('backup_file_path', '/opt/nezha/nz-web/backup', 1, 'nezha备份数据默认存储路径');
INSERT INTO `sys_config`(`param_key`, `param_value`, `status`, `remark`) VALUES ('backup_config', '{\"schedule\":{\"repeat\":1,\"stime\":\"2021-09-12 10:10:10\",\"type\":1,\"nums\":[1,2,3,4,5,6,7]},\"state\":1,\"retention\":1}', 1, 'nezha备份数据列表信息');
INSERT INTO `sys_config`(`param_key`, `param_value`, `status`, `remark`) VALUES ('backup_exclude_tables', 'sys_config,dc,cabinet,alert_message_active,alert_message_history', 1, 'nezha备份排除表名');

