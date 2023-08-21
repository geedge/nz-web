/**
 *  create table sys_user_query_history
 */


SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_user_query_history
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_query_history`;
CREATE TABLE `sys_user_query_history`  (
    `id` int(10) NOT NULL AUTO_INCREMENT,
    `uid` int(10) NOT NULL COMMENT '用户id',
    `query_key` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '搜索 key',
    `query_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '搜索 value',
    `query_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '搜索 value hash',
    `uts` bigint(20) NOT NULL COMMENT '更新时间戳',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;