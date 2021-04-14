package com.nis.modules.agent.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.agent.entity.MonitorConfEvent;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface MonitorConfEventDao extends BaseMapper<MonitorConfEvent> {

    List<MonitorConfEvent> selectList();

}
