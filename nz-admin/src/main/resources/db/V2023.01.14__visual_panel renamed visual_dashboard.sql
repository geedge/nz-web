
/**
        1. visual_panel renamed visual_dashboard
        2. asset_model
        3. monitor_module
        4. visual_chart col and index
        5. sys_log
        6. sys_user_starred
        7. 对 V3_0 -> V3_2 Chart 升级表进行字段更改 table_name = visual_chart_v3_2
 */

DROP TABLE IF EXISTS visual_dashboard;

-- 复制表结构
CREATE TABLE visual_dashboard like visual_panel;

-- 复制表数据
INSERT INTO visual_dashboard SELECT * FROM visual_panel;

-- 删除原来的表
DROP TABLE IF EXISTS visual_panel;


-- 重命名字段
ALTER TABLE `asset_model`  CHANGE IF EXISTS `panel_id` `dashboard_id` INT(10) COMMENT 'dashboard 模板id';

ALTER TABLE `monitor_module` CHANGE  IF EXISTS `panel_id` `dashboard_id` INT(10) COMMENT 'dashboard 模板id';

ALTER TABLE `visual_chart` CHANGE IF EXISTS  `panel_id` `dashboard_id` int(10) NOT NULL COMMENT '图表 id，关联 dashboard 表 当 dashboard_id = 0 代表为模板';

-- 更新 visual_chart 索引
DROP index IF EXISTS `idx_panel_id` ON `visual_chart`;
ALTER TABLE `visual_chart` ADD index  IF NOT EXISTS `idx_dashboard_id` (`dashboard_id`);


-- sys_log & sys_user_starred
ALTER TABLE `sys_log`
MODIFY COLUMN `type` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '可选值\r\nasset\r\nendpoint\r\nproject\r\nmodule\r\nalert rule\r\nalert message\r\ndashboard\r\nchart\r\ndatacenter\r\ncabinet';

UPDATE `sys_log` SET type = "dashboard" WHERE type = "visual panel";

ALTER TABLE `sys_user_starred`
MODIFY COLUMN  `type` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '可选值:datacenter,project,module,endpoint,alertRule,asset,dashboard,chart';

UPDATE `sys_user_starred` SET type = "dashboard" WHERE type = "panel";


-- visual_chart_v3_2
DROP PROCEDURE IF EXISTS change_visual_chart_v3_2_table_col;
DELIMITER //
CREATE PROCEDURE change_visual_chart_v3_2_table_col()
BEGIN
    DECLARE table_count INT;
    SELECT COUNT(*) INTO table_count FROM information_schema.tables WHERE table_name = 'visual_chart_v3_2' AND table_schema = DATABASE();
    IF table_count > 0 THEN
        ALTER TABLE visual_chart_v3_2
            CHANGE IF EXISTS  `panel_id` `dashboard_id` int(10) NOT NULL COMMENT '图表 id，关联 dashboard 表 当 dashboard_id = 0 代表为模板',
            ADD COLUMN IF NOT EXISTS `datasource` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据源 1：metrics\r\n2：logs\r\n3：system\r\n4：misc';
    END IF;
END //
DELIMITER ;
CALL change_visual_chart_v3_2_table_col();

-- asset_model_V3_6
DROP PROCEDURE IF EXISTS change_asset_model_v3_6_table_col;
DELIMITER //
CREATE PROCEDURE change_asset_model_v3_6_table_col()
BEGIN
    DECLARE table_count INT;
    SELECT COUNT(*) INTO table_count FROM information_schema.tables WHERE table_name = 'asset_model_V3_6' AND table_schema = DATABASE();
    IF table_count > 0 THEN
        ALTER TABLE asset_model_V3_6 CHANGE IF EXISTS `panel_id` `dashboard_id` INT(10) COMMENT 'dashboard 模板id';
    END IF;
END //
DELIMITER ;
CALL change_asset_model_v3_6_table_col();
