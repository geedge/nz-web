package com.nis.modules.topo.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.topo.dao.TopoInfoDao;
import com.nis.modules.topo.entity.TopoInfoEntity;
import com.nis.modules.topo.service.TopoInfoService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;


/**
 * 拓扑图 Service 实现
 *

 */
@Service("topoInfoService")
public class TopoInfoServiceImpl extends ServiceImpl<TopoInfoDao, TopoInfoEntity> implements TopoInfoService {
	
	@Autowired
	private TopoInfoDao topoInfoDao;
	
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TopoInfoEntity> queryTopoInfoList(Map<String, Object> params) {
    	Long id= (Long)params.get("id"); 
    	Long systemId= (Long)params.get("systemId");
    	
    	List<TopoInfoEntity> topoInfoList = topoInfoDao.queryTopoInfoList(id,systemId);
		return topoInfoList;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TopoInfoEntity queryTopoInfoById(Long id) {
    	TopoInfoEntity topoInfo = topoInfoDao.queryTopoInfoById(id);
    	return topoInfo;
    }

}
