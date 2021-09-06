package com.nis.modules.endpoint.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.endpoint.entity.MonitorEndpointConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MonitorEndpointConfigDao extends BaseMapper<MonitorEndpointConfig> {

    List<MonitorEndpointConfig> getEndpointConfigInfoListByType(@Param("type") String type);

}
