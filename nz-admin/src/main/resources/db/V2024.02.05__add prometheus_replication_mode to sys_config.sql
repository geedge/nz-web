/*
 * 新增 prometheus多副本模式 配置，只在 非联邦，本地存储模式时有效，默认关闭
 */
REPLACE INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (null, 'prometheus_replication_mode', '0', 1, 'prometheus多副本模式，只在 非联邦，本地存储模式时有效. nz-agent(Global) 配置一致（互为备份）; 1：开启，0：关闭');
