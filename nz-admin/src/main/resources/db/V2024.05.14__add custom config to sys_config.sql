/**
 * 添加用户可自定义的配置项，如果之前存在配置项则不添加
 */

INSERT IGNORE INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (null, 'date_format', 'YYYY-MM-DD HH:mm:ss', 1, '时间格式化');
INSERT IGNORE INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (null, 'pin_policy', '{\"enable\":0,\"length\":6,\"num\":1,\"lower\":1,\"upper\":1,\"spec\":1,\"valid\":30,\"remind\":3,\"retry\":2,\"lockTime\":30}', 1, NULL);
INSERT IGNORE INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (null, 'system_bgImg', '', 1, null);
INSERT IGNORE INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (null, 'system_favicon', '', 1, null);
INSERT IGNORE INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (null, 'interface_name', 'eth0,en0,bond0,ens2f0,enp0s31f6', 1, NULL);
INSERT IGNORE INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (null, 'terminal_storage_type', '1', 1, '存储模式，1: 本地存储，2：S3对象存储');
INSERT IGNORE INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (null, 'language', 'en', 1, NULL);
INSERT IGNORE INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (null, 'theme', 'light', 1, NULL);
UPDATE `sys_config` SET `param_value`='Network Zodiac' WHERE `param_key`='system_name' AND `param_value`='One-stop monitoring system';
