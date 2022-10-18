/**

    1. vsys_asset_rel 新增 task_id 列，关联 vsys_task.id  0: 代表 default 内置vsys
    2. 对已存在的 vsys_asset_rel 数据进行赋值（通过 asset_id&vsys_id 关联 vsys_result 并取 task_id 最大值）

 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `vsys_asset_rel`
ADD COLUMN IF NOT EXISTS `task_id` int(10) NOT NULL COMMENT '关联 vsys_task.id  0: 代表 default 内置vsys';


UPDATE vsys_asset_rel a
INNER JOIN (
	SELECT
		SUBSTRING_INDEX( GROUP_CONCAT( task_id ORDER BY id DESC ), ',', 1 ) AS task_id,
		asset_id,
		vsys_id
	FROM
		vsys_result
	GROUP BY
		asset_id,
		vsys_id
	) b ON a.asset_id = b.asset_id
	AND a.vsys_id = b.vsys_id
	AND b.task_id IS NOT NULL
	SET a.task_id = b.task_id;

SET FOREIGN_KEY_CHECKS = 1;
