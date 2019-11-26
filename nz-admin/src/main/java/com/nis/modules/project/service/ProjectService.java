package com.nis.modules.project.service;

import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.project.entity.ProjectEntity;

public interface ProjectService extends IService<ProjectEntity> {
	
	PageUtils queryPage(Map<String, Object> params);
	
}
