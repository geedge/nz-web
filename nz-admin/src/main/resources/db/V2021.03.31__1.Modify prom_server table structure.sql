/**
 *  1. prom_server 表新增 status_info 、token 字段
 */
ALTER TABLE prom_server ADD COLUMN `status_info` varchar(256) NOT NULL DEFAULT '';
ALTER TABLE prom_server ADD COLUMN`token` varchar(64) NOT NULL;


