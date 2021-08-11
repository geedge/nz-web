/**
*  1. asset 表增加 client_token 字段
*/
ALTER TABLE `asset_asset`  ADD COLUMN `client_token` varchar(255) NOT NULL DEFAULT '' COMMENT '默认：""';