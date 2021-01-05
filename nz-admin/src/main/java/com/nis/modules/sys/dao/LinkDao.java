package com.nis.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.sys.entity.Link;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface LinkDao extends BaseMapper<Link>{
	
	List<Link> queryList(@Param("params") Map<String, Object> params);
}
