package com.nis.modules.endpoint.service.impl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.endpoint.entity.MetadataMetric;
import com.nis.modules.endpoint.service.MetadataMetricService;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlPath;
import cn.hutool.log.Log;

@Service
public class MetadataMetricServiceImpl implements MetadataMetricService {

	private static Log logger = Log.get();

	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	@Autowired
	private PromserverService promService;
	
	@Override
	public List<MetadataMetric> metadata(Integer endpointId) throws IOException {
		// 返回结果
		List<MetadataMetric> metrics = new ArrayList<>();
		String endpoint = "{" + Constant.PROMSERVERUP_ENDPOINT_LABEL + "=\"" + endpointId + "\"}";
		String url = "/api/v1/targets/metadata";
		Promserver server = null;
		Integer pid = null;
		if (endpointId != null) {// endpointId 不为空则通过 redis key 获取prometheus id
			HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
			String pidStr = opsForHash.get(Constant.REDIS_KEY_ENDPOINT_PROM, endpointId.toString());
			pid = Tool.NumberUtil.parseInt(pidStr);
		}
		server = server != null ? server
				: promService.getOneAvaPromServer(pid, null, Constant.PromserverType.GLOBAL.getValue());
		String promPathPrefix = promService.getPromPathPrefix();
		String proxyPath = String.format("%s%s", promPathPrefix, url);
		proxyPath = UrlBuilder.create().setScheme(server.getProtocol()).setHost(server.getHost())
				.setPort(server.getPort()).setPath(UrlPath.of(proxyPath, Charset.forName("UTF-8")))
				.addQuery("match_target", endpoint).toURL().toString();
		// 发送请求
		JSONObject result = AgentUtil.requestGet(url, server.getToken(), JSONObject.class);
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
