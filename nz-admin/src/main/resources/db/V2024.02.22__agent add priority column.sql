
/*
 agent add priority
 */

ALTER TABLE `agent`
ADD COLUMN IF NOT EXISTS `priority` int(5) NOT NULL DEFAULT 0 COMMENT '优先级,默认：0,用于控制优先使用哪一个 agent 查询, 值越大优先级越高，值相等时，根据id 正序排序';
