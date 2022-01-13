package com.nis.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.sys.entity.SysUserStarredEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface SysUserStarredDao extends BaseMapper<SysUserStarredEntity> {

    void removeStarred(@Param("params") Map<String, Object> params);
}
