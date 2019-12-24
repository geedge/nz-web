package com.nis.modules.panel.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.panel.entity.ChartElement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChartElementDao extends BaseMapper<ChartElement> {
    List<Integer> queryElementIds(@Param("chartId")Integer chartId);
}
