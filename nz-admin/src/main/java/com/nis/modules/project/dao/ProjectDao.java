package com.nis.modules.project.dao;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.project.entity.Project;

@Mapper
public interface ProjectDao  extends BaseMapper<Project> {

}
