/**

 *

 *

 */

package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**
 * 系统日志
 *

 */
@Data
@TableName("sys_log")
public class SysLogEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	@TableId
	private Long id;
	/**
	 * 用户名
	 */
	private Integer userId;

	@TableField(exist = false)
	private String username;
	/**
	 * 用户操作
	 */
	private String operation;
	/**
	 * 请求方法
	 */
	private String method;
	/**
	 * 请求参数
	 */
	private String params;
	/**
	 * 执行时长(毫秒)
	 */
	private Long time;
	/**
	 * IP地址
	 */
	private String ip;
	/**
	 *类型
	 */
	private String type;
	/**
	 *
	 */
	private String operaId;
	/**
	 *
	 */
	private String state;
	private String response;
	/**
	 *
	 */
	private String exception;

	/**
	 * 创建时间
	 */
	private Date createDate;

}
