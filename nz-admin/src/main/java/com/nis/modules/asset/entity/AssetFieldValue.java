package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("asset_field_value")
public class AssetFieldValue {

	private Integer id;
	
	private Integer assetId;
	
	private Integer metaId;
	
	private String value;
	
	private String seq;
	
}
