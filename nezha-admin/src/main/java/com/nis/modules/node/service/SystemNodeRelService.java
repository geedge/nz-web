package com.nis.modules.node.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.node.entity.SystemNodeRelEntity;

/**
 * 业务系统 节点 关联表
 * @author shizhendong
 */
public interface SystemNodeRelService extends IService<SystemNodeRelEntity> {
    boolean isCurrentSystem(Long [] nodeIds);
	boolean delete(List<SystemNodeRelEntity> oldSysNodeRel);

}