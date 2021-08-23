package com.nis.modules.endpoint.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.endpoint.entity.MonitorEndpointConfig;

import java.util.List;

public interface MonitorEndpointConfigService extends IService<MonitorEndpointConfig> {

    List<MonitorEndpointConfig> getEndpointConfigInfoList();

}
