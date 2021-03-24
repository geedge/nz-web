ALTER TABLE `sys_user` 
CHANGE COLUMN `password` `pin` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '密码' AFTER `username`;

ALTER TABLE `asset_account` 
CHANGE COLUMN `password` `pin` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'auth_type =1 : 记录登录密码\r\n\r\nauth_type=2 : 记录密钥密码' AFTER `username`,
CHANGE COLUMN `password_tip` `pin_tip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'telnet协议有效' AFTER `user_tip`;

delete from sys_dict where type='chartType' and value='pie';

INSERT INTO `sys_dict`(`name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES ('图表类型', 'chartType', 9, 'pie', '', 9, '饼图', 0, 1, '2021-02-05 10:57:33', NULL);