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
	
	private Integer dgnic;   // 去重之后的nodeGroupId总数  distinct group node id count
	private Integer ngnnc;   // 节点组下空节点的节点组个数    node group node null count 
	private Integer nnc;     // 节点为网元的个数  node net count
	private Integer total;   // 总数
	private List<Long>  nodeGroupIds; // 节点组id
	private List<Long>  nodeIds;      // 节点id
}
