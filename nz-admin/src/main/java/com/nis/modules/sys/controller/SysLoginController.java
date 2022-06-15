package com.nis.modules.sys.controller;


import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.hutool.core.util.ObjectUtil;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.code.kaptcha.Constants;
import com.google.code.kaptcha.Producer;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.utils.CommonUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.HttpContextUtils;
import com.nis.common.utils.IPUtils;
import com.nis.common.utils.MfaUtils;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.sys.entity.AuthTokenEntity;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.entity.Timezone;
import com.nis.modules.sys.redis.SysLoginRedis;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.sys.service.TimezoneService;
import com.nis.modules.sys.shiro.ShiroUtils;

import cn.hutool.core.util.StrUtil;

/**
 * 登录相关
 *

 */
@Controller
public class SysLoginController extends AbstractController {
	@Autowired
	private Producer producer;
	@Autowired
	private RedisTemplate<String,String> redisTemplate;
	@Autowired
	private SysConfigService configService;
	@Autowired
	private SysUserService sysUserService;
	@Autowired
	private SysLoginRedis sysLoginRedis;

	@Autowired
	private TimezoneService timezoneService;

	@Autowired
	private SysConfigService sysConfigService;
	
	@RequestMapping("captcha.jpg")
	public void captcha(HttpServletResponse response)throws IOException {
        response.setHeader("Cache-Control", "no-store, no-cache");
        response.setContentType("image/jpeg");

        //生成文字验证码
        String text = producer.createText();
        //生成图片验证码
        BufferedImage image = producer.createImage(text);
        //保存到shiro session
        ShiroUtils.setSessionAttribute(Constants.KAPTCHA_SESSION_KEY, text);

        ServletOutputStream out = response.getOutputStream();
        ImageIO.write(image, "jpg", out);
	}

