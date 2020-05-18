/*
   sys_config 增加 model chart 模板字段
*/

-- sys_config表增加数据
INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (75, 'model_chart_tpl', '{\"title\":\"\",\"type\":\"line\",\"span\":12,\"height\":400,\"unit\":2,\"elements\":[{\"expression\":\"up\",\"type\":\"normal\"}]}', 1, NULL);