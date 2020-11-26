/**
 * nezha 高可用模式设置
 */
INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'ha_mode', '2', 1, '1：Nezha，mariadb，redis分别部署 ,2：nezha，mariadb，redis共同部署在一台设备');
INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'ha_vip', '192.168.40.42', 1, '当 ha_mode = 2时有效，记录nz-web对外提供的 vip');