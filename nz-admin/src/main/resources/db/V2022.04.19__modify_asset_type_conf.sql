/**
 * asset_type_conf 表新增icon字段
 */

ALTER TABLE asset_type_conf ADD COLUMN IF NOT EXISTS icon VARCHAR ( 256 ) NOT NULL DEFAULT '' COMMENT '图标';
