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
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.module.service.ModuleService;
import com.nis.modules.module.service.MonitorModuleService;
import com.nis.modules.overview.service.OverviewService;
import com.nis.modules.project.dao.MonitorProjectDao;
import com.nis.modules.project.dao.ProjectDao;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.project.entity.Project;
import com.nis.modules.project.service.MonitorProjectService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MonitorProjectServiceImpl extends ServiceImpl<MonitorProjectDao, MonitorProject> implements MonitorProjectService {

    @Autowired
    private AlertMessageService alertMessageService;

    @Autowired
    private MonitorModuleService monitorModuleService;

    @Autowired
    private ProjectDao projectDao;

    @Autowired
    private ModuleService moduleService;

    @Autowired
    private OverviewService overviewService;

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private EndpointDao endpointDao;

    @Override
    public MonitorProject queryProjectEntity(Integer id) {
        MonitorProject project = this.getById(id);
        if (ToolUtil.isEmpty(project)) {
            throw new NZException(RCode.PROJECT_NOTFOUND);
        }
        return project;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<MonitorProject> queryWrapper = new LambdaQueryWrapper<MonitorProject>()
                .eq(ToolUtil.isNotEmpty(params.get("id")), MonitorProject::getId, params.get("id"))
                .like(ToolUtil.isNotEmpty(params.get("name")), MonitorProject::getName, params.get("name"))
                .like(ToolUtil.isNotEmpty(params.get("remark")), MonitorProject::getRemark, params.get("remark"));

        IPage<MonitorProject> page = this.page(new Query<MonitorProject>().getPage(params), queryWrapper);
        return new PageUtils(page);
    }

    private void validateProjectInfo(MonitorProject project) {
        // 1. 名称重复
        List<MonitorProject> list = this.list(new LambdaQueryWrapper<MonitorProject>().eq(MonitorProject::getName, project.getName()));
        boolean flag = false;
        // 当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) || (list != null && list.size() == 1 && list.get(0).getId().equals(project.getId()))) {
            flag = true;
        }

        if (!flag) {
            throw new NZException(RCode.PROJECT_NAME_DUPLICATE);
        }
    }

    @Override
    public Integer saveProject(MonitorProject project) {
        // 1. 校验
        this.validateProjectInfo(project);

        // 保存
        this.save(project);
        return project.getId();
    }

    @Override
    public Integer updateProject(MonitorProject project) {
        // 内置 project 不能编辑
        MonitorProject monitorProject = this.getById(project.getId());
        if(monitorProject.getBuildIn().equals(1)){
            throw new NZException(RCode.PROJECT_BUILDIN_CAN_NOT_EDIT);
        }

        // 参数校验
        this.validateProjectInfo(project);

        // 保存
        this.updateById(project);
        return project.getId();
    }

    @Override
    public void delete(Integer... ids) {
        List<MonitorProject> list = this.list();

        // 1. 内置的 配置不允许删除
        List<Integer> buildInIdList = list.stream().filter(assetTypeConf -> assetTypeConf.getBuildIn().equals(1)).map(MonitorProject::getId).collect(Collectors.toList());
        buildInIdList.retainAll(Arrays.asList(ids));
        if (CollectionUtils.isNotEmpty(buildInIdList)) {
            throw new NZException("These projects are built-in and cannot be deleted. Details: " + buildInIdList.toString(), RCode.PROJECT_BUILDIN_CAN_NOT_REMOVE.getCode());
        }

        // 2. 包含 module 不允许删除
        List<MonitorModule> moduleList = monitorModuleService.list();
        List<Integer> usedProjectIdList = moduleList.stream().map(MonitorModule::getProjectId).distinct().collect(Collectors.toList());
        usedProjectIdList.retainAll(Arrays.asList(ids));
        if (CollectionUtils.isNotEmpty(usedProjectIdList)) {
            throw new NZException("These projects have been used by modules and cannot be deleted. Details: " + usedProjectIdList.toString(), RCode.PROJECT_REMOVE_ERROR.getCode());
        }

        // 删除
        this.removeByIds(Arrays.asList(ids));
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
