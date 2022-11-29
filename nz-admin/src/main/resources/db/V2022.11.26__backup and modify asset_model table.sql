/**
 * 1. 备份并更新 asset_model
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- asset model
DROP  TABLE IF EXISTS asset_model_V3_6;

-- 复制表结构
CREATE TABLE asset_model_V3_6 like asset_model;

-- 复制表数据
INSERT INTO asset_model_V3_6 select * from asset_model;

-- 删除原来数据
DELETE FROM asset_model;

SET FOREIGN_KEY_CHECKS = 1;
