/**
 *  create table sys_lock
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_lock
-- ----------------------------
DROP TABLE IF EXISTS `sys_lock`;
CREATE TABLE `sys_lock`  (
    `lock_key` varchar(256) NOT NULL,
    `lock_type` varchar(256) NOT NULL COMMENT '锁类型；1：乐观锁   2：悲观锁',
    `lock_value` varchar(4096)  NOT NULL DEFAULT '',
    `version` bigint(20) NOT NULL DEFAULT 1,
    PRIMARY KEY (`lock_key`) USING BTREE
) ENGINE = InnoDB ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_lock
-- ----------------------------
INSERT INTO `sys_lock` VALUES ('alert_message_id', '1', '10000', 1);

SET FOREIGN_KEY_CHECKS = 1;
