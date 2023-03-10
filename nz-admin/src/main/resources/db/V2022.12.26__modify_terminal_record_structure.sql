
/**
        1. terminal_record add column storage_type & path
        2. terminal_record modify column content
 */

ALTER TABLE `terminal_record`
ADD COLUMN  `storage_type` int(1) NOT NULL DEFAULT 1 COMMENT '存储模式，1: 本地存储，2：S3对象存储',
ADD COLUMN  `path` varchar(4096) DEFAULT NULL COMMENT 'S3对象存储时有效',
MODIFY COLUMN `content` MEDIUMBLOB NULL COMMENT '记录的是 session开始后的时间偏移量\r\n\r\n最大储存 65K，超过部分再新增一条记录';

