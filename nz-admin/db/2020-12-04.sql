/**
 *  配置表中增加开发人员账号配置,用以限制菜单接口权限
 */
DELETE from sys_config where param_key = "develop_admin";
INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'develop_admin', 'admin', 1, NULL);