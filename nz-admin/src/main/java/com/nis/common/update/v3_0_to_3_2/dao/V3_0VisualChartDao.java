package com.nis.common.update.v3_0_to_3_2.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.dashboard.entity.VisualChart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface V3_0VisualChartDao extends BaseMapper<VisualChart> {

    Map checkTableExists(@Param("tableName") String tableName);

    List<VisualChart> queryList();

    void dropTable(@Param("tableName")String tableName);
}
