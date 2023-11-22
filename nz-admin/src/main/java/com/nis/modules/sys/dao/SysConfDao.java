package com.nis.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.sys.entity.SysConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysConfDao extends BaseMapper<SysConfigEntity> {
    void clearData(@Param("sql") String sql);

    @Select("SELECT version();")
    String getDataBasesVersion();
}
