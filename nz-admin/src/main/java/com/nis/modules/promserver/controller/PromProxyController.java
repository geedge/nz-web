package com.nis.modules.promserver.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mysql.cj.x.protobuf.MysqlxDatatypes;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.promserver.util.PromConstant;
import com.nis.modules.sys.dao.SysConfigDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlPath;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.Header;
import cn.hutool.log.Log;

@Controller
@SuppressWarnings("all")
public class PromProxyController {

	private static final Log logger = Log.get();

	@Autowired
	private PromserverService promserverService;

	@Autowired
	private SysConfigDao sysConfigDao;

	@Autowired
	private RedisTemplate redisTemplate;

	@Value("${connect.timeout:30000}")
	private Integer connectTimeout;
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private SysConfigService sysConfigService;
	
	@RequestMapping(value = "/prom/**")
	public void promProxy(HttpServletRequest request, HttpServletResponse response) {
		String promPath = request.getServletPath().substring(5);
		Integer agentId = request.getParameter("agentId") != null ? Integer.valueOf(request.getParameter("agentId")):null;
		Integer dcId = request.getParameter("dcId") != null ? Integer.valueOf(request.getParameter("dcId")):null;
		Integer type = request.getParameter("type") != null ? Integer.valueOf(request.getParameter("type")):null;

		switch (promPath) {
		case "/api/v1/query":
			// 验证query 参数不能为空
			ValidateUtils.is(request.getParameter("query")).notNull(RCode.PROMSERVER_PARAM_QUERY_ISNULL);
			break;
		case "/api/v1/query_range":
			// 验证query 参数不能为空
			ValidateUtils.is(request.getParameter("query")).notNull(RCode.PROMSERVER_PARAM_QUERY_ISNULL);
			// 验证start 参数不能为空
			ValidateUtils.is(request.getParameter("start")).notNull(RCode.PROMSERVER_PARAM_START_ISNULL);
			// 验证end 参数不能为空
			ValidateUtils.is(request.getParameter("end")).notNull(RCode.PROMSERVER_PARAM_END_ISNULL);
			// 验证step 参数不能为空
			ValidateUtils.is(request.getParameter("step")).notNull(RCode.PROMSERVER_PARAM_STEP_ISNULL);
			break;
		case "/api/v1/series":
			// 验证step 参数不能为空
			ValidateUtils.is(request.getParameter("match[]")).notNull(RCode.PROMSERVER_PARAM_MATCH_ISNULL);
			break;
		default:
			break;
		}

		Promserver server = promserverService.getOneAvaPromServer(agentId, dcId, type);
		if (ToolUtil.isEmpty(server)) {
			// type 其次使用 global
			type = PromConstant.PROMSERVER_GLOBAL;
			server = promserverService.getOneAvaPromServer(agentId, dcId, type);
			ValidateUtils.is(server).notNull(RCode.PROMSERVER_UNAVAILABLE);
		}
		ByteArrayInputStream bais = null;
		try {
        	// 复制请求头
    		HttpHeaders headers = new HttpHeaders();
    		Enumeration<String> headerNames = request.getHeaderNames();
    		while (headerNames.hasMoreElements()) {
    			String hn = headerNames.nextElement();
    			if(Constant.AUTH_TOKEN_CODE.equalsIgnoreCase(hn)){
    				headers.add(hn, request.getHeader(hn));
				}
    		}
    		headers.add(Constant.AUTH_TOKEN_CODE, server.getToken());
    		// 复制 body
    		byte[] body = Tool.IoUtil.readBytes(request.getInputStream());
    		// 重新构造请求参数内容
    		HttpEntity<byte[]> httpEntity = new HttpEntity<byte[]>(body, headers);
    		String queryString = request.getQueryString();
            queryString = StrUtil.isNotBlank(queryString)?queryString:"";
            queryString = URLUtil.decode(queryString);
            if(StrUtil.equals(promPath, "/api/v1/query_range") || StrUtil.equals(promPath, "/api/v1/query")) {
            	if(ReUtil.contains("\\[\\s*\\$interval\\s*]", URLUtil.decode(queryString))) {
            		// 判断step是否小于两个周期的scrape interval
            		Integer defaultScrapeInterval = Integer.valueOf(sysConfigService.getValue(Constant.SYSCONFIG_KEY_SCRAPEINTERVAL));
                	String step = request.getParameter("step");
                	if(StrUtil.isNotBlank(step)) {
                		Integer stepVal = Integer.valueOf(StrUtil.sub(step, 0, step.length()-1));
                    	if(step.endsWith("s")) {
                    		if(stepVal < defaultScrapeInterval*2) {
                    			step = defaultScrapeInterval * 2 + "s";
                    		}
                    	}else if(step.endsWith("m")) {
                    		if(stepVal * 60 < defaultScrapeInterval*2) {
                    			step = defaultScrapeInterval * 2 + "s";
                    		}
                    	}else {
                    		if(stepVal * 60 * 60 < defaultScrapeInterval*2) {
                    			step = defaultScrapeInterval * 2 + "s";
                    		}
                    	}
                	}else {
                		step = defaultScrapeInterval * 2 + "s";
                	}
                	// 正则替换 当查询语句中有[]时  将时间替换为step
                	queryString = URLUtil.encode(URLUtil.decode(queryString).replaceAll("\\[\\s*\\$interval\\s*]", "["+step+"]"));
            	}
            }
            // 构造请求路径
    		String promPathPrefix = promserverService.getPromPathPrefix();
			if (promPath.endsWith("targets")){
				//获取 targets代理 path
				String agentPath = sysConfigService.getValue(Constant.CONFAGENT_PATH);
				promPathPrefix = String.format("%s%s",agentPath, "/prometheus/proxy");
				//获取本身 agent
				server = promserverService.getOne(new LambdaQueryWrapper<Promserver>()
						.eq(ToolUtil.isNotEmpty(dcId),Promserver::getDcId,dcId)
						.eq(ToolUtil.isNotEmpty(type),Promserver::getType,type)
						.eq(ToolUtil.isNotEmpty(agentId),Promserver::getId,agentId));
			}
    		String proxyPath = String.format("%s%s", promPathPrefix, promPath);
            String urlStr = UrlBuilder.create().setScheme(server.getProtocol()).setHost(server.getHost())
            		.setPort(server.getPort()).setPath(UrlPath.of(proxyPath, Charset.forName("UTF-8")))
            		.setQuery(UrlQuery.of(queryString, Charset.forName("UTF-8"))).toString();
            String method = request.getMethod();
            //发送 请求
			ResponseEntity<byte[]> exchange = restTemplate.exchange(new URI(urlStr), HttpMethod.resolve(method), httpEntity, byte[].class);
    		//响应码
    		int statusCode = exchange.getStatusCodeValue();
    		//响应头
    		HttpHeaders httpHeaders = exchange.getHeaders();
    		byte[] responseBody = exchange.getBody();
    		response.reset();
    		response.setStatus(statusCode);
    		Set<Entry<String,List<String>>> entrySet = httpHeaders.entrySet();
    		//设置cors响应头
    		Constant.CORS_HEADER.forEach((k,v)->{
    			response.setHeader(k, v);
    		});
    		for(Entry<String,List<String>> en : entrySet) {
    			String name = en.getKey();
    			List<String> value = en.getValue();
    			if(en.getKey().equalsIgnoreCase(Header.CONTENT_LENGTH.getValue())) {
    				continue;
    			}
    			if(en.getKey().equalsIgnoreCase(Header.TRANSFER_ENCODING.getValue())) {
    				continue;
    			}
    			response.setHeader(name, Tool.StrUtil.join(",", value.toArray()));
    		}
    		/*
    		 * query_range 补零
    		 */
    		if ("/api/v1/query_range".equals(promPath) && statusCode < 300) {
				// 处理缺失时间数据
    			List<String> list = httpHeaders.get(HttpHeaders.CONTENT_ENCODING);
				String contentEncoding = Tool.CollUtil.isNotEmpty(list)?list.get(0):"";
				boolean isGzipStream = StrUtil.equalsIgnoreCase("gzip", contentEncoding);
				bais = new ByteArrayInputStream(responseBody);
				Map map = handleResultData(request,bais,isGzipStream, null);
				// 压缩
				responseBody = isGzipStream ? compressedIntoGzipBytes(JSONObject.toJSONString(map), "UTF-8") : JSONObject.toJSONString(map).getBytes("UTF-8");
			}
    		response.setContentLength(Tool.ArrayUtil.length(responseBody));
    		response.getOutputStream().write(responseBody);
    		response.flushBuffer();
		} catch (Exception e) {
			logger.error(e, "prometheus proxy error,path : {}", promPath);
		} finally {
			Tool.IoUtil.close(bais);
		}
	}

