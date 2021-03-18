package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("asset_field_group")
public class AssetFieldGroup {
	
	private Integer id;
	
	private String name;
	
	private String remark;
	
	private Integer buildIn;
	
	private String seq;
	
}
