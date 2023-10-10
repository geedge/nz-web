package com.nis.modules.dashboard.controller;

import cn.hutool.core.util.StrUtil;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.dashboard.entity.VisualDashboard;
import com.nis.modules.dashboard.service.VisualDashboardService;
import com.nis.modules.dashboard.utils.DashboardConstant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/visual/dashboard")
public class VisualDashboardController {

	@Autowired
	private VisualDashboardService dashboardService;

	@GetMapping("/{id}")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.DASHBOARD)
	public R dashboard(@PathVariable Integer id) {
		VisualDashboard dashboard = dashboardService.queryInfo(id);
		return R.ok(dashboard);
	}

	@GetMapping
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.DASHBOARD)
	public R dashboardList(@RequestParam Map<String, Object> params) {
		PageUtils pageUtils = dashboardService.queryList(params);
		return R.ok(pageUtils);
	}

	@RequestMapping(method = { RequestMethod.POST })
	@SysLog(operation = OperationEnum.ADD, type = TypeEnum.DASHBOARD)
	@RequiresPermissions({ "visual:dashboard:save" })
	public R dashboardSave(@RequestBody VisualDashboard dashboard) {
		ValidateUtils.is(dashboard.getName()).maxLength(64);
		dashboardService.saveVisualDashboard(dashboard);
		return R.ok().putData("dashboard", dashboard);
	}

	@RequestMapping(method = { RequestMethod.PUT })
	@SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.DASHBOARD)
	@RequiresPermissions({ "visual:dashboard:update" })
	public R dashboardUpdate(@RequestBody VisualDashboard dashboard) {
		ValidateUtils.is(dashboard.getId()).notNull(RCode.DASHBOARD_ID_ISNULL)
		.and(dashboard.getName()).maxLength(64);

		dashboardService.updateVisualDashboard(dashboard);
		return R.ok().putData("dashboard", dashboard);
	}

	@DeleteMapping
	@SysLog(operation = OperationEnum.DELETE, type = TypeEnum.DASHBOARD)
	@RequiresPermissions({ "visual:dashboard:delete" })
	public R panleDelete(@RequestParam Integer... ids) {
		ValidateUtils.is(ids).notNull(RCode.DASHBOARD_ID_ISNULL);

		dashboardService.delDashboards(ids);
		return R.ok();
	}

	/**
	 * 排序
	 *
	 * @param dashboardList
	 * @return
	 */
	@PutMapping("/tree")
	@SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.DASHBOARD)
	public R modify(@RequestBody List<VisualDashboard> dashboardList) {
		dashboardService.modify(dashboardList);
		return R.ok();
	}

	@GetMapping("/template")
	public void getTemplateByLanguage(HttpServletResponse response,
	                                  @RequestParam(required = false, defaultValue = "dashboard") String type,
	                                  @RequestParam(required = false, defaultValue = "en") String language,
	                                  @RequestParam(required = false, defaultValue = "1") String format) throws IOException {

		// 校验 LANGUAGE
		if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
			throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
		}
		// 校验 FORMAT
		List<String> supportImportingFileTypes = Arrays.asList(Constant.ImportFileType.values()).stream().map(Constant.ImportFileType::getValue).collect(Collectors.toList());
		if (!supportImportingFileTypes.contains(format)) {
			throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
		}

		dashboardService.getTemplateByLanguage(response, format, type);
	}

	@PostMapping("/import")
    @SysLog(operation = OperationEnum.IMPORT, type = TypeEnum.CHART)
	@RequiresPermissions({ "visual:dashboard:save"})
	public R importChartData(@RequestParam(value = "file") MultipartFile excelFile, @RequestParam Map<String, Object> params) throws IOException {
		Object type = params.get("type");
		ValidateUtils.is(excelFile).notNull(RCode.EXCELFILE_IMPORT_FILE_ISNULL)
				.and(type).notNull(RCode.DASHBOARD_CHART_TEMPLATE_TYPE_ISNULL);
		if(!StrUtil.equalsAnyIgnoreCase(ToolUtil.toStr(type), DashboardConstant.Type.DASHBOARD.getValue(), DashboardConstant.Type.TEMPLATE.getValue())){
			throw new NZException(RCode.DASHBOARD_IMPORT_TYPE_INCORRECT);
		}
		return R.ok(dashboardService.importChartData(excelFile, params));
	}

    @GetMapping("/export")
    @SysLog(operation = OperationEnum.EXPORT, type = TypeEnum.CHART)
    public void export(HttpServletResponse response, @RequestParam Map<String, Object> params) throws IOException {
	    dashboardService.export(response, params);
    }

    @DeleteMapping("/cancelImport")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.CHART)
    public R cancelImport(String seq) {
        ValidateUtils.is(seq).notNull(RCode.UNDO_IMPORT_SEQ_ISNULL);

        dashboardService.cancelImport(seq);
        return R.ok();
    }

	/**
	 * 复制dashboard配置
	 * @param id
	 * @return
	 */
	@PostMapping("/duplicate/{id}")
	@SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.DASHBOARD)
	public R duplicate(@PathVariable Integer id) {
		ValidateUtils.is(id).notNull(RCode.DASHBOARD_ID_ISNULL);
		dashboardService.duplicate(id);
		return R.ok();
	}

	/**
	 * dashboard 快照
	 * @param response
	 * @param request
	 * @param format
	 * @param dashboardId
	 * @param start
	 * @param end
	 * @param vars
	 */
	@GetMapping("/snapshot")
	public void snapshot(HttpServletResponse response, HttpServletRequest request, String format, Integer dashboardId, Long start, Long end, String vars) throws IOException {
		ValidateUtils.is(dashboardId).notNull(RCode.DASHBOARD_ID_ISNULL)
				.and(start).notNull(RCode.AGENT_PARAM_START_ISNULL)
				.and(end).notNull(RCode.AGENT_PARAM_END_ISNULL);
		String language = request.getHeader(Constant.LANGUAGE);
		dashboardService.snapshot(response, format, dashboardId, start, end, language, vars);
	}
}
