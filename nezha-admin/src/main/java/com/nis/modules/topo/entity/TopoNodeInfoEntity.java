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
 * 拓扑节点
 *
 */
@Data
@TableName("topo_node_info")
@KeySequence(value="seq_topo_node_info")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopoNodeInfoEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/**
	 **拓扑节点ID
	 */
	@TableId(type=IdType.INPUT)
	private Long id;
	
	/**
	 * 图片，字典表code值，code,sys_dict 表关联，字典type=topo-img
	 */
	private String img;
	
	/**
	 **TOPO图ID，TOPO图ID 与TOPO_INFO关联
	 */
	private Long topoId;
	
	/**
	 **NODE类型 1：节点组，2：节点，3：其它 4:topo
	 */
	private Integer type;
	
	/**
	 **NODE对应的节点组或节点ID 当：TYPE = 1时，为节点组ID，TYPE = 2时，为节点ID，TYPE =3 时，无意义 type=4时 topo
	 */
	private Integer typeId;
	
	/**
	 **x轴
	 */
	private Integer x;
	
	/**
	 **y轴
	 */
	private Integer y;
	
	/**
	 **图片宽度
	 */
	private Integer width;
	
	/**
	 **图片高度
	 */
	private Integer height;
	
	/**
	 **NODE提示文本信息
	 */
	private String text;
	
	/**
	 * 样式
	 */
	private String style;
	
	/**
	 * 阀值
	 */
	private Double threshold;
	
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
