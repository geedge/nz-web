/**
 * 系统内置 project 更名为 Common
 */
update project set name = 'Common' where id = 1;

/**
 *  ldap template 查询超时时间
 */
DELETE from sys_config where param_key = "ldap_query_timeout";
INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'ldap_query_timeout', '3000', 1, 'ldap template 查询超时时间，单位 milliseconds 毫秒');