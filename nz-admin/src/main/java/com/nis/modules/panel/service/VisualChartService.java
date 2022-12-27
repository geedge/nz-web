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
    
    Integer fromTmpl(Map<String,Object> params);
    
    Integer syncTmpl(Map<String,Object> params);

	void saveRelPanelAndChart(String typeStr, Integer linkId, Integer tempId, String seq, VisualChart chartTemplate);

	List<VisualChart> queryChartWithElementByIds(List<Integer> chartIdList);

	/**
	 * 平铺 获取所有 chart 节点
	 *
	 * @param sourceChartList
	 * @return
	 */
	List<VisualChart> getAllChartList(List<VisualChart> sourceChartList);

	List<VisualChart> queryChartsIncludeElementInfo(Integer panelId);
}
