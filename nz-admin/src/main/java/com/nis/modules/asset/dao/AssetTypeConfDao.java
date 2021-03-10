package com.nis.modules.asset.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.asset.entity.AssetTypeConf;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * AssetTypeConfig
 *
 * @author shizhendong
 * @date 2021-03-08
 */
@Mapper
@Repository
public interface AssetTypeConfDao extends BaseMapper<AssetTypeConf> {

}
