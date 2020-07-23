package com.nis.modules.project.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.endpoint.service.EndpointService;
import com.nis.modules.module.entity.Module;
import com.nis.modules.module.service.ModuleService;
import com.nis.modules.overview.service.OverviewService;
import com.nis.modules.panel.dao.PanelDao;
import com.nis.modules.panel.entity.Chart;
import com.nis.modules.panel.entity.Panel;
import com.nis.modules.panel.service.ChartService;
import com.nis.modules.panel.service.PanelService;
import com.nis.modules.project.dao.ProjectDao;
import com.nis.modules.project.entity.Project;
import com.nis.modules.project.service.ProjectService;
import com.nis.modules.sys.dao.SysConfigDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.shiro.ShiroUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
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
	private PanelDao panelDao;
	
	@Autowired
	private OverviewService overviewService;
	
	@Autowired
	private EndpointService endpointService;

    @Autowired
    private SysConfigDao sysConfigDao;

    @Autowired
    private ChartService chartService;
	
    @Autowired
    private PanelService panelService;
    
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
		// 删除关联 panel
		List<Panel> panels = panelService.list(new QueryWrapper<Panel>().lambda().eq(Panel::getType, Constant.PanelType.PROJECT.getValue()).in(Panel::getLink, ids));
		if(ToolUtil.isNotEmpty(panels)) {
			List<Integer> panelIds = panels.stream().map(Panel::getId).collect(Collectors.toList());
			panelService.delPanels(panelIds.toArray(new Integer[0]));
		}
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

			// 新增时需要新增 panel chart
			this.saveProjectPanel(project);
		} else {
			this.updateById(project);
		}
	}

	private void saveProjectPanel(Project project) {
		Panel panel = new Panel();
		panel.setType(Constant.PanelType.PROJECT.getValue());
		panel.setLink(project.getId());
		panel.setCreateBy(ShiroUtils.getUserId().intValue());
		// 固定格式 panel name format is project_panel_link
		panel.setName("project_panel_" + project.getId());

		panel.setPrev(0);
		panel.setNext(-1);
		panelDao.insert(panel);

        // 配置信息获取
        SysConfigEntity projectConfig = sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_PROJECT_CHART_TPL));
        if (ToolUtil.isEmpty(projectConfig)) {
            log.error(String.format("获取project配置信息失败%s", Constant.SYSCONFIG_KEY_PROJECT_CHART_TPL));
            throw new NZException(RCode.SYS_CONFIG_PROJECTCHARTTPL_NOTFOUND);
        }
        // chart config
        Chart chartConfig = JSONObject.parseObject(projectConfig.getParamValue(), Chart.class);

        Chart target = new Chart();
        BeanUtils.copyProperties(chartConfig, target);
        target.setTitle(ToolUtil.isEmpty(target.getTitle()) ? project.getId() + " Chart" : target.getTitle());
        target.setCreateAt(DateUtil.getUTCTimeByConfigTimeZone());
        target.setParam(ToolUtil.isNotEmpty(target.getParam()) ? JSONObject.toJSONString(target.getParam()) : null);
        chartService.saveChart(panel.getId(), target);
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
			Map handleMap=new HashMap();
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
		    
		    Set<Integer> endpointStatus = overviewService.getEndpointStatus();
		    
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
		    	if(ObjectUtils.isNotEmpty(endpointStatus)) {
		    		List<Integer> endpointStat = module.getEndpointStat();
		    		// 初始化endpoint up down状态
		    		endpointStat.add(0);
		    		endpointStat.add(0);
		    		List<Endpoint> endpoints = endpointService.list(new QueryWrapper<Endpoint>().eq("module_id", module.getId()));
			    	for(Endpoint endpoint : endpoints) {
			    		if(endpointStatus.contains(endpoint.getId())) {
			    			endpointStat.set(0, (endpointStat.get(0)+1));
			    		}else {
			    			endpointStat.set(1, (endpointStat.get(1)+1));
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
