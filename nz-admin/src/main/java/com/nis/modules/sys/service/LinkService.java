package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.sys.entity.Link;

import java.util.List;
import java.util.Map;

public interface LinkService extends IService<Link>{
	
	List<Link> queryList(Map<String, Object> params);
	
	void modifyOrder(Link link);
	
	void saveLink(Link link);
	
	void removeLinks(List<String> ids);

	void saveWeights(List<Link> links);
}
