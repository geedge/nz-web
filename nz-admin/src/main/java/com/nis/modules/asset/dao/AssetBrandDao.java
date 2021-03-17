package com.nis.modules.asset.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.asset.entity.AssetBrand;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 资产品牌 dao
 *
 * @author shizhendong
 * @date 2021-03-16
 */
@Mapper
@Repository
public interface AssetBrandDao extends BaseMapper<AssetBrand> {

}
