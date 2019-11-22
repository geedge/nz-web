package com.nis.modules.system.service;

import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.system.entity.SystemEntity;

public interface SystemService extends IService<SystemEntity> {
	
	PageUtils queryPage(Map<String, Object> params);
	
}
