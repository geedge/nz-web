
REPLACE INTO `sys_config` VALUES (NULL, 'metrics_query_max_series', '-1', 1, '限制 prometheus query_range 接口 result 数组大小 区间范围[-1 - 1000] -1表示不限制');
REPLACE INTO `sys_config` VALUES (NULL, 'logs_query_range_default_limit', '1000', 1, 'LOKI LOG query range 默认 Limit 值 默认 1000');
