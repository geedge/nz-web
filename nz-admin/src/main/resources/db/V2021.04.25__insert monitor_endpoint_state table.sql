/*
 Navicat Premium Data Transfer

 Source Server         : 192.168.40.42
 Source Server Type    : MySQL
 Source Server Version : 100214
 Source Host           : 192.168.40.42:3306
 Source Schema         : nz

 Target Server Type    : MySQL
 Target Server Version : 100214
 File Encoding         : 65001

 Date: 25/04/2021 13:52:18
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for monitor_endpoint_state
-- ----------------------------
DROP TABLE IF EXISTS `monitor_endpoint_state`;
CREATE TABLE `monitor_endpoint_state`  (
  `endpoint_id` int(10) NOT NULL,
  `state` int(4) NOT NULL COMMENT ' ',
  `time` datetime(0) NOT NULL,
  PRIMARY KEY (`endpoint_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
