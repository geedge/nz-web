/**
 * 修改表字段类型
 */
ALTER TABLE `alert_message`
MODIFY COLUMN `summary`  text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '告警摘要' AFTER `severity`,
MODIFY COLUMN `description`  text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '告警描述' AFTER `summary`,
MODIFY COLUMN `labels`  text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '告警labels信息' AFTER `description`;