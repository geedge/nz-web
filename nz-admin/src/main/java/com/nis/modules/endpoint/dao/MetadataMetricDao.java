package com.nis.modules.endpoint.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.endpoint.entity.MetadataMetric;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MetadataMetricDao extends BaseMapper<MetadataMetric> {
    List<Integer> queryIdsByMetric(@Param("metric")String metric);

    Integer queryIdByMetric(@Param("metric")String metric);

    List<MetadataMetric> getLabelsWithMetric();

    List<MetadataMetric> queryMetricList(@Param("moduleId")Integer moduleId, @Param("metric") String metric);
}
