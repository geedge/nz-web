package com.nis.modules.module.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.module.entity.ModuleEntity;

@Mapper
public interface ModuleDao extends BaseMapper<ModuleEntity> {
	
	List<ModuleEntity> queryPage(IPage<ModuleEntity> page,@Param("params") Map<String, Object> params);
	
}
