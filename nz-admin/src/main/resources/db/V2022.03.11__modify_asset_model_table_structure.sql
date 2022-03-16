/**
 * 1. 备份并更新 asset_model
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 复制表结构
CREATE TABLE asset_model_V3_2 like asset_model;

-- 复制表数据
INSERT INTO asset_model_V3_2 select * from asset_model;

-- 删除原来数据
DELETE FROM asset_model;

-- 更新 asset_model 表结构
ALTER TABLE asset_model
ADD COLUMN IF NOT EXISTS `type_id` int(10) NOT NULL COMMENT '关联 ASSET_TYPE_CONF.id',
ADD COLUMN IF NOT EXISTS `sys_object_id` varchar(256) NOT NULL DEFAULT '' COMMENT '默认： "" 唯一不重复 用于snmp 自动扫描设备型号识别';

SET FOREIGN_KEY_CHECKS = 1;
