/**
 *  create table software_asset and software_type
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for software_asset
-- ----------------------------
DROP TABLE IF EXISTS `software_asset`;
CREATE TABLE `software_asset`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '软件名称',
  `asset_id` int(10) NOT NULL DEFAULT -1 COMMENT '硬件资产ID',
  `project_id` int(10) NOT NULL DEFAULT -1 COMMENT '业务系统ID',
  `type_id` int(10) NOT NULL DEFAULT -1 COMMENT '软件类型ID',
  `params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置参数',
  `remark` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '描述',
  `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '是否内置 1:内置，0：非内置，默认：0',
  `cts` bigint(20) NOT NULL COMMENT '添加时间戳',
  `uts` bigint(20) NOT NULL COMMENT '更新时间戳',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for software_type
-- ----------------------------
DROP TABLE IF EXISTS `software_type`;
CREATE TABLE `software_type`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称',
  `category` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '类别',
  `icon` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '图标',
  `params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置参数',
  `remark` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '描述',
  `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '是否内置 1:内置，0：非内置，默认：0',
  `cts` bigint(20) NOT NULL COMMENT '添加时间戳',
  `uts` bigint(20) NOT NULL COMMENT '更新时间戳',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
