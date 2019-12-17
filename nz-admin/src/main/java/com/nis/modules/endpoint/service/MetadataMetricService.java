package com.nis.modules.endpoint.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.endpoint.entity.MetadataMetric;

import java.util.Map;

public interface MetadataMetricService extends IService<MetadataMetric> {
    PageUtils queryPage(Map<String, Object> params);
}
