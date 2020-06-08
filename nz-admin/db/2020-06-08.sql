/*
    panel表结构变更
   1. 增加字段如下： prev、next

   sys_config 增加 project chart 模板字段
*/

-- panel加字段
alter table panel add column prev int(10) NOT NULL  COMMENT '前一个panel';
alter table panel add column next int(10) NOT NULL COMMENT '后一个panel';

-- sysConfig增加projectInfo 模板字段
INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'project_chart_tpl', '{\"title\":\"project Chart\",\"type\":\"projectInfo\",\"span\":12,\"height\":400,\"unit\":2}', 1, NULL);