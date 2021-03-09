package com.nis.modules.asset.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.asset.entity.AssetStateConf;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * AssetPing
 *
 * @author shizhendong
 * @date 2021-03-08
 */
@Mapper
@Repository
public interface AssetStateConfDao extends BaseMapper<AssetStateConf> {

}
