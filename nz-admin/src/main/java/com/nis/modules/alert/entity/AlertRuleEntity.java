package com.nis.modules.alert.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.sys.entity.SysUserEntity;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

/**
 *
 *
 * @author song
 * @email
 * @date 2019-11-25 10:11:01
 */
@Data
@TableName("alert_rule")
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
	 * 比较符号
	 */
	private String operator;

	/**
	 * 阈值
	 */
	private Long threshold;

	/**
	 * 单位
	 */
	private Integer unit;
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
	 * 通知用户ids 可多个
	 */
	private String receiver;

	/**
	 * 是否为内置规则
	 */
	private Integer buildIn;

	/**
	 * 告警消息统计
	 */
	@TableField(exist = false)
	private Integer alertNum;

	/**
	 * 通知的用户信息
	 */
	@TableField(exist = false)
	private List<SysUserEntity> receivers;

}
