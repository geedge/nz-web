package com.nis.modules.sys.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.sys.entity.AuthTokenEntity;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.redis.SysLoginRedis;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.nis.setup.controller.TimezoneController;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mfa")
public class MfaController extends AbstractController {

	@Autowired
	private SysConfigService configService;
	@Autowired
	private SysUserService sysUserService;
	@Autowired
	private SysLoginRedis sysLoginRedis;

	@PostMapping(value = "/genkey")
	public R genKey(@RequestBody SysUserEntity sysUser) {

		SysUserEntity user = this.getUser();// 成功登录后，可以获取到用户信息
		String authToken = sysUser.getAuthToken();
		if (user == null && StringUtils.isBlank(authToken)) {
			return R.error(RCode.SYS_TOKEN_OR_AUTHTOKEN_ISNULL);
		}
		Map<String, Object> obj = new HashMap<>();
		// 更新authToken，生成authKey并保存
		String newAuthToken = MfaUtils.getAuthToken();
		String authKey = MfaUtils.generateSecretKey();// 是否需要存入用户表的otp_key:是
		AuthTokenEntity authTokenEntity = new AuthTokenEntity();
		if (user == null) {// 未成功登录，从 redis 中获取临时令牌
			authTokenEntity = sysLoginRedis.get(authToken);
			if (authTokenEntity == null) {
				return R.error(RCode.SYS_AUTHTOKEN_ISVALID);
			}
			user = sysUserService.getById(authTokenEntity.getUserId());
		}
		// 组织响应内容
		String userName = user.getUsername();
		obj.put("authToken", newAuthToken);
		obj.put("authKey", authKey);
		obj.put("username", userName);
		obj.put("otpauth", MfaUtils.getAuthenticatorBarCode(authKey, userName));
		// 更新 redis 缓存的 authToken 信息
		boolean r = updateAuthToken(authToken, user.getId(), newAuthToken, authKey, sysUser.getMfaLevel());
		if(!r) {
			return R.error(RCode.SYS_TOKEN_REFRESH_FREQUENT);// 超过10 响应操作频繁，请30分钟后重试
		}
		return R.ok(obj);
	}

	private boolean updateAuthToken(String oldAuthToken, Long userId, String newAuthToken, String authKey,
			Integer mfaLevel) {
		// 更新authToken
		AuthTokenEntity authTokenEntity = sysLoginRedis.get(oldAuthToken);
		if (authTokenEntity == null) {
			authTokenEntity = new AuthTokenEntity();
			authTokenEntity.setUserId(userId);
			authTokenEntity.setAuthKey(authKey);
			authTokenEntity.setC(1);
			authTokenEntity.setMfaLevel(mfaLevel);
			sysLoginRedis.saveOrUpdate(authTokenEntity, 30l * 60l, newAuthToken);
		} else {
			int c = authTokenEntity.getC();
			if (c >= 10) {
				return false;
			}
			authTokenEntity.setUserId(userId);
			authTokenEntity.setAuthKey(authKey);
			authTokenEntity.setC(c + 1);
			authTokenEntity.setMfaLevel(mfaLevel);
			sysLoginRedis.delete(oldAuthToken);
			sysLoginRedis.saveOrUpdate(authTokenEntity, 30l * 60l, newAuthToken);
		}

		return true;
	}

	@PostMapping(value = "/bind")
	public R bind(@RequestBody SysUserEntity sysUser) {
		Map<String, Object> result = new HashMap<>();
		String authToken = sysUser.getAuthToken();
		String authCode = sysUser.getAuthCode();// 验证码
		ValidateUtils.is(authToken).notNull(RCode.SYS_AUTHTOKEN_ISNULL).and(authCode)
				.notNull(RCode.SYS_AUTHCODE_ISNULL);

		AuthTokenEntity authTokenEntity = sysLoginRedis.get(authToken);
		if (authTokenEntity == null) {
			return R.error(RCode.SYS_AUTHTOKEN_ISVALID);
		}
		
		String authKey = authTokenEntity.getAuthKey();
		Integer mfaLevel = authTokenEntity.getMfaLevel();
		Long userId = authTokenEntity.getUserId();
		
		//authKey + authCode 校验
		boolean validateTOTPCode = MfaUtils.validateTOTPCode(authKey, authCode);
		if(validateTOTPCode) {//校验成功
			// 验证成功后 生成10组recovery code
			List<String> list = MfaUtils.getRecoveryCode();
			result.put("recoveryCode", list);
			// 并保存recovery code到 sys_user
			SysUserEntity entity = new SysUserEntity();
			entity.setRecoveryCode(StrUtil.join(",", list));
			entity.setOtpKey(authKey);
			if ( mfaLevel != null) {
				entity.setMfaLevel(mfaLevel);
			}
			sysUserService.update(entity, new UpdateWrapper<SysUserEntity>().lambda().eq(SysUserEntity::getId, userId));
			sysLoginRedis.delete(authToken);
			return R.ok(result);
		}
		/**
		 * 校验失败 处理
		 */
		//重新生成 authToken,authKey
		String newAuthToken = MfaUtils.getAuthToken();
		// 更新 redis 缓存的 authToken 信息
		boolean r = updateAuthToken(authToken, userId, newAuthToken, authKey, mfaLevel);
		if(!r) {
			return R.error(RCode.SYS_TOKEN_REFRESH_FREQUENT);// 超过10 响应操作频繁，请30分钟后重试
		}
		return R.error(RCode.SYS_AUTHCODE_IS_INVALID).putData("authToken", newAuthToken);
	}

