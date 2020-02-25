package com.nis.common.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.nis.modules.sys.entity.SysArea;
import com.nis.modules.sys.service.SysAreaService;

@Component
public class SettingConfig implements CommandLineRunner{
	@SuppressWarnings("rawtypes")
	@Autowired
	private RedisTemplate redisTemplate;
	@Autowired
	private SysAreaService sysAreaService;
	
	@SuppressWarnings("unchecked")
	@Override
	public void run(String... args) throws Exception {		 
		List<SysArea> areas = sysAreaService.list();
		if(areas!=null&&areas.size()>0) {
			// 先清空之前的缓存信息 再更新新数据
			while (redisTemplate.opsForList().size("area") > 0){
				redisTemplate.opsForList().leftPop("area");
			}
			redisTemplate.opsForList().rightPushAll("area", areas);
		}
	}

}
