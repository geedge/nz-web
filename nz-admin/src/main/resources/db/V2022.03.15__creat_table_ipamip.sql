
/**
 * 2022年03月15日
 * 创建ipam_ip表
 */
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `ipam_ip`;
CREATE TABLE `ipam_ip`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '主机名  默认值：‘’',
  `subnet_id` int(10) NOT NULL COMMENT '子网id',
  `addr` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '子网地址',
  `mac` varchar(128)  CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT 'MAC 地址',
  `asset_id` int(10)  COMMENT '资产id',
  `state` int(10)  NOT NULL COMMENT '状态 1:available 2:transient 3:used',
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '备注  默认值： ‘’',
  `uts` BIGINT(20) NOT NULL COMMENT '更新时间戳',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '导入唯一标识  默认值： ‘’',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
