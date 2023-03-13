package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.AssetFieldGroup;

import java.util.List;
import java.util.Map;

public interface AssetFieldGroupService extends IService<AssetFieldGroup>{
	
	PageUtils queryList(Map<String,Object> params);
	
	Integer saveOrModifyAssetFieldGroup(AssetFieldGroup assetFieldGroup);
	
	void removeAssetFieldGroups(List<String> assetFieldGroupIds);
}
