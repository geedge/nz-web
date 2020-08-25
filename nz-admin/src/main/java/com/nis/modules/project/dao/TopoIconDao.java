package com.nis.modules.project.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.project.entity.TopoIcon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface TopoIconDao extends BaseMapper<TopoIcon> {

    List<TopoIcon> queryIconList(@Param("params") Map<String, Object> params);
}
