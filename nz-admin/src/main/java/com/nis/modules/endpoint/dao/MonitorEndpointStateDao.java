package com.nis.modules.endpoint.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.endpoint.entity.MonitorEndpointState;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MonitorEndpointStateDao extends BaseMapper<MonitorEndpointState> {

    void deleteAll();

}
