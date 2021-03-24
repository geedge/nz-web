package com.nis.modules.asset.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("asset_account")
public class AssetAccount {
	
	
	private Integer id;
	
	private String name;
	
	private Integer protocol;
	
	private Integer authType;
	
	private String username;
	
	private String pin;
	
	private String priKey;
	
	private Long updateBy;
	
	private Date updateAt;
	
	private String userTip;
	
	private String pinTip;
	
	private Integer weight;
	
	private Integer state;
	
	private String seq;
	
	@TableField(exist = false)
	private String updateById;
	
	@TableField(exist = false)
	private String updateByName;
}
