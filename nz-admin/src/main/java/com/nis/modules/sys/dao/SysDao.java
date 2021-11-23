package com.nis.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.sys.entity.SysConfigEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysDao extends BaseMapper<SysConfigEntity> {
}
