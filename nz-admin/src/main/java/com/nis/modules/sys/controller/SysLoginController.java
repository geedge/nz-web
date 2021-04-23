package com.nis.modules.sys.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.code.kaptcha.Constants;
import com.google.code.kaptcha.Producer;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.entity.Timezone;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.sys.service.TimezoneService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	@Value(value="${nezha.loginExpiration:30}")
	private Integer loginExpiration;
	@Autowired
	private SysConfigService configService;
	@Autowired
	private SysUserService sysUserService;

	@Autowired
	private TimezoneService timezoneService;
	
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
		try{
			Subject subject = ShiroUtils.getSubject();
			UsernamePasswordToken token = new UsernamePasswordToken(username, pin);
			subject.login(token); //登录
			String loginToken=CommonUtils.uuid();
			subject.getSession().setAttribute("token",loginToken);
			Map<String, Object> obj = new HashMap<>();
			obj.put("token", subject.getSession().getId().toString());
			// 查询系统配置信息
			String[] paramkeys = {"system_name", "system_logo", "timezone", "default_cabinet_usize", "map_center_config", "unsaved_change"};
			List<SysConfigEntity> configEntityList = configService.list(new LambdaUpdateWrapper<SysConfigEntity>().in(SysConfigEntity::getParamKey, paramkeys));
			for (SysConfigEntity configEntity : configEntityList) {
				if ("timezone".equals(configEntity.getParamKey())) {
					Timezone timezoneEntity = timezoneService.getOne(new LambdaUpdateWrapper<Timezone>().eq(Timezone::getName, configEntity.getParamValue()));
					obj.put("offset", timezoneEntity.getOffset());
				}
				obj.put(ToolUtil.toCamelCase(configEntity.getParamKey()), configEntity.getParamValue());
			}

            // 获取request
            HttpServletRequest request = HttpContextUtils.getHttpServletRequest();            
            SysUserEntity userEntity = ShiroUtils.getUserEntity();
            userEntity.setLastLoginTime(DateUtil.getUTCTimeByConfigTimeZone());
            // 设置IP地址
            userEntity.setLastLoginIp(IPUtils.getIpAddr(request));
            sysUserService.updateById(userEntity);
            return R.ok(obj);
		}catch (UnknownAccountException e) {
			throw new NZException(RCode.SYS_LOGIN_UNKNOWNACCOUNT);
		}catch (IncorrectCredentialsException e) {
			throw new NZException(RCode.SYS_LOGIN_USERPWD);
		}catch (LockedAccountException e) {
			throw new NZException(RCode.SYS_LOGIN_LOCK);
		}catch (AuthenticationException e) {
			throw new NZException(RCode.SYS_LOGIN_ACCOUNTAUTH);
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
