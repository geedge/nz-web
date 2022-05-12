/**
 * 增加 sys_config snapshot_template_path 快照模板路径
 */

REPLACE INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (null, 'snapshot_template_path', '/opt/nezha/nz-web/public', 1, null);
