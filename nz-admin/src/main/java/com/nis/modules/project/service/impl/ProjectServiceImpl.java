package com.nis.modules.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.Constant.AlertType;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.module.service.ModuleService;
import com.nis.modules.project.dao.ProjectDao;
import com.nis.modules.project.entity.Project;
import com.nis.modules.project.service.ProjectService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service("projectService")
public class ProjectServiceImpl extends ServiceImpl<ProjectDao, Project> implements ProjectService {
	@Autowired
	private ProjectDao projectDao;
	@Autowired
	private ModuleService moduleService;
	@Autowired
	private AlertRuleService alertRuleService;
	@Autowired
	private AlertMessageService alertMessageService;
	
	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		String id = (String)params.get("id");
		String name = (String)params.get("name");
		String remark = (String)params.get("remark");
		
		IPage<Project> page = this.page(
			new Query<Project>().getPage(params),
			new QueryWrapper<Project>()
				.eq(StringUtils.isNotBlank(id),"id", id)
				.like(StringUtils.isNotBlank(name),"name", name)
				.like(StringUtils.isNotBlank(remark), "remark",remark)
		);
		return new PageUtils(page);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeHandle(List ids) {
		projectDao.deleteBatchIds(ids);
		//同时删除相关联的module信息 先查询出要删除的module信息
		List moduleIds =  moduleService.queryModuleIdsByProjectIds(ids);
		if(moduleIds!=null && moduleIds.size()>0) {
			moduleService.removeHandle(moduleIds);
		}
		//删除alert表相关数据信息
		alertRuleService.removeByTypeIds(AlertType.ALERT_TYPE_PROJECT.getCode(), ids);
		alertMessageService.removeByTypeIds(AlertType.ALERT_TYPE_PROJECT.getCode(), ids);
	}

	@Override
	public void validateBulitinRule(List<Integer> ids) {
		List<Project> objs = projectDao.selectBatchIds(ids);
		for(Project obj : objs) {
			if(obj!=null&&obj.getBuildIn().equals(1)){
				throw new NZException(RCode.PROJECT_BUILDIN_CAN_NOT_REMOVE);
			}
		}
		
	}

	@Override
	public boolean validateProjectName(Integer id, String name) {
		List<Project> list = this.list(new QueryWrapper<Project>().eq("name", name));
		boolean flag = false;

		//当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
		if ((list == null || list.size() == 0) ||
				(list != null && list.size() == 1 && list.get(0).getId().equals(id))) {
			flag = true;
		}
		return flag;
	}

}
