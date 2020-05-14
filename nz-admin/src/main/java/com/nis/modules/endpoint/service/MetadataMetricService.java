package com.nis.modules.endpoint.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.endpoint.entity.MetadataMetric;

public interface MetadataMetricService extends IService<MetadataMetric> {

    List<MetadataMetric> metadata(Integer endpointId) throws Exception;

}
