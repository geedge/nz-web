package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("asset_account_rel")
public class AssetAccountRel {
	
	private Integer id;
	
	private Integer assetId;
	
	private Integer accountId;
}
