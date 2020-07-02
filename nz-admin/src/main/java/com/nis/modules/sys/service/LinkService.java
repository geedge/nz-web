package com.nis.modules.sys.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.sys.entity.Link;

public interface LinkService extends IService<Link>{
	
	List<Link> queryList(Map<String, Object> params);
	
	void modifyOrder(Link link);
	
	void saveLink(Link link);
	
	void removeLinks(List<String> ids);
	
}
