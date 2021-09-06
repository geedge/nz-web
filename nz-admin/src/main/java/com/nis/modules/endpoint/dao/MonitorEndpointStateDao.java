package com.nis.modules.endpoint.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.endpoint.entity.MonitorEndpointState;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MonitorEndpointStateDao extends BaseMapper<MonitorEndpointState> {

    void deleteConfStateByType(@Param("type") String type);

}
