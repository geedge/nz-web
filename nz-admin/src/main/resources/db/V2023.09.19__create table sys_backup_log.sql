SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_backup_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_backup_log`;
CREATE TABLE `sys_backup_log`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `uid` int(10) NOT NULL DEFAULT -1 COMMENT '用户ID',
  `type` int(1) NOT NULL COMMENT '操作类型 1:备份，2:还原，3:删除备份文件',
  `filename` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '文件名称',
  `state` int(1) NOT NULL COMMENT '状态 1:成功，0:失败',
  `error_msg` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '错误信息',
  `ts` bigint(20) NOT NULL COMMENT '时间戳',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_uid`(`uid`) USING BTREE,
  INDEX `idx_filename`(`filename`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
