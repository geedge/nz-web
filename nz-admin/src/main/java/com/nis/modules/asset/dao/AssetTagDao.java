package com.nis.modules.asset.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.asset.entity.AssetTag;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 资产标签    dao
 *
 * @author shizhendong
 * @date 2020-06-11
 */
@Mapper
@Repository
public interface AssetTagDao extends BaseMapper<AssetTag> {


}
