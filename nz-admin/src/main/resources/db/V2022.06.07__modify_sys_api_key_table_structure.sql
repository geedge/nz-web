/**
 * 1. sys_api_key 表新增 remark 字段
 */

ALTER TABLE `sys_api_key`
ADD COLUMN `remark` VARCHAR (256) COMMENT '备注';
