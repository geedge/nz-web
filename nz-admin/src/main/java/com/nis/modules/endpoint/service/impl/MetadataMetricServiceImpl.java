package com.nis.modules.endpoint.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.endpoint.dao.MetadataMetricDao;
import com.nis.modules.endpoint.entity.MetadataMetric;
import com.nis.modules.endpoint.service.MetadataMetricService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MetadataMetricServiceImpl extends ServiceImpl<MetadataMetricDao, MetadataMetric> implements MetadataMetricService {
    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        this.page(new Query<MetadataMetric>().getPage(params));

        return null;
    }
}
