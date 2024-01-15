package com.nis.modules.endpoint.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.endpoint.entity.MonitorEndpointAgentMap;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MonitorEndpointAgentMapDao extends BaseMapper<MonitorEndpointAgentMap> {

    @Delete("TRUNCATE monitor_endpoint_agent")
    void deleteAll();

}