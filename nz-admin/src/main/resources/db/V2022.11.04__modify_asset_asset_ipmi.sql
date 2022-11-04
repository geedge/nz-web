/**
        1. asset_asset add column ipmi_ip
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


ALTER TABLE `asset_asset`
    ADD COLUMN IF NOT EXISTS `ipmi_ip` VARCHAR(64) NOT NULL DEFAULT '';


SET FOREIGN_KEY_CHECKS = 1;
