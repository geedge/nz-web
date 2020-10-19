package com.nis.common.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.sys.entity.SysArea;
import com.nis.modules.sys.service.SysAreaService;

@Component
public class SettingConfig implements CommandLineRunner{
	@Autowired
	private RedisTemplate<String,String> redisTemplate;
	@Autowired
	private SysAreaService sysAreaService;
	
	@Override
	public void run(String... args) throws Exception {		 
		List<SysArea> areas = sysAreaService.list();
		if(ToolUtil.isEmpty(areas)) {
			String string = redisTemplate.opsForValue().get(Constant.REDIS_KEY_AREA);
			if(ObjectUtils.isNotEmpty(string)) {
				redisTemplate.opsForValue().set(Constant.REDIS_KEY_AREA, JSONArray.toJSONString(areas));
			}
		}
	}

}
