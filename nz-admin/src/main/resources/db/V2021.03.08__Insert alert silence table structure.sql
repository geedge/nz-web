SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for alert_silence_conf
-- ----------------------------
DROP TABLE IF EXISTS `alert_silence_conf`;
CREATE TABLE `alert_silence_conf`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rule_id` int(10) NOT NULL DEFAULT -1 COMMENT '默认：-1',
  `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '可选值：datacenter，project，module，endpoint，asset',
  `link_id` int(10) NOT NULL DEFAULT -1 COMMENT '默认：-1\r\n\r\n rule_id 和 link_id 不能同时为空',
  `reason` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '记录告警静音原因\r\n\r\n默认：“”',
  `start_at` datetime(0) NOT NULL COMMENT '开始时间',
  `end_at` datetime(0) NOT NULL COMMENT '结束时间',
  `utime` datetime(0) NOT NULL,
  `state` int(1) NOT NULL COMMENT '可选值：\r\n\r\n1:active\r\n\r\n2:pending\r\n\r\n3:expired',
  `seq` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '全局唯一，用于导入撤销\r\n\r\n默认：“”',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;