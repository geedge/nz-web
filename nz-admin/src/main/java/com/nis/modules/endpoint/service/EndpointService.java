package com.nis.modules.endpoint.service;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.metric.dto.MetricsDto;

public interface EndpointService extends IService<Endpoint> {
	
	PageUtils queryPage(Map<String, Object> params);
	
	void validateParam(Endpoint endpointEntity);
	
	void removeByModuleIds(List moduleIds);
	
	List<MetricsDto> queryEndpointStatisticsInfoByModel();
	
	Map importEndpoint(MultipartFile endpointExcelFile,String uuid);
	
	void cancelImport(String seq);
}
