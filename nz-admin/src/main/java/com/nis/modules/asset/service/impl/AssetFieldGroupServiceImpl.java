package com.nis.modules.asset.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.asset.dao.AssetFieldGroupDao;
import com.nis.modules.asset.dao.AssetFieldMetaDao;
import com.nis.modules.asset.dao.AssetFieldValueDao;
import com.nis.modules.asset.entity.AssetFieldGroup;
import com.nis.modules.asset.entity.AssetFieldMeta;
import com.nis.modules.asset.entity.AssetFieldValue;
import com.nis.modules.asset.service.AssetFieldGroupService;

import cn.hutool.core.util.ObjectUtil;

@Service("assetFieldGroupService")
public class AssetFieldGroupServiceImpl extends ServiceImpl<AssetFieldGroupDao,AssetFieldGroup> implements AssetFieldGroupService{

	@Autowired
	private AssetFieldMetaDao assetFieldMetaDao;
	
	@Autowired
	private AssetFieldValueDao assetFieldValueDao;
	
	@Override
	public PageUtils queryList(Map<String, Object> params) {
		String name = (String) params.get("name");
		IPage page = new Query(AssetFieldGroup.class).getPage(params);
		IPage<AssetFieldGroup> result = this.page(page, new QueryWrapper<AssetFieldGroup>().lambda().like(ObjectUtil.isNotEmpty(name), AssetFieldGroup::getName, name));
		return new PageUtils(result);
	}

	@Override
	public Integer saveOrModifyAssetFieldGroup(AssetFieldGroup assetFieldGroup) {
		// 校验name不重复
		String name = assetFieldGroup.getName();
		List<AssetFieldGroup> list = this.list(new QueryWrapper<AssetFieldGroup>().lambda().eq(AssetFieldGroup::getName, name));
		if(ObjectUtil.isNotEmpty(list)) {
			throw new NZException(RCode.ASSET_FIELD_GROUP_NAME_DUPLICATE);
		}
		
		this.saveOrUpdate(assetFieldGroup);
		
		return assetFieldGroup.getId();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeAssetFieldGroups(List<String> assetFieldGroupIds) {
		List<AssetFieldMeta> metas = assetFieldMetaDao.selectList(new QueryWrapper<AssetFieldMeta>().lambda().in(AssetFieldMeta::getGroupId, assetFieldGroupIds));
		// 先删除关联关系 最后删除本身数据信息
		if(ObjectUtils.isNotEmpty(metas)) {
			List<Integer> metaIds = metas.stream().map(AssetFieldMeta::getId).collect(Collectors.toList());
			assetFieldValueDao.delete(new QueryWrapper<AssetFieldValue>().lambda().in(AssetFieldValue::getMetaId, metaIds));
			assetFieldMetaDao.deleteBatchIds(metaIds);
		}
		this.removeByIds(assetFieldGroupIds);
	}

}
