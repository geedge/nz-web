package com.nis.modules.project.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 项目拓扑图配置信息
 *
 * @author shizhendong
 * @date 2020-08-24
 */
@Data
@TableName("project_topo")
public class Topo {
	@TableId
	private Integer id;

	private Integer projectId;

	private Object topo;

	private Date updateAt;

	private Integer updateBy;
}