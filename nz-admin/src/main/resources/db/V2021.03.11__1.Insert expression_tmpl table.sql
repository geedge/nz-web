
/**
 *  1. insert expression_tmpl table
 *  2. expression template excel header configuration
 */
-- ----------------------------
-- Table structure for expression_tmpl
-- ----------------------------
DROP TABLE IF EXISTS `expression_tmpl`;
CREATE TABLE `expression_tmpl`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板名称',
  `gname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'default' COMMENT '组名，默认：default',
  `expression` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '表达式，不能为空',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '描述',
  `build_in` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '1:内置，0：非内置，默认：0',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '用于导入撤销，默认：“”',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;


DELETE from `sys_config` where param_key = "expretmpl_export_header";
INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (183, 'expretmpl_export_header', '{\"cn\":{\"*名称\":\"模板名称(必填) \\n示例: CPU利用率模板\",\"组名\":\"组名 \\n示例: base，默认值: default\",\"*表达式\":\"表达式内容(必填) \\n示例: node_cpu_usage(asset=\\\"{{asset.host}}\\\",device=\\\"{{device}}\\\")\",\"描述\":\"描述 \\n示例: node_export CPU利用率模板\"},\"en\":{\"*Name\":\"Template name (required) \\nExample: CPU utilization template\",\"Group Name\":\"Group Name \\nExample: base, Default value: default\",\"*Expression\":\"Expression content (required) Optional: node_cpu_usage(asset=\\\"{{asset.host}}\\\",device=\\\"{{device}}\\\")\",\"Remark\":\"Remark \\nExample: node_export CPU utilization template\"}}', 1, '表达式导入导出模板表头信息');
