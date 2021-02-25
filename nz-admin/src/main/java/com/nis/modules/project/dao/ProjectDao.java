package com.nis.modules.project.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.project.entity.Project;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectDao  extends BaseMapper<Project> {

}
