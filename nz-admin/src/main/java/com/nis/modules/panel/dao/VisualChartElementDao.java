package com.nis.modules.panel.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.panel.entity.VisualChartElement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VisualChartElementDao extends BaseMapper<VisualChartElement> {

    void updateNameBatch(@Param("list") List<VisualChartElement> allElementList);

}
