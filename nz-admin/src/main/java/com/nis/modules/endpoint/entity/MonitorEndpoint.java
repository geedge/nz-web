package com.nis.modules.endpoint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.project.entity.MonitorProject;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@TableName("monitor_endpoint")
public class MonitorEndpoint implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@TableId(type = IdType.AUTO)
	private Integer id;

	/**
	 * 名称
	 */
	private String name;

	/**
	 * 组件id
	 */
	private Integer moduleId;

	/**
	 * 资产id
	 */
	private Integer assetId;

	private String seq;

	@TableField(exist = false)
	private Object configs;

	@TableField(exist = false)
	private MonitorModule module;

	@TableField(exist = false)
	private AssetAsset asset;

	@TableField(exist = false)
	private Dc dc;

	@TableField(exist = false)
	private MonitorProject project;

	@TableField(exist = false)
	private Integer alertNum;

	@TableField(exist = false)
	private Agent agent;

	@TableField(exist = false)
	private Map config;

	@TableField(exist = false)
	private MonitorEndpointConfig endpointConfig;

	@TableField(exist = false)
	private List<MonitorEndpointConfig> endpointConfigList;

	@TableField(exist = false)
	private List alert;

	/**
	 * 是否收藏 0:未收藏 1:已收藏
	 */
	@TableField(exist = false)
	private Integer starred;

	public void setName(String name) {
		this.name = Tool.StrUtil.trim(name);
	}
}
