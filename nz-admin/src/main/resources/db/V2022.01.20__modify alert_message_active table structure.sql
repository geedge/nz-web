/**
 * 修改 alert_message_active 表结构
 * 1. 修改表结构前，处理重复数据
 * 2. 防止 alert_message_active 出现重复数据，将 hash_key 字段设置为 UNIQUE index ，之后插入数据根据 INSERT INTO VALUES ON DUPLICATE KEY UPDATE 实现 insert or update
 */

DELETE FROM alert_message_active WHERE id NOT IN (
	     SELECT max(id) maxId FROM alert_message_active GROUP BY hash_key
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_hash_key ON alert_message_active(`hash_key`) USING BTREE;
