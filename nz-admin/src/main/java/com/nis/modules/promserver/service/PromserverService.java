package com.nis.modules.promserver.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.promserver.entity.Promserver;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
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

    Map promServerProxy(HttpServletRequest request, HttpServletResponse response, String promPath);

	JSONArray filterResult(JSONArray resultArray, String filter);

    void responseWrite(HttpServletResponse response, HttpHeaders httpHeaders, Integer statusCode, byte[] responseBody) throws Exception;

    ResponseEntity<byte[]> promServerData(HttpServletRequest request, Integer agentId, Integer dcId, Integer type, String promPath, String step, byte[] body, String queryString);

	Map handleResultData(InputStream connInputStream, Boolean isGzipStream, Map<String, String> paramData);

    HttpEntity<byte[]> buildHeader(HttpServletRequest request, String token, byte[] body);
}
