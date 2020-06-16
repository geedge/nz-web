/*
    account 表结构变更

    -- 增加字段如下： params
    -- 删除字段如下： auth_type、user、pwd、private_key、user_tip、password_tip、relogin_password_tip
*/

-- 增加字段
alter table account add column params varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  COMMENT '根据协议不同内容不同，json对象保存';
alter table account modify port int(10) not null;
-- 删除字段
alter table account drop auth_type;
alter table account drop user;
alter table account drop pwd;
alter table account drop private_key;
alter table account drop user_tip;
alter table account drop password_tip;
alter table account drop relogin_password_tip;