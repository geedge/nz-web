package com.nis.modules.mission.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Transient;

import lombok.Data;

/**
 * 任务信息表
 * 
 * @author hkx
 * @email ${email}
 * @date 2019-07-18 10:37:19
 */
@Data
@TableName("mission_info")
public class MissionInfoEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId
	private Long id;
	/**
	 * 
	 */
	private String name;
	/**
	 * 任务类型，1、文件推送，2、命令执行，3、升级部署
	 */
	private String type;
	/**
	 * 任务状态，周期任务有效，0、非周期任务，1、运行，2、暂停，3、终止
	 */
	private String state;
	/**
	 * 1:用户组，2：系统内全部人员，3：无限制，默认2
	 */
	private String viewLevel;
	/**
	 * 是否周期任务，0：不是，1：是
	 */
	private String isLoop;
	/**
	 * 周期间隔，单位分钟
	 */
	private Integer loopInterval;
	/**
	 * 任务参数，json方式保存
	 */
	private String param;
	/**
	 * 备注
	 */
	private String remark;
	/**
	 * 是否删除，1：删除，0：未删除，默认：0
	 */
	private String delFlag;
	/**
	 * 
	 */
	private String operator;
	/**
	 * 操作时间
	 */
	private Date opTime;
	/**
	 * 任务状态描述
	 */
	@TableField(exist=false)
	private String stateDesc;
	
	/**
	 * 查看人    组名或业务系统名称
	 */
	@TableField(exist=false)
	private String viewer;
	
	//关联节点组列表
	@TableField(exist=false)
	private List<MissionNodeRelEntity> mnrList;
	
	//节点组id
	@TableField(exist=false)
	private List<Long> nodeGroupIds;
	
	//节点uuid
	@TableField(exist=false)
	private List<String> nodeIds;

}
