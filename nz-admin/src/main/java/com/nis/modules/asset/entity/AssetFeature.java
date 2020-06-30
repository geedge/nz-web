package com.nis.modules.asset.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;


import lombok.Data;

@Data
@TableName("asset_feature")
public class AssetFeature implements Serializable{
	
	private static final long serialVersionUID = 1L;
	/**
	 * 主键
	 */
	private Integer id;
	/**
	 * 资产id
	 */
	private Integer assetId;
	/**
	 * 键
	 */
	private String item;
	/**
	 * 值
	 */
	private String value;
	/**
	 * 采集方式
	 */
	private String source;
	/**
	 * 更新时间
	 */
	private Date updateAt;
}
