package com.nis.modules.module.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.module.entity.Module;

import java.util.List;
import java.util.Map;

public interface ModuleService extends IService<Module> {
	
	PageUtils queryPage(Map<String, Object> params);
	
	void validateParam(Module moduleEntity);
	
	void removeHandle(List ids);
		
	void validateBulitinRule(List<Integer> ids);

	// 通过snmpParam生成snmp.yml
	String generateYmlFromSnmpParam(Map snmpParam, Module moduleEntity);

	void ValidateAuthForSnmpV3(String auth);

	Map getModuleStatById(Integer moduleId);

	void updateModule(Module moduleEntity);

	void saveModule(Module moduleEntity);
}
