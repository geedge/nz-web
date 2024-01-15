package com.nis.modules.endpoint.service.impl;

import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.service.PromApiService;
import com.nis.modules.endpoint.entity.MetadataMetric;
import com.nis.modules.endpoint.service.MetadataMetricService;
import com.nis.modules.endpoint.service.MonitorEndpointAgentMapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
public class MetadataMetricServiceImpl implements MetadataMetricService {

	private static Log logger = Log.get();

	@Autowired
	private PromApiService promApiService;

	@Autowired
	private MonitorEndpointAgentMapService endpointAgentMapService;
	
	@Override
	public List<MetadataMetric> metadata(Integer endpointId) throws IOException {
		// 返回结果
		List<MetadataMetric> metrics = new ArrayList<>();

		Integer agentId = endpointAgentMapService.getAgentId(endpointId);

		String queryEndpoint = "{" + Constant.PROMSERVERUP_ENDPOINT_LABEL + "=\"" + endpointId + "\"}";

		// 发送请求
		JSONObject result = promApiService.targetsMetadata(null, Tool.StrUtil.toStringOrNull(agentId), null, null, queryEndpoint, null, null);
		if (result != null && "success".equals(result.get("status"))) {
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
