/**
 * 1. 复制 sys_menu 表，在 R__AZ_sys_role_menu.sql 中处理非内置角色和菜单的对应关系
 */

-- 复制表结构
CREATE TABLE sys_menu_backup_v24 like sys_menu;

-- 复制表数据
INSERT INTO sys_menu_backup_v24 select * from sys_menu;
