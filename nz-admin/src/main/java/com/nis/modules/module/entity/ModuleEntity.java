package com.nis.modules.module.entity;

import javax.validation.constraints.NotBlank;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.asset.entity.AssetEntity;
import com.nis.modules.system.entity.SystemEntity;

import lombok.Data;

@Data
@TableName("module")
public class ModuleEntity {
	@TableId
	private Integer id;
	private SystemEntity systemEntity;
	@NotBlank(message="参数名不能为空")
	private Integer SystemId;
	@NotBlank(message="参数名不能为空")
	private Integer assetId;
	@NotBlank(message="参数名不能为空")
	private String host;
	@NotBlank(message="参数名不能为空")
	private Integer port;
	private String param;
	@NotBlank(message="参数名不能为空")
	private String path;
	
}
