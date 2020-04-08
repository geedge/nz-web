package com.nis.modules.project.controller;

import java.util.Arrays;
import java.util.Map;

import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.modules.project.entity.Project;
import com.nis.modules.project.service.ProjectService;
import com.nis.modules.sys.controller.AbstractController;

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
		ValidateUtils.is(project.getName()).notNull(RCode.PROJECT_NAME_ISNULL);
		ValidateUtils.is(project.getName()).maxLength(64,RCode.PROJECT_NAME_TOO_LONG);
		ValidateUtils.is(project.getRemark()).maxLength(1024,RCode.PROJECT_REMARK_TOO_LONG);

		// 名称唯一
		if(!projectService.validateProjectName(project.getId(), project.getName())){
			throw new NZException(RCode.PROJECT_NAME_DUPLICATE);
		}

		projectService.save(project);
		return R.ok();
	}
	
	@PutMapping
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.PROJECT)
	public R modify(@RequestBody Project project) {
		ValidateUtils.is(project.getId()).notNull(RCode.PROJECT_ID_ISNULL);
		projectService.validateBulitinRule(Arrays.asList(project.getId()));
		ValidateUtils.is(project.getName()).notNull(RCode.PROJECT_NAME_ISNULL);
		ValidateUtils.is(project.getName()).maxLength(64,RCode.PROJECT_NAME_TOO_LONG);
		ValidateUtils.is(project.getRemark()).maxLength(1024,RCode.PROJECT_REMARK_TOO_LONG);

		// 名称唯一
		if(!projectService.validateProjectName(project.getId(), project.getName())){
			throw new NZException(RCode.PROJECT_NAME_DUPLICATE);
		}

		projectService.updateById(project);
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
}

