package com.nis.modules.project.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.DateUtil;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.project.dao.TopoDao;
import com.nis.modules.project.entity.Project;
import com.nis.modules.project.entity.Topo;
import com.nis.modules.project.service.ProjectService;
import com.nis.modules.project.service.TopoService;
import com.nis.modules.sys.shiro.ShiroUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;


@Slf4j
@Service("topoService")
public class TopoServiceImpl extends ServiceImpl<TopoDao, Topo> implements TopoService {

    @Autowired
    private ProjectService projectService;

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
        Project project = projectService.getById(projectId);
        if (project == null) {
            throw new NZException(RCode.PROJECT_NOTFOUND);
        }

        Topo topo = this.getOne(new QueryWrapper<Topo>().lambda().eq(Topo::getProjectId, projectId));
        topo = ToolUtil.isEmpty(topo) ? new Topo() : topo;
        topo.setProjectId(projectId);
        topo.setUpdateAt(DateUtil.getUTCTimeByConfigTimeZone());
        topo.setUpdateBy(ShiroUtils.getUserId().intValue());
        topo.setTopo(topoConfig);
        this.saveOrUpdate(topo);
    }
}