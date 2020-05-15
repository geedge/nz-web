/*
   panel表结构变更
   1. 增加字段如下： type、link、build_in

   chart表结构变更
   1. 增加字段如下： pid、build_in

   chart_element表结构变更
   1. 增加字段如下： build_in
*/

-- panel增加字段
alter table panel add column type varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'dashboard' COMMENT 'panel类型 可选值：dashboard，asset，project，module，endpoint, model 默认值：dashboard';
alter table panel add column link int(10) COMMENT '关联id, 当type ！= dashboard 时不能为空';
alter table panel add column build_in char(1) NOT NULL DEFAULT '0' COMMENT '是否内置 1:内置，0：非内置，默认：0';


-- chart增加字段
alter table chart add column pid int(10) COMMENT '模板id';
alter table chart add column build_in char(1) NOT NULL DEFAULT '0' COMMENT '是否内置 1:内置，0：非内置，默认：0';


-- chart_element增加字段
alter table chart_element add column build_in char(1) NOT NULL DEFAULT '0' COMMENT '是否内置 1:内置，0：非内置，默认：0';