package com.nis.modules.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.project.entity.Project;

import java.util.List;
import java.util.Map;

public interface ProjectService extends IService<Project> {
	
	PageUtils queryPage(Map<String, Object> params);
	
	void removeHandle(List ids);
	
	void validateBulitinRule(List<Integer> ids);

	void saveOrUpdateProject(Project project);
	
	Map<String,Object> queryProjectInfo(Integer id);
}
