package com.nis.modules.metric.dto;

import lombok.Data;

@Data
public class MetricsDto {
	private double total;
	private String severity;
	private Integer type;
	private String name;
}
