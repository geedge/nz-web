/**
 *  新增 图谱图配置表 和 拓扑图图标表
 */

/*
 Navicat Premium Data Transfer

 Source Server         : nz-prometheus
 Source Server Type    : MySQL
 Source Server Version : 100214
 Source Host           : 192.168.40.42:3306
 Source Schema         : nz

 Target Server Type    : MySQL
 Target Server Version : 100214
 File Encoding         : 65001

 Date: 25/08/2020 11:26:29
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for project_topo
-- ----------------------------
DROP TABLE IF EXISTS `project_topo`;
CREATE TABLE `project_topo`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` int(10) NOT NULL COMMENT 'project  id',
  `topo` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '拓扑图配置信息',
  `update_at` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `update_by` int(10) NULL DEFAULT NULL COMMENT '更新人id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uni_projectid`(`project_id`) USING BTREE COMMENT '唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for project_topo_icon
-- ----------------------------
DROP TABLE IF EXISTS `project_topo_icon`;
CREATE TABLE `project_topo_icon`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'icon名称',
  `bytes` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'icon图片字节',
  `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '截取自图片后缀，支持jpg，jpeg，png，gif',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;