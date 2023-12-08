/**
 * 1.monitor_event_log table add sync column
 */

ALTER TABLE `monitor_event_log`
ADD COLUMN IF NOT EXISTS `sync` int(10) NOT NULL DEFAULT -1 COMMENT '已同步';
