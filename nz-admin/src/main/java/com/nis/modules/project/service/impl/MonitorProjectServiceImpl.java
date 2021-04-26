package com.nis.modules.project.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.module.service.MonitorModuleService;
import com.nis.modules.project.dao.MonitorProjectDao;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.project.service.MonitorProjectService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MonitorProjectServiceImpl extends ServiceImpl<MonitorProjectDao, MonitorProject> implements MonitorProjectService {

    @Autowired
    private AlertMessageService alertMessageService;

    @Autowired
    private MonitorModuleService monitorModuleService;

    @Autowired
    private MonitorEndpointService monitorEndpointService;

    @Autowired
    private MonitorProjectDao monitorProjectDao;

    @Override
    public MonitorProject queryProjectEntity(Integer id) {
        MonitorProject project = this.getById(id);
        if (ToolUtil.isEmpty(project)) {
            throw new NZException(RCode.PROJECT_NOTFOUND);
        }

        List<MonitorModule> moduleList = monitorModuleService.list(new LambdaQueryWrapper<MonitorModule>().eq(MonitorModule::getProjectId, project.getId()));
        project.setModuleNum(moduleList.size());

        List<Integer> moduleIdList = moduleList.stream().map(MonitorModule::getId).collect(Collectors.toList());
        List<MonitorEndpoint> endpointList = monitorEndpointService.list();
        endpointList = endpointList.stream().filter(endpoint -> moduleIdList.contains(endpoint.getModuleId())).collect(Collectors.toList());
        project.setEndpointNum(endpointList.size());

        long assetNum = endpointList.stream().map(MonitorEndpoint::getAssetId).distinct().count();
        project.setAssetNum(Integer.valueOf(String.valueOf(assetNum)));

        int alertNum = alertMessageService.count(new LambdaQueryWrapper<AlertMessageEntity>().eq(AlertMessageEntity::getProjectId, project.getId()).eq(AlertMessageEntity::getState, 1));
        project.setAlertNum(Integer.valueOf(String.valueOf(alertNum)));

        return project;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String ids = (String) params.get("ids");
        if (StrUtil.isNotBlank(ids)) {
            List<String> listIds = Arrays.asList(ids.split(","));
            params.put("ids", listIds);
        }

        IPage<MonitorProject> page = new Query<MonitorProject>().getPage(params);
        List<MonitorProject> projectList = monitorProjectDao.queryProjectList(page, params);

        page.setRecords(projectList);
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

}
