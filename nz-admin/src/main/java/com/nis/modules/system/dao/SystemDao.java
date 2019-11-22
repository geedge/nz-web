package com.nis.modules.system.dao;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.system.entity.SystemEntity;

@Mapper
public interface SystemDao  extends BaseMapper<SystemEntity> {

}
