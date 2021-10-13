package com.nis.modules.promserver.service;

import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.promserver.entity.Promserver;

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
	
	void updateStatusInfo(Promserver promserver);

	/**
	 * 详情
	 * @param id
	 * @return
	 */
	Promserver queryPromServerDetails(Integer id);

	JSONObject requestAgent(Promserver promServer, String urlPath, String queryString);

	/**
	 * 获取一个 可用的 global agent
	 * @return
	 */
	Promserver getOneAvaliableGlobalAgent();

	String getPromPathPrefix();
}
