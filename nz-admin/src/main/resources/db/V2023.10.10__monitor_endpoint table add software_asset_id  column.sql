/**
        1. monitor_endpoint add software_asset_id
 */

ALTER TABLE `monitor_endpoint`
ADD COLUMN IF NOT EXISTS `software_asset_id` INT(10) NOT NULL DEFAULT -1 COMMENT '软件资产 id';
