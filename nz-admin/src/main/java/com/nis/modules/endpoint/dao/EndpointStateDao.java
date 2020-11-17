package com.nis.modules.endpoint.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.endpoint.entity.EndpointState;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface EndpointStateDao  extends BaseMapper<EndpointState> {

	void deleteAll();
	
}
