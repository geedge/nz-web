/**
 *  confagent_name 改为 改为正则匹配
 */

REPLACE INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'confagent_name', '{\"centos\":\"nz-agent-.*rpm\",\"ubuntu\":\"nz-agent-.*rpm\"}', 1, 'confagent 下载文件名称');
