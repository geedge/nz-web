package com.nis.modules.project.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.project.entity.Project;

public interface ProjectService extends IService<Project> {
	
	PageUtils queryPage(Map<String, Object> params);
	
	void removeHandle(List<String> ids);
}
