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
import java.io.IOException;
import java.util.Arrays;
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
		return R.ok(panel);
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
	public void getTemplateByLanguage(HttpServletResponse response, String type, HttpServletRequest request) throws IOException {
		String language = request.getHeader(Constant.LANGUAGE);
		String format = request.getHeader("format");
		format = Tool.StrUtil.emptyToDefault(format, "1");

		// 校验 LANGUAGE
		if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
			throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
		}
		// 校验 FORMAT
		if (!Arrays.asList(Constant.IMPORTFILE_TYPE_INTVALUES).contains(format)) {
			throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
		}

		visualPanelService.getTemplateByLanguage(response, format, type);
	}

	@PostMapping("/import")
    @SysLog(operation = OperationEnum.IMPORT, type = TypeEnum.CHART)
	@RequiresPermissions({ "visual:panel:save"})
	public R importChartData(MultipartFile excelFile, String type, Integer linkId, HttpServletRequest request) throws IOException {

		String language = request.getHeader(Constant.LANGUAGE);
		ValidateUtils.is(excelFile).notNull(RCode.EXCELFILE_IMPORT_FILE_ISNULL)
				.and(type).notNull(RCode.PANEL_CHART_TEMPLATE_TYPE_ISNULL);
		// 校验 LANGUAGE
		if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
			throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
		}

		Map info = visualPanelService.importChartData(excelFile, type, linkId, language);
		return R.ok(info);
	}

    @GetMapping("/export")
    @SysLog(operation = OperationEnum.EXPORT, type = TypeEnum.CHART)
    public void export(HttpServletResponse response, @RequestParam Map<String, Object> params) throws IOException {
	    Object format = params.get("format");
	    String formatStr = format == null ? "1" : format.toString();
	    // 校验 FORMAT
	    if (!Arrays.asList(Constant.IMPORTFILE_TYPE_INTVALUES).contains(formatStr)) {
		    throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
	    }
	    params.put("format", formatStr);

	    visualPanelService.export(response, params);
    }

    @DeleteMapping("/cancelImport")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.CHART)
    public R cancelImport(String seq) {
        ValidateUtils.is(seq).notNull(RCode.UNDO_IMPORT_SEQ_ISNULL);

        visualPanelService.cancelImport(seq);
        return R.ok();
    }

	/**
	 * 复制panel配置
	 * @param id
	 * @return
	 */
	@PostMapping("/duplicate/{id}")
	@SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.VISUALPANEL)
	public R duplicate(@PathVariable Integer id) {
		ValidateUtils.is(id).notNull(RCode.PANEL_ID_ISNULL);
		visualPanelService.duplicate(id);
		return R.ok();
	}

	/**
	 * panel 快照
	 * @param id
	 * @return
	 */
	@GetMapping("/snapshot")
	public void snapshot(HttpServletResponse response, HttpServletRequest request, String format, Integer panelId, Long start, Long end) {
		ValidateUtils.is(panelId).notNull(RCode.PANEL_ID_ISNULL)
				.and(start).notNull(RCode.PROMSERVER_PARAM_START_ISNULL)
				.and(end).notNull(RCode.PROMSERVER_PARAM_END_ISNULL);
		String language = request.getHeader(Constant.LANGUAGE);
		visualPanelService.snapshot(request,response,format,panelId,start,end,language);
	}
}
