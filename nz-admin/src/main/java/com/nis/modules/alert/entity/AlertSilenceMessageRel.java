package com.nis.modules.alert.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("alert_silence_message_rel")
public class AlertSilenceMessageRel {
	
	@TableId
	private Integer id;
	
	private Integer silenceConfId;
	
	private Long messageId;
	
	private String hashKey;
}
