package com.nis.modules.sys.controller;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.sys.entity.SystemInfoEntity;
import com.nis.modules.sys.service.SystemInfoService;
import com.nis.modules.sys.shiro.ShiroUtils;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@RequestMapping("/system")
public class SystemInfoController extends AbstractController {
	
	@Autowired
	private SystemInfoService systemInfoService;
	
	@RequestMapping("/list")
	@ApiOperation(httpMethod = "GET", value = "业务系统列表查询")
	public R list(@RequestParam Map<String, Object> params){
		PageUtils queryPage = systemInfoService.queryPage(params);
		return R.ok(queryPage);
	}
	
	@RequestMapping("/detail")
	@ApiOperation(httpMethod = "GET", value = "业务系统详情查询")
	public R detail(@ApiParam @RequestParam Long id) {
		ValidateUtils.is(id).notNull(RCode.NOT_NULL_ERROR);
		SystemInfoEntity queryDetail = systemInfoService.queryDetail(id);
		return R.ok(queryDetail);
	}
	
	@RequestMapping("/save")
	@ApiOperation(httpMethod = "POST", response = R.class, value = "保存业务系统")
	public R save(@ApiParam SystemInfoEntity systemInfo){
		//global限制
		if (!ShiroUtils.isGlobal()) {
			return R.error(RCode.NO_PERMISSION_ERROR);
		}
		
		ValidateUtils.is(systemInfo.getName()).notNull(RCode.SYS_SYS_NULLNAME);
		//校验名称重复
		if (systemInfoService.checkName(systemInfo)) {
			return R.error(RCode.SYS_SYS_DUPLICATENAME);
		}
		
		Date now = new Date();
		
		systemInfo.setCreateBy(getUserId());
		systemInfo.setUpdateBy(getUserId());
		systemInfo.setCreateDate(now);
		systemInfo.setUpdateDate(now);
		
		systemInfoService.save(systemInfo);
		
		return R.ok();
	}
	
	@RequestMapping("/update")
	@ApiOperation(httpMethod = "PUT", response = R.class, value = "修改业务系统")
	public R update(@ApiParam SystemInfoEntity systemInfo){
		//global限制
		if (!ShiroUtils.isGlobal()) {
			return R.error(RCode.NO_PERMISSION_ERROR);
		}
		
		ValidateUtils.is(systemInfo.getName()).notNull(RCode.SYS_SYS_NULLNAME)
				.and(systemInfo.getId()).notNull(RCode.NOT_NULL_ERROR);
		//校验名称重复
		if (systemInfoService.checkName(systemInfo)) {
			return R.error(RCode.SYS_SYS_DUPLICATENAME);
		}
		
		Date now = new Date();
		
		systemInfo.setUpdateBy(getUserId());
		systemInfo.setUpdateDate(now);
		
		systemInfoService.updateById(systemInfo);
		
		return R.ok();
	}
	
	@RequestMapping("/delete")
	@ApiOperation(httpMethod = "DELETE", response = R.class, value = "删除业务系统")
	public R delete(@ApiParam @RequestParam Long id){
		ValidateUtils.is(id).notNull(RCode.NOT_NULL_ERROR);
		if (!ShiroUtils.isGlobal()) {//global限制
			return R.error(RCode.NO_PERMISSION_ERROR);
		}
		if (id == Constant.GLOBAL_ID) {//不能删除global
			return R.error(RCode.SYS_SYS_DELGLOBAL);
		}
		systemInfoService.delete(id);
		return R.ok();
	}
}