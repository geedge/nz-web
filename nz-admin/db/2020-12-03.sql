/**
 * 新增 角色 菜单表 , 其中 角色内置 admin\common 两个角色
 */
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `role_id` int(10) NOT NULL,
  `menu_id` int(10) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=46 DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  `code` varchar(64) NOT NULL COMMENT '唯一主键，前端根据此字段判断是否显示按钮或菜单',
  `i18n` varchar(64) NOT NULL,
  `parent_id` int(10) NOT NULL,
  `perms` varchar(512) DEFAULT NULL COMMENT '多个用逗号分隔，后台根据此字段判断用户是否有权限访问接口',
  `type` int(1) NOT NULL COMMENT '1：菜单 2：按钮',
  `route` varchar(512) DEFAULT NULL,
  `order_num` int(10) NOT NULL COMMENT '默认： 1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4;


DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  `i18n` varchar(64) NOT NULL,
  `remark` varchar(256) DEFAULT NULL,
  `build_in` int(1) NOT NULL DEFAULT 0 COMMENT '可选值：\r\n\r\n0：非内置\r\n\r\n1：内置',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4;

INSERT INTO `sys_role`(`id`, `name`, `i18n`, `remark`, `build_in`) VALUES (1, 'admin', 'admin', '系统内置管理员角色', 1);
INSERT INTO `sys_role`(`id`, `name`, `i18n`, `remark`, `build_in`) VALUES (2, 'common', 'common', '系统内置普通用户角色', 1);


DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `role_id` int(10) NOT NULL,
  `menu_id` int(10) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=46 DEFAULT CHARSET=utf8mb4;


DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `user_id` int(10) NOT NULL,
  `role_id` int(10) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4;