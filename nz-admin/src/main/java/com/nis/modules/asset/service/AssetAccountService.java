package com.nis.modules.asset.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.AssetAccount;

public interface AssetAccountService extends IService<AssetAccount>{
	
	PageUtils queryPage(Map<String, Object> params);
		
	void removeByIds(List<Integer> ids);
	
	AssetAccount queryAssetAccountById(Integer id);
	
	void saveAssetAccountRel(Integer accountId, List<Integer> assetIds);
	
	PageUtils queryPageByAssetId(Integer assetId);
	
	void saveOrUpdateAssetAccount(AssetAccount assetAccount);
}
