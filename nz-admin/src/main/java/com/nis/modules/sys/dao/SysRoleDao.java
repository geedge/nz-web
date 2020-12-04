package com.nis.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.sys.entity.SysRoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysRoleDao extends BaseMapper<SysRoleEntity> {

    List<SysRoleEntity> getRolesByUesrId(@Param("userId") Integer userId);
}
