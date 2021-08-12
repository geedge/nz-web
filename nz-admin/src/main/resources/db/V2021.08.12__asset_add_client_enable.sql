/**
*  1. asset 表增加 client_enable 字段
*/
ALTER TABLE `asset_asset`  ADD COLUMN `client_enable` int(1) NOT NULL DEFAULT 0 COMMENT '默认：0';