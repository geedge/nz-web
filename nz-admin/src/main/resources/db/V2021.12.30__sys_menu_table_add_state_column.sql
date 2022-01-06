/**
 * 1、sys_menu add state column
 * 2、dashboard overview， setting reset 隐藏，state = 0
 */

ALTER TABLE `sys_menu` ADD COLUMN `state` INT (1) NOT NULL DEFAULT 1 COMMENT '默认：1 0:停用 1:启用';

UPDATE sys_menu
SET state = 0
WHERE
	CODE IN ( "overview", "system_reset_reset" );
