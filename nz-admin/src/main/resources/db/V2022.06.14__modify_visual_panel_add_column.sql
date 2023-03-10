/**
 * 1. visual_panel 表新增 新增 param，remark ，uts 字段
 */

ALTER TABLE `visual_panel`
ADD COLUMN `param` VARCHAR (4096) NOT NULL DEFAULT '{}' COMMENT 'panel 参数',
ADD COLUMN `remark` VARCHAR (256) NOT NULL DEFAULT '' COMMENT '描述信息',
ADD COLUMN `uts` BIGINT (20) NOT NULL COMMENT '更新时间戳';
