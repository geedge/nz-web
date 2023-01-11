package com.nis.modules.endpoint.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.endpoint.entity.MonitorEndpointState;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MonitorEndpointStateDao extends BaseMapper<MonitorEndpointState> {

    void updateBatchEndpointState(@Param("datas")List<MonitorEndpointState> datas,@Param("type")String type);
}
