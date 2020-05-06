/*
   alert_rule表结构变更
   1. 结构变更字段如下： receiver
*/


-- 结构变更
alter table alert_rule modify column receiver varchar(64);