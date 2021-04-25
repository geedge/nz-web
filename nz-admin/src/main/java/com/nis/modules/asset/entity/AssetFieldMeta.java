package com.nis.modules.asset.entity;

import java.util.List;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("asset_field_meta")
public class AssetFieldMeta {
	
	private Integer id;
	
	private String name;
	
	private String metaKey;
	
	private String groupId;
	
	private Integer search;
	
	private Integer display;
	
	private String type;
	
	private String param;
	
	private Integer buildIn;
	
	private String seq;
	
	private String remark;
	
	@TableField(exist = false)
	private AssetFieldGroup group;

	@TableField(exist = false)
	private String key;

	/**
	 * field value value
	 */
	@TableField(exist = false)
	private List<String> value;

	/**
	 * field value assetId
	 */
	@TableField(exist = false)
	private Integer assetId;
}
