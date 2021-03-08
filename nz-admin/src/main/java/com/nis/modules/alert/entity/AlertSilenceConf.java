package com.nis.modules.alert.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.Idc;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.module.entity.Module;
import com.nis.modules.project.entity.Project;

import lombok.Data;

@Data
@TableName("alert_silence_conf")
public class AlertSilenceConf {
	
	@TableId
	private Integer id;
	
	private Integer ruleId;
	
	private String type;
	
	private Integer linkId;
	
	private String reason;
	
	private Date startAt;
	
	private Date endAt;
	
	private Date utime;
	
	private Integer state;
	
	private String seq;
	
	@TableField(exist = false)
	private Idc datacenter;
	
	@TableField(exist = false)
	private Project project;
	
	@TableField(exist = false)
	private Module module;
	
	@TableField(exist = false)
	private Asset asset;
	
	@TableField(exist = false)
	private Endpoint endpoint;
	
	@TableField(exist = false)
	private AlertRuleEntity alertRule;
	
	@TableField(exist = false)
	private String ruleName;
	
	@TableField(exist = false)
	private String linkName;
}
