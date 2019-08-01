package com.nis.modules.topo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.topo.entity.TopoInfoEntity;

import java.util.List;
import java.util.Map;


/**
 * 拓扑图 Service
 *
 */
public interface TopoInfoService extends IService<TopoInfoEntity> {
	
	
	List<TopoInfoEntity> queryTopoInfoList(Map<String, Object> params);
	
	TopoInfoEntity queryTopoInfoById(Long id);
	
	List<SysDictEntity>  queryNodeImgList();
	
}
