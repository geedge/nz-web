package com.nis.modules.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.module.entity.Module;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.project.entity.Topo;

import java.util.List;
import java.util.Map;

public interface TopoService extends IService<Topo> {

    Map queryTopoByProjectId(Integer projectId);

    void updateProjectTopo(Integer projectId, String topoConfig);

    void removeNodeAndLineByModules(List<Module> modules);

    void removeTopoConfigNodeAndLineByModules(List<MonitorModule> modules);
}