	/**
	 * 登录
	 */
	@ResponseBody
	@RequestMapping(value = "/sys/login", method = RequestMethod.POST)
	@SysLog(operation = OperationEnum.LOGIN,type = TypeEnum.SYSTEM)
	public R login(@RequestBody SysUserEntity sysUser) {
		String username = sysUser.getUsername();
		String pin = StrUtil.str(sysUser.getPin());
		Map<String, Object> result = new HashMap<>();
		boolean flag = false;
		try{
			SysUserEntity user = sysUserService.getOne(new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getName, username));
			if(ToolUtil.isNotEmpty(user)) {
				sysUser.setPinTime(user.getPinTime());
			}else {
				sysUser.setPinTime(new Date());
			}
			sysUserService.checkPin(sysUser,true);
			Subject subject = ShiroUtils.getSubject();
			UsernamePasswordToken token = new UsernamePasswordToken(username, pin);
			subject.login(token); //登录
			String loginToken=CommonUtils.uuid();
			subject.getSession().setAttribute("token",loginToken);
			String sessionId = subject.getSession().getId().toString();
			String mfaAuthEnableStr = configService.getValueOrDefault("mfa_auth_enable", "0");
			int mfaAuthEnable = StrUtil.isEmpty(mfaAuthEnableStr) ? 0 : Integer.valueOf(mfaAuthEnableStr);
			//登录成功后: sys_user.mfa_level > 0 || sys_config.mfa_auth_enable == 1 则响应 authFlag=1 & 生成 authToken （临时token，储存到redis，过期时间 30min）
			SysUserEntity u = sysUserService.getOne(new QueryWrapper<SysUserEntity>().lambda().eq(SysUserEntity::getUsername, username));
			int mfaLevel = u.getMfaLevel();
			String otpKey = u.getOtpKey();
			
			int authFlag = 0;
			int authBind = 0;
			if(mfaAuthEnable == 1 || mfaLevel > 0) {
				authFlag = 1;
				authBind = Tool.StrUtil.isBlank(otpKey)?1:0;
				String authToken = MfaUtils.getAuthToken();
				//临时token，储存到redis，过期时间 30min
				AuthTokenEntity authTokenEntity = new AuthTokenEntity();
				authTokenEntity.setUserId(u.getId());
				authTokenEntity.setAuthKey(u.getOtpKey());
				authTokenEntity.setC(1);
				authTokenEntity.setToken(sessionId);
				sysLoginRedis.saveOrUpdate(authTokenEntity,30l*60l,authToken);
				//需要mfa 验证
				result.put("authToken", authToken);
			}else {
				result.put("token", subject.getSession().getId().toString());
				 // 获取request
	            HttpServletRequest request = HttpContextUtils.getHttpServletRequest();            
	            SysUserEntity userEntity = ShiroUtils.getUserEntity();
	            userEntity.setLastLoginTime(new Date());
	            // 设置IP地址
	            userEntity.setLastLoginIp(IPUtils.getIpAddr(request));
	            sysUserService.updateById(userEntity);
			}
			result.put("authFlag", authFlag);
			result.put("authBind", authBind);
			result.put("mfaAuthEnable", mfaAuthEnable);
			result.put("user", u);
			// 查询系统配置信息
			String[] paramkeys = {"system_name", "system_logo", "timezone", "default_cabinet_usize", "map_center_config", "unsaved_change","prometheus_federation_enabled"};
			List<SysConfigEntity> configEntityList = configService.list(new LambdaUpdateWrapper<SysConfigEntity>().in(SysConfigEntity::getParamKey, paramkeys));
			for (SysConfigEntity configEntity : configEntityList) {
				if ("timezone".equals(configEntity.getParamKey())) {
					Timezone timezoneEntity = timezoneService.getOne(new LambdaUpdateWrapper<Timezone>().eq(Timezone::getName, configEntity.getParamValue()));
					result.put("offset", timezoneEntity.getOffset());
				}
				result.put(ToolUtil.toCamelCase(configEntity.getParamKey()), configEntity.getParamValue());
			}

			String pinPolicy = sysConfigService.getValue("pin_policy");
			if(ToolUtil.isNotEmpty(pinPolicy)) {
				Map<String,Object> pinPolicyInfo = (Map<String, Object>) JSON.parse(pinPolicy);
				// 密码有效时间
				Integer valid = (Integer) pinPolicyInfo.get("valid");
				Integer remind = (Integer) pinPolicyInfo.get("remind");
				remind = remind == null ? 3 : remind;
				if(valid > 0){
					long pwdExpiredTime = DateUtils.MILLIS_PER_DAY * valid - Tool.DateUtil.betweenMs(sysUser.getPinTime(),new Date());
					result.put("pwdExpiredTime",pwdExpiredTime);
					//pwdExpiredTime 低于三天给出提醒
					if (pwdExpiredTime < DateUtils.MILLIS_PER_DAY * remind){
						result.put("pwdRemindTime",pwdExpiredTime);
					}
				}else {
					//一直有效
					result.put("pwdExpiredTime",-1);
				}
			}
			// 登录成功标识
			flag = true;
            return R.ok(result);
		}catch (UnknownAccountException e) {
			throw new NZException(RCode.SYS_LOGIN_UNKNOWNACCOUNT);
		}catch (IncorrectCredentialsException e) {
			throw new NZException(RCode.SYS_LOGIN_USERPWD);
		}catch (LockedAccountException e) {
			throw new NZException(RCode.SYS_LOGIN_LOCK);
		}catch (AuthenticationException e) {
			throw new NZException(RCode.SYS_LOGIN_ACCOUNTAUTH);
		}finally {
			if(!flag) {
				sysUserService.checkRedisRetry(sysUser);
			}else {
				// 登录成功清除可能之前密码错误保留的缓存
				String redisKey = String.format("sysUserLogin_%s", sysUser.getUsername());
				redisTemplate.delete(redisKey);
			}
		}
	}


	/**
	 * 退出
	 */
	@RequestMapping(value = "logout", method = RequestMethod.GET)
	@ResponseBody
	@SysLog(operation = OperationEnum.LOGOUT,type = TypeEnum.SYSTEM)
	public R logout(HttpServletRequest request) {
		ShiroUtils.logout();
		String requestToken=request.getHeader(Constant.AUTH_TOKEN_CODE);
		if(StringUtils.isBlank(requestToken)){
			requestToken=request.getParameter(Constant.AUTH_TOKEN_CODE);
		}
		try {
			redisTemplate.delete(requestToken);
		}catch (RuntimeException e){
				logger.debug("登出操作删除redis信息失败，失败key：{}",requestToken);
		}
		return R.ok();
	}

}
