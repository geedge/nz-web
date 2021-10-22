
/**
 *  1、 visual_chart 增加 x y 列
 */

ALTER TABLE `visual_chart`
ADD COLUMN `x` int(10) NOT NULL DEFAULT 0 COMMENT 'x轴起点',
ADD COLUMN `y` int(10) NOT NULL DEFAULT 0 COMMENT 'y轴起点';
