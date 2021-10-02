/**
 *  1、agent增加 protocol 属性，默认
 */

ALTER TABLE `prom_server` 
ADD COLUMN `protocol` varchar(32) NOT NULL DEFAULT 'http' COMMENT 'https，http';