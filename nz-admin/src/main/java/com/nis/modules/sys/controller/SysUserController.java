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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/sys/user")
public class SysUserController extends AbstractController {

	@Autowired
	private SysUserService sysUserService;

	/**
	 * 所有用户列表
	 */
	@RequestMapping("/list")
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
	@PostMapping("/save")
	@SysLog(operation = OperationEnum.ADD,type = TypeEnum.ACCOUNT)
	public R save(@RequestBody SysUserEntity user){
		sysUserService.saveUser(user);
		return R.ok();
	}

	/**
	 * 修改用户
	 */
	@PutMapping("/update")
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.ACCOUNT)
	public R update(@RequestBody SysUserEntity user){
		ValidateUtils.is(user.getUserId()).notNull(RCode.SYS_USER_ID_ISNULL);

		sysUserService.update(user);
		return R.ok();
	}

	/**
	 * 删除用户
	 */
	@DeleteMapping("/delete")
	@SysLog(operation = OperationEnum.DELETE,type = TypeEnum.ACCOUNT)
	public R delete(String userIds) {
		sysUserService.deleteUsers(userIds);
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
