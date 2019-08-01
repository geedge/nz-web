package com.nis.modules.topo.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.topo.dao.TopoNodeInfoDao;
import com.nis.modules.topo.entity.TopoNodeInfoEntity;
import com.nis.modules.topo.service.TopoNodeInfoService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 拓扑节点
 *

 */
@Service("topoNodeInfoService")
public class TopoNodeInfoServiceImpl extends ServiceImpl<TopoNodeInfoDao, TopoNodeInfoEntity> implements TopoNodeInfoService {
	@Autowired
	private TopoNodeInfoDao topoNodeInfoDao;
	
	@Override
    public void delete(Long id) {
		topoNodeInfoDao.delete(id);

    }


}
