package com.nis.modules.detect.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.detect.entity.DeteTypeInfoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface DeteTypeInfoDao extends BaseMapper<DeteTypeInfoEntity> {
    Map<String, String> queryTables(@Param("sql") String sql);

    void createTable(@Param("sql") String sql);

    int checkDetectSetConfigExists(@Param("typeId")Long typeId);

    void addTableFields(@Param("sql") String sql);

    void delTableFields(@Param("sql") String sql);

    void alterTableFields(@Param("sql") String sql);

    void addTableIndexs(@Param("sql") String sql);

    void delTableIndexs(@Param("sql") String sql);

    void dropTable(@Param("sql") String sql);
}
