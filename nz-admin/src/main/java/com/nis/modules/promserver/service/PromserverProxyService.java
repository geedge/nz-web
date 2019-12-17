package com.nis.modules.promserver.service;

import com.alibaba.fastjson.JSONObject;

import java.util.Map;

public interface PromserverProxyService {
    // /api/v1/query
    JSONObject query(String api, Map<String, Object> params);

    // /api/v1/query_range
    JSONObject queryRange(String api, Map<String, Object> params);

    // /api/v1/series
    JSONObject series(String api, Map<String, Object> params);

    // /api/v1/labels
    JSONObject labels(String api, Map<String, Object> params);

    // /api/v1/label/<label_name>/values
    JSONObject label(String api, Map<String, Object> params);

    JSONObject excuteMethod(String methodName, String api, Map<String, Object> params);

    JSONObject defaultExcute(String api, Map<String, Object> params);
}
