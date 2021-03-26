package com.nis.modules.asset.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.AssetFieldMeta;

public interface AssetFieldMetaService extends IService<AssetFieldMeta>{
	
	AssetFieldMeta queryAssetFieldMeta(Integer id);

	PageUtils queryList(Map<String,Object> map);
	
	Integer saveOrModify(AssetFieldMeta assetFieldMeta);

	List<AssetFieldMeta> getAssetFieldMetaListByAssetId(Integer assetId);
}
