/**
 * 1. modify alert_rule receiver colnum type to text.
 * 2. modify int_alert_rule receiver colnum type to text.
 */

ALTER TABLE `alert_rule`
MODIFY COLUMN IF EXISTS `receiver` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '通知用户';

ALTER TABLE `int_alert_rule`
MODIFY COLUMN IF EXISTS `receiver` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '通知用户';
