package com.nis.modules.agent.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.Header;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.nis.common.utils.Tool;
import org.apache.http.Consts;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.nis.common.utils.Constant;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.sys.service.SysConfigService;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlPath;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.log.Log;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

	public static <T> T requestGet(Agent server, String path, String queryString, Class<T> responseType) {
		return request(server, HttpMethod.GET, path, queryString, null, responseType);
	}

	public static <T> T request(Agent server, HttpMethod method, String path, String queryString, Object body,
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

	/**
	 * GET 请求
	 *
	 * @param server
	 * @param url
	 * @param requestParam 支持 key - value & key - values
	 * @return
	 */
	public static JSONObject getForObject(Agent server, String url, Map requestParam) {
		UrlBuilder urlBuilder = UrlBuilder.create()
				.setScheme(server.getProtocol())
				.setHost(server.getHost())
				.setPort(server.getPort())
				.setPath(UrlPath.of(url, Charset.forName("UTF-8")));

		UrlQuery urlQuery = new UrlQuery();
		Iterator<Map.Entry<String, Object>> iterator = ((Map<String, Object>) requestParam).entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, Object> entry = iterator.next();
			Object value = entry.getValue();
			if (value == null) continue;
			if (value instanceof Collection) {
				List list = new ArrayList((Collection) value);
				list.stream().forEach(obj -> urlQuery.add(entry.getKey(), obj.toString()));
				iterator.remove();
			}
		}

		urlQuery.addAll(requestParam);
		String urlStr = urlBuilder.setQuery(urlQuery).toString();

		// 添加 agent token
		HttpHeaders headers = new HttpHeaders();
		// agent token
		headers.add(Constant.AUTH_TOKEN_CODE, server.getToken());

		HttpEntity entity = new HttpEntity(headers);
		ResponseEntity<String> responseEntity = null;
		try {
			responseEntity = restTemplate.exchange(new URI(urlStr), HttpMethod.GET, entity, String.class);
		} catch (URISyntaxException e) {
			log.error(e);
		}
		log.info("agent request url:{}, responseStatus:{}", urlStr, responseEntity.getStatusCodeValue());

		if (log.isDebugEnabled()) {
			log.debug("agent request body:{}", urlStr, responseEntity.getStatusCodeValue(), responseEntity.getBody());
		}
		return JSONObject.parseObject(responseEntity.getBody(), JSONObject.class, Feature.OrderedField);
	}


	/**
	 * agent api 代理接口
	 *
	 * @param server
	 * @param url
	 * @param request
	 * @param response
	 */
	public static void agentApiProxy(Agent server, String url, HttpServletRequest request, HttpServletResponse response) {
		try {
			// 重新构造请求参数内容，header body 添加 agent token
			HttpHeaders headers = new HttpHeaders();
			Enumeration<String> headerNames = request.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String hn = headerNames.nextElement();
				if (Constant.AUTH_TOKEN_CODE.equalsIgnoreCase(hn)) {
					continue;
				}
				headers.add(hn, request.getHeader(hn));
			}
			// agent token
			headers.add(Constant.AUTH_TOKEN_CODE, server.getToken());

			byte[] body = Tool.IoUtil.readBytes(request.getInputStream());
			HttpEntity httpEntity = new HttpEntity(body, headers);

			String queryString = request.getQueryString();
			queryString = StrUtil.isNotBlank(queryString) ? queryString : "";
			queryString = URLUtil.decode(queryString);

			String urlStr = UrlBuilder.create()
					.setScheme(server.getProtocol())
					.setHost(server.getHost())
					.setPort(server.getPort())
					.setPath(UrlPath.of(url, Charset.forName("UTF-8")))
					.setQuery(UrlQuery.of(queryString, Charset.forName("UTF-8"))).toString();

			String method = request.getMethod();
			ResponseEntity<byte[]> exchange = restTemplate.exchange(new URI(urlStr), HttpMethod.resolve(method), httpEntity, byte[].class);

			int statusCode = exchange.getStatusCodeValue();
			log.info("agent request url:{}, responseStatus:{}", urlStr, statusCode);

			HttpHeaders httpHeaders = exchange.getHeaders();

			byte[] responseBody = exchange.getBody();
			response.reset();
			response.setStatus(statusCode);
			Set<Map.Entry<String, List<String>>> entrySet = httpHeaders.entrySet();
			// 设置 cors 响应头
			Constant.CORS_HEADER.forEach((k, v) -> {
				response.setHeader(k, v);
			});
			for (Map.Entry<String, List<String>> en : entrySet) {
				String name = en.getKey();
				List<String> value = en.getValue();
				if (en.getKey().equalsIgnoreCase(Header.CONTENT_LENGTH.getValue())) {
					continue;
				}
				if (en.getKey().equalsIgnoreCase(Header.TRANSFER_ENCODING.getValue())) {
					continue;
				}
				response.setHeader(name, Tool.StrUtil.join(",", value.toArray()));
			}

			response.setContentLength(Tool.ArrayUtil.length(responseBody));
			response.getOutputStream().write(responseBody);
			response.flushBuffer();
		} catch (Exception e) {
			log.error(e, "agent proxy api error, url: {}", url);
		}
	}

	/**
	 * agent ota 代理接口
	 *
	 * @param url
	 * @param method
	 * @param token
	 * @param file
	 * @param md5
	 */
	public static <T> T requestPost(String url, HttpMethod method, String token, File updateFile, String md5){
		// 创建httpclient对象
		CloseableHttpClient httpClient = HttpClients.createDefault();
		// 创建httpPost请求
		HttpPost httppost = new HttpPost(url);
		try {
			MultipartEntity entity = new MultipartEntity(HttpMultipartMode.RFC6532, null, Charset.forName("UTF-8"));
			// 文件参数
			FileBody filebody = new FileBody(updateFile);
			entity.addPart("file", filebody);
			entity.addPart("md5", new StringBody(md5, Consts.UTF_8));
			//token
			httppost.addHeader(HttpHeaders.AUTHORIZATION, token);
			httppost.setEntity(entity);
			// 发送请求
			httpClient.execute(httppost);
		} catch (IOException e) {
			log.error("agent update failed",500);
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				log.error(e);
			}
		}
		return null;
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
