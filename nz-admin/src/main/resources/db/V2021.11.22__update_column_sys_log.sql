
/**
 * 2021年11月22日
 * sys_log修改operaid类型 防止参数过长报错
 */

ALTER TABLE `sys_log` 
MODIFY COLUMN `opera_id` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1'