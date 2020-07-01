
/**
 *  创建asset_feature表
 * 
 * */
DROP TABLE IF EXISTS `asset_feature`;
CREATE TABLE `asset_feature`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `asset_id` int(10) NOT NULL COMMENT '资产id',
  `item` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `source` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '采集方式，ssh/snmp',
  `update_at` datetime(0) NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;