/**
 * 删除历史数据升级后的无用表
 *
 */

DROP TABLE IF EXISTS `account`;
DROP TABLE IF EXISTS `alert_message`;
DROP TABLE IF EXISTS `asset`;
DROP TABLE IF EXISTS `asset_tag`;
DROP TABLE IF EXISTS `chart`;
DROP TABLE IF EXISTS `chart_element`;
DROP TABLE IF EXISTS `conf_event`;
DROP TABLE IF EXISTS `endpoint`;
DROP TABLE IF EXISTS `endpoint_state`;
DROP TABLE IF EXISTS `idc`;
DROP TABLE IF EXISTS `idc_traffic`;
DROP TABLE IF EXISTS `metadata_label`;
DROP TABLE IF EXISTS `metadata_metric`;
DROP TABLE IF EXISTS `model`;
DROP TABLE IF EXISTS `module`;
DROP TABLE IF EXISTS `notification_script`;
DROP TABLE IF EXISTS `panel`;
DROP TABLE IF EXISTS `project`;
DROP TABLE IF EXISTS `qrtz_blob_triggers`;
DROP TABLE IF EXISTS `qrtz_calendars`;
DROP TABLE IF EXISTS `qrtz_cron_triggers`;
DROP TABLE IF EXISTS `qrtz_fired_triggers`;
SET foreign_key_checks = 0;
DROP TABLE IF EXISTS `qrtz_job_details`;
SET foreign_key_checks = 1;
DROP TABLE IF EXISTS `qrtz_locks`;
DROP TABLE IF EXISTS `qrtz_paused_trigger_grps`;
DROP TABLE IF EXISTS `qrtz_scheduler_state`;
DROP TABLE IF EXISTS `qrtz_simple_triggers`;
DROP TABLE IF EXISTS `qrtz_simprop_triggers`;
DROP TABLE IF EXISTS `qrtz_triggers`;
DROP TABLE IF EXISTS `sys_area`;
DROP TABLE IF EXISTS `terminal_log`;
DROP TABLE IF EXISTS `user_group`;
DROP TABLE IF EXISTS `user_group_rel`;
DROP TABLE IF EXISTS `user_notification`;