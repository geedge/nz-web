/**

 *

 *

 */

package com.nis.modules.sys.controller;

import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.xss.XssUtil;
import com.nis.modules.sys.service.SysLogService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


/**
 * 系统日志
 *

 */
@RestController
@RequestMapping("/sys/log")
public class SysLogController {
	@Autowired
	private SysLogService sysLogService;
	
	/**
	 * 列表
	 */
	@GetMapping
	public R list(@RequestParam Map<String, Object> params){
		params = XssUtil.filterParameters(params);
		PageUtils page = sysLogService.queryPage(params);

		return R.ok(page);
	}
	
}
