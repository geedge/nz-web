package com.nis.modules.agent.util;

import cn.hutool.core.lang.Opt;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlPath;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.Header;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.nis.common.exception.NZException;
import com.nis.common.protobuf.Remote.WriteRequest;
import com.nis.common.protobuf.Types.Label;
import com.nis.common.protobuf.Types.TimeSeries;
import com.nis.common.utils.CommonUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.PromApiService;
import com.nis.modules.sys.service.SysConfService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.xerial.snappy.Snappy;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Configuration
@SuppressWarnings("all")
public class AgentUtil {

	private static Log log = Log.get();
	private static RestTemplate restTemplate;
	private static SysConfService sysConfService;
	private static PromApiService promApiService;
	
	@Value("${nezha.installPath:install/}")
	private String installPath;

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
		String agentPath = sysConfService.getValueOrDefault(Constant.CONFAGENT_PATH, "");
		path = path.startsWith("/") ? (String.format("%s%s", agentPath, path))
				: (String.format("%s/%s", agentPath, path));
		/*String urlString = UrlBuilder.create().setScheme(server.getProtocol()).setHost(server.getHost())
				.setPort(server.getPort()).setPath(UrlPath.of(path, Charset.forName("UTF-8")))
				.setQuery(UrlQuery.of(queryString, Charset.forName("UTF-8"), false, true)).toString();*/
		String urlString = new UrlBuilder(
				server.getProtocol(),
				server.getHost(),
				Integer.valueOf(server.getPort()),
				UrlPath.of(path, Charset.forName("UTF-8")),
				UrlQuery.of(queryString, Charset.forName("UTF-8"), false, true),
				null,
				StandardCharsets.UTF_8).toString();

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

