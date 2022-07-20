package com.nis.modules.endpoint.service.impl;

import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.service.PromApiService;
import com.nis.modules.endpoint.entity.MetadataMetric;
import com.nis.modules.endpoint.service.MetadataMetricService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
public class MetadataMetricServiceImpl implements MetadataMetricService {

	private static Log logger = Log.get();

	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	@Autowired
	private PromApiService promApiService;
	
	@Override
	public List<MetadataMetric> metadata(Integer endpointId) throws IOException {
		// 返回结果
		List<MetadataMetric> metrics = new ArrayList<>();

		Integer pid = null;
		if (endpointId != null) {// endpointId 不为空则通过 redis key 获取prometheus id
			HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
			String pidStr = opsForHash.get(Constant.REDIS_KEY_ENDPOINT_PROM, endpointId.toString());
			pid = Tool.NumberUtil.parseInt(pidStr);
		}

		String queryEndpoint = "{" + Constant.PROMSERVERUP_ENDPOINT_LABEL + "=\"" + endpointId + "\"}";

		// 发送请求
		JSONObject result = promApiService.targetsMetadata(null, pid.toString(), null, null, queryEndpoint, null, null);
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
