package com.nis.modules.project.controller;

import java.util.Arrays;
import java.util.Map;

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
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.project.entity.Project;
import com.nis.modules.project.service.ProjectService;
import com.nis.modules.sys.controller.AbstractController;

@RestController
@RequestMapping("/project")
public class ProjectController extends AbstractController {
	
	@Autowired
	private ProjectService projectService;
	
	@GetMapping
	public R list(@RequestParam Map<String, Object> params){
		PageUtils page = projectService.queryPage(params);
		return R.ok(page);
	}
	
	@PostMapping
	@SysLog("save project")
	public R save(@RequestBody Project systemEntity) {
		ValidateUtils.is(systemEntity.getName()).notNull(RCode.PROJECT_NAME_ISNULL);
		projectService.save(systemEntity);
		return R.ok();
	}
	
	@PutMapping
	@SysLog("update project")
	public R modify(@RequestBody Project systemEntity) {
		ValidateUtils.is(systemEntity.getId()).notNull(RCode.PROJECT_ID_ISNULL);
		ValidateUtils.is(systemEntity.getName()).notNull(RCode.PROJECT_NAME_ISNULL);
		projectService.updateById(systemEntity);
		return R.ok();
	}
	
	@DeleteMapping
	@SysLog("delete project")
	public R remove(String ids) {
		ValidateUtils.is(ids).notNull(RCode.PROJECT_ID_ISNULL);
		String[] split = ids.split(",");
		projectService.removeByIds(Arrays.asList(split));
		return R.ok();
	}
}

