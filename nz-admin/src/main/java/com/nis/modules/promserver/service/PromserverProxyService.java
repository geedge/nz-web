package com.nis.modules.promserver.service;

import java.util.Map;

import com.alibaba.fastjson.JSONObject;

public interface PromserverProxyService {

	JSONObject promRequest(String url, String method, Map<String, Object> params, Integer promId, Integer idcId,
			Integer type, Integer endpointId) throws Exception;

	// /api/v1/query
	JSONObject query(Map<String, Object> params) throws Exception;

	// /api/v1/query_range
	JSONObject queryRange(Map<String, Object> params) throws Exception;

	// /api/v1/series
	JSONObject series(Map<String, Object> params) throws Exception;

	// /api/v1/labels
	JSONObject labels(Map<String, Object> params) throws Exception;


}
