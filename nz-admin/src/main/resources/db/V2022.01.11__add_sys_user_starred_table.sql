/**
 *  1、新增 SYS_USER_STARRED 表
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure SYS_USER_STARRED
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_starred`;
CREATE TABLE `sys_user_starred`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `uid` int(10) NOT NULL,
  `type` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '可选值:datacenter,project,module,endpoint,alertRule,asset,panel,chart',
  `tid` int(10) NOT NULL,
  `cts` bigint(20) NOT NULL COMMENT '创建时间戳',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;