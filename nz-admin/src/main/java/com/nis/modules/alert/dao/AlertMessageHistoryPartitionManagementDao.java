package com.nis.modules.alert.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AlertMessageHistoryPartitionManagementDao {

    /**
     * 根据 表名 查询  PARTITION_NAME & TABLE_ROWS
     *
     * @param tableName
     * @param partitionNames
     * @return
     */
    List<String> getPartitionNamesByTabelName(@Param("tableName") String tableName, @Param("partitionNames") String... partitionNames);

    void createIntervalsPartitions(@Param("map") Map<String, String> partitionNameAndExprMap, @Param("tableName") String tableName, @Param("columnName") String columnName);

    void addPartitions(@Param("tableName") String tableName, @Param("partitionName") String partitionName, @Param("partitionExpr") String partitionExpr);

    void dropPartitionByPnames(@Param("tableName") String tableName, @Param("list") List<String> cleanPartitionList);

}