	public static <T> T requestGet(String url, String token, Class<T> responseType) {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.AUTHORIZATION, token);
		HttpEntity httpEntity = new HttpEntity(headers);
		// 发送 请求
		ResponseEntity<T> exchange = restTemplate.exchange(url, HttpMethod.GET, httpEntity, responseType);
		return exchange.getBody();
	}

	/**
	 * 返回 html
	 * @param url
	 * @param token
	 * @param responseType: String.class
	 * @param <T>
	 * @return
	 */
	public static <T> T requestAgentProxy(String url, String token, Class<T> responseType) {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.AUTHORIZATION, token);
		HttpEntity httpEntity = new HttpEntity(headers);
		List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
		for(HttpMessageConverter<?> converter : converters){
			if(converter instanceof MappingJackson2HttpMessageConverter){
				try{
					((MappingJackson2HttpMessageConverter) converter).setSupportedMediaTypes(Arrays.asList(MediaType.TEXT_HTML));
				}catch(Exception e){
					log.error(e);
				}
			}
		}
		// 发送 请求
		ResponseEntity<T> exchange = restTemplate.exchange(url, HttpMethod.GET, httpEntity, responseType);
		return exchange.getBody();
	}

	/**
	 *  prometheus remote write
	 * @param url
	 * @param token
	 * @param body
	 * @return
	 * @throws IOException 
	 * @throws UnsupportedEncodingException 
	 * @throws URISyntaxException 
	 * @throws RestClientException 
	 */
	public static ResponseEntity<String> remoteWrite(List<TimeSeries> timeSeriesList) throws UnsupportedEncodingException, IOException, RestClientException, URISyntaxException {
		Agent server = promApiService.getOneAvaliableGlobalAgent();
		String pathPrefix = promApiService.getPromApiProxyPathPrefix();
		// cortex /api/v1/push 接口，不存在 /prometheus 前缀，此处 replaceAll 处理
		String path = Tool.StrUtil.contains(pathPrefix, "/cortex") ? String.format("%s%s", pathPrefix.replaceAll("/prometheus", ""),"/api/v1/push") : String.format("%s%s", pathPrefix,"/api/v1/write");
		String token = server.getToken();
		Label label = Label.newBuilder().setName("nz_agent_id").setValue(Tool.StrUtil.toString(server.getId())).build();
		// 构造请求路径
		UrlBuilder urlBuilder = UrlBuilder.create().setScheme(server.getProtocol()).setHost(server.getHost())
				.setPort(server.getPort()).setPath(UrlPath.of(path, Charset.forName("UTF-8")));
		path = urlBuilder.build();
		log.debug("remote write path: {}",path);
		// 设置请求头
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.AUTHORIZATION, token);
		headers.add("Content-Encoding","snappy");
		headers.add("Content-Type","application/x-protobuf");
		headers.add("User-Agent","network zodiac");
		headers.add("X-Prometheus-Remote-Write-Version", "0.1.0");
		// 遍历 timeseries 添加 nz_agent_id 标签，避免Global prometheus缺少数据的问题
		for (int i = 0, j = timeSeriesList.size(); i < j; i++) {
			TimeSeries ts = timeSeriesList.get(i);
			List<Label> labelsList = ts.toBuilder().getLabelsList();
			labelsList = Tool.CollUtil.defaultIfEmpty(labelsList, Tool.ListUtil.list(true));
			Label nzAgentIdLabel = labelsList.stream().filter(tempLabel -> StrUtil.equals(tempLabel.getName(), "nz_agent_id")).findFirst().orElse(null);
			if (ObjectUtil.isNotNull(nzAgentIdLabel)) {
				List<Label> finalLabelsList = labelsList;
				int index = IntStream.range(0, labelsList.size())
						.filter(value -> StrUtil.equals("nz_agent_id", finalLabelsList.get(value).getName()))
						.findFirst().orElse(-1);
				TimeSeries nts = ts.toBuilder().removeLabels(index).addLabels(label).build();
				timeSeriesList.set(i, nts);
			} else {
				TimeSeries nts = ts.toBuilder().addLabels(label).build();
				timeSeriesList.set(i, nts);
			}
		}
		//protobuf 序列化
		WriteRequest writeRequest = WriteRequest.newBuilder().addAllTimeseries(timeSeriesList).build();
		byte[] byteArray = writeRequest.toByteArray(); // protobuf 序列化
		byte[] compress = Snappy.compress(byteArray); // snappy 压缩
		HttpEntity httpEntity = new HttpEntity(compress, headers);
		// 发送 请求
		ResponseEntity<String> exchange = null;
		exchange = restTemplate.exchange(new URI(path), HttpMethod.POST, httpEntity,String.class);
		return exchange;
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
		/*UrlBuilder urlBuilder = UrlBuilder.create().setScheme(server.getProtocol()).setHost(server.getHost())
				.setPort(server.getPort()).setPath(UrlPath.of(url, Charset.forName("UTF-8")));*/

		UrlBuilder urlBuilder = new UrlBuilder(server.getProtocol(),
				server.getHost(),
				Integer.valueOf(server.getPort()),
				UrlPath.of(url, Charset.forName("UTF-8")),
				null,
				null,
				StandardCharsets.UTF_8);

		UrlQuery urlQuery = new UrlQuery(true);
		Iterator<Map.Entry<String, Object>> iterator = ((Map<String, Object>) requestParam).entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, Object> entry = iterator.next();
			Object value = entry.getValue();
			if (value == null)
				continue;
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
		} catch (Exception e) {
			log.error(e, "agent request error. url:{}", urlStr);
			String message = e.getMessage();
			if (ObjectUtil.isNotNull(e.getCause())) {
				message = e.getCause().getMessage();
			}
			throw new NZException(message, HttpStatus.INTERNAL_SERVER_ERROR.value());
		}

		Integer getStatusCodeValue = Opt.ofNullable(responseEntity).map(ResponseEntity::getStatusCodeValue).get();
		String responseBody = Opt.ofNullable(responseEntity).map(ResponseEntity::getBody).get();

		log.info("agent request url:{}, responseStatus:{}", urlStr, getStatusCodeValue);
		if (log.isDebugEnabled()) {
			log.debug("agent request url:{} responseStatus: {} body:{}", urlStr, getStatusCodeValue, responseBody);
		}

		if (Tool.ObjectUtil.equal(HttpStatus.OK.value(), getStatusCodeValue)) {
			return JSONObject.parseObject(responseBody, JSONObject.class, Feature.OrderedField);
		} else {
			log.warn("agent request error. url:{} responseStatus: {} response: {}", urlStr, getStatusCodeValue, responseBody);
			throw new NZException(responseBody, getStatusCodeValue);
		}
	}

	/**
	 * agent ota 升级
	 * 
	 * @param agent
	 * @return
	 */
	public String agentOTA(Agent agent) {
		// 获取agent更新包
		String json = sysConfService.getValue("confagent_name");
		String agentPath = sysConfService.getValue(Constant.CONFAGENT_PATH);
		UrlBuilder urlBuilder = UrlBuilder.create().setScheme(agent.getProtocol()).setHost(agent.getHost())
				.setPort(agent.getPort())
				.setPath(UrlPath.of(String.format("%s%s", agentPath, "/ota"), Charset.forName("UTF-8")));
		String url = urlBuilder.build();
		/**
		 * 1、获取文件，并计算md5
		 */
		File rpmFile = this.getAgentRpmFile("centos");
		// 获取文件md5
		String updateFileMd5 = Tool.DigestUtil.md5Hex(rpmFile);
		// md5加密
		String md5 = Tool.AesUtil.encrypt(updateFileMd5, Constant.AES_SECRET_KEY);
		/**
		 * 2、构建post 请求参数
		 */
		// 添加 agent token
		HttpHeaders headers = new HttpHeaders();
		headers.add(Constant.AUTH_TOKEN_CODE, agent.getToken());
		MediaType type = MediaType.parseMediaType("multipart/form-data");
		headers.setContentType(type);
		// 设置请求体，注意是LinkedMultiValueMap
		FileSystemResource fileSystemResource = new FileSystemResource(rpmFile);
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
		form.add("file", fileSystemResource);
		form.add("md5", md5);
		// 用HttpEntity封装整个请求报文
		HttpEntity<MultiValueMap<String, Object>> files = new HttpEntity<>(form, headers);
		log.info("agent ota, url: {},file:{},md5:{},agent:{}", url, rpmFile, updateFileMd5,
				Tool.JSONUtil.toJsonStr(agent));
		/**
		 * 3、发送请求
		 */
		String result = restTemplate.postForObject(url, files, String.class);
		if (log.isDebugEnabled()) {
			log.debug("agent ota result: {}", result);
		}
		return result;
	}

	/**
	 * 获取 agent rpm file 对象
	 * 
	 * @param os 默认：centos
	 * @return
	 */
	public File getAgentRpmFile(String os) {
		os = Tool.StrUtil.isEmpty(os) ? "centos" : os;
		String json = sysConfService.getValue("confagent_name");
		JSONObject confagentNames = JSONObject.parseObject(json);
		// 配置的正则
		String confagentName = (String) confagentNames.get(os);
		Pattern confagentPattern = Pattern.compile(confagentName);
		try {
			File installPathFile = CommonUtils.getResourceFile(installPath);
			log.info("installPath exist: {},isDir:{}", installPathFile == null ? false : installPathFile.exists(),
					installPathFile == null ? false : installPathFile.isDirectory());
			if (installPathFile == null || !installPathFile.isDirectory()) {
				throw new NZException("file download error", 500);
			}

			File[] listFiles = installPathFile.listFiles((file, name) -> {
				return confagentPattern.matcher(name).find();
			});

			if (Tool.ArrayUtil.isEmpty(listFiles)) {
				throw new NZException("file download error", 500);
			}
			File file = CommonUtils.getResourceFile(listFiles[0].getAbsolutePath());
			return file;
		} catch (Exception e) {
			log.error(e, "agent rpm error");
			throw new NZException("file download error", 500);
		}
	}

	/**
	 * agent api 代理接口
	 *
	 * @param server
	 * @param url
	 * @param request
	 * @param response
	 */
	public static void agentApiProxy(Agent server, String url, HttpServletRequest request,
			HttpServletResponse response) {
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

			/*String urlStr = UrlBuilder.create().setScheme(server.getProtocol()).setHost(server.getHost())
					.setPort(server.getPort()).setPath(UrlPath.of(url, Charset.forName("UTF-8")))
					.setQuery(UrlQuery.of(queryString, Charset.forName("UTF-8"), false, true)).toString();*/

			String urlStr = new UrlBuilder(
					server.getProtocol(),
					server.getHost(),
					Integer.valueOf(server.getPort()),
					UrlPath.of(url, Charset.forName("UTF-8")),
					UrlQuery.of(queryString, Charset.forName("UTF-8"), false, true),
					null,
					StandardCharsets.UTF_8).toString();

			String method = request.getMethod();
			ResponseEntity<byte[]> exchange = restTemplate.exchange(new URI(urlStr), HttpMethod.valueOf(method),
					httpEntity, byte[].class);

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

	@Autowired
	public void setRestTemplate(RestTemplate restTemplate) {
		AgentUtil.restTemplate = restTemplate;
	}

	@Autowired
	public void setSysConfService(SysConfService sysConfService) {
		AgentUtil.sysConfService = sysConfService;
	}
	
	@Autowired
	public void setPromApiService(PromApiService promApiService) {
		AgentUtil.promApiService = promApiService;
	}
}
