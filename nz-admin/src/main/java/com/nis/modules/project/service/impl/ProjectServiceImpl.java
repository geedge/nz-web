package com.nis.modules.project.service.impl;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.project.dao.ProjectDao;
import com.nis.modules.project.entity.ProjectEntity;
import com.nis.modules.project.service.ProjectService;

@Service("projectService")
public class ProjectServiceImpl extends ServiceImpl<ProjectDao, ProjectEntity> implements ProjectService {

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		String id = (String)params.get("id");
		String name = (String)params.get("name");
		String remark = (String)params.get("remark");
		
		IPage<ProjectEntity> page = this.page(
			new Query<ProjectEntity>().getPage(params),
			new QueryWrapper<ProjectEntity>()
				.eq(StringUtils.isNotBlank(id),"id", id)
				.like(StringUtils.isNotBlank(name),"name", name)
				.like(StringUtils.isNotBlank(remark), "remark",remark)
		);
		return new PageUtils(page);
	}

}
