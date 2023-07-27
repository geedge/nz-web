/**
 * event: beforeValidate
 * 如果表 `flyway_schema_history` 不存在则创建
 *
 * 2023-07-13
 * 1. 删除 sys_timezone 表，清空 flyway_schema_history 中 ’R__AZ_sys_timezone.sql‘记录，避免验证失败
 *
 * 2023-07-18
 * 1. 删除 V2023.07.17__update integration dashboard template.sql 文件，清空 flyway_schema_history 的记录，避免验证失败
 *
 * 2023-07-19
 * 1. V2023.07.18__asset_type_config modify init data.sql SQL 改动，修改 checksum 值
 *
 * 2023-07-27
 * 1. 删除 V2023.07.18.1__modify visual_chart x, y colnum type to double.sql 记录
 *
 */

-- ----------------------------
-- Table structure for flyway_schema_history
-- ----------------------------
CREATE TABLE IF NOT EXISTS `flyway_schema_history` (
  `installed_rank` int(11) NOT NULL,
  `version` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `description` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `type` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `script` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `checksum` int(11) NULL DEFAULT NULL,
  `installed_by` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `installed_on` timestamp(0) NOT NULL DEFAULT current_timestamp(0),
  `execution_time` int(11) NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`) USING BTREE,
  INDEX `flyway_schema_history_s_idx`(`success`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;


DELETE
FROM
	flyway_schema_history
WHERE
	script = "R__AZ_sys_timezone.sql";

DELETE
FROM
	flyway_schema_history
WHERE
	script = "V2023.07.17__update integration dashboard template.sql";

UPDATE flyway_schema_history
SET `checksum` = 475861812
WHERE
	script = "V2023.07.18__asset_type_config modify init data.sql";

DELETE
FROM
	flyway_schema_history
WHERE
	script = "V2023.07.18.1__modify visual_chart x, y colnum type to double.sql";

