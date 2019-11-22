package com.nis.modules.system.controller;

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
import com.nis.modules.sys.controller.AbstractController;
import com.nis.modules.system.entity.SystemEntity;
import com.nis.modules.system.service.SystemService;

@RestController
@RequestMapping("/system")
public class SystemController extends AbstractController {
	
	@Autowired
	private SystemService systemService;
	
	/**
	 * 所有配置列表
	 */
	@GetMapping
	public R list(@RequestParam Map<String, Object> params){
		PageUtils page = systemService.queryPage(params);
		return R.ok().put("page", page);
	}
	
	@PostMapping
	public R save(@RequestBody SystemEntity systemEntity) {
		ValidateUtils.is(systemEntity.getName()).notNull(RCode.SYSTEM_NAME_ISNULL);
		systemService.save(systemEntity);
		return R.ok();
	}
	
	@PutMapping
	public R modify(@RequestBody SystemEntity systemEntity) {
		ValidateUtils.is(systemEntity.getId()).notNull(RCode.SYSTEM_ID_ISNULL);
		ValidateUtils.is(systemEntity.getName()).notNull(RCode.SYSTEM_NAME_ISNULL);
		systemService.updateById(systemEntity);
		return R.ok();
	}
	
	@DeleteMapping
	public R remove(@RequestBody Long[] ids) {
		ValidateUtils.is(ids).notNull(RCode.SYSTEM_ID_ISNULL);
		systemService.removeByIds(Arrays.asList(ids));
		return R.ok();
	}
}

