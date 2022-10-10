/**

        1. vsys_asset_rel add columns state del_flag
        2. tsg asset insert default vsys rel

 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


ALTER TABLE `vsys_asset_rel`
ADD COLUMN IF NOT EXISTS `state` int(10) NOT NULL  COMMENT '下发状态	 记录下发最终结果 1：成功 0：失败',
ADD COLUMN IF NOT EXISTS `del_flag` int(10) NOT NULL DEFAULT 0 COMMENT '是否删除	1：是  0：否  默认：0';


INSERT INTO vsys_asset_rel ( asset_id, vsys_id, state, del_flag, uts )
SELECT
ass.id,
0,
1,
0,
REPLACE(unix_timestamp(current_timestamp(3)),'.','')
FROM
	asset_asset ass
	LEFT JOIN asset_model model ON ass.model_id = model.id
WHERE
	model.tsg_appliance = 1;


SET FOREIGN_KEY_CHECKS = 1;
