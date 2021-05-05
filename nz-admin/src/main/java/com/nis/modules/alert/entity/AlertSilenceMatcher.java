package com.nis.modules.alert.entity;

import lombok.Data;

@Data
public class AlertSilenceMatcher {
	
	private String name;
	
	private String value;
	
	private Integer regex;
	
}
