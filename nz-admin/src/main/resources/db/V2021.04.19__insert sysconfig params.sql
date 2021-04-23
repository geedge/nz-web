INSERT INTO `sys_config`( `param_key`, `param_value`, `status`, `remark`) VALUES ( 'system_logo', '', 1, NULL);
INSERT INTO `sys_config`( `param_key`, `param_value`, `status`, `remark`) VALUES ( 'default_scrape_interval', '60', 1, '默认60s');
INSERT INTO `sys_config`( `param_key`, `param_value`, `status`, `remark`) VALUES ( 'default_scrape_timeout', '30', 1, '默认30s');

DELETE from `sys_config` where `param_key` = 'max_terminal_num';

INSERT INTO `sys_config`( `param_key`, `param_value`, `status`, `remark`) VALUES ( 'terminal_timeout', '30', 1, '默认：30，单位：minute');
INSERT INTO `sys_config`( `param_key`, `param_value`, `status`, `remark`) VALUES ( 'terminal_telnet_user_tip', 'ogin:', 1, '默认：ogin:');
INSERT INTO `sys_config`( `param_key`, `param_value`, `status`, `remark`) VALUES ( 'terminal_telnet_pin_tip', 'assword:', 1, '默认：assword:');
INSERT INTO `sys_config`( `param_key`, `param_value`, `status`, `remark`) VALUES ( 'terminal_record_local_retention', '365', 1, '默认：365，单位：天,record数据保留天数');

DELETE from `sys_config` where `param_key` like 'email%' and `param_key` not in ( 'email_template');

INSERT INTO `sys_config`( `param_key`, `param_value`, `status`, `remark`) VALUES ( 'email_enable', 'off', 1, '是否启用邮件');
INSERT INTO `sys_config`( `param_key`, `param_value`, `status`, `remark`) VALUES ( 'email_host', '', 1, '');
INSERT INTO `sys_config`( `param_key`, `param_value`, `status`, `remark`) VALUES ( 'email_port', '25', 1, '');
INSERT INTO `sys_config`( `param_key`, `param_value`, `status`, `remark`) VALUES ( 'email_timeout', '10', 1, '默认：10');
INSERT INTO `sys_config`( `param_key`, `param_value`, `status`, `remark`) VALUES ( 'email_auth_account', '', 1, '');
INSERT INTO `sys_config`( `param_key`, `param_value`, `status`, `remark`) VALUES ( 'email_auth_password', ':', 1, '');
INSERT INTO `sys_config`( `param_key`, `param_value`, `status`, `remark`) VALUES ( 'email_send_account', '', 1, '');
INSERT INTO `sys_config`( `param_key`, `param_value`, `status`, `remark`) VALUES ( 'email_test_account', '', 1, '');
INSERT INTO `sys_config`( `param_key`, `param_value`, `status`, `remark`) VALUES ( 'email_security_type', 'NONE', 1, 'NONE SSL TLS');
INSERT INTO `sys_config`( `param_key`, `param_value`, `status`, `remark`) VALUES ( 'session_timeout', '30', 1, '默认：30，不小于 15 单位：minute');

DROP TABLE IF EXISTS `sys_timezone`;
CREATE TABLE `sys_timezone` (
  `id` int(4) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL COMMENT 'timezone id',
  `offset` int(10) NOT NULL COMMENT '单位：milliseconds',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_timezone
-- ----------------------------
INSERT INTO `sys_timezone` VALUES (1, 'Asia/Shanghai', 28800000);
INSERT INTO `sys_timezone` VALUES (2, 'Asia/Almaty', 21600000);
INSERT INTO `sys_timezone` VALUES (3, 'UTC', 0);


ALTER TABLE `sys_config`
MODIFY COLUMN `param_value` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL AFTER `param_key`;

ALTER TABLE `link`
DROP COLUMN `prev`,
DROP COLUMN `next`,
ADD COLUMN `weight` int(10) NOT NULL AFTER `build_in`;

ALTER TABLE `visual_chart` 
ADD COLUMN `var_type` int(1) NULL COMMENT '模板或使用模板创建有效，pid != null | panel_id == 0 时不能为空\r\n\r\n1: asset\r\n\r\n2: endpoint' AFTER `seq`,
ADD COLUMN `var_id` int(10) NULL COMMENT 'pid != null 时不能为空\r\n\r\n根据var_type 分别对应 asset.id, endpoint.id，用于 表达式变量替换' AFTER `var_type`;
