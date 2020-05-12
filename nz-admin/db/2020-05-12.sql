/*
    sys_config 增加 测试邮件发送模板信息
*/

-- 增加内置
INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'test_email_send_template', '<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"></head><body><div style=\"padding:40px 0; height:auto; min-height:100px; text-align:center;\"><span align=\"center\" style=\"margin:0 auto; min-width:290px; max-width:750px;\">Nezha test sending email.</span></div></body></html>', 1, NULL);