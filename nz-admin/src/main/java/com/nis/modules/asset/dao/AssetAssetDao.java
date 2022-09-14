package com.nis.modules.asset.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.panel.entity.VisualChart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

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

    /**
     * 根据 之前的 modelId & typeId update model_id
     *
     * @param id
     * @param oldModelId
     * @param typeId
     */
    @Update("UPDATE asset_asset SET model_id = #{newModelId} WHERE model_id = #{oldModelId} AND type_id = #{typeId};")
    void updateModelId(@Param("newModelId") Integer id, @Param("oldModelId") Integer oldModelId, @Param("typeId") Integer typeId);

    /**
     * 获取 tsg 设备列表
     */
    List<AssetAsset> getTsgAssetListbyModel();

}
