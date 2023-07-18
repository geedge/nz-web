/**
 * 1. modify visual_chart x, y colnum type to double
 */

ALTER TABLE `visual_chart`
MODIFY COLUMN IF EXISTS `x` double(5, 2) NOT NULL DEFAULT 0 COMMENT 'x轴起点',
MODIFY COLUMN IF EXISTS `y` double(5, 2) NOT NULL DEFAULT 0 COMMENT 'y轴起点';
