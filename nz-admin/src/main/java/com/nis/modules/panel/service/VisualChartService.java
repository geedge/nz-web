package com.nis.modules.panel.service;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.panel.entity.Chart;
import com.nis.modules.panel.entity.VisualChart;

public interface VisualChartService extends IService<VisualChart> {
	
	List<VisualChart> queryList(Map<String,Object> params);
	
	VisualChart queryInfo(Integer id);
	
	void validateChart(VisualChart visualChart);
	
	Integer saveChart(VisualChart chart);
	
	Integer updateChart(VisualChart chart);
	
	void delCharts(Integer[] ids, Integer sync);
	
	void modify(VisualChart chart);
	
	void weights(Integer panelId,List<VisualChart> chartList);
    
    void delVisualChartsByPanelId(Integer... panelId);
    
    Map<Integer,Integer> queryChartNum();
}