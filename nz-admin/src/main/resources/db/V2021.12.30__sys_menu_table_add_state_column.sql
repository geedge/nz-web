/**
 * 1、sys_menu add state column
 */

ALTER TABLE `sys_menu` ADD COLUMN `state` INT (1) NOT NULL DEFAULT 1 COMMENT '默认：1 0:停用 1:启用';
