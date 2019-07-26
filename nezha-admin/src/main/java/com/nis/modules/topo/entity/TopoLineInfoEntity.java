package com.nis.modules.topo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 拓扑线
 *
 */
@Data
@TableName("topo_line_info")
@KeySequence(value="seq_topo_line_info")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopoLineInfoEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/**
	 **拓扑线ID
	 */
	@TableId(type=IdType.INPUT)
	private Long id;
	
	/**
	 **线起点 TOPO_NODE_INFO 表的ID
	 */
	private Long src;
	
	/**
	 **线终点 TOPO_NODE_INFO 表的ID
	 */
	private Long dest;
	
	/**
	 * 样式
	 */
	private String style;
	
	/**
	 * 线条文本内容
	 */
	private String text;
	
	/**
	 **TOPO图ID，TOPO图ID 与TOPO_INFO关联
	 */
	private Long topoId;
	
	/**
	 * 创建人
	 */
	private Long operator;
	
	/**
	 * 创建时间
	 */
	private Date opTime;
	
	/**
	 * 删除标识，1：删除，0：未删除
	 */
	private String delFlag;
	
}
