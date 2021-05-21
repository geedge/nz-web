package com.nis.modules.metric.dto;

import lombok.Data;

@Data
public class MetricsDto {

	private Integer id;

	private String name;

	private String model;

	private String brand;

	private String state;

	private String type;

	private String severity;

	private Integer total;
}
