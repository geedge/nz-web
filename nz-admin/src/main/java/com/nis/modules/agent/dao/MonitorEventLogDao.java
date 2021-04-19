package com.nis.modules.agent.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.agent.entity.MonitorEventLog;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface MonitorEventLogDao extends BaseMapper<MonitorEventLog> {

}
