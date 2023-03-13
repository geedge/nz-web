package com.nis.modules.alert.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("alert_notify_log")
public class AlertNotifyLog {

	private Integer id;
	
	private Long messageId;
	
	private Long userId;
	
	private Integer methodId;
	
	private Integer messageState;
	
	private Integer state;
	
	private String errorMsg;
	
	private Date time;
}
