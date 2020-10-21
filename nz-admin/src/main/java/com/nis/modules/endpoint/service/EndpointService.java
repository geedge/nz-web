package com.nis.modules.endpoint.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.metric.dto.MetricsDto;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface EndpointService extends IService<Endpoint> {
	
	PageUtils queryPage(Map<String, Object> params);
	
	void validateParam(Endpoint endpointEntity);
	
	List<MetricsDto> queryEndpointStatisticsInfoByModule();
	
	Map importEndpoint(MultipartFile endpointExcelFile);
	
	void cancelImport(String seq);

    void getExportTemplateByLanguage(HttpServletResponse response, String language);

	void export(HttpServletResponse response, Map<String, Object> params);

    void deleteEndpointByIds(List<Integer> endpointIdList);
}
