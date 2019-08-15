package com.nis.modules.node.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import lombok.Data;

/**
 * 机房信息表
 * 
 * @author shizhendong
 * @date 2019-07-18 09:58:18
 */
@Data
@TableName("node_room")
@KeySequence(value="seq_node_room")
public class NodeRoomEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@TableId(type= IdType.INPUT)
	private Long id;
	/**
	 * 机房名称
	 */
	private String name;
	/**
	 * 行数
	 */
	private Integer roomRows;
	/**
	 * 列数
	 */
	private Integer roomCols;
	/**
	 * 地区
	 */
	private Long addr;
	/**
	 * 备注
	 */
	private String remarks;
	/**
	 * 操作人
	 */
	private Long operator;
	/**
	 * 操作时间
	 */
	private Date opTime;

	/**
	 * 是否删除，1：删除，0：未删除，默认：0
	 */
	@TableLogic
	private String delFlag;

	// 位置名称
	@TableField(exist = false)
	private String addrName;

	/**
	 * 机房关联的机柜信息
	 */
	@TableField(exist = false)
	private List<NodeCabinetEntity> cabinets;
}
