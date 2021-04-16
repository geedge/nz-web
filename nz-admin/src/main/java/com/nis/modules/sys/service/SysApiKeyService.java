package com.nis.modules.sys.service;

import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.sys.entity.SysApiKey;

public interface SysApiKeyService extends IService<SysApiKey>{
	
	PageUtils queryPage(Map<String,Object> params);
	
	Integer addSysApiKey(SysApiKey sysApiKey);
}
