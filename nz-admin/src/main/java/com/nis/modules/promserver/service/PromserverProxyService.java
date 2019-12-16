package com.nis.modules.promserver.service;

import com.alibaba.fastjson.JSONObject;
import com.nis.modules.promserver.entity.Promserver;

import java.util.List;
import java.util.Map;

public interface PromserverProxyService {
    JSONObject requestPromAPI(String url);

    String getPromServer();


    String buildUrl(String baseUrl, Map<String, Object> params, Promserver promserver);

    Long formatTimeToUnixTimestemp(String time);

    List<Map<String, Object>> formateTargetsData(JSONObject responseData);
}
