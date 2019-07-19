package com.nis.modules.sys.controller;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.sys.entity.SystemInfoEntity;
import com.nis.modules.sys.service.SystemInfoService;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@RequestMapping("/system")
public class SystemInfoController extends AbstractController {
	
	@Autowired
	private SystemInfoService systemInfoService;
	
	@RequestMapping("/list")
	public R list(@RequestParam Map<String, Object> params){
		//单独校验的方式：
		//如下面的写法就是校验usergroupId非空、page非空且最大值30
		ValidateUtils.check(params.get("usergroupId")).notNull(RCode.ERROR)
					.and(params.get("page")).notNull();

		return R.ok().put("page", null);
	}
	
	@RequestMapping("/save")
	@ApiOperation(httpMethod = "POST", response = SystemInfoEntity.class, value = "保存业务系统")
	public R save(@ApiParam @RequestBody SystemInfoEntity systemInfo){
		ValidateUtils.is(systemInfo.getName()).notNull(RCode.SYS_SYS_NULLNAME);
		
		Date now = new Date();
		
		systemInfo.setCreateBy(getUserId());
		systemInfo.setUpdateBy(getUserId());
		systemInfo.setCreateDate(now);
		systemInfo.setUpdateDate(now);
		
		systemInfoService.save(systemInfo);
		
		return R.ok();
	}
}