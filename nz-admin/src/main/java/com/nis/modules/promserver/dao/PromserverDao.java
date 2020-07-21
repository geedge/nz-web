package com.nis.modules.promserver.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.promserver.entity.Promserver;

@Mapper
public interface PromserverDao extends BaseMapper<Promserver>{
	
	void updateStatus(@Param("promservers") List<Promserver> promservers);
}
