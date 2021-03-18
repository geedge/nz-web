package com.nis.modules.asset.service;

import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.AssetFieldGroup;

public interface AssetFieldGroupService extends IService<AssetFieldGroup>{
	
	PageUtils queryList(Map<String,Object> params);
	
	Integer saveOrModifyAssetFieldGroup(AssetFieldGroup assetFieldGroup);
}
