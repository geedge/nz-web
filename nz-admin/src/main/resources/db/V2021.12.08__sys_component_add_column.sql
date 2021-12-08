
/**
 * 2021年12月08日
 * 1、删除 sys_component 历史数据
 * 2、新增 id字段
 */
delete from `sys_component`;
ALTER TABLE `sys_component` 
ADD COLUMN `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键' AFTER `utime`,
DROP PRIMARY KEY,
ADD PRIMARY KEY (`id`) USING BTREE;