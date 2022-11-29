package com.nis.common.update.v3_5_to_3_6.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.asset.entity.AssetModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface V3_5_AssetModelDao extends BaseMapper<AssetModel> {

    Map checkTableExists(@Param("tableName") String tableName);

    void dropTable(@Param("tableName") String tableName);

    List<AssetModel> queryList();

}
