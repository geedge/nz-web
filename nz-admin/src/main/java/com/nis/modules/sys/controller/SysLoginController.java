/**

 *

 *

 */

package com.nis.modules.sys.controller;


import com.google.code.kaptcha.Constants;
import com.google.code.kaptcha.Producer;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RedisUtils;
import com.nis.modules.romote.util.CommonUtils;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.subject.Subject;
import org.apache.tomcat.util.http.RequestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import sun.security.provider.MD5;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
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
	public R login(HttpServletRequest request, String username, String password, String captcha) {
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
			String romoteIp=request.getRemoteAddr();
			obj.put("token",loginToken);
			ValueOperations<String, String> ops = redisTemplate.opsForValue();
			ops.set(loginToken,DigestUtils.md5Hex(romoteIp+username),30, TimeUnit.MINUTES);
			return R.ok(obj);
		}catch (UnknownAccountException e) {
			return R.error(RCode.SYS_LOGIN_UNKNOWNACCOUNT);
		}catch (IncorrectCredentialsException e) {
			return R.error(RCode.SYS_LOGIN_USERPWD);
		}catch (LockedAccountException e) {
			return R.error(RCode.SYS_LOGIN_LOCK);
		}catch (AuthenticationException e) {
			return R.error(RCode.SYS_LOGIN_ACCOUNTAUTH);
		}
	}
	
	
	/**
	 * 退出
	 */
	@RequestMapping(value = "logout", method = RequestMethod.GET)
	@ResponseBody
	public R logout(HttpServletRequest request) {
		ShiroUtils.logout();
		String requestToken=request.getHeader(Constant.AUTH_TOKEN_CODE);
		if(StringUtils.isBlank(requestToken)){
			requestToken=request.getParameter(Constant.AUTH_TOKEN_CODE);
		}
		try {
			redisTemplate.delete(requestToken);
		}catch (Exception e){
			//do nothing
		}


		return R.ok();
	}
	
}
