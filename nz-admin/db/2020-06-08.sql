/*
    panel表结构变更
   1. 增加字段如下： prev、next
*/

-- panel加字段
alter table panel add column prev int(10) NOT NULL  COMMENT '前一个panel';
alter table panel add column next int(10) NOT NULL COMMENT '后一个panel';