package com.nis.modules.alert.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 
 * 
 * @author song
 * @email 
 * @date 2019-11-25 10:11:01
 */
@Data
@TableName("alert_message")
public class AlertMessageEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@TableId
	private Integer id;
	/**
	 * 告警名
	 */
	private String alertName;
	/**
	 * 告警主机id
	 */
	private Integer assetId;
	/**
	 * 告警级别
	 */
	private String severity;
	/**
	 * 告警摘要
	 */
	private String summary;
	/**
	 * 告警描述
	 */
	private String description;
	/**
	 * 告警labels信息
	 */
	private String labels;
	/**
	 * 开始时间
	 */
	private Date startAt;
	/**
	 * 结束时间
	 */
	private Date endAt;
	/**
	 * 状态
	 */
	private Integer state;

}
