package com.nis.modules.endpoint.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.endpoint.entity.MetadataMetric;

import java.util.List;
import java.util.Map;

public interface MetadataMetricService extends IService<MetadataMetric> {
    PageUtils queryPage(Map<String, Object> params);

    List<Integer> queryIdsByMetric(String metric);

    Integer queryIdByMetric(String metric);

    List<MetadataMetric> metadata(Integer endpointId);

    Map series(Map<String, Object> params);
}
