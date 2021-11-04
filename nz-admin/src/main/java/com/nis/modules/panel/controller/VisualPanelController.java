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

import javax.servlet.http.HttpServletRequest;
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
	public R panel(@PathVariable Integer id) {
		VisualPanel panel = visualPanelService.queryInfo(id);
		return R.ok().putData("data", panel);
	}

	@GetMapping
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.VISUALPANEL)
	public R panelList(@RequestParam Map<String, Object> params) {
		List<VisualPanel> panels = visualPanelService.queryList(params);
		return R.ok().putData("list",panels);
	}

	@RequestMapping(method = { RequestMethod.POST })
	@SysLog(operation = OperationEnum.ADD, type = TypeEnum.VISUALPANEL)
	@RequiresPermissions({ "visual:panel:save" })
	public R panelSave(@RequestBody VisualPanel panel) {
		ValidateUtils.is(panel.getName()).maxLength(64);
		visualPanelService.saveVisualPanel(panel);
		return R.ok().putData("panel", panel);
	}

	@RequestMapping(method = { RequestMethod.PUT })
	@SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.VISUALPANEL)
	@RequiresPermissions({ "visual:panel:update" })
	public R panelUpdate(@RequestBody VisualPanel panel) {
		ValidateUtils.is(panel.getId()).notNull(RCode.PANEL_ID_ISNULL)
		.and(panel.getName()).maxLength(64);

		visualPanelService.updateVisualPanel(panel);
		return R.ok().putData("panel", panel);
	}

	@DeleteMapping
	@SysLog(operation = OperationEnum.DELETE, type = TypeEnum.VISUALPANEL)
	@RequiresPermissions({ "visual:panel:delete" })
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
	public R modify(@RequestBody List<VisualPanel> panelList) {
		visualPanelService.modify(panelList);
		return R.ok();
	}
	
	@GetMapping("/template")
    public void getExportTemplateByLanguage(HttpServletResponse response, String type,HttpServletRequest request) {
        String language = request.getHeader(Constant.LANGUAGE);

        visualPanelService.getExportTemplateByLanguage(response, language, type);
    }
	
	@PostMapping("/import")
    @SysLog(operation = OperationEnum.IMPORT, type = TypeEnum.CHART)
	@RequiresPermissions({ "visual:panel:save"})
    public R importAlertFromExcelFile(MultipartFile excelFile, String type, Integer linkId,HttpServletRequest request) {
        String language = request.getHeader(Constant.LANGUAGE);
        ValidateUtils.is(excelFile).notNull(RCode.PANEL_IMPORT_FILE_ISNULL)
                .and(type).notNull(RCode.PANEL_CHART_TEMPLATE_TYPE_ISNULL);

        return R.ok(visualPanelService.importChartFromExcelFile(excelFile, type, linkId, language));
    }

    @GetMapping("/export")
    @SysLog(operation = OperationEnum.EXPORT, type = TypeEnum.CHART)
    public void exportAssetsFromExcel(HttpServletResponse response, @RequestParam Map<String, Object> params) {
        visualPanelService.export(response, params);
    }

    @DeleteMapping("/cancelImport")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.CHART)
    public R cancelImport(String seq) {
        ValidateUtils.is(seq).notNull(RCode.PANEL_CANCELIMPORTSEQ_ISNULL);

        visualPanelService.cancelImport(seq);
        return R.ok();
    }
    
}
