/**
 *  1. 新增 terminal_session,terminal_cmd,terminal_record三张表
 */

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

 Date: 03/02/2021 14:48:45
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for terminal_cmd
-- ----------------------------
DROP TABLE IF EXISTS `terminal_cmd`;
CREATE TABLE `terminal_cmd`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '会话id',
  `cmd` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户输入命令',
  `time` int(10) NULL DEFAULT NULL COMMENT '单位：ms\r\n\r\n记录的是 session开始后的时间偏移量',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for terminal_record
-- ----------------------------
DROP TABLE IF EXISTS `terminal_record`;
CREATE TABLE `terminal_record`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '会话ID',
  `start_time` int(10) NOT NULL COMMENT '单位：ms\r\n\r\nrecord 开始时间，记录的是会话开始后的时间偏移量',
  `end_time` int(10) NOT NULL COMMENT '单位：ms\r\n\r\nrecord 结束时间，记录的是会话开始后的时间偏移量',
  `content` mediumblob NOT NULL COMMENT '记录的是 session开始后的时间偏移量\r\n\r\n最大储存 65K，超过部分再新增一条记录',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for terminal_session
-- ----------------------------
DROP TABLE IF EXISTS `terminal_session`;
CREATE TABLE `terminal_session`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '会话id',
  `asset_id` int(10) NULL DEFAULT NULL COMMENT '关联资产id',
  `host` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '登录主机',
  `port` int(10) NULL DEFAULT NULL COMMENT '端口',
  `protocol` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '协议，可选值：SSH，TELNET。统一大写',
  `auth_type` int(10) NULL DEFAULT NULL COMMENT '认证类型，1：密码，2：私钥证书  认证类型，1：密码，2：私钥证书\r\n\r\nssh可选值：1、2\r\n\r\ntelnet可选值：1',
  `login_user` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '登录主机的用户名',
  `user_id` int(10) NOT NULL COMMENT 'nezha系统内用户id',
  `remote_addr` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '客户端ip地址',
  `start_time` datetime(0) NOT NULL COMMENT '开始时间',
  `end_time` datetime(0) NULL DEFAULT NULL COMMENT '结束时间',
  `status` int(1) NOT NULL COMMENT '0：连接中\r\n\r\n1：发送失败（没有通过ssh或telnet发送成功）\r\n\r\n2：已结束\r\n\r\n3：强制退出\r\n\r\n4：未知错误',
  `kill_user_id` int(10) NULL DEFAULT NULL COMMENT '强制关闭用户id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;