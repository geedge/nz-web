/**
*  1. sys_user 表增加 mfa_level,otp_level,recovery_code 字段
*/
ALTER TABLE `sys_user` 
ADD COLUMN `mfa_level` int(1) NOT NULL DEFAULT 0 COMMENT '默认：0，0：关闭，1：开启，2：强制开启' ,
ADD COLUMN `otp_key` varchar(255) NOT NULL DEFAULT '' COMMENT 'mfa 私钥',
ADD COLUMN `recovery_code` varchar(1024) NOT NULL DEFAULT '' COMMENT '恢复码' ;

-- sys_config 表增加 mfa_auth_enable 全局配置
INSERT ignore  INTO `sys_config`(`param_key`, `param_value`, `status`, `remark`) VALUES ('mfa_auth_enable', '0', 1, '1：开启，0：关闭');
