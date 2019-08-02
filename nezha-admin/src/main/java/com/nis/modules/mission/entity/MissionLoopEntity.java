package com.nis.modules.mission.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

@Data
public class MissionLoopEntity implements Serializable{

	private static final long serialVersionUID = 1L;
	private Date startTime;
	private Date endTime;
}
