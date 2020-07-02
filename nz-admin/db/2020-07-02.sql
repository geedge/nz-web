/**
 * 新增link表
 */
DROP TABLE IF EXISTS `link`;
CREATE TABLE `link`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `create_by` int(10) NULL DEFAULT NULL COMMENT '创建人id',
  `url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'url',
  `prev` int(10) NOT NULL COMMENT '前一个panel 记录前一个link.id\r\n\r\n如果前面没有内容了，记录为 0',
  `next` int(10) NOT NULL COMMENT '后一个panel 记录后一个link.id\r\n\r\n如果后面没有内容了，记录为 -1	',
  `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '1:内置，0：非内置，默认：0',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;