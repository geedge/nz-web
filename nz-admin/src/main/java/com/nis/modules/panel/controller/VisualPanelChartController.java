package com.nis.modules.panel.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

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
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.panel.entity.Chart;
import com.nis.modules.panel.entity.Panel;
import com.nis.modules.panel.entity.VisualChart;
import com.nis.modules.panel.service.VisualChartService;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;


@RestController
@RequestMapping("/visual/panel/chart")
public class VisualPanelChartController {
	
	@Autowired
	private VisualChartService visualChartService;
	
	@GetMapping(value={"/{id}"})
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.VISUALCHART)
    @RequiresPermissions({"visualchart:info"})
    public R chartInfo(@PathVariable Integer id){
		VisualChart visualChart = visualChartService.queryInfo(id);
        return R.ok().putData("data", visualChart);
    }

	@GetMapping
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.VISUALCHART)
    @RequiresPermissions({"visualchart:info"})
    public R chartList(@RequestParam Map<String,Object> params){
		List<VisualChart> visualCharts = visualChartService.queryList(params);
        return R.ok().putData("list", visualCharts);
    }
	
    @PostMapping
    @SysLog(operation = OperationEnum.ADD,type = TypeEnum.VISUALCHART)
    @RequiresPermissions({"visualchart:save"})
    public R chartSave(@RequestBody VisualChart chart){
    	visualChartService.validateChart(chart);
    	Integer id = visualChartService.saveChart(chart);
    	return R.ok().putData("id", id);
    }

    @PutMapping
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.VISUALCHART)
    @RequiresPermissions({"visualchart:update"})
    public R chartUpdate(@RequestBody VisualChart chart){
        ValidateUtils.is(chart.getId()).notNull(RCode.CHART_ID_ISNULL);

        visualChartService.validateChart(chart);
        Integer id = visualChartService.updateChart(chart);
        return R.ok().putData("id", id);
    }

    @DeleteMapping
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.VISUALCHART)
    @RequiresPermissions({"visualchart:delete"})
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
    @RequiresPermissions({"visualchart:modify"})
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
    @RequiresPermissions({"visualchart:weights"})
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
    
}
