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


SET FOREIGN_KEY_CHECKS = 1;
