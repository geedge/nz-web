package com.nis.modules.dashboard.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;
@Data
public class ChartSeriesEntity implements Serializable{
	private static final long serialVersionUID = 1L;
	private Double value;
	private Date time;
	private String formatTime;
}
