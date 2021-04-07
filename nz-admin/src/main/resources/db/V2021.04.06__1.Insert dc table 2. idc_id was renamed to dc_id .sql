/**
 * 1. insert dc table
 * 2. cabinet prom_server 表字段  idc_id 更名 为 dc_id
 */
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for asset_asset
-- ----------------------------
DROP TABLE IF EXISTS `dc`;
CREATE TABLE `dc`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '名称',
  `location` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '位置',
  `tel` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '联系电话',
  `principal` int(10) NULL DEFAULT NULL COMMENT '机房负责人',
  `seq` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '全局唯一，用于导入撤销',
  `x` int(10) NULL DEFAULT NULL COMMENT '列',
  `y` int(10) NULL DEFAULT NULL COMMENT '行',
  `longitude` double(8, 5) NULL DEFAULT NULL COMMENT '经度 -180,180',
  `latitude` double(8, 5) NULL DEFAULT NULL COMMENT '纬度 -90，90',
  `state` varchar(8) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT 'ON' COMMENT '可选值：ON,OFF 默认：ON',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;


/**
 * cabinet prom_server 表字段  idc_id 更名 为 dc_id
 */
ALTER TABLE `cabinet` CHANGE COLUMN `idc_id` `dc_id` int(11) NOT NULL COMMENT '数据中心id';
ALTER TABLE `prom_server` CHANGE COLUMN `idc_id` `dc_id` int(11) NOT NULL COMMENT '数据中心id';


DROP TABLE IF EXISTS `visual_chart`;
CREATE TABLE `visual_chart`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '图表标题',
  `panel_id` int(10) NOT NULL COMMENT '图表id，关联panel表\r\n\r\n当 panel_id =0 代表为模板',
  `group_id` int(10) NOT NULL COMMENT '关联 visual_chart.id，关联 chart  的 type 只能为 group',
  `span` int(10) NOT NULL COMMENT 'type = group，span = 12, 用户修改无效',
  `height` int(10) NOT NULL COMMENT 'type = group，height = -1，用户修改无效',
  `update_by` int(10) NOT NULL COMMENT '更新人',
  `update_at` datetime(0) NOT NULL COMMENT '更新时间',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '图表类型 line\r\nbar\r\ntable\r\nstackArea\r\nsingleStat\r\nurl\r\nassetInfo\r\nalertList\r\ngroup',
  `unit` int(10) NOT NULL COMMENT '界面显示采用二级菜单',
  `weight` int(10) NOT NULL DEFAULT 0 COMMENT '默认为 0，用于chart排序',
  `param` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '参数 记录type的具体参数信息，数据格式为json对象\r\n\r\n当type = url时\r\n\r\n{\r\n\r\n    “url”:\"https://www.baidu.com\"\r\n\r\n}',
  `pid` int(10) NULL DEFAULT NULL COMMENT '模板id 图表通过模板创建时不能为空，便于后续模板修改同步修改',
  `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '1:内置，0：非内置，默认：0',
  `remark` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '默认：\"\"',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '导入唯一标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;


DROP TABLE IF EXISTS `visual_chart_element`;
CREATE TABLE `visual_chart_element`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `chart_id` int(10) NOT NULL COMMENT '图表id，关联chart表',
  `expression` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'promql表达式',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'normal：简单表达式，可页面解析，如：up{asset=\"192.168.40.40\",module=\"kafka\"}\r\n\r\nexpert：用户输入表达式，解析困难，直接在页面显示',
  `legend` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '图例别名表达式',
  `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '是否内置',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '导入唯一标识 用于导入撤销',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;


DROP TABLE IF EXISTS `visual_expression_tmpl`;
CREATE TABLE `visual_expression_tmpl`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板名称',
  `gname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'default' COMMENT '组名，默认：default',
  `expression` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '表达式，不能为空',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '描述',
  `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '1:内置，0：非内置，默认：0',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '用于导入撤销，默认：“”',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;


DROP TABLE IF EXISTS `visual_panel`;
CREATE TABLE `visual_panel`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '图表名',
  `create_by` int(10) NOT NULL COMMENT '创建人id',
  `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'dashboard' COMMENT '可选值：dashboard，asset，project，module，endpoint, model\r\n\r\n默认值：dashboard',
  `link` int(10) NULL DEFAULT NULL COMMENT '关联id',
  `pid` int(10) NOT NULL DEFAULT 0 COMMENT '父id 默认0',
  `weight` int(10) NOT NULL DEFAULT 0 COMMENT '排序 默认0',
  `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '是否内置',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '默认：“”\r\n\r\n用于导入撤销',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

INSERT INTO `sys_dict`(`name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES ('图表类型', 'chartType', 10, 'group', '', 10, '组', 0, 0, '2021-04-06 14:30:18', NULL);
INSERT INTO `sys_config`(`param_key`, `param_value`, `status`, `remark`) VALUES ('confagent_name', '{\"centos\":\"confagent-v2.0-x86.rpm\",\"ubuntu\":\"confagent-v2.0-x86.zip\"}', 1, 'confagent 下载文件名称');

SET FOREIGN_KEY_CHECKS = 1;


