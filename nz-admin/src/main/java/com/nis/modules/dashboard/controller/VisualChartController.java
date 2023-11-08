package com.nis.modules.dashboard.controller;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.dashboard.entity.VisualChart;
import com.nis.modules.dashboard.service.VisualChartService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/visual/dashboard/chart")
public class VisualChartController {

	@Autowired
	private VisualChartService visualChartService;

	@GetMapping(value={"/{id}"})
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.VISUAL_CHART)
    public R chartInfo(@PathVariable Integer id){
		VisualChart visualChart = visualChartService.queryInfo(id);
        return R.ok(visualChart);
    }

	@GetMapping
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.VISUAL_CHART)
    public R chartList(@RequestParam Map<String,Object> params){
		PageUtils page = visualChartService.queryPage(params);
        return R.ok(page);
    }

    @PostMapping
    @SysLog(operation = OperationEnum.ADD,type = TypeEnum.VISUAL_CHART)
    @RequiresPermissions({"visual:chart:save"})
    public R chartSave(@RequestBody VisualChart chart){
    	Integer id = visualChartService.saveChart(chart);
    	return R.ok().putData("id", id);
    }

    @PutMapping
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.VISUAL_CHART)
    @RequiresPermissions({"visual:chart:update"})
    public R chartUpdate(@RequestBody VisualChart chart){
        ValidateUtils.is(chart.getId()).notNull(RCode.CHART_ID_ISNULL);

        Integer id = visualChartService.updateChart(chart);
        return R.ok().putData("id", id);
    }

    @DeleteMapping
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.VISUAL_CHART)
    @RequiresPermissions({"visual:chart:delete"})
    public R chartDelete(@RequestParam Integer[] ids, Integer sync){
        ValidateUtils.is(ids).notNull(RCode.CHART_ID_ISNULL);

        visualChartService.delCharts(Tool.ListUtil.list(true, ids), sync);
        return R.ok();
    }

    /**
     * charts 调整大小接口
     *
     * @param chart
     * @return
     */
    @PutMapping(value = {"/modify"})
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.VISUAL_CHART)
    public R modify(@RequestBody VisualChart chart) {
        ValidateUtils.is(chart.getId()).notNull(RCode.CHART_ID_ISNULL)
                .and(chart.getSpan()).notNull(RCode.CHART_SPAN_ISNULL)
                .and(chart.getHeight()).notNull(RCode.CHART_HEIGHT_ISNULL);


        visualChartService.modify(chart);
        return R.ok();
    }


    /**
     * 调整位置接口
     *
     * @param params
     * @return
     */
    @PutMapping(value = {"/weights"})
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.VISUAL_CHART)
    public R weights(@RequestBody Map<String,Object> params) {
    	Integer dashboardId = (Integer)params.get("dashboardId");
    	Object data = params.get("charts");
    	List<VisualChart> chartList = JSON.parseArray(JSON.toJSONString(data),VisualChart.class);
    	if(ObjectUtil.isEmpty(dashboardId)) {
    		throw new NZException(RCode.DASHBOARD_NOT_EXIST);
    	}else if(ObjectUtil.isEmpty(chartList)) {
    		throw new NZException(RCode.CHART_WEIGHT_ISNULL);
    	}
        visualChartService.weights(dashboardId,chartList);
        return R.ok();
    }

    /**
     * charts 模板同步接口
     *
     * @param params pid & ids cannot be empty at the same time
     * @return
     */
    @PostMapping(value = {"/syncTmpl"})
    @RequiresPermissions({"visual:chart:save"})
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.VISUAL_CHART)
    public synchronized R syncTmpl(@RequestBody Map<String, Object> params) {
        // validate
        // pid and chartIds cannot be empty at the same time
        Integer pid = (Integer) params.get("pid");
        List<Integer> chartIdList = (List<Integer>) params.get("ids");

        if (Tool.BooleanUtil.and(Tool.ObjectUtil.isNull(pid),
                Tool.CollUtil.isEmpty(chartIdList))) {
            return R.error(RCode.CHART_SYNCTMPL_ERROR);
        }

        chartIdList = Tool.CollUtil.defaultIfEmpty(chartIdList, Tool.ListUtil.empty());
        Integer total = visualChartService.chartTmplSynch(pid, chartIdList);
        return R.ok().putData("total", total);
    }
}
