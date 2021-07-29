/**
 *  1.log 查询最大条数限制
 *  2.log节点数量
 */


DELETE from `sys_config` where param_key = "lokilog_query_max_series";
DELETE from `sys_config` where param_key = "lokilog_query_node_num";

INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'lokilog_query_max_series', '-1', 1, 'log 查询最大条数限制，-1代表不做限制');
INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'lokilog_query_node_num', '100', 1, 'log节点数量');
