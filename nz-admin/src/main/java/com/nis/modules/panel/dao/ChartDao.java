package com.nis.modules.panel.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.panel.entity.Chart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChartDao extends BaseMapper<Chart> {
    List<Chart> queryCharts(@Param("panelId") Integer panelId, @Param("chartId")Integer id, @Param("title")String title);

    void deleteByIds(@Param("panelId") Integer panelId, @Param("ids") List<Integer> deleteIds);

    void updateSizeById(@Param("chart") Chart firstChart);


    void updateWeightBatch(@Param("charts") List<Chart> updateCharts);
}
