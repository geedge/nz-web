package com.nis.modules.endpoint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetStateConf;
import com.nis.modules.asset.entity.Dc;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("monitor_endpoint_config")
public class MonitorEndpointConfig implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@TableId(type = IdType.AUTO)
	private Integer id;

	/**
	 * endpointId
	 */
	private Integer endpointId;

	/**
	 * 监控类型	 可选值：  metrics，logs，trace（预留）
	 */
	private String type;

	/**
	 * 配置信息  默认：""
	 */
	private String config;

	/**
	 * 配置版本 默认：1 update +1
	 */
	private Integer version;

	/**
	 * 默认：1
	 * 1：开启
	 * 0：关闭
	 * -1: 未配置
	 */
	private Integer enable;

	/**
	 * 	计算 endpointId + type + config 的 hash 值，用于确保相同配置不重复
	 */
	private String hash;

	private String seq;

	/**
	 * 版本
	 */
	@TableField(exist = false)
	private Integer ver;

	/**
	 * 状态
	 */
	@TableField(exist = false)
	private Integer state;

	/**
	 * 状态最后更新时间
	 */
	@TableField(exist = false)
	private Date lastUpdate;

	@TableField(exist = false)
	private AssetAsset asset;

	@TableField(exist = false)
	private Dc dc;

	@TableField(exist = false)
	private AssetStateConf stateConf;

}
