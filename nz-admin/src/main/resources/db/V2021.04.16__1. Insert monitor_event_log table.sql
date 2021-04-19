/**
 * 1. 新增 monitor_event_log 表
 */
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for monitor_event_log
-- ----------------------------
DROP TABLE IF EXISTS `monitor_event_log`;
CREATE TABLE `monitor_event_log`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `event` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '唯一不重复\r\n\r\n包含值：cmd，prometheus，rule，snmp，blackbox',
  `counter` int(10) NOT NULL COMMENT '描述信息，update  +1',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of monitor_event_log
-- ----------------------------
INSERT INTO `monitor_event_log` VALUES (1, 'blackbox', 0);
INSERT INTO `monitor_event_log` VALUES (2, 'prometheus', 0);
INSERT INTO `monitor_event_log` VALUES (3, 'rule', 0);
INSERT INTO `monitor_event_log` VALUES (4, 'cmd', 0);
INSERT INTO `monitor_event_log` VALUES (5, 'snmp', 0);

SET FOREIGN_KEY_CHECKS = 1;
