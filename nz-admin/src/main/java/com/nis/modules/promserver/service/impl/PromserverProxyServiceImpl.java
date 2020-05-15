package com.nis.modules.promserver.service.impl;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
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
    	CloseableHttpClient client = null;
    	CloseableHttpResponse response = null;
    	try {
    		client = HttpClientBuilder.create().build();
        	HttpRequestBase request = null;
        	switch (method.toUpperCase()) {
    		case "GET":
    			request = new HttpGet(promUrl);
    			break;
    		case "POST":
    			request = new HttpPost(promUrl);
    		case "PUT":
    			request = new HttpPut(promUrl);
    		case "DELETE":
    			request = new HttpDelete(promUrl);
    		default:
    			request = new HttpPost(promUrl);
    			break;
    		}
        	if(!"GET".equalsIgnoreCase(method)&& !"DELETE".equalsIgnoreCase(method)) {
        		String queryString = genQueryString(params);
        		 StringEntity entity = new StringEntity(queryString,"utf-8");
        		((HttpEntityEnclosingRequestBase)request).setEntity(entity);
        	}
        	
        	response = client.execute(request);
        	int statusCode = response.getStatusLine().getStatusCode();
        	String reasonPhrase = response.getStatusLine().getReasonPhrase();
        	String content = EntityUtils.toString(response.getEntity());
        	if(statusCode >= 400) {
    			throw new RuntimeException(String.format("code:%s,message:%s,content:%s", statusCode,reasonPhrase,content));
    		}
        	return JSONObject.parseObject(content);
		} finally {
			HttpClientUtils.closeQuietly(client);
			HttpClientUtils.closeQuietly(response);
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
