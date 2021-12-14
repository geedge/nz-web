
/**
 * 2021年12月13日
 * 删除 alert talon status 和 monitor endpoint state 表中的时间字段
 */

ALTER TABLE `asset_talon_status` DROP COLUMN `last_update`;

ALTER TABLE `monitor_endpoint_state` DROP COLUMN `time`;