	public Map handleResultData(HttpServletRequest request, InputStream connInputStream, Boolean isGzipStream,Map<String, String> paramData) {
		StringBuffer resultStr = new StringBuffer();
		InputStreamReader inputStreamReader = null;
		BufferReaderWrapper bufferedReader = null;
		try {
			inputStreamReader = new InputStreamReader(isGzipStream ? new GZIPInputStream(connInputStream) : connInputStream, "UTF-8");
			bufferedReader = new BufferReaderWrapper(inputStreamReader);
			String str = null;
			while ((str = bufferedReader.load()) != null) {
				resultStr.append(str);
			}
		} catch (IOException e) {
			logger.error(String.format("从流中获取JSON字符串失败，错误信息是：%s", e));
		} finally {
			Tool.IoUtil.close(bufferedReader);
			Tool.IoUtil.close(inputStreamReader);
		}

		Map result = new HashMap();
		if (StringUtils.isNotEmpty(resultStr.toString())) {
			result = JSONObject.parseObject(resultStr.toString(), Map.class);
		}

		if (result != null && result.get("data") != null) {
			String nullType = request.getParameter("nullType");
			nullType = StringUtils.isEmpty(nullType) ? "connected" : nullType;
			Integer start = Integer.valueOf(request.getParameter("start"));
			Integer end = Integer.valueOf(request.getParameter("end"));
			String step = request.getParameter("step");
			String filter = request.getParameter("filter");
			if(paramData != null){
				nullType = paramData.get("nullType");
				start = Integer.valueOf(paramData.get("start"));
				end = Integer.valueOf(paramData.get("end"));
				step = paramData.get("step");
				filter = paramData.get("filter");
			}
			Map data = (Map) result.get("data");
			JSONArray resultArray = (JSONArray) data.get("result");
			// 去除prometheus返回结果中不符合条件的数据
			if(StrUtil.isNotBlank(filter)) {
				filter = URLUtil.decode(filter);
				resultArray = this.filterResult(resultArray, filter);
			}
			// 查询结果集超过系统设置最大限制 , -1 表示不做限制
			ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
			String queryMaxSeries = valueOperations.get(Constant.METRICS_QUERY_MAX_SERIES);
			if (StringUtils.isEmpty(queryMaxSeries)) {
				SysConfigEntity configEntity = sysConfigDao.selectOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, Constant.METRICS_QUERY_MAX_SERIES));
				valueOperations.set(Constant.METRICS_QUERY_MAX_SERIES, configEntity.getParamValue(), 30, TimeUnit.MINUTES);
				queryMaxSeries = configEntity.getParamValue();
			}

