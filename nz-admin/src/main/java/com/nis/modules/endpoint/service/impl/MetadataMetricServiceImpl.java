package com.nis.modules.endpoint.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.endpoint.dao.MetadataMetricDao;
import com.nis.modules.endpoint.entity.MetadataMetric;
import com.nis.modules.endpoint.service.MetadataLabelService;
import com.nis.modules.endpoint.service.MetadataMetricService;
import com.nis.modules.module.entity.Module;
import com.nis.modules.module.service.ModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MetadataMetricServiceImpl extends ServiceImpl<MetadataMetricDao, MetadataMetric> implements MetadataMetricService {
    @Autowired
    private ModuleService moduleService;
    @Autowired
    private MetadataLabelService labelService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        Integer moduleId=params.get("moduleId")==null?null:Integer.parseInt((String)params.get("moduleId"));

        String metric=params.get("metric")==null?null:(String)params.get("metric");


        IPage<MetadataMetric> page = this.page(new Query<MetadataMetric>().getPage(params));

        List<MetadataMetric> list=this.baseMapper.queryMetricList(moduleId,metric);
        page.setRecords(list);
        return new PageUtils(page);
    }

    @Override
    public List<Integer> queryIdsByMetric(String metric) {
        return this.baseMapper.queryIdsByMetric(metric);

    }

    @Override
    public Integer queryIdByMetric(String metric) {
        return this.baseMapper.queryIdByMetric(metric);
    }
}
