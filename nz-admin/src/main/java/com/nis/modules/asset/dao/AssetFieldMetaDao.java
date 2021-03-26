package com.nis.modules.asset.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.asset.entity.AssetFieldMeta;

@Mapper
@Repository
public interface AssetFieldMetaDao extends BaseMapper<AssetFieldMeta>{
	
	AssetFieldMeta selectAssetFieldMeta(Integer id);
	
	List<AssetFieldMeta> selectAssetFieldMetaList(@Param("page")IPage<AssetFieldMeta> page,@Param("params")Map<String,Object> params);

	// 包含 group \ value \ meta 信息
	List<AssetFieldMeta> selectMateInfoListByAssetId(@Param("assetId") Integer assetId);
}
