package com.nis.modules.asset.service.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.asset.dao.AssetFieldMetaDao;
import com.nis.modules.asset.dao.AssetFieldValueDao;
import com.nis.modules.asset.entity.AssetFieldMeta;
import com.nis.modules.asset.entity.AssetFieldValue;
import com.nis.modules.asset.service.AssetFieldMetaService;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

@Service("assetFieldMetaService")
public class AssetFieldMetaServiceImpl extends ServiceImpl<AssetFieldMetaDao,AssetFieldMeta> implements AssetFieldMetaService{
	@Autowired
	private AssetFieldMetaDao assetFieldMetaDao;
	
	@Autowired
	private AssetFieldValueDao assetFieldValueDao;
	
	@Override
	public AssetFieldMeta queryAssetFieldMeta(Integer id) {
		
		AssetFieldMeta assetFieldMeta = assetFieldMetaDao.selectAssetFieldMeta(id);
		
		return assetFieldMeta;
	}

	@Override
	public PageUtils queryList(Map<String, Object> params) {
		IPage<AssetFieldMeta> page = new Query<AssetFieldMeta>().getPage(params);
		String groupIds = (String) params.get("groupIds");
        if(StrUtil.isNotBlank(groupIds)) {
        	List<String> listIds = Arrays.asList(groupIds.split(","));
        	params.put("groupIds",listIds);
        }
		List<AssetFieldMeta> assetFieldMetas =assetFieldMetaDao.selectAssetFieldMetaList(page,params);
		page.setRecords(assetFieldMetas);
		return new PageUtils(page);
	}

	@Override
	public Integer saveOrModify(AssetFieldMeta assetFieldMeta) {
		
		// 验证key值和type值
		String type = assetFieldMeta.getType();
		if(!Constant.ASSET_FIELD_META_VALUE.contains(type)) {
			throw new NZException(RCode.ASSET_FIELD_META_TYPE_ERROR);
		}
		String key = assetFieldMeta.getMetaKey();
		if(!key.matches("[\\w]+")) {
			throw new NZException(RCode.ASSET_FIELD_META_KEY_ERROR);
		}
		
		List<AssetFieldMeta> list = this.list(new QueryWrapper<AssetFieldMeta>().lambda().eq(AssetFieldMeta::getMetaKey, key).ne(ObjectUtil.isNotEmpty(assetFieldMeta.getId()),AssetFieldMeta::getId, assetFieldMeta.getId()));
		
		if(ObjectUtil.isNotEmpty(list)) {
			throw new NZException(RCode.ASSET_FIELD_META_KEY_DUPLICATE);
		}
		
		if(ObjectUtil.isEmpty(assetFieldMeta.getParam())){
			assetFieldMeta.setParam(JSON.toJSONString(new HashMap()));
		}
		this.saveOrUpdate(assetFieldMeta);
		
		return assetFieldMeta.getId();
	}

	@Override
	public List<AssetFieldMeta> getAssetFieldMetaListByAssetId(Integer assetId) {
		List<AssetFieldMeta> metaList = assetFieldMetaDao.selectMateInfoListByAssetId(assetId);
		for (AssetFieldMeta meta : metaList) {
			meta.setKey(meta.getMetaKey());
		}
		return metaList;
	}

	@Override
	public void removeAssetFieldMetas(List<String> assetFieldMetaIds) {
		// 先删除关联关系 最后删除本身数据信息
		assetFieldValueDao.delete(new QueryWrapper<AssetFieldValue>().lambda().in(AssetFieldValue::getMetaId, assetFieldMetaIds));
		assetFieldMetaDao.deleteBatchIds(assetFieldMetaIds);
	}

}
