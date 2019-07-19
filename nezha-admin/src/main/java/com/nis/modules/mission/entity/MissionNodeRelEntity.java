package com.nis.modules.mission.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 任务节点关系表
 * 
 * @author hkx
 * @email ${email}
 * @date 2019-07-18 10:37:19
 */
@Data
@TableName("mission_node_rel")
public class MissionNodeRelEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId
	private Long id;
	/**
	 * 任务id
	 */
	private Long missionId;
	/**
	 * 关联类型，1：节点组，2：节点
	 */
	private String type;
	/**
	 * 关联id
	 */
	private Long relId;
	/**
	 * 是否删除，1：删除，0：未删除，默认：0
	 */
	private String delFlag;
	/**
	 * 操作时间
	 */
	private Date opTime;

}
