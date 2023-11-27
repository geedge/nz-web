package com.nis.modules.project.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.module.service.MonitorModuleService;
import com.nis.modules.project.dao.MonitorProjectDao;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.project.service.MonitorProjectService;
import com.nis.modules.sys.entity.SysUserStarredEntity;
import com.nis.modules.sys.service.SysUserStarredService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    @Autowired
    private SysUserStarredService starredService;

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

        List<AlertMessageEntity> alertMessageList = alertMessageService.list(new LambdaQueryWrapper<AlertMessageEntity>().eq(AlertMessageEntity::getProjectId, project.getId()));
        project.setAlertNum(alertMessageList.size());
        List<Map<String, Object>> alert = alertMessageService.getAlertsStatBySeverity("project",id);
        project.setAlert(alert);

        // 收藏标识 0：未收藏 1：已收藏
        List<SysUserStarredEntity> tUserStatList = starredService.list(new LambdaQueryWrapper<SysUserStarredEntity>()
                .select(SysUserStarredEntity::getId)
                .eq(SysUserStarredEntity::getTid, project.getId())
                .eq(SysUserStarredEntity::getType, "project")
                .eq(SysUserStarredEntity::getUid, ShiroUtils.getUserId())
                .last("limit 1")
        );
        project.setStarred(CollUtil.isEmpty(tUserStatList) ? 0 : 1);
        return project;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String ids = (String) params.get("ids");
        if (StrUtil.isNotBlank(ids)) {
            List<String> listIds = Arrays.asList(ids.split(","));
            params.put("ids", listIds);
        }
        //当前用户id
        params.put("uid",ShiroUtils.getUserId().intValue());
        IPage page = new Query(MonitorProject.class).getPage(params);
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
        project.setName(StrUtil.trim(project.getName()));
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
        project.setName(StrUtil.trim(project.getName()));
        this.validateProjectInfo(project);

        // 保存
        this.updateById(project);
        return project.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R delete(Integer force, Integer... ids) {
        boolean forceData = false;
        List resultList = Tool.ListUtil.list(true);

        List<Integer> idList = new ArrayList<>(Arrays.asList(ids));
        List<MonitorProject> delProjects = this.list(new LambdaQueryWrapper<MonitorProject>().in(MonitorProject::getId, idList));
        if (Tool.CollUtil.isEmpty(delProjects)) {
            return R.ok().putData("list", resultList);
        }

        // 内置 project 不能删除
        List<Integer> buildInIdList = delProjects.stream().filter(monitorProject -> monitorProject.getBuildIn().equals(1)).map(MonitorProject::getId).collect(Collectors.toList());
        if (Tool.CollUtil.isNotEmpty(buildInIdList)) {
            throw new NZException(RCode.PROJECT_BUILDIN_CAN_NOT_REMOVE.setParam(buildInIdList.toString()));
        }

        for (MonitorProject projectEntity : delProjects) {
            Map<Object, Object> relObjectInfoMap = this.getProjectDelRelObjectInfoMap(projectEntity);
            if (Tool.CollUtil.isNotEmpty((List) relObjectInfoMap.get("relates"))) forceData = true;
            resultList.add(relObjectInfoMap);
        }

        force = Tool.ObjectUtil.defaultIfNull(force, 0);
        switch (force) {
            case 0: {// 0：存在关联数据时，删除失败
                if (forceData) {
                    return R.error(RCode.PROJECT_RELOBJ_CAN_NOT_REMOVE).putData("list", resultList);
                }
            }
            case 1:  // 1：存在关联数据时，同时删除关联数据
                break;
            default:
                throw new NZException(RCode.ASSET_FORCE_INVALIDE);
        }

        // 删除自身和关联内容
        // project
        this.removeByIds(idList);

        // module & endpoint force = 1
        List<MonitorModule> removeModuleList = monitorModuleService.list(new LambdaQueryWrapper<MonitorModule>().in(MonitorModule::getProjectId, idList));
        if(Tool.CollUtil.isNotEmpty(removeModuleList)){
            Integer[] removeModuleIdList = removeModuleList.stream().map(MonitorModule::getId).toArray(n -> new Integer[n]);
            monitorModuleService.removeModules(1, removeModuleIdList);
        }

        // starred
        starredService.remove(new LambdaQueryWrapper<SysUserStarredEntity>()
                .eq(SysUserStarredEntity::getType, "project")
                .in(SysUserStarredEntity::getTid, idList));

        // alert
        alertMessageService.removeByProjectIds(idList);
        return R.ok().putData("list", resultList);
    }


    /**
     * 获取 project 删除提示关联信息：
     *      module
     *      endpoint
     * @return
     */
    private Map<Object, Object> getProjectDelRelObjectInfoMap(MonitorProject project) {
        List<Map> relatesList = Tool.ListUtil.list(true);

        // module
        List<MonitorModule> moduleList = monitorModuleService.list(new LambdaQueryWrapper<MonitorModule>().eq(MonitorModule::getProjectId, project.getId()));
        if (Tool.CollUtil.isNotEmpty(moduleList)) {
            // endpoint list
            List<MonitorEndpoint> endpointList = monitorEndpointService.list();
            for (MonitorModule module : moduleList) {
                List<Map> relList = Tool.ListUtil.list(true);
                List<MonitorEndpoint> endpointListOfModule = endpointList.stream().filter(e -> e.getModuleId().equals(module.getId())).collect(Collectors.toList());
                for (MonitorEndpoint endpoint : endpointListOfModule) {
                    Map<Object, Object> map = Tool.MapUtil.builder()
                            .put("id", endpoint.getId())
                            .put("name", endpoint.getName())
                            .put("type", "endpoint")
                            .build();
                    relList.add(map);
                }
                Map<Object, Object> map = Tool.MapUtil.builder()
                        .put("id", module.getId())
                        .put("name", module.getName())
                        .put("type", "module")
                        .put("relates", relList)
                        .build();
                relatesList.add(map);
            }
        }

        Map<Object, Object> delRelObjectMap = Tool.MapUtil.builder()
                .put("id", project.getId())
                .put("name", project.getName())
                .put("type", "project")
                .put("relates", relatesList)
                .build();

        return delRelObjectMap;
    }

    @Override
    public List<MonitorModule> getModuleListByProjectId(Integer projectId) {
        ValidateUtils.is(projectId).notNull(RCode.PROJECT_ID_ISNULL);

        List<MonitorModule> moduleList = monitorModuleService.list(new LambdaQueryWrapper<MonitorModule>().eq(MonitorModule::getProjectId, projectId));
        return moduleList;
    }

    @Override
    public List<MonitorEndpoint> getEndpointListByProjectId(Integer projectId) {
        ValidateUtils.is(projectId).notNull(RCode.PROJECT_ID_ISNULL);

        List<MonitorEndpoint> endpointList = this.baseMapper.queryEndpointListByProjectId(projectId);
        return endpointList;
    }

    @Override
    public List<AssetAsset> getAssetListByProjectId(Integer projectId) {
        ValidateUtils.is(projectId).notNull(RCode.PROJECT_ID_ISNULL);

        List<AssetAsset> assetList = this.baseMapper.queryAssetListByProjectId(projectId);
        return assetList;
    }

    @Override
    public List<Dc> getDcListByProjectId(Integer projectId) {
        ValidateUtils.is(projectId).notNull(RCode.PROJECT_ID_ISNULL);

        List<Dc> dcList = this.baseMapper.queryDcListByProjectId(projectId);
        return dcList;
    }

}
