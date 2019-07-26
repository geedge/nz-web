package com.nis.modules.topo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 拓扑图
 *
 */
@Data
@TableName("topo_info")
@KeySequence(value="seq_topo_info")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopoInfoEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/**
	 **拓扑图ID
	 */
	@TableId(type=IdType.INPUT)
	private Long id;

	/**
	 * 拓扑图名称
	 */
	private String name;

	/**
	 * 业务系统ID
	 */
	private Long systemId;

	/**
	 * 图片宽度
	 */
	private Integer width;
	
	/**
	 * 图片高度
	 */
	private Integer height;
	
	/**
	 * 样式
	 */
	private String style;

	/**
	 * 创建时间
	 */
	private Date opTime;

	/**
	 * 创建人
	 */
	private Long operator;
	
	/**
	 * 备注
	 */
	private String remarks;
	
	/**
	 * 删除标识，1：删除，0：未删除
	 */
	private String delFlag;
	
	/**
	 * 拓扑图对于的点
	 */
    @TableField(exist = false)
    private List<TopoNodeInfoEntity> topoNodes;
    
    /**
     * 拓扑图对于的线
     */
    @TableField(exist = false)
    private List<TopoLineInfoEntity> topoLines;
	
}
