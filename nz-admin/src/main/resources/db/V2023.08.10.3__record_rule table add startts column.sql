/**
 *
 *    alert_rule table add param column
 *
 **/

ALTER TABLE `record_rule`
ADD COLUMN IF NOT EXISTS `startts` bigint(20) NOT NULL DEFAULT 0 COMMENT '开始执行时间';
