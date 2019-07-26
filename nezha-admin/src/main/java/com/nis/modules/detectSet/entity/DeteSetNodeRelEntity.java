package com.nis.modules.detectSet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 监测信息节点关系表
 * 
 * @author nezha
 * @email 
 * @date 2019-07-25 16:35:48
 */
@Data
@TableName("dete_set_node_rel")
@KeySequence(value="seq_dete_set_node_rel")
public class DeteSetNodeRelEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId(type=IdType.INPUT)
	private Long id;
	/**
	 * 设置id
	 */
	private Long setId;
	/**
	 * 1、node,2、nodeGroup
	 */
	private Long type;
	/**
	 * 关联id
	 */
	private Long relId;
	/**
	 * 是否删除，1：删除，0：未删除，默认：0
	 */
	private String delFlag;
	/**
	 * 
	 */
	private Long operator;
	/**
	 * 操作时间
	 */
	private Date opTime;

}
