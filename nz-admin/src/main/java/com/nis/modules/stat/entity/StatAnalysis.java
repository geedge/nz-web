package com.nis.modules.stat.entity;

import lombok.Data;

@Data
public class StatAnalysis {
	
	private Integer id;
	
	private String type;
	
	private Integer num;
	
	private String name;
	
	private Integer total;
	
	private Integer online;
}
