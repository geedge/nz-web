package com.nis.modules.panel.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.panel.entity.Chart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChartDao extends BaseMapper<Chart> {
    List<Chart> queryCharts(@Param("panelId") Integer panelId);

    List<Integer> queryChartIds(@Param("panelIds") Integer... panelIds);

    void deleteByIds(@Param("panelId") Integer panelId, @Param("ids") List<Integer> deleteIds);
}
