/**
 * 更新 alert message 邮件发送模板
 */
update sys_config set  param_value = "<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"></head><body><div style=\"padding:40px 0; height:auto; min-height:100px; text-align:center;\"><span align=\"center\" style=\"margin:0 auto; min-width:290px; max-width:750px;\">${alertMessage.description}</span></div></body></html>" where param_key = "email_template";