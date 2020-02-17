package com.nis.modules.endpoint.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.module.entity.Module;
import com.nis.modules.project.entity.Project;

import lombok.Data;

@Data
@TableName("endpoint")
public class Endpoint {
	@TableId
	private Integer id;
	private Integer moduleId;
	private Integer assetId;
	private String host;
	private Integer port;
	private String param;
	private String path;
	@TableField(exist = false)
	private Module module;
	@TableField(exist = false)
	private Asset asset;
	@TableField(exist = false)
	private Project project;
	@TableField(exist = false)
	private Long lastUpdate;
	@TableField(exist = false)
	private Integer state;
	private String seq;
	public Endpoint() {
	}

	public Endpoint(Integer moduleId, Integer assetId, String host, Integer port, String param, String path) {
		this.moduleId = moduleId;
		this.assetId = assetId;
		this.host = host;
		this.port = port;
		this.param = param;
		this.path = path;
	}
}
