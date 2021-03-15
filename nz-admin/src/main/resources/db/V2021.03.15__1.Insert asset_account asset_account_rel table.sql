SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for asset_account
-- ----------------------------
DROP TABLE IF EXISTS `asset_account`;
CREATE TABLE `asset_account`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `protocol` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '1:SSH\r\n\r\n2:TELNET',
  `auth_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '1：用户名密码\r\n\r\n2：密钥',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '可选值：SSH，TELNET 统一大写',
  `password` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'auth_type =1 : 记录登录密码\r\n\r\nauth_type=2 : 记录密钥密码',
  `pri_key` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `update_by` int(10) NOT NULL,
  `update_at` datetime(0) NOT NULL,
  `user_tip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'telnet协议有效',
  `password_tip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'telnet协议有效',
  `weight` int(3) NOT NULL COMMENT '值：1-100\r\n\r\n默认：10',
  `state` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '0：停用\r\n\r\n1：启用',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '全局唯一，用于导入撤销\r\n',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;


SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for asset_account_rel
-- ----------------------------
DROP TABLE IF EXISTS `asset_account_rel`;
CREATE TABLE `asset_account_rel`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `asset_id` int(10) NOT NULL COMMENT '资产id',
  `account_id` int(10) NOT NULL COMMENT '账号id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `asset_account_unique`(`asset_id`, `account_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 42 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;