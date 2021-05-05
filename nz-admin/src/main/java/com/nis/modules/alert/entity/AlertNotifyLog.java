package com.nis.modules.alert.entity;


import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("alert_notify_log")
public class AlertNotifyLog {

	private Integer id;
	
	private Long messageId;
	
	private Long userId;
	
	private String method;
	
	private Integer messageState;
	
	private Integer state;
	
	private String errorMsg;
	
	private Date time;
}
