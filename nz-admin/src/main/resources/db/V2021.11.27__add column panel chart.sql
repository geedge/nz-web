
/**
 * 2021年11月27日
 * visual chart 新增datasource字段 更改 
 * visual panel 注释修改
 */

ALTER TABLE `visual_panel` 
MODIFY COLUMN `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'dashboard' COMMENT '可选值：dashboard，asset，project，module，endpoint, model,overview\r\n\r\n默认值：dashboard';

ALTER TABLE `visual_chart` 
ADD COLUMN `datasource` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据源 1：metrics\r\n2：logs\r\n3：system\r\n4：misc' ;