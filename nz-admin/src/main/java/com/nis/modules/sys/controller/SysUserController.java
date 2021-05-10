package com.nis.modules.sys.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.common.validator.Assert;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.sys.shiro.ShiroUtils;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sys/user")
public class SysUserController extends AbstractController {

	@Autowired
	private SysUserService sysUserService;

	/**
	 * 所有用户列表
	 */
	@RequestMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ACCOUNT)
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
	@RequestMapping("/pin")
	public R password(String oldPin, String newPin){
		Assert.isBlank(newPin, "新密码不为能空");

		//原密码
		oldPin = ShiroUtils.sha256(oldPin, getUser().getSalt());
		//新密码
		newPin = ShiroUtils.sha256(newPin, getUser().getSalt());

		//更新密码
		boolean flag = sysUserService.updatePassword(getUserId(), oldPin, newPin);
		if(!flag){
			throw new NZException(RCode.SYS_USER_OLDPWD);
		}

		return R.ok();
	}

	/**
	 * 用户信息
	*/ 
	@RequestMapping("/{id}")
	public R info(@PathVariable("id") Long id){
		ValidateUtils.is(id).notNull(RCode.SYS_USER_ID_ISNULL);
		SysUserEntity user = sysUserService.queryUserInfo(id);
		return R.ok().put("user", user);
	}

	/**
	 * 保存用户
	 */
	@PostMapping
	@SysLog(operation = OperationEnum.ADD,type = TypeEnum.ACCOUNT)
    @RequiresPermissions("sys:user:save")
	public R save(@RequestBody SysUserEntity user){
		Integer id = sysUserService.saveUser(user);
		return R.ok().putData("id", id);
	}

	/**
	 * 修改用户
	 */
	@PutMapping
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.ACCOUNT)
    @RequiresPermissions("sys:user:update")
	public R update(@RequestBody SysUserEntity user){
		ValidateUtils.is(user.getId()).notNull(RCode.SYS_USER_ID_ISNULL);

		Integer id = sysUserService.update(user);
		return R.ok().putData("id", id);
	}

	/**
	 * 删除用户
	 */
	@DeleteMapping
	@SysLog(operation = OperationEnum.DELETE,type = TypeEnum.ACCOUNT)
    @RequiresPermissions("sys:user:delete")
	public R delete(String ids) {
		sysUserService.deleteUsers(ids);
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
			queryParam.setId(Long.valueOf((String)object));
		}
		queryParam.setUsername((String)params.get("name"));

		if (sysUserService.checkName(queryParam)) {
			throw new NZException(RCode.SYS_USER_DUPLICATENAME);
		} else {
			return R.ok();
		}
	}

	/**
	 * 用户权限查询接口
	 * @param request
	 * @return
	 */
	@PostMapping("/permissions")
	public R permissions(HttpServletRequest request){
		return R.ok(sysUserService.getUserPermissions(request));
	}
}
