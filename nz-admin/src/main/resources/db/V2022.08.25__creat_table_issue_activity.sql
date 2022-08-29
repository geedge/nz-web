
/**
 * 2022年08月25日
 * 创建issue_activity表
 */
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for issue_activity
-- ----------------------------
CREATE TABLE IF NOT EXISTS `issue_activity`  (
     `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
     `type` int(1) NOT NULL COMMENT '类型',
     `issue_id` int(10) NOT NULL COMMENT '问题id',
     `reply_id` int(10) NOT NULL COMMENT '回复id 默认 -1',
     `content` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '评论内容',
     `cts` bigint(20) NOT NULL COMMENT '添加时间戳',
     `uts` bigint(20) NOT NULL COMMENT '更新时间戳',
     `cid` int(10) NOT NULL COMMENT '创建人',
     `uid` int(10) NOT NULL COMMENT '更新人',
     PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '问题动态表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
