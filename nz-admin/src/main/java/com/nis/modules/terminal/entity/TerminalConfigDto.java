package com.nis.modules.terminal.entity;

import lombok.Data;

@Data
public class TerminalConfigDto {
	private Integer height;
	private Integer width;
	private Integer cols;
	private Integer rows;
}
