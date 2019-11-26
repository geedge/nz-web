package com.nis.modules.promserver.service;

import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.promserver.entity.Promserver;

public interface PromserverService extends IService<Promserver> {

	PageUtils queryPage(Map<String, Object> params);
	
	void validateParam(Promserver promserverEntity);
	
}
