package com.nis.modules.endpoint.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.endpoint.dao.MonitorEndpointConfigDao;
import com.nis.modules.endpoint.entity.MonitorEndpointConfig;
import com.nis.modules.endpoint.service.MonitorEndpointConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MonitorEndpointConfigServiceImpl extends ServiceImpl<MonitorEndpointConfigDao, MonitorEndpointConfig> implements MonitorEndpointConfigService {

    @Autowired
    private  MonitorEndpointConfigDao endpointConfigDao;

    @Override
    public List<MonitorEndpointConfig> getEndpointConfigInfoList() {
        return endpointConfigDao.queryEndpointConfigInfoList();
    }
}
