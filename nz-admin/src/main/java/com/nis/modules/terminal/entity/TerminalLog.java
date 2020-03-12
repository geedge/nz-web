package com.nis.modules.terminal.entity;

import java.util.Date;

import lombok.Data;

@Data
public class TerminalLog {
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
}
