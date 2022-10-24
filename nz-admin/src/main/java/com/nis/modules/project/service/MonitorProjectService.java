package com.nis.modules.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.project.entity.MonitorProject;

import java.util.List;
import java.util.Map;

public interface MonitorProjectService extends IService<MonitorProject> {

    MonitorProject queryProjectEntity(Integer id);

    PageUtils queryPage(Map<String, Object> params);

    Integer saveProject(MonitorProject project);

    Integer updateProject(MonitorProject project);

    /**
     * project 删除方法
     * @param force 0：存在关联数据时，删除失败 1：存在关联数据时，同时删除关联数据 默认：0
     * @param ids
     * @return
     */
    R delete(Integer force, Integer... ids);

    /**
     * 通过 projectId  获取 module 列表
     *      *
     * @param projectId
     * @return
     */
    List<MonitorModule> getModuleListByProjectId(Integer projectId);

    /**
     * 通过 projectId  获取 module 列表
     * @param projectId
     * @return
     */
    List<MonitorEndpoint> getEndpointListByProjectId(Integer projectId);

    List<AssetAsset> getAssetListByProjectId(Integer projectId);

    List<Dc> getDcListByProjectId(Integer projectId);
}
