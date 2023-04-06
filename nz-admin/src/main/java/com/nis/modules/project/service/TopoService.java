package com.nis.modules.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.project.entity.Topo;
import com.nis.modules.project.entity.TopoIcon;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface TopoService extends IService<Topo> {

    Map queryTopoByProjectId(Integer projectId);

    void updateProjectTopo(Integer projectId, String topoConfig);

    void removeTopoConfigNodeAndLineByModules(List<MonitorModule> modules);

    void topoImport(Integer projectId, Map topoConfigMap, List<TopoIcon> icons);

    void topoExport(Integer projectId, HttpServletResponse response);

}
