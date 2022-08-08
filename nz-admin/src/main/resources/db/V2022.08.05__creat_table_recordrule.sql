/**
 * 2022年08月05日
 * 创建record_rule表
 */
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `record_rule`;
CREATE TABLE `record_rule`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '规则名称 ,唯一索引',
  `type` int(1) NOT NULL COMMENT '类型 1：Metrics 2: Logs',
  `inr` int(10) NOT NULL DEFAULT '0' COMMENT '默认值：0，单位：s  不小于 15  当 < 15, 则为 Scrape interval ',
  `expr` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '表达式',
  `labels` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '{}' COMMENT '标签 默认：{}',
  `state` int(1)  NOT NULL DEFAULT '0' COMMENT '默认启用 状态 1：启用 0：禁用',
  `build_in` int(1)  NOT NULL DEFAULT '0' COMMENT '1：内置 0：非内置，默认：0',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '导入唯一标识  默认值： ‘’',
  `cid` int(10) NOT NULL COMMENT '创建者id',
  `cts` BIGINT(20) NOT NULL COMMENT '创建时间',
  `uid` int(10) NOT NULL COMMENT '更新者id',
  `uts` BIGINT(20) NOT NULL COMMENT '更新时间',
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '备注  默认值： ‘’',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `name`(`name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;