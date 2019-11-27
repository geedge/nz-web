package com.nis.modules.alert.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 
 * 
 * @author song
 * @email 
 * @date 2019-11-25 10:11:01
 */
@Data
@TableName("alert_rule")
@Component
public class AlertRuleEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键id
	 */
	@TableId
	private Integer id;
	/**
	 * 告警名称
	 */
	private String alertName;
	/**
	 * 运算表达式
	 */
	private String expr;
	/**
	 * 持续时间,单位s
	 */
	private Integer last;
	/**
	 * 告警级别
	 */
	private String severity;
	/**
	 * 告警摘要
	 */
	private String summary;
	/**
	 * 告警详细描述
	 */
	private String description;
	/**
	 * 通知用户组
	 */
	private Integer receiver;

}
