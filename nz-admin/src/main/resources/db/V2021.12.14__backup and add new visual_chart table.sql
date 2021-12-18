/**
 * 1. 备份并添加新 visual_chart
 * 2. visual_chart_element 新增 name state col
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `visual_chart_element`
ADD COLUMN `name` varchar(64) NOT NULL DEFAULT '' COMMENT '默认："" 初始化按照 每个chart数据  A-Z 顺序添加',
ADD COLUMN `state` char(1) NOT NULL DEFAULT '1' COMMENT '默认： 1';


ALTER TABLE visual_chart RENAME TO visual_chart_V3_2;

-- ----------------------------
-- Table structure for visual_chart
-- ----------------------------
DROP TABLE IF EXISTS `visual_chart`;
CREATE TABLE `visual_chart`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '图表标题',
  `panel_id` int(10) NOT NULL COMMENT '图表id，关联panel表\r\n\r\n当 panel_id =0 代表为模板',
  `group_id` int(10) NOT NULL COMMENT '关联 visual_chart.id，关联 chart  的 type 只能为 group',
  `span` double(5, 2) NOT NULL COMMENT 'type = group，span = 12, 用户修改无效',
  `height` double(5, 2) NOT NULL COMMENT 'type = group，height = -1，用户修改无效',
  `update_by` int(10) NOT NULL COMMENT '更新人',
  `update_at` datetime(0) NOT NULL COMMENT '更新时间',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '图表类型 line\r\nbar\r\ntable\r\nstackArea\r\nsingleStat\r\nurl\r\nassetInfo\r\nalertList\r\ngroup',
  `unit` int(10) NOT NULL DEFAULT 2 COMMENT '界面显示采用二级菜单',
  `weight` int(10) NOT NULL DEFAULT 0 COMMENT '默认为 0，用于chart排序',
  `param` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '参数 记录type的具体参数信息，数据格式为json对象\r\n\r\n当type = url时\r\n\r\n{\r\n\r\n    “url”:\"https://www.baidu.com\"\r\n\r\n}',
  `pid` int(10) NULL DEFAULT NULL COMMENT '模板id 图表通过模板创建时不能为空，便于后续模板修改同步修改',
  `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '1:内置，0：非内置，默认：0',
  `remark` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '默认：\"\"',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '导入唯一标识',
  `var_type` int(1) NULL DEFAULT NULL COMMENT '模板或使用模板创建有效，pid != null | panel_id == 0 时不能为空\r\n\r\n1: asset\r\n\r\n2: endpoint',
  `var_id` int(10) NULL DEFAULT NULL COMMENT 'pid != null 时不能为空\r\n\r\n根据var_type 分别对应 asset.id, endpoint.id，用于 表达式变量替换',
  `x` double(5, 2) NOT NULL DEFAULT 0 COMMENT 'x轴起点',
  `y` double(5, 2) NOT NULL DEFAULT 0 COMMENT 'y轴起点',
  `datasource` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据源 1：metrics\r\n2：logs\r\n3：system\r\n4：misc',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE,
  INDEX `idx_panel_id`(`panel_id`) USING BTREE,
  INDEX `idx_group_id`(`group_id`) USING BTREE,
  INDEX `idx_type`(`type`) USING BTREE,
  INDEX `idx_var_type`(`var_type`) USING BTREE,
  INDEX `idx_var_id`(`var_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
