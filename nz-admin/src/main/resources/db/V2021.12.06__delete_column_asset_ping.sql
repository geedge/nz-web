
/**
 * 2021年12月06日
 * asset ping删除rtt和last update字段  添加主键
 */

ALTER TABLE `asset_ping` 
DROP COLUMN `rtt`,
DROP COLUMN `last_update`,
ADD COLUMN `id` int NOT NULL AUTO_INCREMENT,
ADD PRIMARY KEY (`id`);