	@PostMapping(value = "/verify")
	@SysLog(operation = OperationEnum.LOGIN, type = TypeEnum.SYSTEM)
	public R verify(@RequestBody SysUserEntity sysUser) {
		String authToken = sysUser.getAuthToken();
		String authCode = sysUser.getAuthCode();// 验证码（超过6位是 recovery code）
		ValidateUtils.is(authToken).notNull(RCode.SYS_AUTHTOKEN_ISNULL);
		
		AuthTokenEntity authTokenEntity = sysLoginRedis.get(authToken);
		if (authTokenEntity == null ) {
			return R.error(RCode.SYS_AUTHTOKEN_ISVALID);
		}
		Long userId = authTokenEntity.getUserId();
		Map<String, Object> obj = new HashMap<>();
		String newAuthToken = MfaUtils.getAuthToken();
		
		if(Tool.StrUtil.isBlank(authCode) || authCode.length() < 6) {
			return R.error(RCode.SYS_AUTHCODE_IS_INVALID).putData("authToken", newAuthToken);
		}
		//获取用户信息
		SysUserEntity user = sysUserService.getById(userId);
		if(authCode.length() == 6) {
			//authKey + authCode 校验
			boolean validateTOTPCode = MfaUtils.validateTOTPCode(user.getOtpKey(), authCode);
			if(!validateTOTPCode) {
				boolean r = updateAuthToken(authToken, userId, newAuthToken, null, null);
				if(!r) {
					return R.error(RCode.SYS_TOKEN_REFRESH_FREQUENT);// 超过10 响应操作频繁，请30分钟后重试
				}
				return R.error(RCode.SYS_AUTHCODE_IS_INVALID).putData("authToken", newAuthToken);
			}
		}else {
			// 超过6位是 recovery code,recovery code 一个只能用一次？？
			String recoveryCodes = user.getRecoveryCode();
			List<String> recoveryCodeList = StrUtil.split(recoveryCodes, ',');
			//不是 recovery code
			if (!recoveryCodeList.contains(authCode)) {
				boolean r = updateAuthToken(authToken, userId, newAuthToken, null, null);
				if(!r) {
					return R.error(RCode.SYS_TOKEN_REFRESH_FREQUENT);// 超过10 响应操作频繁，请30分钟后重试
				}
				return R.error(RCode.SYS_AUTHCODE_IS_INVALID).putData("authToken", newAuthToken);
			}else {
				//更新 recovery code
				recoveryCodeList.remove(authCode);
				SysUserEntity entity = new SysUserEntity();
				entity.setRecoveryCode(StrUtil.join(",", recoveryCodeList));
				sysUserService.update(entity,new UpdateWrapper<SysUserEntity>().lambda().eq(SysUserEntity::getId, userId));
			}
		}
		sysLoginRedis.delete(authToken);
		//校验通过，返回登录成功token
		String token = authTokenEntity.getToken();
		obj.put("token", token);
		SysUserEntity userEntity = ShiroUtils.getUserEntityBySessionId(token);
		// 查询系统配置信息
		String[] paramkeys = {"system_name", "system_logo", "timezone", "default_cabinet_usize", "map_center_config", "unsaved_change","prometheus_federation_enabled","mfa_auth_enable"};
		List<SysConfigEntity> configEntityList = configService.list(new LambdaUpdateWrapper<SysConfigEntity>().in(SysConfigEntity::getParamKey, paramkeys));
		for (SysConfigEntity configEntity : configEntityList) {
			if ("timezone".equals(configEntity.getParamKey())) {
				TimezoneController.Timezone timezoneEntity = TimezoneController.queryTimezoneByName(configEntity.getParamValue());
				obj.put("offset", timezoneEntity.getOffset());
			}
			obj.put(ToolUtil.toCamelCase(configEntity.getParamKey()), configEntity.getParamValue());
		}

        // 获取request
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();            
        userEntity.setLastLoginTime(new Date());
        // 设置IP地址
        userEntity.setLastLoginIp(IPUtils.getIpAddr(request));
		// 设置 recoveryCode 为 null，避免重复更新 recoveryCode 值
		userEntity.setRecoveryCode(null);
        sysUserService.updateById(userEntity);
		return R.ok(obj);
	}
}
