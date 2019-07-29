package com.nis.modules.dashboard.entity;

import java.io.Serializable;

import lombok.Data;
@Data
public class ChartEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	private ChartSingleEntity chartSingleEntity;
	private ChartTableEntity chartTableEntity;
	private ChartStatisticalEntity chartStatisticalEntity;
}
