package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.sys.entity.SysArea;

import java.util.Map;

public interface SysAreaService extends IService<SysArea> {
	
	Map queryPage(Map<String, Object> params);
	
}
