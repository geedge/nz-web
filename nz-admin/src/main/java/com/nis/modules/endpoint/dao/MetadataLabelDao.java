package com.nis.modules.endpoint.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.endpoint.entity.MetadataLabel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface MetadataLabelDao extends BaseMapper<MetadataLabel> {

    List<Integer> queryMetricIds(@Param("name")String name, @Param("value")String value);

    List<Map<String, String>> queryLabelNamesByMetricIds(@Param("metricIds")List<Integer> metricIds, @Param("name") String name);

    List<Map<String, String>> queryLabelNamesByMetricId(@Param("metricId")Integer metricId, @Param("name") String name);
}
