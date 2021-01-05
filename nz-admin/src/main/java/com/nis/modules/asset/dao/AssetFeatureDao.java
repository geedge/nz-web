package com.nis.modules.asset.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.asset.entity.AssetFeature;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 
 * @author Th
 *
 */
@Mapper
@Repository
public interface AssetFeatureDao extends BaseMapper<AssetFeature>{

}
