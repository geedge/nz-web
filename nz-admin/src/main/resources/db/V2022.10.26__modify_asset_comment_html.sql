/**
        1. asset_comment add columns html
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


ALTER TABLE `asset_comment`
    ADD COLUMN IF NOT EXISTS `html` TEXT NOT NULL COMMENT '评论内容 html 格式';


SET FOREIGN_KEY_CHECKS = 1;
