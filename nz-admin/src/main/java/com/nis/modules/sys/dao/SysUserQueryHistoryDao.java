package com.nis.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.sys.entity.SysUserQueryHistoryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SysUserQueryHistoryDao extends BaseMapper<SysUserQueryHistoryEntity> {

    List<SysUserQueryHistoryEntity> queryList(@Param("params") Map<String, Object> params, IPage page);

    void deleteQueryHistory(@Param("params")Map params);
}
