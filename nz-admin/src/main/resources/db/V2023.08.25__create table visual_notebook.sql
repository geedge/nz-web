/**
 *  create table visual_notebook
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for visual_notebook
-- ----------------------------
DROP TABLE IF EXISTS `visual_notebook`;
CREATE TABLE `visual_notebook`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称',
  `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'none' COMMENT '笔记类型 可选值：none（默认）, runbook, postmortem, documentation, report, investigation',
  `param` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '{}' COMMENT 'notebook 参数',
  `charts` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置参数',
  `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '是否内置 1:内置，0：非内置，默认：0',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '描述',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '导入唯一标识',
  `cts` bigint(20) NOT NULL COMMENT '添加时间戳',
  `uts` bigint(20) NOT NULL COMMENT '更新时间戳',
  `cid` int(10) NOT NULL DEFAULT -1 COMMENT '创建人ID',
  `uid` int(10) NOT NULL DEFAULT -1 COMMENT '更新人ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
