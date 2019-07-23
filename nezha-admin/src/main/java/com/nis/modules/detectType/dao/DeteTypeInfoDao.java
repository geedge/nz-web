package com.nis.modules.detectType.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.detectType.entity.DeteTypeInfoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface DeteTypeInfoDao extends BaseMapper<DeteTypeInfoEntity> {
    Map<String, String> queryTables(@Param("tableName")String tableName);

    void createTable(DeteTypeInfoEntity deteTypeInfo);
}
