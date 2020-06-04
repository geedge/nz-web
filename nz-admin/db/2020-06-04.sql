/*
    alert_rule表结构变更
   1. 增加字段如下： operator、threshold、unit
*/

-- alert_rule增加字段
alter table alert_rule add column operator varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL  COMMENT '比较符号';
alter table alert_rule add column threshold bigint NOT NULL COMMENT '阈值';
alter table alert_rule add column unit int(10) NOT NULL COMMENT '单位';