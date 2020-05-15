package com.nis.modules.panel.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.panel.entity.Chart;
import com.nis.modules.panel.entity.Panel;
import com.nis.modules.panel.service.ChartService;
import com.nis.modules.panel.service.PanelService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/panel")
public class PanelController {
    @Autowired
    private PanelService panelService;
    @Autowired
    private ChartService chartService;

    @GetMapping
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.PANEL)
    public R panelList(@RequestParam Map<String,Object> params){

        PageUtils page=panelService.queryPanels(params);

        return R.ok(page);
    }

    @RequestMapping(method = {RequestMethod.POST})
    @SysLog(operation = OperationEnum.ADD,type = TypeEnum.PANEL)
    public R panelSave( @RequestBody Panel panel){

        ValidateUtils.is(panel.getName()).notNull(RCode.PANEL_NAME_ISNULL);

        // 校验name是否重复
        if(!panelService.validatePanelName(panel.getId(), panel.getName())){
            throw new NZException(RCode.PANEL_NAME_DUPLICATE);
        }

        Long userId = ShiroUtils.getUserId();

        panel.setCreateBy(userId.intValue());

        panelService.save(panel);

        // 添加之后相应 panel 对象
        Map<String, Panel> resultMap = new HashMap<>(2);
        resultMap.put("panel", panel);
        return R.ok(resultMap);
    }

    @RequestMapping(method = {RequestMethod.PUT})
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.PANEL)
    public R panelUpdate( @RequestBody Panel panel){

        ValidateUtils.is(panel.getName()).notNull(RCode.PANEL_NAME_ISNULL);
        ValidateUtils.is(panel.getId()).notNull(RCode.PANEL_ID_ISNULL);

        // 校验name是否重复
        if(!panelService.validatePanelName(panel.getId(), panel.getName())){
            throw new NZException(RCode.PANEL_NAME_DUPLICATE);
        }

        Long userId = ShiroUtils.getUserId();

        panel.setCreateBy(userId.intValue());

        panelService.updateById(panel);

        // 修改之后相应 panel 对象
        Map<String, Panel> resultMap = new HashMap<>(2);
        resultMap.put("panel", panel);
        return R.ok(resultMap);
    }

    @DeleteMapping
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.PANEL)
    public R panleDelete(@RequestParam Integer ... ids){
        ValidateUtils.is(ids).notNull(RCode.PANEL_ID_ISNULL);

        panelService.delPanels(ids);

        return R.ok();
    }

    @GetMapping(value={"/{id}/charts"})
    public R chartList(@PathVariable("id") Integer panelId, @RequestParam(value="id",required = false) Integer id,@RequestParam(value="title",required = false)String title){
        List<Chart> charts=chartService.queryCharts(panelId,id,title);
        Map<String,List<Chart>> map=new HashMap<String,List<Chart>>();
        map.put("list",charts);
        return R.ok(map);
    }

    @PostMapping("/{id}/charts")
    public R chartSave(@PathVariable("id") Integer panelId,@RequestBody Chart chart){

        chart.setPanelId(panelId);

        chartService.validateChart(panelId,chart);

        chartService.saveChart(chart);

        return R.ok();
    }

    @PutMapping("/{id}/charts")
    public R chartUpdate(@PathVariable("id") Integer panelId,@RequestBody Chart chart){

        chart.setPanelId(panelId);

        chartService.validateChart(panelId,chart);

        chartService.updateChart(chart);

        return R.ok();
    }

    @DeleteMapping("/{id}/charts")
    public R chartDelete(@PathVariable("id") Integer panelId,@RequestParam Integer ... ids){
        chartService.validatePanel(panelId);
        ValidateUtils.is(ids).notNull(RCode.CHART_ID_ISNULL);
        chartService.delCharts(panelId, ids);
        return R.ok();
    }


    /**
     * charts 调整位置接口
     *
     * @param panelId
     * @param chart
     * @return
     */
    @PutMapping(value = {"/{id}/charts/modify"})
    public R modify(@PathVariable("id") Integer panelId, @RequestBody Chart chart) {
        // 校验参数
        chartService.validatePanel(panelId);
        ValidateUtils.is(chart.getId()).notNull(RCode.CHART_ID_ISNULL)
                .and(chart.getSpan()).notNull(RCode.CHART_SPAN_ISNULL)
                .and(chart.getHeight()).notNull(RCode.CHART_HEIGHT_ISNULL)
                .and(chart.getPrev()).notNull(RCode.CHART_PREV_ISNULL)
                .and(chart.getNext()).notNull(RCode.CHART_NEXT_ISNULL);

        chartService.modify(panelId, chart);
        return R.ok();
    }
    
    /**
     * chart复制
     * @param panelId
     * @param id
     * @return
     */
    @PostMapping(value={"/{id}/charts/duplicate"})
    public R duplicate(@PathVariable("id") Integer panelId, @RequestParam(value="id",required = false) Integer id){
    	// 校验参数
        chartService.validatePanel(panelId);
    	ValidateUtils.is(id).notNull(RCode.CHART_ID_ISNULL);
    	try {
			chartService.duplicate(panelId, id);
		} catch (Exception e) {
			return R.error(500, e.getMessage());
		}
        return R.ok();
    }
}
