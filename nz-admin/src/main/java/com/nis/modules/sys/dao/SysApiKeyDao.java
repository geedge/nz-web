package com.nis.modules.sys.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.sys.entity.SysApiKey;

@Mapper
public interface SysApiKeyDao extends BaseMapper<SysApiKey>{

	List<SysApiKey> queryList(IPage page,@Param("params") Map<String, Object> params);
	
}
