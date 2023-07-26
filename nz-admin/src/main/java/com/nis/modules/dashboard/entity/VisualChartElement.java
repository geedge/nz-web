package com.nis.modules.dashboard.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class VisualChartElement implements Serializable {

	private static final long serialVersionUID = 1L;

	private Integer id;

	private String name;

	private String expression;

	private String type;

	private String legend;

	private Integer state;

	private Integer orderNum = 0;

}
