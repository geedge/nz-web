/*
    新增 asset_tag 表、asset_tag 表增加关联conf_event触发器
*/


SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for asset_tag
-- ----------------------------
DROP TABLE IF EXISTS `asset_tag`;
CREATE TABLE `asset_tag`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `asset_id` int(10) NOT NULL COMMENT '资产id',
  `tag` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'key',
  `value` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'value',
  `seq` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '全局唯一UUID，用于导入撤销',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Triggers structure for table asset_tag
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_asset_insert_copy1`;
delimiter ;;
CREATE TRIGGER `tri_asset_insert_copy1` AFTER INSERT ON `asset_tag` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'asset';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table asset_tag
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_asset_update_copy1`;
delimiter ;;
CREATE TRIGGER `tri_asset_update_copy1` AFTER UPDATE ON `asset_tag` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'asset';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table asset_tag
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_asset_delete_copy1`;
delimiter ;;
CREATE TRIGGER `tri_asset_delete_copy1` AFTER DELETE ON `asset_tag` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'asset';
end
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;