/**
 * 新增 Table structure for alert_rule_sched_log
 */

DROP TABLE IF EXISTS `alert_rule_sched_log`;
CREATE TABLE `alert_rule_sched_log`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `rule_id` int(10) NOT NULL,
  `rule_state` int(1) NOT NULL,
  `state` int(10) NOT NULL,
  `msg` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `ts` bigint(20) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rule_id`(`rule_id`) USING BTREE,
  INDEX `idx_state`(`state`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;


/**
 *  1. 修改 sys_user username 字段长度限制
 *  2. 修改 alert_rule operator 字段
 *  3. 修改 asset_field_meta remark 字段长度限制
 */

ALTER TABLE  `sys_user`
MODIFY COLUMN `username` varchar(64) NOT NULL COMMENT '用户名';

ALTER TABLE  `alert_rule`
MODIFY COLUMN  `operator` varchar(64) CHARACTER SET utf8mb4 NOT NULL DEFAULT '' COMMENT '比较符号';

ALTER TABLE  `asset_field_meta`
MODIFY COLUMN  `remark` varchar(256) NOT NULL DEFAULT '';
