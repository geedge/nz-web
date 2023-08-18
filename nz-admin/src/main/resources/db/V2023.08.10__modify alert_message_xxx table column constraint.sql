/**
 * 1. modify alert_message_active table column constraint
 * 2. modify alert_message_history table column constraint
 * 3. modify alert_message_silence table column constraint
 */

UPDATE alert_message_active
SET project_id = IFNULL(project_id, -1),
    module_id = IFNULL(module_id, -1),
    endpoint_id = IFNULL(endpoint_id, -1),
    asset_id = IFNULL(asset_id, -1),
    dc_id = IFNULL(dc_id, -1)
WHERE project_id IS NULL
   OR module_id IS NULL
   OR endpoint_id IS NULL
   OR asset_id IS NULL
   OR dc_id IS NULL;

UPDATE alert_message_history
SET project_id = IFNULL(project_id, -1),
    module_id = IFNULL(module_id, -1),
    endpoint_id = IFNULL(endpoint_id, -1),
    asset_id = IFNULL(asset_id, -1),
    dc_id = IFNULL(dc_id, -1)
WHERE project_id IS NULL
   OR module_id IS NULL
   OR endpoint_id IS NULL
   OR asset_id IS NULL
   OR dc_id IS NULL;

UPDATE alert_message_silence
SET project_id = IFNULL(project_id, -1),
    module_id = IFNULL(module_id, -1),
    endpoint_id = IFNULL(endpoint_id, -1),
    asset_id = IFNULL(asset_id, -1),
    dc_id = IFNULL(dc_id, -1)
WHERE project_id IS NULL
   OR module_id IS NULL
   OR endpoint_id IS NULL
   OR asset_id IS NULL
   OR dc_id IS NULL;


ALTER TABLE alert_message_active
MODIFY COLUMN IF EXISTS `project_id`    int(10) NOT NULL DEFAULT -1 COMMENT '关联 project id',
MODIFY COLUMN IF EXISTS `module_id`     int(10) NOT NULL DEFAULT -1 COMMENT '关联 module id',
MODIFY COLUMN IF EXISTS `endpoint_id` 	int(10) NOT NULL DEFAULT -1 COMMENT '关联 endpoint id',
MODIFY COLUMN IF EXISTS `asset_id` 		int(10) NOT NULL DEFAULT -1 COMMENT '关联 asset id',
MODIFY COLUMN IF EXISTS `dc_id` 		int(10) NOT NULL DEFAULT -1 COMMENT '关联 dc id';

ALTER TABLE alert_message_history
MODIFY COLUMN IF EXISTS `project_id`   	int(10) NOT NULL DEFAULT -1 COMMENT '关联 project id',
MODIFY COLUMN IF EXISTS `module_id`  	int(10) NOT NULL DEFAULT -1 COMMENT '关联 module id',
MODIFY COLUMN IF EXISTS `endpoint_id` 	int(10) NOT NULL DEFAULT -1 COMMENT '关联 endpoint id',
MODIFY COLUMN IF EXISTS `asset_id` 		int(10) NOT NULL DEFAULT -1 COMMENT '关联 asset id',
MODIFY COLUMN IF EXISTS `dc_id` 		int(10) NOT NULL DEFAULT -1 COMMENT '关联 dc id';

ALTER TABLE alert_message_silence
MODIFY COLUMN IF EXISTS `project_id`   	int(10) NOT NULL DEFAULT -1 COMMENT '关联 project id',
MODIFY COLUMN IF EXISTS `module_id`  	int(10) NOT NULL DEFAULT -1 COMMENT '关联 module id',
MODIFY COLUMN IF EXISTS `endpoint_id` 	int(10) NOT NULL DEFAULT -1 COMMENT '关联 endpoint id',
MODIFY COLUMN IF EXISTS `asset_id` 		int(10) NOT NULL DEFAULT -1 COMMENT '关联 asset id',
MODIFY COLUMN IF EXISTS `dc_id` 		int(10) NOT NULL DEFAULT -1 COMMENT '关联 dc id';
