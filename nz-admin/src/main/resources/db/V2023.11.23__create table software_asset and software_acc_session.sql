SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for software_acc_session
-- ----------------------------
DROP TABLE IF EXISTS `software_acc_session`;
CREATE TABLE `software_acc_session`  (
    `id` int(10) NOT NULL AUTO_INCREMENT,
    `said` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '软件名称',
    `ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT 'IP地址',
    `status` int(1) NOT NULL COMMENT '授权状态, 0：未授权,1：已授权,2：未知错误（网络无法访问）',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for software_asset
-- ----------------------------
DROP TABLE IF EXISTS `software_asset`;
CREATE TABLE `software_asset`  (
    `id` int(10) NOT NULL AUTO_INCREMENT,
    `name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '软件名称',
    `asset_id` int(10) NOT NULL DEFAULT -1 COMMENT '硬件资产ID',
    `type_id` int(10) NOT NULL DEFAULT -1 COMMENT '软件类型ID',
    `params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置参数',
    `date_issued` bigint(20) NULL DEFAULT NULL COMMENT '签发时间,时间戳,单位：ms',
    `date_expires` bigint(20) NULL DEFAULT NULL COMMENT '失效时间,时间戳,单位：ms',
    `concurrency` int(10) NULL DEFAULT NULL COMMENT '授权数量,type = TSG-OS Concentrate 时有效',
    `logins` int(10) NULL DEFAULT NULL COMMENT '已登录数量,type = TSG-OS Concentrate 时有效',
    `status` int(1) NULL DEFAULT NULL COMMENT '授权状态, 0：未授权,1：已授权,2：未知错误（网络无法访问）',
    `remark` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '描述',
    `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '是否内置 1:内置，0：非内置，默认：0',
    `cts` bigint(20) NOT NULL COMMENT '添加时间戳',
    `cid` int(10) NOT NULL COMMENT '添加用户id',
    `uts` bigint(20) NOT NULL COMMENT '更新时间戳',
    `uid` int(10) NOT NULL COMMENT '更新用户id',
    `last_scan_ts` bigint(20) NOT NULL DEFAULT -1 COMMENT '最后扫描时间,单位：ms',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
