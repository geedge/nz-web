package com.nis.modules.endpoint.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.endpoint.entity.Endpoint;

public interface EndpointService extends IService<Endpoint> {
	
	PageUtils queryPage(Map<String, Object> params);
	
	void validateParam(Endpoint endpointEntity);
	
	void removeByModuleIds(List moduleIds);
}
