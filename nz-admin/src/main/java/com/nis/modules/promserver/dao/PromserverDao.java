package com.nis.modules.promserver.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.promserver.entity.Promserver;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PromserverDao extends BaseMapper<Promserver>{
	
    void updateStatusInfo(@Param("promserver")Promserver promserver);
}
