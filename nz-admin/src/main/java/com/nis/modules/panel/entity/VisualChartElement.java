package com.nis.modules.panel.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.io.Serializable;

@Data
@TableName("visual_chart_element")
public class VisualChartElement implements Serializable {

	private static final long serialVersionUID = 1L;

	private Integer id;
	
	private Integer chartId;
	
	private String expression;
	
	private String type;
	
	private String legend;
	
	private Integer buildIn;
	
	private String seq;

	private String name;

	private Integer state;

}
