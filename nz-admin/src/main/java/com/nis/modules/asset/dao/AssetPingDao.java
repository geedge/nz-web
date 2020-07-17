package com.nis.modules.asset.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.asset.entity.AssetPing;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * AssetPing
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Mapper
@Repository
public interface AssetPingDao extends BaseMapper<AssetPing> {

    void deleteAll();

    List<Map> getAssetStatGroupByStatus();
}
