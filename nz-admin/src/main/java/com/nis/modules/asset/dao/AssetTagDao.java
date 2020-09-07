package com.nis.modules.asset.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.asset.entity.AssetTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 资产标签    dao
 *
 * @author shizhendong
 * @date 2020-06-11
 */
@Mapper
@Repository
public interface AssetTagDao extends BaseMapper<AssetTag> {

    List<AssetTag> getTagsBykeyOrderCounts(@Param("key") String key);

    List<AssetTag> getTagValsByKeyOrderCounts(@Param("key") String key, @Param("value") String value);

    List<Integer> getAssetIdsByTagParams(@Param("params") Map<String, List<String>> tagParams);

    List<Map> getTagStat();
}
