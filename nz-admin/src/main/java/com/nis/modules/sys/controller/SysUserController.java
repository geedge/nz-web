package com.nis.modules.sys.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.common.utils.TypeEnum;
import com.nis.common.validator.Assert;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.sys.shiro.ShiroUtils;


@RestController
@RequestMapping("/sys/user")
public class SysUserController extends AbstractController {

	@Autowired
	private SysUserService sysUserService;

	@Autowired
	private SysConfigService sysConfigService;
	
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
	 * 修改登录用户密码
	 */
	@RequestMapping("/pin")
	public R password(Integer id,String oldPin, String newPin){
		Assert.isBlank(newPin, "新密码不为能空");
		SysUserEntity user = (id== null)? this.getUser() : sysUserService.getById(id);
		//原密码
		oldPin = ShiroUtils.sha256(oldPin, user.getSalt());
		//新密码
		newPin = ShiroUtils.sha256(newPin, user.getSalt());

		//更新密码
		boolean flag = sysUserService.updatePassword(user.getId(), oldPin, newPin);
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
		String mfaAuthEnable = sysConfigService.getValueOrDefault("mfa_auth_enable", "0");
		user.setMfaAuthEnable(Integer.valueOf(mfaAuthEnable));
		return R.ok().put("user", user);
	}
	/**
	 * 用户个人中心
	 * @return
	 */
	@GetMapping("/profile")
	public R profile() {
		Long userId = this.getUserId();
		return this.info(userId);
	}
	
	/**
	 * 用户修改个人信息
	 * @return
	 */
	@PutMapping("/profile")
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.ACCOUNT)
	public R profile(@RequestBody SysUserEntity user) {
		Integer id = sysUserService.update(user);
		return R.ok().putData("id", id);
	}
	
	@RequestMapping("/disableMfa")
	public R disableMfa() {
		Long userId = this.getUserId();
		String mfaAuthEnable = sysConfigService.getValueOrDefault("mfa_auth_enable", "0");
		if(Tool.StrUtil.equals("1", mfaAuthEnable)) {
			throw new NZException(RCode.SYS_USER_DISABLE_MFA_ERROR);
		}
		SysUserEntity userEntity = sysUserService.getById(userId);
		if(userEntity.getMfaLevel() == 3) {
			throw new NZException(RCode.SYS_USER_DISABLE_MFA_ERROR);
		}
		userEntity.setMfaLevel(0);
		userEntity.setOtpKey(Tool.StrUtil.EMPTY);
		userEntity.setRecoveryCode(Tool.StrUtil.EMPTY);
		sysUserService.updateById(userEntity);
		logger.info("userName:{},mfa disabled",userEntity.getUsername());
		return R.ok();
	}
	
	@RequestMapping("/resetMfa")
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.ACCOUNT)
    @RequiresPermissions("sys:user:update")
	public R resetMfa(String[] ids) {
		if(Tool.StrUtil.isAllBlank(ids)) {
			SysUserEntity entity = new SysUserEntity();
			entity.setOtpKey(Tool.StrUtil.EMPTY);
			entity.setRecoveryCode(Tool.StrUtil.EMPTY);
			boolean result = sysUserService.update(entity, new UpdateWrapper<SysUserEntity>().lambda().in(SysUserEntity::getId, ids));
			logger.info("reset mfa ids:{},result:{},mfa disabled",ids,result);
		}
		return R.ok();
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
