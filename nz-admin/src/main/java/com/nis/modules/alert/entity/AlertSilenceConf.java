package com.nis.modules.alert.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("alert_silence_conf")
public class AlertSilenceConf {
	
	@TableId
	private Integer id;
	
	private String name;
	
	private String remark;
	
	private String matchers;
	
	private Date startAt;
	
	private Date endAt;
	
	private Date utime;
	
	private Integer state;
	
	private String seq;

	@TableField(exist = false)
	private Integer alerts;

}
