package com.nis.modules.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.project.entity.MonitorProject;

import java.util.Map;

public interface MonitorProjectService extends IService<MonitorProject> {

    MonitorProject queryProjectEntity(Integer id);

    PageUtils queryPage(Map<String, Object> params);

    Integer saveProject(MonitorProject project);

    Integer updateProject(MonitorProject project);

    void delete(Integer... ids);

    Map<String, Object> queryProjectInfo(Integer id);
}
