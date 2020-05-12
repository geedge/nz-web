/*
    sys_config 增加 测试邮件发送模板信息
*/

-- 增加内置
INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'test_email_send_template', '<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"></head><body><div style=\"padding:40px 0; height:auto; min-height:100px; text-align:center;\"><span align=\"center\" style=\"margin:0 auto; min-width:290px; max-width:750px;\">Nezha test sending email.</span></div></body></html>', 1, NULL);

/**
 *  sys_config 增加reset 表信息
 */
INSERT INTO `sys_config`(`param_key`, `param_value`, `status`, `remark`) VALUES ('reset_table_infos', '[\"Delete from account\",\"Delete from alert_rule where build_in !=1 \",\"Delete from asset\",\"Delete from cabinet\",\"Delete from chart\",\"Delete from chart_element\",\"Delete from endpoint\",\"Delete from idc\",\"Delete from maintenance_record\",\"Delete from model\",\"Delete from module where build_in !=1 \",\"Delete from panel\",\"Delete from project where build_in !=1 \",\"Delete from prom_server\",\"Delete from snmp_mib\",\"Delete from sys_dict where type IN (\'assetType\',\'vendor\')\",\"Delete from idc_traffic\",\"Delete from sys_user where username!=\'admin\'\"]', 1, '清空项目数据信息');