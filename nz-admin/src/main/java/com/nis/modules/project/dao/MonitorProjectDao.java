package com.nis.modules.project.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.project.entity.MonitorProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface MonitorProjectDao extends BaseMapper<MonitorProject> {

    List<MonitorProject> queryProjectList(IPage page, @Param("params") Map<String, Object> params);

}
