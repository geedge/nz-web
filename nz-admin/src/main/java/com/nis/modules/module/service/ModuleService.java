package com.nis.modules.module.service;

import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.module.entity.ModuleEntity;

public interface ModuleService extends IService<ModuleEntity> {
	
	PageUtils queryPage(Map<String, Object> params);
	
	void validateParam(ModuleEntity moduleEntity);
}
