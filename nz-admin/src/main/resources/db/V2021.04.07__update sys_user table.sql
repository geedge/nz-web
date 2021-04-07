ALTER TABLE `sys_user` 
ADD COLUMN `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户姓名\r\n\r\n(用于系统内展示用户名称)' AFTER `source`,
ADD COLUMN `last_login_time` datetime NULL COMMENT '最后登录时间' AFTER `name`,
ADD COLUMN `last_login_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '最后登录ip	' AFTER `last_login_time`,
ADD COLUMN `mobile` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '手机号' AFTER `last_login_ip`;
CHANGE COLUMN `user_id` `id` int(10) NOT NULL AUTO_INCREMENT FIRST,
CHANGE COLUMN `create_time` `create_at` datetime(0) NULL DEFAULT NULL COMMENT '创建时间' AFTER `status`,
ADD COLUMN `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户姓名\r\n\r\n(用于系统内展示用户名称)' AFTER `source`,
ADD COLUMN `last_login_time` datetime NULL COMMENT '最后登录时间' AFTER `name`,
ADD COLUMN `last_login_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '最后登录ip	' AFTER `last_login_time`,
ADD COLUMN `mobile` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '手机号' AFTER `last_login_ip`,
ADD COLUMN `create_by` int(10) NULL COMMENT '添加人' AFTER `mobile`,
DROP PRIMARY KEY,
ADD PRIMARY KEY (`id`) USING BTREE;

