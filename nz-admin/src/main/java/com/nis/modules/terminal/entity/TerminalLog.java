package com.nis.modules.terminal.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("terminal_log")
public class TerminalLog implements Serializable {
	@TableId
	private Integer id;
	private String host;
	private Integer port;
	private String protocol;
	private Integer authType;
	private String user;
	private String cmd;
	private Long userId;
	private Date time;
	private Integer status;

	/**
	 * 操作查询开始时间参数
	 */
	@TableField(exist = false)
	private String startTime;
	/**
	 * 操作查询结束时间参数
	 */
	@TableField(exist = false)
	private String endTime;
	@TableField(exist = false)
	private String userName;
}
