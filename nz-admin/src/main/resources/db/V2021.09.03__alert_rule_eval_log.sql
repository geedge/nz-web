/**
 * 新增 alert_rule_eval_log 表
 */ 
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for alert_rule_eval_log
-- ----------------------------
DROP TABLE IF EXISTS `alert_rule_eval_log`;
CREATE TABLE `alert_rule_eval_log`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `rule_id` int(10) NOT NULL,
  `state` int(10) NOT NULL,
  `msg` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `stts` bigint(20) NOT NULL,
  `etts` bigint(20) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_message_id`(`rule_id`) USING BTREE,
  INDEX `idx_state`(`etts`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;