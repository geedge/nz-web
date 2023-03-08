package com.nis.modules.panel.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.panel.entity.VisualChart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface VisualChartDao extends BaseMapper<VisualChart> {

    VisualChart queryInfo(Integer id);

    List<VisualChart> queryList(@Param("list") List<Integer> idList);

    void updateSizeById(@Param("chart") VisualChart chart);
    
    void updateWeightBatch(@Param("panelId")Integer panelId , @Param("charts") List<VisualChart> charts);
    
	List<VisualChart> queryPage(IPage page,@Param("params") Map<String, Object> params);

	List<VisualChart> queryPage(@Param("params") Map<String, Object> params);

    // 查询model和module实例化的图表数据信息
    List<VisualChart> selectInstanceChart(@Param("type")Integer type,@Param("id")Integer id);

    void insertBatchChart(@Param("list") List<VisualChart> oldChartListByPanelId);

    List<VisualChart> queryChartsIncludeElementInfo(@Param("panelId") Integer panelId);
}
