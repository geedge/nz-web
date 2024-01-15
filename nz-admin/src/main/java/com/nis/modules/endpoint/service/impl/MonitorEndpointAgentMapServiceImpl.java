package com.nis.modules.endpoint.service.impl;

import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Tool;
import com.nis.modules.endpoint.dao.MonitorEndpointAgentMapDao;
import com.nis.modules.endpoint.entity.MonitorEndpointAgentMap;
import com.nis.modules.endpoint.service.MonitorEndpointAgentMapService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MonitorEndpointAgentMapServiceImpl extends ServiceImpl<MonitorEndpointAgentMapDao, MonitorEndpointAgentMap> implements MonitorEndpointAgentMapService {

    private static final Log log = Log.get();

    @Override
    public Integer getAgentId(Integer endpointId) {
        if (endpointId == null) {
            return null;
        }
        MonitorEndpointAgentMap entity = this.getOne(new LambdaQueryWrapper<MonitorEndpointAgentMap>().eq(MonitorEndpointAgentMap::getEndpointId, endpointId));
        Integer agentId = Optional.ofNullable(entity).map(MonitorEndpointAgentMap::getAgentId).orElse(null);
        return agentId;
    }

    @Override
    public void updateMapping(Map<Integer, Integer> endpointAgentMap) {
        try {
            // 先删后增
            this.getBaseMapper().deleteAll();
            if (Tool.MapUtil.isNotEmpty(endpointAgentMap)) {
                List<MonitorEndpointAgentMap> mapList = endpointAgentMap.entrySet().stream()
                        .map(entry -> new MonitorEndpointAgentMap(entry.getKey(), entry.getValue())).toList();
                this.saveBatch(mapList);
            }
        } catch (Exception e) {
            log.error(e, "[updateMapping] [error]");
        }
    }

}
