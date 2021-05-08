package com.nis.modules.promserver.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.promserver.entity.Promserver;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface PromserverDao extends BaseMapper<Promserver>{
	
    void updateStatusInfo(@Param("promserver")Promserver promserver);

    List<Promserver> queryList(IPage page, @Param("params") Map<String, Object> params);
}
