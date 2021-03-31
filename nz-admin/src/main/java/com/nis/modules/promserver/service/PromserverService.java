package com.nis.modules.promserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.promserver.entity.Promserver;

import java.util.List;
import java.util.Map;

public interface PromserverService extends IService<Promserver> {

	PageUtils queryPage(Map<String, Object> params);
	
	/**
	 * 查找一个可用的 prometheus server
	 * @param promId
	 * @param idcId
	 * @param type 默认 global
	 * @return
	 */
	Promserver getOneAvaPromServer(Integer promId,Integer idcId,Integer type);

	Integer saveOrUpdatePromserver(Promserver promserverEntity, boolean modify);
	
	void updateStatus(List<Promserver> promservers);

	/**
	 * 详情
	 * @param id
	 * @return
	 */
	Promserver queryPromServerDetails(Integer id);
}
