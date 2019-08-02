package com.nis.modules.mission.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 任务结果表
 * 
 * @author th
 * @email ${email}
 * @date 2019-07-18 10:37:19
 */
@Data
@TableName("mission_result")
public class MissionResultEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId
	private Long id;
	/**
	 * 
	 */
	private Long missionId;
	/**
	 * 
	 */
	private Long uuid;
	/**
	 * 任务结果描述信息
	 */
	private String descr;
	/**
	 * 任务执行状态
	 */
	private String state;
	/**
	 * 第几周期
	 */
	private Integer cycle;
	/**
	 * 回传文件路径
	 */
	private String file;
	/**
	 * 操作时间
	 */
	private Date opTime;

}
