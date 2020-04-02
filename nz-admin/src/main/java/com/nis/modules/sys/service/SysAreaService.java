package com.nis.modules.sys.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.sys.entity.SysArea;

public interface SysAreaService extends IService<SysArea> {
	
	Map queryPage(Map<String, Object> params);
	
}
