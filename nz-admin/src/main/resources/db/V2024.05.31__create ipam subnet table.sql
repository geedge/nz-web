/**
 * create ipam_subnet table
 */
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ipam_subnet
-- ----------------------------

DROP TABLE IF EXISTS `ipam_subnet`;
CREATE TABLE `ipam_subnet`  (
  `id` INT(10) NOT NULL AUTO_INCREMENT,
  `dc_id` INT(10) NOT NULL COMMENT '数据中心id: 关联 dc.id',
  `protocol` INT(1) NOT NULL COMMENT 'IP类型 4：IPV4 6: IPV6',
  `type` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '子网类型:可选值：single range cidr',
  `addr` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '子网地址',
  `cap` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '总容量 默认值：0',
  `param` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '{}' COMMENT '总容量 默认值：{}',
  `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '备注,默认值： ""',
  `seq` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '导入唯一标识,默认值： ""',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;


-- ----------------------------
-- delete ipam_ip table
-- ----------------------------
DROP TABLE IF EXISTS `ipam_ip`;

SET FOREIGN_KEY_CHECKS = 1;



