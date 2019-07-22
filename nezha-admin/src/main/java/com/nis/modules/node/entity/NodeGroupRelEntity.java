package com.nis.modules.node.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

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
public class NodeGroupRelEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键ID
	 */
	@TableId
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
	private String desc;
	/**
	 * 操作人
	 */
	private Long operator;
	/**
	 * 是否删除，1：删除，0：未删除，默认：0
	 */
	private String delFlag;
	/**
	 * 操作时间
	 */
	private Date opTime;
}
