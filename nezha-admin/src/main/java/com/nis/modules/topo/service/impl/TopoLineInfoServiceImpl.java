package com.nis.modules.topo.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.topo.dao.TopoLineInfoDao;
import com.nis.modules.topo.dao.TopoNodeInfoDao;
import com.nis.modules.topo.entity.TopoLineInfoEntity;
import com.nis.modules.topo.service.TopoLineInfoService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 拓扑线
 *

 */
@Service("topoLineInfoService")
public class TopoLineInfoServiceImpl extends ServiceImpl<TopoLineInfoDao, TopoLineInfoEntity> implements TopoLineInfoService {
	@Autowired
	private TopoLineInfoDao topoLineInfoDao;

	@Override
    public void delete(Long id) {
		topoLineInfoDao.delete(id);

    }

}
