package com.nis.common.update.v3_2_to_3_3.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.asset.entity.AssetModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface V3_2_AssetModelDao extends BaseMapper<AssetModel> {

    Map checkTableExists(@Param("tableName") String tableName);

    void dropTable(@Param("tableName") String tableName);

    List<Map<String, Object>> queryAssetModelIdAndTypesMapping();

    List<AssetModel> queryList();

}
