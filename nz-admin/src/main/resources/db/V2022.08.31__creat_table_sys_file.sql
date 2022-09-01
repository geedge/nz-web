
/**
 * 2022年08月31日
 * 创建sys_file、sys_file_chunk表
 */
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_file
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_file`  (
     `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
     `uuid` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '唯一索引',
     `name` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '文件名称',
     `type` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '文件类型 默认：application/octet-stream',
     `size` bigint(20) NOT NULL COMMENT '文件大小 默认：0',
     `md5` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '文件MD5',
     `meta` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '用户自定义元数据',
     `storage` int(1) NOT NULL COMMENT '存储类型\r\n1: DB\r\n2: S3\r\n3: file\r\n默认：1',
     `path` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '存储路径\r\n默认：“”\r\n当storage=2 时，保存 s3 地址\r\n当storage=3 时，保存 file 路径',
     `ts` bigint(20) NOT NULL COMMENT '时间戳',
     PRIMARY KEY (`id`) USING BTREE,
     UNIQUE INDEX `uuid`(`uuid`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_file_chunk
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_file_chunk`  (
   `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
   `file_id` int(10) NOT NULL COMMENT '关联 sys_file.id',
   `chunk` longblob NOT NULL COMMENT '数据块',
   PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
