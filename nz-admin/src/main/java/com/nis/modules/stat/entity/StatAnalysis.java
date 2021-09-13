package com.nis.modules.stat.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatAnalysis {
	
	private Integer id;
	
	private String type;
	
	private Integer num;
	
	private String name;
	
	private Integer total;
	
	private Integer online;
	
	private Integer ok;
	
	private Integer alarm;
	
	private Integer state;
	
	private Integer alert;
	
	private Integer endpoint;
	
	private Integer asset;

	private Integer pid;
}
