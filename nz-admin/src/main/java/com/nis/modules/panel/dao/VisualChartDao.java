package com.nis.modules.panel.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.panel.entity.VisualChart;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VisualChartDao extends BaseMapper<VisualChart> {

    VisualChart queryInfo(Integer id);

    List<VisualChart> queryList(@Param("list") List<Integer> idList);

    void updateSizeById(@Param("chart") VisualChart chart);
    
    void updateWeightBatch(@Param("panelId")Integer panelId , @Param("charts") List<VisualChart> charts);
    
    List<VisualChart> selectChartNum();
    
    List<VisualChart> selectTmplCharts(@Param("params") Map<String,Object> params);
    
	List<VisualChart> queryPage(IPage page,@Param("params") Map<String, Object> params);

	List<VisualChart> queryPage(@Param("params") Map<String, Object> params);

    List<VisualChart> queryChartWithElementByIds(@Param("list")List<Integer> chartIdList);
}