			Integer queryMaxSeriesIntVal = Integer.valueOf(queryMaxSeries);
			if (!queryMaxSeriesIntVal.equals(-1) && resultArray.size() > queryMaxSeriesIntVal) {
				data.put("result", Collections.emptyList());
				result.put("message", String.format("Configure the maximum number of Series: %s, query the total number of Series: %s", queryMaxSeriesIntVal, resultArray.size()));
			} else {
				data.put("result", addMissingData(nullType, start, end, step, resultArray));
			}
			result.put("data", data);
		}
		return result;
	}

	private static JSONArray addMissingData(String nullType, Integer start, Integer end, String step, JSONArray resultArray) {
		Map<String, Object> nullTypeAndValueMap = new HashMap(2);
		switch (nullType) {
			case "null": {
				nullTypeAndValueMap.put(nullType, null);
				break;
			}
			case "zero": {
				nullTypeAndValueMap.put(nullType, "0");
				break;
			}
			default:
				break;
		}

		// 获取完整时间序列
		List<Integer> timeList = getTheCompleteTimeList(start, end, step);

		JSONObject metric = null;
		List<JSONArray> values = null;
		Map<Integer, Object> valuesMap;
		JSONArray tempArray;
		Integer time;
		Iterator<JSONArray> valuesIterator;
		int timeListSize = timeList.size();
		for (int i = 0; i < resultArray.size(); i++) {
			metric = (JSONObject) resultArray.get(i);
			values = (List<JSONArray>) metric.get("values");
			// 如果缺失数据 进行补全 只有 nullType 为 null zero 进行补充值
			if (values.size() < timeList.size() && !"connected".equals(nullType)) {
				valuesMap = new HashMap<>();
				for (JSONArray objects : values) {
					valuesMap.put(Integer.valueOf(objects.get(0).toString()), 1);
				}
				for (int j = 0; j < timeListSize; j++) {
					time = timeList.get(j);
					if (valuesMap.get(time) == null) {
						tempArray = new JSONArray();
						tempArray.add(time);
						tempArray.add(nullTypeAndValueMap.get(nullType));
						values.add(j, tempArray);
					}
				}
			}
		}
		return resultArray;
	}

	public static List<Integer> getTheCompleteTimeList(Integer start, Integer end, String stepStr) {
		// 最后以为不是字母 默认单位为  s
		stepStr = Character.isDigit(stepStr.charAt(stepStr.length() - 1)) ? stepStr += "s" : stepStr;
		String endChart = stepStr.substring(stepStr.length() - 1, stepStr.length());

		Integer step = Integer.parseInt(stepStr.substring(0, stepStr.length() - 1));
		switch (endChart) {
			case "s": {
				break;
			}
			case "m": {
				step *= 60;
				break;
			}
			case "h": {
				step *= 3600;
				break;
			}
			case "d": {
				step *= 86400;
				break;
			}
			default:
				break;
		}

		List<Integer> timeList = new ArrayList<>();
		for (int j = start; j <= end; j = j + step) {
			timeList.add(j);
		}

		return timeList;
	}

	public static byte[] compressedIntoGzipBytes(String str, String encoding) {
		if (str == null || str.length() == 0) {
			return null;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GZIPOutputStream gzip = null;
		try {
			gzip = new GZIPOutputStream(out);
			gzip.write(str.getBytes());
			// gzip.close();
		} catch (IOException e) {
			logger.error(e);
		} finally {
			IOUtils.closeQuietly(gzip, out);
		}
		return out.toByteArray();
	}
	
	public JSONArray filterResult(JSONArray resultArray,String filter) {
		Map parse = (Map) JSONObject.parse(filter);
		JSONObject metric = null;
		for (int i = resultArray.size()-1; i >= 0; i--) {
			boolean flag = false;
			metric = (JSONObject) resultArray.get(i);
			JSONObject object = (JSONObject) metric.get("metric");
			Map javaObject = JSONObject.toJavaObject(object, Map.class);
			for(Object key : parse.keySet()) {
				if(!ToolUtil.equals(parse.get(key), javaObject.get(key))) {
					flag = true;
				}
			}
			if(flag) {
				resultArray.remove(i);
			}
		}
		return resultArray;
	}
}
