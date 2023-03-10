/**
        1. asset_model add column panel_id
        2. monitor_module  add column panel_id
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


ALTER TABLE `asset_model`
    ADD COLUMN  `panel_id` INT(10) COMMENT 'panel 模板id';

ALTER TABLE `monitor_module`
    ADD COLUMN  `panel_id` INT(10) COMMENT 'panel 模板id';

SET FOREIGN_KEY_CHECKS = 1;
