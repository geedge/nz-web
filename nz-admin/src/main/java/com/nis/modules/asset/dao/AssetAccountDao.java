package com.nis.modules.asset.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.asset.entity.AssetAccount;

@Mapper
@Repository
public interface AssetAccountDao extends BaseMapper<AssetAccount>{

	List<AssetAccount> selectAssetAccounts(IPage<AssetAccount> page,@Param("params")Map<String,Object> params);

	AssetAccount selectAssetAccountById(Integer id);
	
	List<AssetAccount> selectAssetAccountsByAssetId(IPage<AssetAccount> page,@Param("assetId")Integer assetId);
}
