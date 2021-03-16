SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for alert_notify_log
-- ----------------------------
DROP TABLE IF EXISTS `alert_notify_log`;
CREATE TABLE `alert_notify_log`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `message_id` int(10) NOT NULL COMMENT '关联 alert_message.id',
  `user_id` int(10) NOT NULL COMMENT '关联 sys_user.id',
  `method` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '通知方式	VARCHARemail 或 NOTIFICATION_SCRIPT.name',
  `message_state` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '消息状态1: active2: expired',
  `state` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '通知状态0:失败1:成功',
  `error_msg` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '发送错误信息',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;