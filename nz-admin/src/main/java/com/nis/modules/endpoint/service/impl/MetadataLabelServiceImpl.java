package com.nis.modules.endpoint.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.endpoint.dao.MetadataLabelDao;
import com.nis.modules.endpoint.entity.MetadataLabel;
import com.nis.modules.endpoint.service.MetadataLabelService;
import com.nis.modules.endpoint.service.MetadataMetricService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MetadataLabelServiceImpl extends ServiceImpl<MetadataLabelDao, MetadataLabel> implements MetadataLabelService {
    @Autowired
    private MetadataMetricService metricService;
    @Override
    public List<Integer> queryMetricIds(String name, String value) {
        return this.baseMapper.queryMetricIds(name,value);
    }

    /**
     * field: name value all
     * */
    @Override
    public List<Map<String, String>> queryLabels(String metric, String name,String field) {

//        List<Integer> metricIds=metricService.queryIdsByMetric(metric);

        Integer metricId=metricService.queryIdByMetric(metric);

        QueryWrapper<MetadataLabel> labelQueryWrapper = new QueryWrapper<>();

//        labelQueryWrapper.in("metric_id",metricIds);

        labelQueryWrapper.eq("metric_id",metricId);

        labelQueryWrapper.like(StringUtils.isNotBlank(name),"name",name);

//        List<Map<String,String>> list = this.baseMapper.queryLabelNamesByMetricIds(metricIds,name);
        List<Map<String,String>> list = this.baseMapper.queryLabelNamesByMetricId(metricId,name,field);


        return list;
    }
}
