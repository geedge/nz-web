package com.nis.modules.node.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 节点 节点组 关系表
 * 
 * @author shizhendong
 * @date 2019-07-18 09:58:18
 */
@Data
@TableName("node_group_rel")
@KeySequence(value="seq_node_group_rel")
public class NodeGroupRelEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键ID
	 */
	@TableId(type= IdType.INPUT)
	private Long id;
	/**
	 * 节点组id
	 */
	private Long groupId;
	/**
	 * 节点uuid
	 */
	private Long uuid;
	/**
	 * 描述信息
	 */
	private String descr;
	/**
	 * 操作人
	 */
	private Long operator;
	/**
	 * 是否删除，1：删除，0：未删除，默认：0
	 */
	@TableLogic
	private String delFlag;
	/**
	 * 操作时间
	 */
	private Date opTime;

	public NodeGroupRelEntity(Long groupId, Long uuid, Long operator, Date opTime) {
		this.groupId = groupId;
		this.uuid = uuid;
		this.operator = operator;
		this.opTime = opTime;
	}

	public NodeGroupRelEntity() {
	}
}
