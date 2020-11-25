package com.nis.modules.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.endpoint.dao.EndpointDao;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.endpoint.service.EndpointService;
import com.nis.modules.module.entity.Module;
import com.nis.modules.module.service.ModuleService;
import com.nis.modules.overview.service.OverviewService;
import com.nis.modules.project.dao.ProjectDao;
import com.nis.modules.project.entity.Project;
import com.nis.modules.project.entity.Topo;
import com.nis.modules.project.service.ProjectService;
import com.nis.modules.project.service.TopoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service("projectService")
@Slf4j
public class ProjectServiceImpl extends ServiceImpl<ProjectDao, Project> implements ProjectService {
	@Autowired
	private ProjectDao projectDao;

	@Autowired
	private ModuleService moduleService;

	@Autowired
	private AlertMessageService alertMessageService;
	
	@Autowired
	private OverviewService overviewService;
	
	@Autowired
	private EndpointService endpointService;

    @Autowired
    private TopoService topoService;

    @Autowired
    private EndpointDao endpointDao;
    
	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		String id = (String)params.get("id");
		String name = (String)params.get("name");
		String remark = (String)params.get("remark");
		
		IPage<Project> page = this.page(
			new Query<Project>().getPage(params),
			new QueryWrapper<Project>().lambda()
				.eq(StringUtils.isNotBlank(id),Project::getId, id)
				.like(StringUtils.isNotBlank(name),Project::getName, name)
				.like(StringUtils.isNotBlank(remark), Project::getRemark,remark)
		);
		return new PageUtils(page);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeHandle(List ids) {
		//如果project下有module则不允许删除
		List<Module> modules = moduleService.list(new QueryWrapper<Module>().lambda().in(Module::getProjectId, ids));
		if (CollectionUtils.isNotEmpty(modules)) {
			List<Project> projects = this.list(new QueryWrapper<Project>().lambda().in(Project::getId, ids));
			Set<String> projectNames = projects.stream().map(Project::getName).collect(Collectors.toSet());
    		throw new NZException("These projects contains module can't remove, projects info : "+projectNames.toString(),RCode.PROJECT_REMOVE_ERROR.getCode());
		}
		projectDao.deleteBatchIds(ids);
		//删除alert表相关数据信息
		alertMessageService.removeByProjectIds(ids);
		// 删除关联的 topo
		topoService.remove(new QueryWrapper<Topo>().lambda().in(Topo::getProjectId, ids));
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
	@Transactional(rollbackFor = Exception.class)
	public void saveOrUpdateProject(Project project) {
		// 名称唯一
		if (!this.validateProjectName(project.getId(), project.getName())) {
			throw new NZException(RCode.PROJECT_NAME_DUPLICATE);
		}

		if (ToolUtil.isEmpty(project.getId())) {
			this.save(project);
		} else {
			this.updateById(project);
		}
	}

	public boolean validateProjectName(Integer id, String name) {
		List<Project> list = this.list(new QueryWrapper<Project>().lambda().eq(Project::getName, name));
		boolean flag = false;

		//当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
		if ((list == null || list.size() == 0) ||
				(list != null && list.size() == 1 && list.get(0).getId().equals(id))) {
			flag = true;
		}
		return flag;
	}

	@Override
	public Map<String,Object> queryProjectInfo(Integer id) {
		Map<String,Object> result = new HashMap<String,Object>();
		Project project = projectDao.selectById(id);
		if(ObjectUtils.isNotEmpty(project)) {
			
			// project高中低alert message数据
			List<Map<String, Object>> maps = alertMessageService.listMaps(new QueryWrapper<AlertMessageEntity>().eq("project_id", id).eq("state",1).groupBy("severity").select("severity","count(*) as count"));
			Map projectAlertMessageInfos =new HashMap();
			List<Integer> alertStat = project.getAlertStat();
			for(Map<String,Object> map:maps) {
				projectAlertMessageInfos.put(map.get("severity"),((Long)map.get("count")).intValue());
			}
			
			alertStat.add(ObjectUtils.isEmpty(projectAlertMessageInfos.get(Constant.ALERT_SEVERITY_HIGH))?0:(int) projectAlertMessageInfos.get(Constant.ALERT_SEVERITY_HIGH));
			alertStat.add(ObjectUtils.isEmpty(projectAlertMessageInfos.get(Constant.ALERT_SEVERITY_MEDIUM))?0:(int) projectAlertMessageInfos.get(Constant.ALERT_SEVERITY_MEDIUM));
			alertStat.add(ObjectUtils.isEmpty(projectAlertMessageInfos.get(Constant.ALERT_SEVERITY_LOW))?0:(int) projectAlertMessageInfos.get(Constant.ALERT_SEVERITY_LOW));

		    List<Module> modules = moduleService.list(new LambdaQueryWrapper<Module>().eq(Module::getProjectId, id));
		    if(ObjectUtils.isEmpty(modules)) {
		    	modules=new ArrayList<Module>();
		    }
			// 根据 asset 停用状态 获取对应 endpoint ids
			List<Integer> suspendedEndpointIds = endpointDao.getSuspendedEndpointIdsByAssetState();
			// 停用的 endpoint ids
			List<Integer> disabledEndpointIds = endpointDao.getDisabledEndpointIds();
			suspendedEndpointIds.addAll(disabledEndpointIds);
			suspendedEndpointIds = suspendedEndpointIds.stream().distinct().collect(Collectors.toList());

			Set<Integer> endpointUpIds = overviewService.getUpStatusEndpointIds(suspendedEndpointIds);
		    
		    for(Module module : modules) {
		    	//module高中低alert message数据
		    	List<Map<String, Object>> moduleMaps = alertMessageService.listMaps(new QueryWrapper<AlertMessageEntity>().eq("module_id", module.getId()).eq("state",1).groupBy("severity").select("severity","count(*) as count"));
		    	Map moduleAlertMessageInfos = new HashMap();
		    	for(Map<String,Object> moduleMap : moduleMaps) {
		    		moduleAlertMessageInfos.put(moduleMap.get("severity"),((Long)moduleMap.get("count")).intValue());
		    	}
		    	List<Integer> moduleAlertStat = module.getAlertStat();
		    	moduleAlertStat.add(ObjectUtils.isEmpty(moduleAlertMessageInfos.get(Constant.ALERT_SEVERITY_HIGH))?0:(int) moduleAlertMessageInfos.get(Constant.ALERT_SEVERITY_HIGH));
		    	moduleAlertStat.add(ObjectUtils.isEmpty(moduleAlertMessageInfos.get(Constant.ALERT_SEVERITY_MEDIUM))?0:(int) moduleAlertMessageInfos.get(Constant.ALERT_SEVERITY_MEDIUM));
		    	moduleAlertStat.add(ObjectUtils.isEmpty(moduleAlertMessageInfos.get(Constant.ALERT_SEVERITY_LOW))?0:(int) moduleAlertMessageInfos.get(Constant.ALERT_SEVERITY_LOW));
		    	//组件endpoint up down统计
		    	if(ObjectUtils.isNotEmpty(endpointUpIds)) {
		    		List<Integer> endpointStat = module.getEndpointStat();
		    		// 初始化endpoint up down suspended 状态
		    		endpointStat.add(0);
		    		endpointStat.add(0);
		    		endpointStat.add(0);
		    		List<Endpoint> endpoints = endpointService.list(new QueryWrapper<Endpoint>().eq("module_id", module.getId()));
			    	for(Endpoint endpoint : endpoints) {
					    if (endpointUpIds.contains(endpoint.getId())) {
						    endpointStat.set(0, (endpointStat.get(0) + 1));
					    } else if (suspendedEndpointIds.contains(endpoint.getId())) {
						    endpointStat.set(2, (endpointStat.get(2) + 1));
					    } else {
						    endpointStat.set(1, (endpointStat.get(1) + 1));
					    }
			    	}
		    	}
		    }
		    result.put("basic", project);
		    result.put("module", modules);
		}
	    return result;
	}

}
