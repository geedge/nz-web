package com.nis.modules.endpoint.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.module.entity.Module;

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
	private Module module;
}
