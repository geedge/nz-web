package com.nis.modules.alert.entity;

import lombok.Data;

import java.io.Serializable;
import java.text.SimpleDateFormat;

/**
 *
 *
 * @author song
 * @email
 * @date 2019-11-25 10:11:01
 */
@Data
public class AlertMessageWithExport implements Serializable{
	private static final long serialVersionUID = 1L;

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


	/**
	 * 主键
	 */
	private Integer id;
	/**
	 * 告警规则
	 */
	private String alertName;
	/**
	 * 数据中心
	 * */
	private String datacenter;
	/**
	 *
	 */
	private String project;
	/**
	 *
	 */
	private String module;
	/**
	 *
	 */
	private String endpoint;
	/**
	 *
	 */
	private String asset;
	/**
	 *
	 */
	private String labels;
	/**
	 *
	 */
	private String severity;
	/**
	 *
	 */
	private String summary;
	/**
	 * 告警描述
	 */
	private String description;
	/**
	 * 开始时间
	 */
	private String startAt;
	/**
	 * 结束时间
	 */
	private String endAt;
	/**
	 * 状态
	 */
	private Integer state;



}
