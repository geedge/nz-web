/**
 * 重置 sys_config license value
 */

REPLACE INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (null, 'license', '', 1, null);
