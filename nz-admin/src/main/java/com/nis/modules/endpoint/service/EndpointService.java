package com.nis.modules.endpoint.service;

import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.endpoint.entity.EndpointEntity;

public interface EndpointService extends IService<EndpointEntity> {
	
	PageUtils queryPage(Map<String, Object> params);
	
	void validateParam(EndpointEntity endpointEntity);
}
