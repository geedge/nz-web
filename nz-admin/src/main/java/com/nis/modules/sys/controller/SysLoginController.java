/**

 *

 *

 */

package com.nis.modules.sys.controller;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.code.kaptcha.Constants;
import com.google.code.kaptcha.Producer;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
		String password = sysUser.getPassword();
	//public R login( String username, String password, String captcha) {
		/*String kaptcha = ShiroUtils.getKaptcha(Constants.KAPTCHA_SESSION_KEY);
		if(!captcha.equalsIgnoreCase(kaptcha)){
			return R.error(RCode.SYS_LOGIN_CAPTCHA);
		}*/
		try{
			Subject subject = ShiroUtils.getSubject();
			UsernamePasswordToken token = new UsernamePasswordToken(username, password);
			subject.login(token); //登录
			String loginToken=CommonUtils.uuid();
			subject.getSession().setAttribute("token",loginToken);
			Map<String,String> obj=new HashMap<String,String>();
			obj.put("token",loginToken);
			ValueOperations<String, String> ops = redisTemplate.opsForValue();
			HashMap<String, String> map = new HashMap<String,String>();
			map.put("username",username);
			map.put("password",password);
			String json=JSON.toJSONString(map);
			String value = Base64.getEncoder().encodeToString(json.getBytes());
			ops.set(loginToken,value ,this.loginExpiration, TimeUnit.MINUTES);
			// 查询系统配置信息
			String [] paramkeys = {"system_name","timezone","default_cabinet_uszie","max_terminal_num","asset_ping_switch"};
			List<SysConfigEntity> configEntityList = configService.list(new LambdaUpdateWrapper<SysConfigEntity>().in(SysConfigEntity::getParamKey,paramkeys));
			Constant.loginUsers.add(username);
            configEntityList.forEach(configEntity -> obj.put(ToolUtil.toCamelCase(configEntity.getParamKey()),configEntity.getParamValue()));
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

		SysUserEntity userEntity = (SysUserEntity) SecurityUtils.getSubject().getPrincipal();
		Constant.loginUsers.remove(userEntity.getUsername());

		ShiroUtils.logout();
		String requestToken=request.getHeader(Constant.AUTH_TOKEN_CODE);
		if(StringUtils.isBlank(requestToken)){
			requestToken=request.getParameter(Constant.AUTH_TOKEN_CODE);
		}
		try {
			redisTemplate.delete(requestToken);
		}catch (Exception e){
			logger.debug("登出操作删除redis信息失败，失败key：{}",requestToken);
		}


		return R.ok();
	}

}
