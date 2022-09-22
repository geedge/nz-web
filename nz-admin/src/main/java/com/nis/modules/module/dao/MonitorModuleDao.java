package com.nis.modules.module.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.metric.dto.MetricsDto;
import com.nis.modules.module.entity.MonitorModule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface MonitorModuleDao extends BaseMapper<MonitorModule> {

    List<MonitorModule> queryPage(IPage page, @Param("params") Map<String, Object> params);

    // nz_endpoint_nums 指标 / 按模块统计endpoint数量
    List<MetricsDto> nzEndpointNumsMetric();

    List<AssetAsset> queryAssetListByModuleId(@Param("moduleId") Integer moduleId);

    List<Dc> queryDcListByModuleId(@Param("moduleId") Integer moduleId);

    String queryAssetMoudules(@Param("assetId") Integer assetId);

}
