/**
 *
 */
DELETE from sys_config where param_key = "export_max_line";
INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'export_max_line', '10000', 1, '告警导出最大行数');

DELETE from sys_config where param_key = "endpoint_query_interval";
INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'endpoint_query_interval', '30', 1, 'endpoint 状态采集定时任务周期 单位 s 秒，默认和 prometheus scrape_interval 保持一致');
