package com.nis.modules.sys.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.CtlService;
import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.sys.shiro.ShiroUtils;

import cn.hutool.log.Log;

@Service
public class CtlServiceImpl implements CtlService {
	private static final Log log = Log.get();
	
	@Autowired
	private SysUserService sysUserService;
	
	@Override
	public Map<String,String> ase(String content) {
		Map<String,String> result =new HashMap<String,String>();
		String encrypt = Tool.AesUtil.encrypt(content, Constant.AES_SECRET_KEY);
		result.put("content", content);
		result.put("encrypt", encrypt);
		return result;
	}

	@Override
	public Map<String, Object> resetAdmin() {
		String pin = Tool.RandomUtil.randomNumbers(10);
		SysUserEntity user = sysUserService.getOne(new QueryWrapper<SysUserEntity>().lambda().eq(SysUserEntity::getUsername, "admin"));
		if(ToolUtil.isEmpty(user)) {
			user = new SysUserEntity();
		}
		user.setName("admin");
		user.setUsername("admin");
		user.setPinTime(new Date());
		user.setLang("en");
		user.setCreateAt(new Date());
		//sha256加密
		String salt = RandomStringUtils.randomAlphanumeric(20);
		user.setSalt(salt);
		user.setPin(ShiroUtils.sha256(pin, user.getSalt()));
		user.setCreateBy(0);
		sysUserService.saveOrUpdate(user);
		
		Map<String,Object> result =new HashMap<String,Object>();
		result.put("id", user.getId());
		result.put("name", "admin");
		result.put("username", "admin");
		result.put("pin", pin);
		return result;
	}

	
}
