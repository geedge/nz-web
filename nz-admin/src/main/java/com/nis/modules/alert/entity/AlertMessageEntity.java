package com.nis.modules.alert.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.asset.entity.Asset;

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
	 * 主键们  eg:[1,2,3]
	 */
	@TableField(exist = false)
	private String ids;
	/**
	 * 告警名
	 */
	private String alertName;
	/**
	 * 告警类型
	 * */
	private Integer type;
	/**
	 * 告警类型关联id
	 */
	private Integer linkId;
	/**
	 * 告警主机ip
	 */
	private String host;
	/**
	 * 告警主机对象
	 */
	@TableField(exist = false)
	private Asset asset;
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

	/**
	 * linkId对应实体类
	 * */
	@TableField(exist = false)
	private Object linkObject;

}
