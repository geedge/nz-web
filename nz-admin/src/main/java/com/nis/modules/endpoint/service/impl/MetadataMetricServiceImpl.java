package com.nis.modules.endpoint.service.impl;

import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.Constant;
import com.nis.common.utils.DigestUtils;
import com.nis.modules.endpoint.entity.MetadataMetric;
import com.nis.modules.endpoint.service.MetadataMetricService;
import com.nis.modules.promserver.service.impl.PromserverProxyServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class MetadataMetricServiceImpl implements MetadataMetricService {

	private static Log logger = Log.get();

	@Autowired
	private PromserverProxyServiceImpl promserverProxyService;

	@Override
	public List<MetadataMetric> metadata(Integer endpointId) throws IOException{
		// 返回结果
		List<MetadataMetric> metrics = new ArrayList<>();
		Map<String,Object> queryParam = new HashMap<String,Object>();
		String endpoint = "{" + Constant.PROMSERVERUP_ENDPOINT_LABEL + "=\"" + endpointId + "\"}";
		queryParam.put("match_target", DigestUtils.urlEncode(endpoint));
		String url = "/api/v1/targets/metadata";
		JSONObject result = promserverProxyService.promRequest(url, "GET",queryParam, null, null, 2, endpointId);
		if (result != null && result.get("status").equals("success")) {
			JSONArray array = (JSONArray) result.get("data");
			JSONObject jsonObject;
			MetadataMetric metadataMetric;
			for (int i = 0; i < array.size(); i++) {
				jsonObject = (JSONObject) array.get(i);
				metadataMetric = JSONObject.parseObject(jsonObject.toJSONString(), MetadataMetric.class);
				if (metadataMetric != null) {
					metrics.add(metadataMetric);
				}
			}
			// 去除重复数据
			metrics = new ArrayList<>(new HashSet<>(metrics));
			return metrics;
		} else {
			logger.error("query api/v1/targets/metadata error {}", result);
		}
		return metrics;
	}

}
