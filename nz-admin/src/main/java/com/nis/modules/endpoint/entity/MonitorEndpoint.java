package com.nis.modules.endpoint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.project.entity.MonitorProject;
import lombok.Data;

import java.io.Serializable;

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


	/**
	 * exporter地址
	 */
	private String host;

	/**
	 * export端口
	 */
	private Integer port;

	/**
	 * 配置参数 当type=HTTP,默认为 module.configs
	 */
	private String configs;

	/**
	 * 计算 moduleId + assetId + host + port + configs 的hash值，用于确保相同配置不重复
	 */
	private String hash;

	/**
	 * 是否启用 可选值
	 * 0：停用
	 * 1：启用
	 */
	private Integer enabled;

	private String seq;

	@TableField(exist = false)
	private Integer state;

	@TableField(exist = false)
	private MonitorModule module;

	@TableField(exist = false)
	private AssetAsset asset;

	@TableField(exist = false)
	private MonitorProject project;

	@TableField(exist = false)
	private StateInfo stateInfo;

	@TableField(exist = false)
	private EndpointState endpointState;

	@TableField(exist = false)
	private Integer alertNum;
}
