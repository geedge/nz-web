package com.nis.modules.node.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.node.dao.SystemNodeRelDao;
import com.nis.modules.node.entity.SystemNodeRelEntity;
import com.nis.modules.node.service.SystemNodeRelService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.stereotype.Service;

import java.util.List;


@Service("systemNodeRelService")
public class SystemNodeRelServiceImpl extends ServiceImpl<SystemNodeRelDao, SystemNodeRelEntity> implements SystemNodeRelService {

    @Override
    public boolean isCurrentSystem(Long[] nodeIds) {
        Long currSystemId = ShiroUtils.getCurrSystemId();
        QueryWrapper<SystemNodeRelEntity> systemNodeRelQueryWrapper=new QueryWrapper<SystemNodeRelEntity>();
        systemNodeRelQueryWrapper.in("uuid",nodeIds);
        systemNodeRelQueryWrapper.eq("system_id",currSystemId);
        List<SystemNodeRelEntity> list = this.list(systemNodeRelQueryWrapper);

        return list!=null && list.size()==nodeIds.length;
    }
}
