package com.nis.modules.sys.controller;


import com.nis.common.exception.NZException;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.validator.Assert;

import org.apache.commons.lang.ArrayUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysUserRoleService;
import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.sys.shiro.ShiroUtils;

import io.swagger.annotations.ApiOperation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 系统用户
 *

 */
@RestController
@RequestMapping("/sys/user")
public class SysUserController extends AbstractController {
	@Autowired
	private SysUserService sysUserService;
	@Autowired
	private SysUserRoleService sysUserRoleService;
	
	/**
	 * 所有用户列表
	 */
	@RequestMapping("/list")
//	@RequiresPermissions("sys:user:list")
	public R list(@RequestParam Map<String, Object> params){
		PageUtils page = sysUserService.queryPage(params);

		return R.ok(page);
	}
	
	/**
	 * 获取登录的用户信息
	 */
	@RequestMapping("/info")
	public R info(){
		return R.ok().put("user", getUser());
	}
	
	/**
	 * 修改登录用户密码
	 */
	@RequestMapping("/password")
	public R password(String password, String newPassword){
		Assert.isBlank(newPassword, "新密码不为能空");

		//原密码
		password = ShiroUtils.sha256(password, getUser().getSalt());
		//新密码
		newPassword = ShiroUtils.sha256(newPassword, getUser().getSalt());
				
		//更新密码
		boolean flag = sysUserService.updatePassword(getUserId(), password, newPassword);
		if(!flag){
			throw new NZException(RCode.SYS_USER_OLDPWD);
		}
		
		return R.ok();
	}
	
	/**
	 * 用户信息
	 */
	@RequestMapping("/info/{userId}")
//	@RequiresPermissions("sys:user:info")
	public R info(@PathVariable("userId") Long userId){
		SysUserEntity user = sysUserService.getById(userId);
		
		//获取用户所属的角色列表
//		List<Long> roleIdList = sysUserRoleService.queryRoleIdList(userId);
//		user.setRoleIdList(roleIdList);
		
		return R.ok().put("user", user);
	}
	
	/**
	 * 保存用户
	 */
	@RequestMapping("/save")
//	@RequiresPermissions("sys:user:save")
	public R save(@RequestBody SysUserEntity user){
		ValidateUtils.is(user.getUsername()).notNull().maxLength(64)
						.and(user.getPassword()).notNull().maxLength(64)
						.and(user.getEmail()).notNull().email();
		sysUserService.saveUser(user);
		
		return R.ok();
	}
	
	/**
	 * 修改用户
	 */
	@RequestMapping("/update")
//	@RequiresPermissions("sys:user:update")
	public R update(@RequestBody SysUserEntity user){
		ValidateUtils.is(user.getUsername()).notNull().max(64)
		.and(user.getEmail()).notNull().email();

		sysUserService.update(user);
		
		return R.ok();
	}
	
	/**
	 * 删除用户
	 */
	@RequestMapping("/delete")
//	@RequiresPermissions("sys:user:delete")
//	public R delete(@RequestBody Long[] userIds){
	public R delete(String userIds) {
		ValidateUtils.is(userIds).notNull();
		String[] split = userIds.split(",");
		if(ArrayUtils.contains(split, "1")){
			throw new NZException(RCode.SYS_USER_DELADMIN);
		}
		
		if(ArrayUtils.contains(split, getUserId().toString())){
			throw new NZException(RCode.SYS_USER_CANNOTDEL);
		}

		sysUserService.removeByIds(Arrays.asList(split));
		
		return R.ok();
	}
	
	/**
	 * 用户名称校验
	 * @param params
	 * @return
	 */
	@RequestMapping(value = "nameCheck", method = RequestMethod.GET)
	@ApiOperation(httpMethod = "GET", response = R.class, value = "用户名称校验")
	public R nameCheck(@RequestParam Map<String, Object> params) {
		ValidateUtils.is(params.get("name")).notNull();
		//校验名称重复
		SysUserEntity queryParam = new SysUserEntity();
		Object object = params.get("id");
		if (object != null && !StringUtils.isEmpty(object.toString())) {
			queryParam.setUserId(Long.valueOf((String)object));
		}
		queryParam.setUsername((String)params.get("name"));
		
		if (sysUserService.checkName(queryParam)) {
			throw new NZException(RCode.SYS_USER_DUPLICATENAME);
		} else {
			return R.ok();
		}
	}
}
