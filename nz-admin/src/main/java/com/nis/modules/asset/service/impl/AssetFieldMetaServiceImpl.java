package com.nis.modules.asset.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.asset.dao.AssetFieldMetaDao;
import com.nis.modules.asset.dao.AssetFieldValueDao;
import com.nis.modules.asset.entity.AssetFieldMeta;
import com.nis.modules.asset.entity.AssetFieldValue;
import com.nis.modules.asset.service.AssetFieldMetaService;
import com.nis.modules.sys.entity.SysUserStarredEntity;
import com.nis.modules.sys.service.SysUserStarredService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("assetFieldMetaService")
public class AssetFieldMetaServiceImpl extends ServiceImpl<AssetFieldMetaDao,AssetFieldMeta> implements AssetFieldMetaService{
	@Autowired
	private AssetFieldMetaDao assetFieldMetaDao;
	
	@Autowired
	private AssetFieldValueDao assetFieldValueDao;

	@Autowired
	private SysUserStarredService starredService;
	
	@Override
	public AssetFieldMeta queryAssetFieldMeta(Integer id) {

		//当前用户id
		Long uid = ShiroUtils.getUserId();

		AssetFieldMeta assetFieldMeta = assetFieldMetaDao.selectAssetFieldMeta(id,uid);
		
		return assetFieldMeta;
	}

	@Override
	public PageUtils queryList(Map<String, Object> params) {
		IPage page = new Query(AssetFieldMeta.class).getPage(params);
		String groupIds = (String) params.get("groupIds");
        if(StrUtil.isNotBlank(groupIds)) {
        	List<String> listIds = Arrays.asList(groupIds.split(","));
        	params.put("groupIds",listIds);
        }
        if (ToolUtil.isNotEmpty(params.get("ids"))) {
            params.put("ids", Arrays.asList(String.valueOf(params.get("ids")).split(StringPool.COMMA)));
        }

        //当前用户id
		params.put("uid",ShiroUtils.getUserId());
		List<AssetFieldMeta> assetFieldMetas =assetFieldMetaDao.selectAssetFieldMetaList(page,params);
		page.setRecords(assetFieldMetas);
		return new PageUtils(page);
	}

	@Override
	public Integer saveOrModify(AssetFieldMeta assetFieldMeta) {
		
		// 验证key值和type值
		String type = assetFieldMeta.getType();
		if(!Constant.ASSET_FIELD_META_VALUE.contains(type.toLowerCase())) {
			throw new NZException(RCode.ASSET_FIELD_META_TYPE_ERROR);
		}
		String key = assetFieldMeta.getMetaKey();
		if(!key.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
			throw new NZException(RCode.ASSET_FIELD_META_KEY_ERROR);
		}

		assetFieldMeta.setName(StrUtil.trim(assetFieldMeta.getName()));
		List<AssetFieldMeta> list = this.list(new QueryWrapper<AssetFieldMeta>().lambda()
				.ne(ObjectUtil.isNotEmpty(assetFieldMeta.getId()),AssetFieldMeta::getId, assetFieldMeta.getId())
				.and(wrapper -> wrapper.eq(AssetFieldMeta::getMetaKey, key).or().eq(AssetFieldMeta::getName,assetFieldMeta.getName())));
		
		if(ObjectUtil.isNotEmpty(list)) {
			throw new NZException(RCode.ASSET_FIELD_META_KEY_NAME_DUPLICATE);
		}
		if (ObjectUtil.isNotEmpty(assetFieldMeta.getId())){
			AssetFieldValue assetFieldValue = assetFieldValueDao.selectOne(new LambdaQueryWrapper<AssetFieldValue>()
					.eq(AssetFieldValue::getMetaId, assetFieldMeta.getId())
					.last("limit 1"));
			AssetFieldMeta oldAssetFieldMeta = this.getById(assetFieldMeta.getId());
			// 被引用的 meta 不能变更 type
			if (ObjectUtil.isNotEmpty(assetFieldValue) && !oldAssetFieldMeta.getType().equalsIgnoreCase(type)){
				throw new NZException(RCode.ASSET_FIELD_META_REFERENCED);
			}
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
		List<Integer> metaIds = metaList.stream().map(meta -> meta.getId()).collect(Collectors.toList());
		List<AssetFieldValue> assetFieldValues = assetFieldValueDao.selectList(new LambdaQueryWrapper<AssetFieldValue>()
				.in(CollUtil.isNotEmpty(metaIds), AssetFieldValue::getMetaId, metaIds)
				.eq(ObjectUtil.isNotEmpty(assetId), AssetFieldValue::getAssetId,assetId));

		Map<Integer, List<AssetFieldValue>> assetFieldValueByMetaId = assetFieldValues.stream().collect(Collectors.groupingBy(AssetFieldValue::getMetaId));

		for (AssetFieldMeta meta : metaList) {
			List<String> values = assetFieldValueByMetaId.get(meta.getId()).stream()
					.filter(assetFieldValue -> assetFieldValue.getAssetId().equals(meta.getAssetId()))
					.map(assetFieldValue -> assetFieldValue.getValue())
					.collect(Collectors.toList());
			meta.setValue(values);
			meta.setKey(meta.getMetaKey());
		}
		return metaList;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeAssetFieldMetas(List<String> assetFieldMetaIds, Integer force) {
		//判断是否被asset 引用
		List<AssetFieldValue> assetFieldValues = assetFieldValueDao.selectList(new LambdaQueryWrapper<AssetFieldValue>().in(AssetFieldValue::getMetaId, assetFieldMetaIds));

		switch (force){
			case 0:
				//存在关联数据时，删除失败
				if (ObjectUtil.isNotEmpty(assetFieldValues)){
					throw new NZException(RCode.ASSET_FIELD_META_REFERENCED);
				}
				assetFieldMetaDao.deleteBatchIds(assetFieldMetaIds);
				break;
			case 1:
				// 先删除关联关系 最后删除本身数据信息
				assetFieldValueDao.delete(new QueryWrapper<AssetFieldValue>().lambda().in(AssetFieldValue::getMetaId, assetFieldMetaIds));
				assetFieldMetaDao.deleteBatchIds(assetFieldMetaIds);
				break;
		}
		//删除starred
		starredService.remove(new LambdaQueryWrapper<SysUserStarredEntity>()
				.eq(SysUserStarredEntity::getType,"assetFieldMeta")
				.in(SysUserStarredEntity::getTid,assetFieldMetaIds)
		);
	}

}
