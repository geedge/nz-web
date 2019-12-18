package com.nis.modules.endpoint.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.endpoint.entity.MetadataLabel;

import java.util.List;
import java.util.Map;

public interface MetadataLabelService extends IService<MetadataLabel> {
    List<Integer> queryMetricIds(String name,String value);

    List<Map<String, String>> queryLabels(String metric, String name);
}
