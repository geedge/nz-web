/**
* 1. sys_config 新增 LOKI LOG query range  Limit 默认值
*/

DELETE from `sys_config` where param_key = "loki_query_range_default_limit";
INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'loki_query_range_default_limit', '1000', 1, 'LOKI LOG query range 默认 Limit 值 默认 1000');
