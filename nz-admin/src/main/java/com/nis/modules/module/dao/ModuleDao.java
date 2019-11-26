package com.nis.modules.module.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.module.entity.Module;

@Mapper
public interface ModuleDao extends BaseMapper<Module> {
	
	List<Module> queryPage(IPage<Module> page,@Param("params") Map<String, Object> params);
	
}
