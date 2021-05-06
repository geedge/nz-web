package com.nis.modules.project.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.project.dao.TopoDao;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.project.entity.Topo;
import com.nis.modules.project.service.MonitorProjectService;
import com.nis.modules.project.service.TopoService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service("topoService")
public class TopoServiceImpl extends ServiceImpl<TopoDao, Topo> implements TopoService {

    @Autowired
    private MonitorProjectService projectService;

    @Override
    public Map queryTopoByProjectId(Integer projectId) {
        Topo topo = this.getOne(new QueryWrapper<Topo>().lambda().eq(Topo::getProjectId, projectId));
        if (ToolUtil.isEmpty(topo) || ToolUtil.isEmpty(topo.getTopo())) {
            return null;
        }
        return JSONObject.parseObject(topo.getTopo().toString(), Map.class);
    }

    @Override
    public void updateProjectTopo(Integer projectId, String topoConfig) {
        // 校验是否存在改 project
        MonitorProject project = projectService.getById(projectId);
        if (project == null) {
            throw new NZException(RCode.PROJECT_NOTFOUND);
        }

        Topo topo = this.getOne(new QueryWrapper<Topo>().lambda().eq(Topo::getProjectId, projectId));
        topo = ToolUtil.isEmpty(topo) ? new Topo() : topo;
        topo.setProjectId(projectId);
        topo.setUpdateAt(new Date());
        topo.setUpdateBy(ShiroUtils.getUserId().intValue());
        topo.setTopo(topoConfig);
        this.saveOrUpdate(topo);
    }

    @Override
    public void removeTopoConfigNodeAndLineByModules(List<MonitorModule> modules) {
        Set<Integer> projectIds = modules.stream().map(MonitorModule::getProjectId).collect(Collectors.toSet());
        List<Topo> topos = this.list(new QueryWrapper<Topo>().lambda().in(Topo::getProjectId, projectIds));
        if (CollectionUtils.isEmpty(topos)) {
            return;
        }
        Map<Integer, Object> topoProjectIdAndConfigMap = topos.stream().collect(Collectors.toMap(Topo::getProjectId, Topo::getTopo));

        // module 是批量删除，删除的或许不属于同一个 project
        Map<Integer, List<MonitorModule>> projectIdAndMoudles = modules.stream().collect(Collectors.groupingBy(MonitorModule::getProjectId));

        Object topoConfig;
        Map<String, JSONArray> configMap;
        List<Integer> moduleIds;
        for (Map.Entry<Integer, List<MonitorModule>> entry : projectIdAndMoudles.entrySet()) {
            topoConfig = topoProjectIdAndConfigMap.get(entry.getKey());
            if (ToolUtil.isEmpty(topoConfig)) {
                continue;
            }

            moduleIds = entry.getValue().stream().map(MonitorModule::getId).collect(Collectors.toList());
            configMap = JSONObject.parseObject(topoConfig.toString(), Map.class);
            configMap.put("pens", this.removeModuleEntity(configMap.get("pens"), moduleIds));
            topoProjectIdAndConfigMap.put(entry.getKey(), JSONObject.toJSONString(configMap));
        }

        Date now = new Date();
        Integer userId = ShiroUtils.getUserId().intValue();
        for (Topo topo : topos) {
            topo.setTopo(topoProjectIdAndConfigMap.get(topo.getProjectId()).toString());
            topo.setUpdateBy(userId);
            topo.setUpdateAt(now);
        }
        this.saveOrUpdateBatch(topos);
    }


    private JSONArray removeModuleEntity(JSONArray pens, List<Integer> moduleIds) {
        Map<String, Object> node, nodeData;
        Integer moduleId, type;
        Iterator<Object> iterator = pens.iterator();
        while (iterator.hasNext()) {
            node = (Map<String, Object>) iterator.next();
            type = (Integer) node.get("type");
            // type 为 0 是 module 节点，只处理 节点
            if (!type.equals(0)) {
                continue;
            }

            nodeData = (Map<String, Object>) node.get("data");
            moduleId = (Integer) nodeData.get("moduleId");
            if (moduleIds.contains(moduleId)) {
                iterator.remove();
            }
        }
        return pens;
    }
}
