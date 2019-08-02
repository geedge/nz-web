package com.nis.modules.mission.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import lombok.Data;
/**
 * 此类用于做任务数据组装使用
 * @author Th
 *
 */
@Data
public class MissionDto implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private String commandEnname;
	private Integer commandType;
	/**
	 * 升级时间
	 */
	private Date startTime;
	private List<ParamForExecuteInstruct> paramForExecuteInstructs;  // 用于命令执行力的 备份
	private ParamForExecuteInstruct paramForExecuteInstruct;   // 用于命令执行里的 停止进程
	private MissionLoopEntity missionLoopEntity;
	private ParamForUpgrade paramForUpgrade;
	private List<ParamForUpgrade> paramForUpgrades;
}
