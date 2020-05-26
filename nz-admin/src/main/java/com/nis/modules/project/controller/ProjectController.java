package com.nis.modules.project.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.project.entity.Project;
import com.nis.modules.project.service.ProjectService;
import com.nis.modules.sys.controller.AbstractController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/project")
public class ProjectController extends AbstractController {
	
	@Autowired
	private ProjectService projectService;
	
	@GetMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.PROJECT)
	public R list(@RequestParam Map<String, Object> params){
		PageUtils page = projectService.queryPage(params);
		return R.ok(page);
	}
	
	@PostMapping
	@SysLog(operation = OperationEnum.ADD,type = TypeEnum.PROJECT)
	public R save(@RequestBody Project project) {
		ValidateUtils.is(project.getName()).notNull(RCode.PROJECT_NAME_ISNULL)
				.and(project.getName()).maxLength(64, RCode.PROJECT_NAME_TOO_LONG)
				.and(project.getRemark()).maxLength(1024, RCode.PROJECT_REMARK_TOO_LONG);

		projectService.saveOrUpdateProject(project);
		return R.ok();
	}
	
	@PutMapping
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.PROJECT)
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
	public R remove(@RequestParam Integer ... ids) {
		ValidateUtils.is(ids).notNull(RCode.PROJECT_ID_ISNULL);
		projectService.validateBulitinRule(Arrays.asList(ids));
		projectService.removeHandle(Arrays.asList(ids));
		return R.ok();
	}
	
	@GetMapping(value="info")
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.PROJECT)
	public R info(@RequestParam Integer id) {
		ValidateUtils.is(id).notNull(RCode.PROJECT_ID_ISNULL);
		Map<String, Object> result = projectService.queryProjectInfo(id);
		return R.ok(result);
	}
}

