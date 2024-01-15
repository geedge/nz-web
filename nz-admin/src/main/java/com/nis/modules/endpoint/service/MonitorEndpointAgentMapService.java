package com.nis.modules.endpoint.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.endpoint.entity.MonitorEndpointAgentMap;

import java.util.Map;

public interface MonitorEndpointAgentMapService extends IService<MonitorEndpointAgentMap> {

    Integer getAgentId(Integer endpointId);

    void updateMapping(Map<Integer, Integer> endpointAgentMap);

}
