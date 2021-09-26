package com.nis.modules.asset.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.panel.entity.VisualChart;

/**
 * 资产    dao
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Mapper
@Repository
public interface AssetAssetDao extends BaseMapper<AssetAsset> {

    List<AssetAsset> queryAssetList(IPage page, @Param("params") Map<String, Object> params);
    /**
     * 根据column 统计asset 数量
     * @param column
     * @param params
     * @return
     */
    List<Map<String,Object>> queryAssetGroupBy(@Param("column") String column,@Param("params") Map<String, Object> params);

    void updateAssetStateBatch(@Param("ids") List<Integer> ids,@Param("params")Map<String,Object> params);

    List<VisualChart> getAssetsByChartIds(List<Integer> chartIds);

    void updateBrandIdByModelId(@Param("modelId") Integer modelId, @Param("brandId") Integer brandId);
}
