
/**
 * 2022年08月08日
 * 创建issue_record表
 */
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `issue_asset_rel`  (
    `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `issue_id` int(10) NOT NULL COMMENT '问题id',
    `asset_id` int(10) NOT NULL COMMENT '资产id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

CREATE TABLE IF NOT EXISTS `issue_record`  (
     `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
     `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标题',
     `type` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'default' COMMENT '类别',
     `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '维护内容',
     `state` int(0) NOT NULL COMMENT '状态：\r\n\n1：打开\n\r\n2：已指派\r\n\n3：处理中\r\n\n4：挂起\r\n\n5：已解决\r\n\n6：关闭\r\n\n7：取消',
     `priority` int(0) NOT NULL COMMENT '优先级：\r\n\n1：高\r\n\n2：中\r\n\n3：低',
     `cts` bigint(20) NOT NULL COMMENT '添加时间戳',
     `uts` bigint(20) NOT NULL COMMENT '更新时间戳',
     `cid` int(10) NOT NULL COMMENT '创建人id',
     `uid` int(10) NOT NULL COMMENT '更新人id',
     `rid` int(10) NOT NULL COMMENT '报告人 reporter id',
     `aid` int(10) NOT NULL DEFAULT -1 COMMENT '指派人 assignee id，默认：-1（代表未指派）',
     PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
