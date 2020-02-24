package com.nis.modules.sys.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.modules.sys.service.SysAreaService;

/**
 * 区域
 * @author Th
 *
 */
@RestController
@RequestMapping("/area")
public class SysAreaController {
	@Autowired
	private SysAreaService sysAreaService;
	@GetMapping
	public R list(@RequestParam Map<String, Object> params){
		PageUtils page = sysAreaService.queryPage(params);
		return R.ok(page);
	}
	
}
