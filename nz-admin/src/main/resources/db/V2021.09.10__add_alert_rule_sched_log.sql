/**
 * 新增 Table structure for alert_rule_sched_log
 */

DROP TABLE IF EXISTS `alert_rule_sched_log`;
CREATE TABLE `alert_rule_sched_log`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `rule_id` int(10) NOT NULL,
  `rule_state` int(1) NOT NULL,
  `state` int(10) NOT NULL,
  `msg` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `ts` bigint(20) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rule_id`(`rule_id`) USING BTREE,
  INDEX `idx_state`(`state`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;
