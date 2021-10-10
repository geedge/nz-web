/**
 *  1、新增 sys_component 表记录 可用 nz-web
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_component
-- ----------------------------
DROP TABLE IF EXISTS `sys_component`;
CREATE TABLE `sys_component`  (
  `serverid` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `component` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '组件名称',
  `host` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'ip地址',
  `port` int(10) NOT NULL COMMENT '端口号',
  `protocol` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'http/https',
  `utime` bigint(20) NOT NULL COMMENT '最后更新时间',
  PRIMARY KEY (`serverid`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;