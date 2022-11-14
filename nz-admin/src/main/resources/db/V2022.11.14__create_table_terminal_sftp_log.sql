/**
        1.create table terminal_sftp_log
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for terminal_sftp_log
-- ----------------------------
DROP TABLE IF EXISTS `terminal_sftp_log`;
CREATE TABLE `terminal_sftp_log`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'session uuid',
  `type` int(10) NOT NULL COMMENT '1: 上传文件  2：下载文件  3：新建文件夹  4：删除文件',
  `name` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件名',
  `ts` bigint(20) NOT NULL COMMENT '单位：ms',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
