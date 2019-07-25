package com.nis.modules.detect.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.detect.entity.DeteTypeInfoEntity;
import com.nis.modules.detect.entity.DeteTypeMetaEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DeteTypeInfoDao extends BaseMapper<DeteTypeInfoEntity> {
    Map<String, String> queryTables(@Param("tableName")String tableName);

    void createTable(DeteTypeInfoEntity deteTypeInfo);

    int checkDetectSetConfigExists(@Param("typeId")Long typeId);

    void addTableFields(@Param("tableName")String tableName, List<DeteTypeMetaEntity> metaAddList);

    void delTableFields(@Param("tableName")String tableName, List<DeteTypeMetaEntity> metaDelList);

    void alterTableFields(@Param("tableName")String tableName, List<DeteTypeMetaEntity> metaUpdateList);

    void addTableIndexs(@Param("tableName")String tableName, List<DeteTypeMetaEntity> indexAddList);

    void delTableIndexs(@Param("tableName")String tableName, List<DeteTypeMetaEntity> indexDelList);

    void dropTable(@Param("tableName")String tableName);
}
