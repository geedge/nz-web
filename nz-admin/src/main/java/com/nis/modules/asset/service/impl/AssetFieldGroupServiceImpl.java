package com.nis.modules.asset.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.asset.dao.AssetFieldGroupDao;
import com.nis.modules.asset.entity.AssetFieldGroup;
import com.nis.modules.asset.entity.AssetFieldMeta;
import com.nis.modules.asset.service.AssetFieldGroupService;

import cn.hutool.core.util.ObjectUtil;

@Service("assetFieldGroupService")
public class AssetFieldGroupServiceImpl extends ServiceImpl<AssetFieldGroupDao,AssetFieldGroup> implements AssetFieldGroupService{

	@Override
	public PageUtils queryList(Map<String, Object> params) {
		String name = (String) params.get("name");
		IPage<AssetFieldGroup> page = new Query<AssetFieldGroup>().getPage(params);
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

}
