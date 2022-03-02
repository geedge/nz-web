
/**
 * 2022年3月2日15:14:20
 * 1、更新 alert message 表结构，新增 last_at 字段
 * 2、设置 last_at 初始值
 */
ALTER TABLE `alert_message_active` 
ADD COLUMN IF NOT EXISTS `last_at` datetime(0) NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '最近一次接收时间';

ALTER TABLE `alert_message_silence` 
ADD COLUMN IF NOT EXISTS `last_at` datetime(0) NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '最近一次接收时间';

ALTER TABLE `alert_message_history` 
ADD COLUMN IF NOT EXISTS `last_at` datetime(0) NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '最近一次接收时间';

update alert_message_active set last_at = end_at where last_at = '1970-01-01 00:00:00';
update alert_message_silence set last_at = end_at where last_at = '1970-01-01 00:00:00';
update alert_message_history set last_at = end_at where last_at = '1970-01-01 00:00:00';

ALTER TABLE `alert_message_active` 
ADD INDEX IF NOT EXISTS `idx_last_at`(`last_at`) USING BTREE;

ALTER TABLE `alert_message_silence` 
ADD INDEX IF NOT EXISTS `idx_last_at`(`last_at`) USING BTREE;

ALTER TABLE `alert_message_history` 
ADD INDEX IF NOT EXISTS `idx_last_at`(`last_at`) USING BTREE;