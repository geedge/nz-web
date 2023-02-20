package com.nis.modules.integration.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.integration.entity.IntModuleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface IntModuleDao extends BaseMapper<IntModuleEntity> {

    List<IntModuleEntity> queryList(IPage page, @Param("param") Map param);

    IntModuleEntity queryInfo(@Param("id") Integer id);
}
