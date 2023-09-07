/**

        1. alert_rule drop dashboard_id
        2. alert_rule add param

 */

ALTER TABLE `alert_rule`
DROP COLUMN IF EXISTS `dashboard_id`,
ADD  COLUMN IF NOT EXISTS `param` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '其它参数';
