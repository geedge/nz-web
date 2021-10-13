package com.nis.modules.agent.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.nis.common.utils.Constant;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.sys.service.SysConfigService;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlPath;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.log.Log;

@Configuration
@SuppressWarnings("all")
public class AgentUtil {

	private static Log log = Log.get();
	private static RestTemplate restTemplate;
	private static SysConfigService sysConfigService;

	/**
	 * 加载 cmdTmpl 字符串 并转为 map
	 * 
	 * @param configTmplStr
	 * @return
	 */
	public static Map<String, String> loadServiceConfigStr(String configTmplStr) {
		// 匹配有参类型 --[\w\.-]+?\s 可匹配无参类型
		Pattern pattern = Pattern.compile("--([\\w\\.-]+?)='?(.+?)'?\\s+?");
		Matcher matcher = pattern.matcher(configTmplStr);

		Map<String, String> result = new HashMap<>(16);
		while (matcher.find()) {
			result.put(matcher.group(1).trim(), matcher.group(2).trim());
		}
		return result;
	}

	public static <T> T requestGet(Promserver server, String path, String queryString, Class<T> responseType) {
		return request(server, HttpMethod.GET, path, queryString, null, responseType);
	}

	public static <T> T request(Promserver server, HttpMethod method, String path, String queryString, Object body,
			Class<T> responseType) {
		String agentPath = sysConfigService.getValueOrDefault(Constant.CONFAGENT_PATH, "");
		path = path.startsWith("/") ? (String.format("%s%s", agentPath,path)) :(String.format("%s/%s", agentPath,path));
		String urlString = UrlBuilder.create().setScheme(server.getProtocol()).setHost(server.getHost())
				.setPort(server.getPort()).setPath(UrlPath.of(path, Charset.forName("UTF-8")))
				.setQuery(UrlQuery.of(queryString, Charset.forName("UTF-8"))).toString();
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.AUTHORIZATION, server.getToken());
		HttpEntity httpEntity = body == null ? new HttpEntity(headers) : new HttpEntity(body, headers);
		// 发送 请求
		return request(urlString, method, server.getToken(), body, responseType);
	}

	
	
	public static <T> T request(String url, HttpMethod method, String token, Object body, Class<T> responseType) {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.AUTHORIZATION, token);
		HttpEntity httpEntity = body == null ? new HttpEntity(headers) : new HttpEntity(body, headers);
		// 发送 请求
		ResponseEntity<T> exchange = null;
		try {
			exchange = restTemplate.exchange(new URI(url), method, httpEntity, responseType);
		} catch (URISyntaxException e) {
			log.error(e);
		}
		return exchange.getBody();
	}

	public static <T> T requestGet(String url,String token, Class<T> responseType) {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.AUTHORIZATION, token);
		HttpEntity httpEntity = new HttpEntity(headers);
		// 发送 请求
		ResponseEntity<T> exchange = restTemplate.exchange(url, HttpMethod.GET, httpEntity, responseType);
		return exchange.getBody();
	}
	
	@Autowired
	public void setRestTemplate(RestTemplate restTemplate) {
		AgentUtil.restTemplate = restTemplate;
	}
	
	@Autowired
	public void setSysconfigService(SysConfigService sysConfigService) {
		AgentUtil.sysConfigService = sysConfigService;
	}
}
