
/**
 * 2022年09月06日
 * 创建asset_comment表
 */
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for asset_comment
-- ----------------------------
CREATE TABLE IF NOT EXISTS `asset_comment` (
    `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `asset_id` int(10) NOT NULL COMMENT '资产id',
    `reply_id` int(10) NOT NULL COMMENT '回复id 默认 -1',
    `content` text NOT NULL COMMENT '评论内容',
    `cts` bigint(20) NOT NULL COMMENT '添加时间戳',
    `uts` bigint(20) NOT NULL COMMENT '更新时间戳',
    `cid` int(10) NOT NULL COMMENT '创建人id	',
    `uid` int(10) NOT NULL COMMENT '更新人id',
 PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='资产评论表';

SET FOREIGN_KEY_CHECKS = 1;
