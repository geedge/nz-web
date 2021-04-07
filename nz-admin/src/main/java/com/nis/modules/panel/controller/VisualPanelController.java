package com.nis.modules.panel.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.panel.entity.VisualChart;
import com.nis.modules.panel.entity.VisualPanel;
//import com.nis.modules.panel.service.VisualChartService;
import com.nis.modules.panel.service.VisualPanelService;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/visual/panel")
public class VisualPanelController {

	@Autowired
	private VisualPanelService visualPanelService;

	@GetMapping("/{id}")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.VISUALPANEL)
	@RequiresPermissions({ "visualpanel:info" })
	public R panel(@PathVariable Integer id) {
		VisualPanel panel = visualPanelService.queryInfo(id);
		return R.ok().putData("data", panel);
	}

	@GetMapping
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.VISUALPANEL)
	@RequiresPermissions({ "visualpanel:list" })
	public R panelList(@RequestParam Map<String, Object> params) {
		List<VisualPanel> panels = visualPanelService.queryList(params);
		return R.ok().putData("list",panels);
	}

	@RequestMapping(method = { RequestMethod.POST })
	@SysLog(operation = OperationEnum.ADD, type = TypeEnum.VISUALPANEL)
	@RequiresPermissions({ "visualpanel:save" })
	public R panelSave(@RequestBody VisualPanel panel) {
		visualPanelService.saveVisualPanel(panel);
		return R.ok().putData("panel", panel);
	}

	@RequestMapping(method = { RequestMethod.PUT })
	@SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.VISUALPANEL)
	@RequiresPermissions({ "visualpanel:update" })
	public R panelUpdate(@RequestBody VisualPanel panel) {
		ValidateUtils.is(panel.getId()).notNull(RCode.PANEL_ID_ISNULL);

		visualPanelService.updateVisualPanel(panel);
		return R.ok().putData("panel", panel);
	}

	@DeleteMapping
	@SysLog(operation = OperationEnum.DELETE, type = TypeEnum.VISUALPANEL)
	@RequiresPermissions({ "visualpanel:delete" })
	public R panleDelete(@RequestParam Integer... ids) {
		ValidateUtils.is(ids).notNull(RCode.PANEL_ID_ISNULL);

		visualPanelService.delPanels(ids);
		return R.ok();
	}

	/**
	 * 排序
	 * 
	 * @param panelList
	 * @return
	 */
	@PutMapping("/tree")
	@SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.VISUALPANEL)
	@RequiresPermissions({ "visualpanel:modify" })
	public R modify(@RequestBody List<VisualPanel> panelList) {
		visualPanelService.modify(panelList);
		return R.ok();
	}
	
	@GetMapping("/template")
    @RequiresPermissions({"visualpanel:import:template"})
    public void getExportTemplateByLanguage(HttpServletResponse response, String language, String type) {
        if (ToolUtil.isEmpty(language)) throw new NZException(RCode.PANEL_EXPORT_HEADER_LANGUAGE_ISNULL);

        visualPanelService.getExportTemplateByLanguage(response, language, type);
    }
	
	@PostMapping("/import")
    @SysLog(operation = OperationEnum.IMPORT, type = TypeEnum.CHART)
    @RequiresPermissions({"visualpanel:import"})
    public R importAlertFromExcelFile(MultipartFile excelFile, String type, Integer linkId, String language) {
        ValidateUtils.is(excelFile).notNull(RCode.PANEL_IMPORT_FILE_ISNULL)
                .and(type).notNull(RCode.PANEL_CHART_TEMPLATE_TYPE_ISNULL)
                .and(language).notNull(RCode.PANEL_EXPORT_HEADER_LANGUAGE_ISNULL);

        return R.ok(visualPanelService.importChartFromExcelFile(excelFile, type, linkId, language));
    }

    @GetMapping("/export")
    @SysLog(operation = OperationEnum.EXPORT, type = TypeEnum.CHART)
    @RequiresPermissions({"visualpanel:export"})
    public void exportAssetsFromExcel(HttpServletResponse response, @RequestParam Map<String, Object> params) {
        visualPanelService.export(response, params);
    }

    @DeleteMapping("/cancelImport")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.CHART)
    @RequiresPermissions({"visualpanel:cancel:import"})
    public R cancelImport(String seq) {
        ValidateUtils.is(seq).notNull(RCode.PANEL_CANCELIMPORTSEQ_ISNULL);

        visualPanelService.cancelImport(seq);
        return R.ok();
    }
    
}
