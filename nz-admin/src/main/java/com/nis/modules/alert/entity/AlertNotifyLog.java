package com.nis.modules.alert.entity;


import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("alert_notify_log")
public class AlertNotifyLog {

	private Integer id;
	
	private Integer messageId;
	
	private Long userId;
	
	private String method;
	
	private Integer messageState;
	
	private Integer state;
	
	private String errorMsg;
}
