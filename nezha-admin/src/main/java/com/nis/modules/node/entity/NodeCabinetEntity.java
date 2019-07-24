package com.nis.modules.node.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import lombok.Data;

/**
 * 机柜信息表
 * @author shizhendong
 * @date 2019-07-18 09:58:18
 */
@Data
@TableName("node_cabinet")
public class NodeCabinetEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 机柜ID
	 */
	@TableId
	private Long id;
	/**
	 * 机柜名称
	 */
	private String name;
	/**
	 * 机房id
	 */
	private Long roomId;
	/**
	 * 行号
	 */
	private Integer roomRow;
	/**
	 * 列号
	 */
	private Integer roomCol;
	/**
	 * u位数量
	 */
	private Integer ubit;
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

	//机房名称
	@TableField(exist = false)
	private String roomName;

	//机柜关联的节点信息
	@TableField(exist = false)
	private List<NodeInfoEntity> nodes;
}
