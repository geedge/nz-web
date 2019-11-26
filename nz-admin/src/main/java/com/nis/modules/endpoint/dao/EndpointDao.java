package com.nis.modules.endpoint.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.endpoint.entity.EndpointEntity;

@Mapper
public interface EndpointDao  extends BaseMapper<EndpointEntity> {
	
	List<EndpointEntity> queryPage(IPage<EndpointEntity> page,@Param("params") Map<String, Object> params);
	
}
