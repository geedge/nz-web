-- ----------------------------
-- Table structure for asset_type_conf
-- ----------------------------
DROP TABLE IF EXISTS `asset_type_conf`;
CREATE TABLE `asset_type_conf`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `pid` int(10) NOT NULL DEFAULT 0 COMMENT '父id 默认为0',
  `vm` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '是否为虚拟机 0:不是虚拟机\r\n\r\n1:是虚拟机',
  `vmh` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '是否为虚拟机宿主 0:不是宿主机，不支持挂载 虚拟机\r\n\r\n1:是宿主机，支持挂载虚拟机',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `ssh` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'ssh服务 0：关闭\r\n\r\n1：开启',
  `telnet` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'telnet服务	0：关闭\r\n\r\n1：开启 ',
  `remark` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '1:内置，0：非内置，默认：0\r\n\r\n内置数据不允许删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
