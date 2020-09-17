package com.nis.modules.panel.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.panel.entity.Chart;
import com.nis.modules.panel.entity.Panel;
import com.nis.modules.panel.service.ChartService;
import com.nis.modules.panel.service.PanelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
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
        List<Panel> panels = panelService.queryList(params);
        Map list = new HashMap();
        list.put("list",panels);
        return R.ok(list);
    }

    @RequestMapping(method = {RequestMethod.POST})
    @SysLog(operation = OperationEnum.ADD,type = TypeEnum.PANEL)
    public R panelSave( @RequestBody Panel panel){
        ValidateUtils.is(panel.getName()).notNull(RCode.PANEL_NAME_ISNULL);

        panelService.saveOrUpdatePanel(panel);
        // 添加之后相应 panel 对象
        Map<String, Panel> resultMap = new HashMap<>(2);
        resultMap.put("panel", panel);
        return R.ok(resultMap);
    }

    @RequestMapping(method = {RequestMethod.PUT})
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.PANEL)
    public R panelUpdate( @RequestBody Panel panel){
        ValidateUtils.is(panel.getName()).notNull(RCode.PANEL_NAME_ISNULL)
                     .and(panel.getId()).notNull(RCode.PANEL_ID_ISNULL);

        panelService.saveOrUpdatePanel(panel);
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

    /**
     * panel位置调整
     * @param panel
     * @return
     */
    @PutMapping("/modify")
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.PANEL)
    public R modify(@RequestBody Panel panel){
        ValidateUtils.is(panel.getId()).notNull(RCode.PANEL_ID_ISNULL)
                .and(panel.getType()).notNull(RCode.PANEL_TYPE_ISNULL)
                .and(panel.getPrev()).notNull(RCode.PANEL_PREV_ISNULL)
                .and(panel.getNext()).notNull(RCode.PANEL_NEXT_ISNULL);

        panelService.modify(panel);
        return R.ok();
    }

    @GetMapping(value={"/{id}/charts"})
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.CHART)
    public R chartList(@PathVariable("id") Integer panelId, @RequestParam(value="id",required = false) Integer id,@RequestParam(value="title",required = false)String title){
        List<Chart> charts=chartService.queryCharts(panelId,id,title);
        Map<String,List<Chart>> map=new HashMap<String,List<Chart>>();
        map.put("list",charts);
        return R.ok(map);
    }

    @PostMapping("/{id}/charts")
    @SysLog(operation = OperationEnum.ADD,type = TypeEnum.CHART)
    public R chartSave(@PathVariable("id") Integer panelId,@RequestBody Chart chart){

        chartService.validateChart(panelId, chart);
        chartService.saveChart(panelId, chart);
        return R.ok();
    }

    @PutMapping("/{id}/charts")
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.CHART)
    public R chartUpdate(@PathVariable("id") Integer panelId,@RequestBody Chart chart){
        ValidateUtils.is(chart.getId()).notNull(RCode.CHART_ID_ISNULL);

        chartService.validateChart(panelId, chart);
        chartService.updateChart(panelId, chart);
        return R.ok();
    }

    @DeleteMapping("/{id}/charts")
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.CHART)
    public R chartDelete(@PathVariable("id") Integer panelId,@RequestParam Integer[] ids, Integer sync){
        ValidateUtils.is(ids).notNull(RCode.CHART_ID_ISNULL);

        chartService.delCharts(panelId, ids, sync);
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
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.CHART)
    public R modify(@PathVariable("id") Integer panelId, @RequestBody Chart chart) {
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
     * @param chart
     * @return
     */
    @PostMapping(value={"/{id}/charts/duplicate"})
    @SysLog(operation = OperationEnum.ADD,type = TypeEnum.CHART)
    public R duplicate(@PathVariable("id") Integer panelId, @RequestBody Chart chart){
    	ValidateUtils.is(chart.getId()).notNull(RCode.CHART_ID_ISNULL);
    	Chart result = null;
    	try {
    		result = chartService.duplicate(panelId, chart.getId());
		} catch (Exception e) {
			return R.error(500, e.getMessage());
		}
        return R.ok(result);
    }

    @GetMapping("/template")
    public void getExportTemplateByLanguage(HttpServletResponse response, String language) {
        if (ToolUtil.isEmpty(language)) throw new NZException(RCode.PANEL_EXPORT_HEADER_LANGUAGE_ISNULL);

        chartService.getExportTemplateByLanguage(response, language);
    }

    @PostMapping("/import")
    @SysLog(operation = OperationEnum.IMPORT, type = TypeEnum.CHART)
    public R importAlertFromExcelFile(MultipartFile excelFile) {
        ValidateUtils.is(excelFile).notNull(RCode.PANEL_IMPORT_FILE_ISNULL);

        return R.ok(chartService.importChartFromExcelFile(excelFile));
    }

    @GetMapping("/export")
    @SysLog(operation = OperationEnum.EXPORT, type = TypeEnum.CHART)
    public void exportAssetsFromExcel(HttpServletResponse response, @RequestParam Map<String, Object> params) {
        chartService.export(response, params);
    }

    @DeleteMapping("/cancelImport")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.CHART)
    public R cancelImport(String seq) {
        ValidateUtils.is(seq).notNull(RCode.PANEL_CANCELIMPORTSEQ_ISNULL);

        chartService.cancelImport(seq);
        return R.ok();
    }
}
