package com.nis.modules.panel.controller;

import java.util.List;
import java.util.Map;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.panel.entity.VisualChart;
import com.nis.modules.panel.service.VisualChartService;

import cn.hutool.core.util.ObjectUtil;


@RestController
@RequestMapping("/visual/panel/chart")
public class VisualPanelChartController {
	
	@Autowired
	private VisualChartService visualChartService;
	
	@GetMapping(value={"/{id}"})
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.VISUALCHART)
    public R chartInfo(@PathVariable Integer id){
		VisualChart visualChart = visualChartService.queryInfo(id);
        return R.ok(visualChart);
    }

	@GetMapping
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.VISUALCHART)
    public R chartList(@RequestParam Map<String,Object> params){
		PageUtils page = visualChartService.queryPage(params);
        return R.ok(page);
    }
	
    @PostMapping
    @SysLog(operation = OperationEnum.ADD,type = TypeEnum.VISUALCHART)
    @RequiresPermissions({"visual:chart:save"})
    public R chartSave(@RequestBody VisualChart chart){
    	visualChartService.validateChart(chart);
    	Integer id = visualChartService.saveChart(chart);
    	return R.ok().putData("id", id);
    }

    @PutMapping
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.VISUALCHART)
    @RequiresPermissions({"visual:chart:update"})
    public R chartUpdate(@RequestBody VisualChart chart){
        ValidateUtils.is(chart.getId()).notNull(RCode.CHART_ID_ISNULL);

        visualChartService.validateChart(chart);
        Integer id = visualChartService.updateChart(chart);
        return R.ok().putData("id", id);
    }

    @DeleteMapping
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.VISUALCHART)
    @RequiresPermissions({"visual:chart:delete"})
    public R chartDelete(@RequestParam Integer[] ids, Integer sync){
        ValidateUtils.is(ids).notNull(RCode.CHART_ID_ISNULL);

        visualChartService.delCharts(ids, sync);
        return R.ok();
    }
    
    /**
     * charts 调整大小接口
     *
     * @param panelId
     * @param chart
     * @return
     */
    @PutMapping(value = {"/modify"})
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.VISUALCHART)
    public R modify(@RequestBody VisualChart chart) {
        ValidateUtils.is(chart.getId()).notNull(RCode.CHART_ID_ISNULL)
                .and(chart.getSpan()).notNull(RCode.CHART_SPAN_ISNULL)
                .and(chart.getHeight()).notNull(RCode.CHART_HEIGHT_ISNULL);
             

        visualChartService.modify(chart);
        return R.ok();
    }

    
    /**
     * 调整位置接口
     * @param panelId
     * @param chartList
     * @return
     */
    @PutMapping(value = {"/weights"})
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.VISUALCHART)
    public R weights(@RequestBody Map<String,Object> params) {
    	Integer panelId = (Integer)params.get("panelId");
    	Object data = params.get("charts");
    	List<VisualChart> chartList = JSON.parseArray(JSON.toJSONString(data),VisualChart.class);
    	if(ObjectUtil.isEmpty(panelId)) {
    		throw new NZException(RCode.CHART_PANEL_ISNULL);
    	}else if(ObjectUtil.isEmpty(chartList)) {
    		throw new NZException(RCode.CHART_WEIGHT_ISNULL);
    	}
        visualChartService.weights(panelId,chartList);
        return R.ok();
    }
    
    /**
     * charts 通过模板添加
     */
    @PostMapping(value = {"/fromTmpl"})
    @SysLog(operation = OperationEnum.ADD,type = TypeEnum.VISUALCHART)
    @RequiresPermissions({"visual:chart:save"})
    public R fromTmpl(@RequestBody Map<String,Object> params) {
    	Integer total = visualChartService.fromTmpl(params);
    	return R.ok().putData("total", total);
    }
    
    /**
     * charts 通过模板添加
     */
    @PostMapping(value = {"/syncTmpl"})
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.VISUALCHART)
    @RequiresPermissions({"visual:chart:save"})
    public synchronized R syncTmpl(@RequestBody Map<String,Object> params) {
    	Integer total = visualChartService.syncTmpl(params);
    	return R.ok().putData("total", total);
    }
}
