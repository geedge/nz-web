package com.nis.modules.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.panel.entity.VisualChart;

import java.util.List;
import java.util.Map;

public interface VisualChartService extends IService<VisualChart> {
	
	PageUtils queryPage(Map<String,Object> params);
	
	VisualChart queryInfo(Integer id);
	
	void validateChart(VisualChart visualChart);
	
	Integer saveChart(VisualChart chart);
	
	Integer updateChart(VisualChart chart);
	
	void delCharts(Integer[] ids, Integer sync);
	
	void modify(VisualChart chart);
	
	void weights(Integer panelId,List<VisualChart> chartList);
    
    void delVisualChartsByPanelId(Integer... panelId);
    
    Map<Integer,Integer> queryChartNum();
    
    Integer fromTmpl(Map<String,Object> params);
    
    Integer syncTmpl(Map<String,Object> params);

	void saveRelPanelAndChart(String typeStr, Integer linkId, String seq, VisualChart chartTemplate);
}
