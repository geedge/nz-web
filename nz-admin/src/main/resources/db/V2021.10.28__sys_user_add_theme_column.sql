
/**
 * 2021年10月28日
 * sys_user 增加theme字段
 */

ALTER TABLE `sys_user` 
ADD COLUMN `theme` int(10) NOT NULL DEFAULT 1 COMMENT '主题 默认：1  1:  light 2：dark';

ALTER TABLE `sys_role` 
MODIFY COLUMN `i18n` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '';