/**
 * 初始化sql 缺失字段默认值等 sql
 */

ALTER TABLE `sys_config`
MODIFY COLUMN `param_value`  longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL AFTER `param_key`;

ALTER TABLE `idc_traffic`
MODIFY COLUMN `port`  int(10) NOT NULL DEFAULT 161 COMMENT 'snmp端口 默认为161' AFTER `host`,
MODIFY COLUMN `version`  int(1) NOT NULL DEFAULT 2 COMMENT 'snmp版本 默认为2' AFTER `port`,
MODIFY COLUMN `community`  varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'public' COMMENT 'snmp团体名 默认为public' AFTER `version`;

ALTER TABLE `alert_rule`
MODIFY COLUMN `alert_name`  varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '告警名称' AFTER `id`,
MODIFY COLUMN `expr`  varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '运算表达式' AFTER `alert_name`,
MODIFY COLUMN `last`  int(11) NULL COMMENT '持续时间,单位s' AFTER `expr`,
MODIFY COLUMN `severity`  varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '告警级别' AFTER `last`;

ALTER TABLE `alert_message`
MODIFY COLUMN `hash_key`  varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT 'redis hash key' AFTER `dc_id`;

ALTER TABLE `sys_user`
MODIFY COLUMN `lang`  varchar(4) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '默认语言' AFTER `create_time`,
MODIFY COLUMN `source`  varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT 'system' COMMENT '用户来源\r\n可选值：\r\n\r\nsystem（系统内注册用户）\r\n\r\nldap （ldap认证用户）\r\n\r\n默认：system' AFTER `lang`;

ALTER TABLE `prom_server`
MODIFY COLUMN `port`  int(10) NOT NULL DEFAULT 9090 COMMENT '端口 默认9090' AFTER `host`,
MODIFY COLUMN `status`  int(1) NOT NULL DEFAULT 0 COMMENT '默认：0， 1：可用，0：prometheus server 不可用，-1：configagent不可用，-2：都不可用' AFTER `type`;


ALTER TABLE `sys_area`
MODIFY COLUMN `pids`  varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 0 COMMENT '所有父id' AFTER `pid`;

ALTER TABLE `sys_log`
MODIFY COLUMN `user_id`  int(10) NOT NULL COMMENT '用户名' AFTER `id`,
MODIFY COLUMN `operation`  varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '用户操作' AFTER `user_id`,
MODIFY COLUMN `type`  varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '可选值\r\n\r\nasset\r\n\r\nendpoint\r\n\r\nproject\r\n\r\nmodule\r\n\r\nalert rule\r\n\r\nalert message\r\n\r\npanel\r\n\r\nchart\r\n\r\ndatacenter\r\n\r\ncabinet' AFTER `operation`,
MODIFY COLUMN `opera_id`  varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '记录操作类型的id' AFTER `type`,
MODIFY COLUMN `method`  varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '请求方法' AFTER `opera_id`,
MODIFY COLUMN `time`  bigint(19) NULL COMMENT '执行时长(毫秒)' AFTER `params`,
MODIFY COLUMN `ip`  varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT 'IP地址' AFTER `time`,
MODIFY COLUMN `create_date`  datetime NOT NULL COMMENT '创建时间' AFTER `exception`;

