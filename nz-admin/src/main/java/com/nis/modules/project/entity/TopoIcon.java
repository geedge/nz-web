package com.nis.modules.project.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 拓扑图图标
 *
 * @author shizhendong
 * @date 2020-08-24
 */
@Data
@TableName("project_topo_icon")
public class TopoIcon {
	@TableId
	private Integer id;

	private String name;

	private String bytes;

	private String type;
	
	private String unit;

	private Integer width;

	private Integer height;
}
