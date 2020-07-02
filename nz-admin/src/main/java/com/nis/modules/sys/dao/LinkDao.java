package com.nis.modules.sys.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.sys.entity.Link;

@Mapper
public interface LinkDao extends BaseMapper<Link>{
	
	List<Link> queryList(@Param("params") Map<String, Object> params);
}
