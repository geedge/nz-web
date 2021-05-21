package com.nis.modules.asset.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.asset.entity.AssetModel;
import com.nis.modules.metric.dto.MetricsDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 资产型号 dao
 *
 * @author shizhendong
 * @date 2021-03-16
 */
@Mapper
@Repository
public interface AssetModelDao extends BaseMapper<AssetModel> {

    List<AssetModel> queryList(IPage page, @Param("params") Map<String, Object> params);

    // nz_asset_nums / 按型号统计资产数量
    List<MetricsDto> nzAssetNumsMetric();
}
