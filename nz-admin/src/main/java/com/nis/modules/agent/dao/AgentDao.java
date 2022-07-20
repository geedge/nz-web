package com.nis.modules.agent.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.agent.entity.Agent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AgentDao extends BaseMapper<Agent>{
	
    void updateStatusInfo(@Param("agent")Agent agent);

    List<Agent> queryList(IPage page, @Param("params") Map<String, Object> params);
}
