package com.nis.modules.endpoint.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.metric.dto.MetricsDto;

@Mapper
public interface EndpointDao  extends BaseMapper<Endpoint> {
	
	List<Endpoint> queryPage(IPage<Endpoint> page,@Param("params") Map<String, Object> params);
	
	void deleteByModuleIds(@Param("moduleIds")List moduleIds);
	
	List<MetricsDto> selectEndpointStatisticsInfoByModel();
}
