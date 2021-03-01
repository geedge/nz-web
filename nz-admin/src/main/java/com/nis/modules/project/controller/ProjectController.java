package com.nis.modules.project.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.project.entity.Project;
import com.nis.modules.project.entity.TopoIcon;
import com.nis.modules.project.service.ProjectService;
import com.nis.modules.project.service.TopoIconService;
import com.nis.modules.project.service.TopoService;
import com.nis.modules.sys.controller.AbstractController;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/project")
public class ProjectController extends AbstractController {
	
	@Autowired
	private ProjectService projectService;

	@Autowired
	private TopoService topoService;

	@Autowired
	private TopoIconService topoIconService;
	
	@GetMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.PROJECT)
	@RequiresPermissions({"project:list"})
	public R list(@RequestParam Map<String, Object> params){
		PageUtils page = projectService.queryPage(params);
		return R.ok(page);
	}
	
	@PostMapping
	@SysLog(operation = OperationEnum.ADD,type = TypeEnum.PROJECT)
	@RequiresPermissions({"project:save"})
	public R save(@RequestBody Project project) {
		ValidateUtils.is(project.getName()).notNull(RCode.PROJECT_NAME_ISNULL)
				.and(project.getName()).maxLength(64, RCode.PROJECT_NAME_TOO_LONG)
				.and(project.getRemark()).maxLength(1024, RCode.PROJECT_REMARK_TOO_LONG);

		projectService.saveOrUpdateProject(project);
		return R.ok();
	}
	
	@PutMapping
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.PROJECT)
	@RequiresPermissions({"project:update"})
	public R modify(@RequestBody Project project) {
		ValidateUtils.is(project.getId()).notNull(RCode.PROJECT_ID_ISNULL)
				.and(project.getName()).notNull(RCode.PROJECT_NAME_ISNULL)
				.and(project.getName()).maxLength(64, RCode.PROJECT_NAME_TOO_LONG)
				.and(project.getRemark()).maxLength(1024, RCode.PROJECT_REMARK_TOO_LONG);
		projectService.validateBulitinRule(Arrays.asList(project.getId()));

		projectService.saveOrUpdateProject(project);
		return R.ok();
	}
	
	@DeleteMapping
	@SysLog(operation = OperationEnum.DELETE,type = TypeEnum.PROJECT)
	@RequiresPermissions({"project:delete"})
	public R remove(@RequestParam Integer ... ids) {
		ValidateUtils.is(ids).notNull(RCode.PROJECT_ID_ISNULL);
		projectService.validateBulitinRule(Arrays.asList(ids));
		projectService.removeHandle(Arrays.asList(ids));
		return R.ok();
	}
	
	@GetMapping(value="info")
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.PROJECT)
	@RequiresPermissions({"project:info"})
	public R info(@RequestParam Integer id) {
		ValidateUtils.is(id).notNull(RCode.PROJECT_ID_ISNULL);
		Map<String, Object> result = projectService.queryProjectInfo(id);
		return R.ok(result);
	}

	/**
	 * topo icon查询
	 *
	 * @param params
	 * @return
	 */
	@GetMapping(value = "/topo/icon")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.PROJECT)
	@RequiresPermissions({"project:topo:icon:list"})
	public R queryIconList(@RequestParam Map<String, Object> params) {
		Map result = new HashMap(2);
		List<TopoIcon> topoIcons = topoIconService.queryIconList(params);
		result.put("list", topoIcons);
		return R.ok(result);
	}

	/**
	 * topo icon新增
	 *
	 * @param name
	 * @param file
	 * 
	 * @return
	 */
	@PostMapping("/topo/icon")
	@SysLog(operation = OperationEnum.ADD, type = TypeEnum.PROJECT)
	@RequiresPermissions({"project:topo:icon:save"})
	public R save(String name, @RequestParam(value = "file", required = false) MultipartFile file,String unit) {
		ValidateUtils.is(name).notNull(RCode.PROJECT_TOPOICON_NAME_ISNULL)
		.is(unit).notNull(RCode.PROJECT_TOPOICON_UNIT_ISNULL)
		.and(file).notNull(RCode.PROJECT_TOPOICON_FILE_ISNULL);

		
		try {
			Integer iconId = topoIconService.saveTopoIcon(name, file,unit);
			Map result = new HashMap(2);
			result.put("id", iconId);
			return R.ok(result);
		} catch (IOException e) {
			logger.error("拓朴图图标保存失败，错误信息是：" + e);
			return R.error(RCode.ERROR);
		}
	}

	/**
	 * topo icon删除
	 *
	 * @param ids
	 * @return
	 */
	@DeleteMapping("/topo/icon")
	@SysLog(operation = OperationEnum.DELETE, type = TypeEnum.PROJECT)
	@RequiresPermissions({"project:topo:icon:delete"})
	public R removeIcons(@RequestParam Integer... ids) {
		ValidateUtils.is(ids).notNull(RCode.PROJECT_ID_ISNULL);

		topoIconService.removeIcons(ids);
		return R.ok();
	}


	/**
	 * icon图标预览
	 *
	 * @param id
	 * @param response
	 */
	@GetMapping(value = "/topo/icon/{id}")
	@RequiresPermissions({"project:topo:icon:info"})
	public void downloadIconFile(@PathVariable("id") Integer id, HttpServletResponse response) {
		ValidateUtils.is(id).notNull(RCode.PROJECT_ICONID_ISNULL);

		topoIconService.downloadIconFileById(id, response);
	}

	/**
	 * 系统topo查询
	 *
	 * @param projectId
	 */
	@GetMapping(value = "/topo")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.PROJECT)
	@RequiresPermissions({"project:topo:list"})
	public R queryTopo(Integer projectId) {
		ValidateUtils.is(projectId).notNull(RCode.PROJECT_ID_ISNULL);

		Map result = new HashMap(2);
		result.put("topo", topoService.queryTopoByProjectId(projectId));
		return R.ok(result);
	}

	@PutMapping("/topo")
	@SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.PROJECT)
	@RequiresPermissions({"project:topo:update"})
	public R updateProjectTopo(@RequestBody Map<String, Object> params) {
		Integer projectId = (Integer) params.get("projectId");
		String topoConfig = (String) params.get("topo");

		ValidateUtils.is(projectId).notNull(RCode.PROJECT_ID_ISNULL)
				.and(topoConfig).notNull(RCode.PROJECT_TOPOCONFIG_ISNULL);

		topoService.updateProjectTopo(projectId, topoConfig);
		return R.ok();
	}
	
	/**
	 * icon unit 查询
	 *
	 * @param id
	 * @param response
	 */
	@GetMapping(value = "/topo/iconUnit")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.PROJECT)
	/* @RequiresPermissions({"project:topo:icon:unit"}) */
	public R queryTopoUnit(String name) {

		List<String> result = topoIconService.queryTopoIconUnitName(name);
		
		return R.ok(result);
	}
}