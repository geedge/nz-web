/**
 *  1、alert_message_history start_at 添加索引
 *  2、修改 alert_message_history 表主键定义，因为要根据 start_at 字段进行分区，所以将 id,start_at 定义为联合主键
 */

CREATE INDEX idx_start_at ON alert_message_history(`start_at`) USING BTREE;

ALTER TABLE alert_message_history MODIFY id BIGINT ( 20 ) NOT NULL COMMENT '主键';
ALTER TABLE alert_message_history DROP PRIMARY KEY;
ALTER TABLE alert_message_history ADD PRIMARY KEY ( id, start_at ) USING BTREE;
ALTER TABLE alert_message_history MODIFY id BIGINT ( 20 ) NOT NULL AUTO_INCREMENT COMMENT '主键';
