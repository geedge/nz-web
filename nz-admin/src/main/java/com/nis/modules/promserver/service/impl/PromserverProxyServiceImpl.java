package com.nis.modules.promserver.service.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.exception.NZException;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverProxyService;
import com.nis.modules.promserver.service.PromserverService;
@Service
public class PromserverProxyServiceImpl implements PromserverProxyService {

    private static final Logger logger= LoggerFactory.getLogger(PromserverProxyServiceImpl.class);
    @Autowired
    private PromserverService promserverService;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    public JSONObject promRequest(String url,String method,Map<String,Object> params,Integer promId,Integer idcId,Integer type,Integer endpointId) throws Exception {
    	if(endpointId != null) {// endpointId 不为空则通过 redis key 获取prometheus id
    		Object pid = redisTemplate.opsForHash().get(Constant.REDIS_KEY_ENDPOINT_PROM, endpointId.toString());
    		if(pid == null) {
    			throw new NZException(RCode.PROMSERVER_NOTEXSITS);
    		}
    		promId = Integer.valueOf(pid+"");
    	}
    	Promserver server = promserverService.getOneAvaPromServer(promId, idcId, type);
    	String promUrl = genPromUrl(server.getHost()	, server.getPort(), url, method, params);
    	logger.debug("promurl:{}",promUrl);
		HttpURLConnection conn = null;
		OutputStream connOutputStream = null;
		InputStream connInputStream = null;
		try {
			conn = (HttpURLConnection) new URL(promUrl).openConnection();
			conn.setRequestMethod(method);
			if (!"GET".equalsIgnoreCase(method)) {
				conn.setDoOutput(true);
				conn.setDoInput(true);
				conn.setUseCaches(false);
				connOutputStream = conn.getOutputStream();
				String queryString = genQueryString(params);
				if(StringUtils.isNotBlank(queryString)) {
					connOutputStream.write(queryString.getBytes("UTF-8"));
				}
			}
			conn.connect();
			int responseCode = conn.getResponseCode();
			connInputStream = (responseCode < 400)? conn.getInputStream():conn.getErrorStream();
			String responseMessage = conn.getResponseMessage();
			byte[] buffer = new byte[1024];
			StringBuilder sb = new StringBuilder();
			int idx = -1;
			while((idx = connInputStream.read(buffer)) > -1) {
				sb.append(new String(buffer,0,idx,"UTF-8"));
			}
			if(responseCode >= 400) {
				throw new RuntimeException(String.format("code:%s,message:%s,content:%s", responseCode,responseMessage,sb.toString()));
			}
			return JSONObject.parseObject(sb.toString());
		}finally {
			IOUtils.closeQuietly(connOutputStream,connInputStream);
			conn.disconnect();
		}
    }
    
    /**
     * 生成 prometheus url
     * @param host
     * @param port
     * @param url
     * @param method
     * @param params
     * @return
     */
    public static String genPromUrl(String host,Integer port,String url,String method,Map<String,Object> params) {
    	String queryStr = "";
    	//当 method 为 get 时，需要将参数拼接到 url上
    	if("GET".equalsIgnoreCase(method)&&params != null && !params.isEmpty()) {
    		queryStr= genQueryString(params);
    	}
    	return String.format("http://%s:%s%s%s", host,port,url,queryStr.toString());
    }
    
    /**
     * 生成querystring
     * @param params
     * @return
     */
    public static String genQueryString(Map<String,Object> params) {
    	StringBuilder queryStr = new StringBuilder();
    	//当 method 为 get 时，需要将参数拼接到 url上
    	if(params != null && !params.isEmpty()) {
    		queryStr.append("?");
    		for(Entry<String, Object> en : params.entrySet()) {
    			queryStr.append(en.getKey());
    			queryStr.append("=");
    			Object value = en.getValue();
    			if(value!=null) {
    				if(value.getClass().isArray() || value instanceof Iterable<?>) {
    					queryStr.append(StringUtils.join(value, ","));
    				}else {
    					queryStr.append(value);
    				}
    			}
    			queryStr.append("&");
    		}
    		queryStr.deleteCharAt(queryStr.length()-1);
    	}
    	return queryStr.toString();
    }

	@Override
	public JSONObject query(Map<String, Object> params) throws Exception {
		String url = "/api/v1/query";
		String method = "GET";
		Integer promId = null;
		Integer idcId = null;
		Integer type = null;
		Integer endpointId = null;
		return promRequest(url, method, params, promId, idcId, type, endpointId);
	}

	@Override
	public JSONObject queryRange(Map<String, Object> params) throws Exception {
		String url = "/api/v1/query_range";
		String method = "GET";
		Integer promId = null;
		Integer idcId = null;
		Integer type = 1;
		Integer endpointId = null;
		return promRequest(url, method, params, promId, idcId, type, endpointId);
	}

	@Override
	public JSONObject series(Map<String, Object> params) throws Exception {
		String url = "/api/v1/series";
		String method = "GET";
		Integer promId = null;
		Integer idcId = null;
		Integer type = 1;
		Integer endpointId = null;
		return promRequest(url, method, params, promId, idcId, type, endpointId);
	}

	@Override
	public JSONObject labels(Map<String, Object> params) throws Exception {
		String url = "/api/v1/labels";
		String method = "GET";
		Integer promId = null;
		Integer idcId = null;
		Integer type = 1;
		Integer endpointId = null;
		return promRequest(url, method, params, promId, idcId, type, endpointId);
	}

}
