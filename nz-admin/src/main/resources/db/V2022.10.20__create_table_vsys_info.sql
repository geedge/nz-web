/**
        1. vsys_task add column asset_ids
        2. vsys_asset_rel add column result_id
        3. fill vsys_asset_rel result_id data by vsys_result end id group by asset_id&vsys_id
        4. create table vsys_info
        5. fill vsys_info data by vsys_task
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `vsys_task`
ADD COLUMN IF NOT EXISTS `asset_ids` text NOT NULL DEFAULT '' COMMENT 'asset ids';

ALTER TABLE `vsys_asset_rel`
ADD COLUMN IF NOT EXISTS `result_id` int(10) NOT NULL COMMENT '最后一次 result.id, 0: 代表 default 内置vsys';


UPDATE vsys_asset_rel a
LEFT JOIN (
	SELECT
		vsys_id,
		asset_id,
		SUBSTRING_INDEX ( GROUP_CONCAT( id ORDER BY id DESC ), ',', 1 ) AS result_id
	FROM
		vsys_result
	GROUP BY
		vsys_id,
		asset_id
	) b ON a.vsys_id = b.vsys_id
	AND a.asset_id = b.asset_id
	SET a.result_id = b.result_id
WHERE
	b.result_id IS NOT NULL;

-- ----------------------------
-- Table structure for vsys_info
-- ----------------------------
DROP TABLE IF EXISTS `vsys_info`;
CREATE TABLE `vsys_info`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `task_id` int(10) NOT NULL COMMENT '关联 vsys_task.id',
  `vsys_id` int(10) NOT NULL,
  `device_tag` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `params` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `state` int(1) NOT NULL DEFAULT 1 COMMENT '是否有效   1：是  0：否  默认：1',
  `build_in` int(1) NOT NULL DEFAULT 0 COMMENT '1：内置   0：非内置    默认：0',
  `ts` bigint(20) NOT NULL COMMENT '时间戳',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;


INSERT INTO `vsys_info` ( `task_id`, `vsys_id`, `device_tag`, `params`, `state`, `build_in`, `ts` ) SELECT
b.id,
b.vsys_id,
b.device_tag,
b.params,
1,
0,
REPLACE ( unix_timestamp( CURRENT_TIMESTAMP ( 3 ) ), '.', '' )
FROM
	( SELECT max( id ) AS id FROM `vsys_task` GROUP BY `vsys_id` ) AS a
	JOIN `vsys_task` AS b ON a.id = b.id;


SET FOREIGN_KEY_CHECKS = 1;
