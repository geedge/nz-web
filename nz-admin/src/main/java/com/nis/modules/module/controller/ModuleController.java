package com.nis.modules.module.controller;

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

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.module.entity.ModuleEntity;
import com.nis.modules.module.service.ModuleService;
import com.nis.modules.sys.controller.AbstractController;

@RestController
@RequestMapping("/module")
public class ModuleController extends AbstractController {
	
	@Autowired
	private ModuleService moduleService;
	
	@GetMapping
	public R list(@RequestParam Map<String, Object> params){
		PageUtils page = moduleService.queryPage(params);
		return R.ok().put("page", page);
	}
	
	@PostMapping
	public R save(@RequestBody ModuleEntity moduleEntity) {
		moduleService.validateParam(moduleEntity);
		moduleService.save(moduleEntity);
		return R.ok();
	}
	
	@PutMapping
	public R modify(@RequestBody ModuleEntity moduleEntity) {
		ValidateUtils.is(moduleEntity.getId()).notNull(RCode.MODULE_ID_ISNULL);
		moduleService.validateParam(moduleEntity);
		moduleService.updateById(moduleEntity);
		return R.ok();
	}
	
	@DeleteMapping
	public R remove(@RequestBody Long[] ids) {
		ValidateUtils.is(ids).notNull(RCode.MODULE_ID_ISNULL);
		moduleService.removeByIds(Arrays.asList(ids));
		return R.ok();
	}
}
