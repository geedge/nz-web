package com.nis.modules.module.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.module.entity.Module;

public interface ModuleService extends IService<Module> {
	
	PageUtils queryPage(Map<String, Object> params);
	
	void validateParam(Module moduleEntity);
	
	void removeHandle(List ids);
	
	List queryModuleIdsByProjectIds(List projectIds);
	
	void validateBulitinRule(List<Integer> ids);
}
