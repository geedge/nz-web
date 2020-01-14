package com.nis.modules.panel.controller;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.panel.entity.Chart;
import com.nis.modules.panel.entity.Panel;
import com.nis.modules.panel.service.ChartService;
import com.nis.modules.panel.service.PanelService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/panel")
public class PanelController {
    @Autowired
    private PanelService panelService;
    @Autowired
    private ChartService chartService;

    @GetMapping
    public R panelList(@RequestParam Map<String,Object> params){

        PageUtils page=panelService.queryPanels(params);

        return R.ok(page);
    }

    @RequestMapping(method = {RequestMethod.POST})
    public R panelSave( @RequestBody Panel panel){

        ValidateUtils.is(panel.getName()).notNull(RCode.PANEL_NAME_ISNULL);

        Long userId = ShiroUtils.getUserId();

        panel.setCreateBy(userId.intValue());

        panelService.save(panel);

        // 添加之后相应 panel 对象
        Map<String, Panel> resultMap = new HashMap<>(2);
        resultMap.put("panel", panel);
        return R.ok(resultMap);
    }

    @RequestMapping(method = {RequestMethod.PUT})
    public R panelUpdate( @RequestBody Panel panel){

        ValidateUtils.is(panel.getName()).notNull(RCode.PANEL_NAME_ISNULL);
        ValidateUtils.is(panel.getId()).notNull(RCode.PANEL_ID_ISNULL);

        Long userId = ShiroUtils.getUserId();

        panel.setCreateBy(userId.intValue());

        panelService.updateById(panel);

        // 修改之后相应 panel 对象
        Map<String, Panel> resultMap = new HashMap<>(2);
        resultMap.put("panel", panel);
        return R.ok(resultMap);
    }

    @DeleteMapping
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
        chartService.delCharts(ids);
        return R.ok();
    }
}
