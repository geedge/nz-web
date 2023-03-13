package com.nis.modules.asset.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.asset.entity.AssetFieldMeta;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface AssetFieldMetaDao extends BaseMapper<AssetFieldMeta>{
	
	AssetFieldMeta selectAssetFieldMeta(@Param("id") Integer id,@Param("uid")Long uid);
	
	List<AssetFieldMeta> selectAssetFieldMetaList(@Param("page")IPage page,@Param("params")Map<String,Object> params);

	// 包含 group \ value \ meta 信息
	List<AssetFieldMeta> selectMateInfoListByAssetId(@Param("assetId") Integer assetId);
}
