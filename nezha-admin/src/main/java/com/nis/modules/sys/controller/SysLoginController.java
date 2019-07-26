/**

 *

 *

 */

package com.nis.modules.sys.controller;


import com.google.code.kaptcha.Constants;
import com.google.code.kaptcha.Producer;
import com.nis.modules.sys.entity.SystemInfoEntity;
import com.nis.modules.sys.entity.UserGroupEntity;
import com.nis.modules.sys.service.SystemInfoService;
import com.nis.modules.sys.service.UserGroupService;
import com.nis.modules.sys.shiro.ShiroUtils;

import com.nis.common.utils.R;
import com.nis.common.utils.RCode;

import org.apache.shiro.authc.*;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
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
	private SystemInfoService systemInfoService;
	@Autowired
	private UserGroupService usergroupService;
	
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
	public R login(String username, String password, String captcha) {
		String kaptcha = ShiroUtils.getKaptcha(Constants.KAPTCHA_SESSION_KEY);
		if(!captcha.equalsIgnoreCase(kaptcha)){
			return R.error(RCode.SYS_LOGIN_CAPTCHA);
		}
		
		try{
			Subject subject = ShiroUtils.getSubject();
			UsernamePasswordToken token = new UsernamePasswordToken(username, password);
			subject.login(token); //登录
			
			Long userId = getUserId();
			//检查是否有可用的业务系统
			List<SystemInfoEntity> systemInfosByUserId = systemInfoService.querySystemInfosByUserId(userId);
			if (systemInfosByUserId == null || systemInfosByUserId.size() == 0) {
				subject.logout();
				return R.error(RCode.SYS_LOGIN_NOSYSTEM);
			}
			//查出该用户所属的所有用户组
			List<UserGroupEntity> queryByUser = usergroupService.queryByUser(userId);
			getUser().setUsergroups(queryByUser);
			return R.ok(systemInfosByUserId);
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
	 * 登录后选择业务系统
	 * @param params
	 * @param request
	 * @return
	 */
	@ResponseBody
	@RequestMapping("/setSystem")
	public R setSystem(@RequestParam Map<String, Object> params){
		String s = (String) params.get("systemId");
		Long systemId = null;
		try {
			systemId = Long.valueOf(s);
			if (systemId == null || systemId < 1) {
				return R.error(RCode.SYS_LOGIN_SELECTSYS);
			} else {
				Long userId = getUserId();
				List<SystemInfoEntity> systemInfosByUserId = systemInfoService.querySystemInfosByUserId(userId);
				
				//判断当前用户是否拥有请求来的systemId权限
				boolean flag = false;
				for (SystemInfoEntity systemInfoEntity : systemInfosByUserId) {
					if (systemInfoEntity.getId().longValue() == systemId.longValue()) {
						flag = true;
						//设置为当前systemId
						getUser().setCurrSystemId(systemInfoEntity.getId());
						break;
					}
				}
				if (flag) {
					return R.ok();
				} else {
					return R.error(RCode.SYS_LOGIN_SELECTSYS);
				}
			}
		} catch (NumberFormatException e) {
			return R.error(RCode.SYS_LOGIN_SELECTSYS);
		}
	}
	
	/**
	 * 退出
	 */
	@RequestMapping(value = "logout", method = RequestMethod.GET)
	public String logout() {
		ShiroUtils.logout();
		return "redirect:login.html";
	}
	
}
