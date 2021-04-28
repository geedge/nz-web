package com.nis.modules.module.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.project.entity.MonitorProject;

import java.util.List;
import java.util.Map;

public interface MonitorModuleService extends IService<MonitorModule> {

    MonitorModule queryModuleInfo(Integer id);

    PageUtils queryPage(Map<String, Object> params);

    List<MonitorProject> queryModuleTree(String name);

    Integer saveModule(MonitorModule module);

    Integer updateModule(MonitorModule module);

    void removeModules(Integer... ids);
}
