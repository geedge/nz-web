package com.nis.modules.promserver.controller;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlPath;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.http.HttpConnection;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.BufferReaderWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.promserver.util.PromConstant;
import com.nis.modules.sys.dao.SysConfigDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
	
	@RequestMapping(value = "/prom/**")
	public void promProxy(HttpServletRequest request, HttpServletResponse response) {
		String promPath = request.getServletPath().substring(5);
		Integer promId = request.getParameter("promId") != null ? Integer.valueOf(request.getParameter("promId")):null;
		Integer idcId = request.getParameter("idcId") != null ? Integer.valueOf(request.getParameter("idcId")):null;
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
		// type 优先使用 thanos_query
		// type = (type == null) ? PromConstant.PROMSERVER_THANOS_QUERY : type;
		Promserver server = promserverService.getOneAvaPromServer(promId, idcId, type);
		if (ToolUtil.isEmpty(server)) {
			// type 其次使用 global
			type = PromConstant.PROMSERVER_GLOBAL;
			server = promserverService.getOneAvaPromServer(promId, idcId, type);
			ValidateUtils.is(server).notNull(RCode.PROMSERVER_UNAVAILABLE);
		}
		// type 默认为 global
		// type = (type == null) ? PROMSERVER_GLOBAL : type;
		// Promserver server = promserverService.getOneAvaPromServer(promId, idcId, type);
//			logger.debug("path:{},prometheus server:{}", promPath, JSONUtils.valueToString(server));
		// 请求 prometheus server
		requestProm(server.getHost(), server.getPort(), promPath, server.getToken(), request, response);
	}

	/**
	 * 请求 prometheus
	 * 
	 * @param host
	 * @param port
	 * @param path
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public void requestProm(String host, int port, String path, String token, HttpServletRequest request, HttpServletResponse response) {
//		String queryString = request.getQueryString();
		String queryString = ReflectUtil.invoke(request, "getQueryString");

		// 请求 prometheus url
//		String url = String.format("http://%s:%s%s?%s", host, port, path, queryString);
		// String  url = UrlBuilder.create().setScheme("http").setHost(host).setPort(port).appendPath(path).toString()+"?"+queryString;

		SysConfigEntity configEntity = sysConfigDao.selectOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, Constant.CONFAGENT_PATH));
		String proxyPath = String.format("%s%s%s", configEntity.getParamValue(), "/prometheus/proxy", path);

		String url = UrlBuilder.create().setScheme("http").setHost(host).setPort(port).setPath(UrlPath.of(proxyPath, Charset.forName("UTF-8"))).toURL().toString() + "?" + queryString;

		logger.debug("prometheus url: {}", url);
		String method = request.getMethod();
		HttpURLConnection conn = null;
		ServletInputStream reqInputStream = null;
		ServletOutputStream resOutputStream = null;
		OutputStream connOutputStream = null;
		InputStream connInputStream = null;
		try {
//			conn = (HttpURLConnection) new URL(url).openConnection();
//			conn = (HttpURLConnection) URLUtil.toUrlForHttp(url).openConnection();
			conn = HttpConnection.create(url, null).getHttpURLConnection();
			reqInputStream = request.getInputStream();
			resOutputStream = response.getOutputStream();
			conn.setRequestMethod(method);
			// 复制请求头
			Enumeration<String> headerNames = request.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String hn = headerNames.nextElement();
				/*conn.addRequestProperty(XssUtil.stripXSS(hn), XssUtil.stripXSS(request.getHeader(hn)));*/
				if(Constant.AUTH_TOKEN_CODE.equalsIgnoreCase(hn)){
					continue;
				}
				ReflectUtil.invoke(conn,"addRequestProperty",hn,request.getHeader(hn));
			}

			ReflectUtil.invoke(conn, "addRequestProperty", Constant.AUTH_TOKEN_CODE, token);

			if (!"GET".equalsIgnoreCase(method)) {
				conn.setDoOutput(true);
				conn.setDoInput(true);
				conn.setUseCaches(false);
				connOutputStream = conn.getOutputStream();
				IOUtils.copy(reqInputStream, connOutputStream);
			}
			conn.setConnectTimeout(connectTimeout);
			conn.connect();
			int responseCode = conn.getResponseCode();
			connInputStream = (responseCode < 400)? conn.getInputStream():conn.getErrorStream();
			String responseMessage = conn.getResponseMessage();
			Map<String, List<String>> responseHeaders = conn.getHeaderFields();
			//复制响应头
			for(Entry<String, List<String>> en : responseHeaders.entrySet()) {
				String key = en.getKey();
				if (StringUtils.isEmpty(key)) continue;
				List<String> value = en.getValue();
//				response.addHeader(XssUtil.stripXSS(key), XssUtil.stripXSS(StringUtils.join(value, "; ")));
				ReflectUtil.invoke(response,"addHeader",key,StringUtils.join(value, "; "));
			}
//			response.setStatus(responseCode, responseMessage);
			response.setStatus(responseCode);
			
			String promPath = request.getServletPath().substring(5);
			String nullType = request.getParameter("nullType");
			if ("/api/v1/query_range".equals(promPath) && responseCode < 400) {
				// 处理缺失时间数据
				Map map = handleResultData(request, connInputStream);

				// 压缩
				byte[] gzipBytes = compressedIntoGzipBytes(JSONObject.toJSONString(map), "UTF-8");

				String transfer = response.getHeader("Transfer-Encoding");
				if (StringUtils.isEmpty(transfer)) {
					response.setContentLength(gzipBytes.length);
				}

				resOutputStream.write(gzipBytes);
				resOutputStream.flush();//flush 输出流
			} else {
				IOUtils.copy(connInputStream, resOutputStream);
				resOutputStream.flush();//flush 输出流
			}
		} catch (IOException e) {
			try {
				response.sendError(500, "request error");
			} catch (IOException e1) {
				logger.error("proxy request error",e1);
			}
			logger.error("request error : ",e);
		}finally {
			IOUtils.closeQuietly(reqInputStream,resOutputStream,connOutputStream,connInputStream);
			if(ObjectUtil.isNotEmpty(conn)) {
				conn.disconnect();
			}
		}
	}

	private Map handleResultData(HttpServletRequest request, InputStream connInputStream) {
		StringBuffer resultStr = new StringBuffer();
		InputStreamReader inputStreamReader = null;
		BufferReaderWrapper bufferedReader = null;
		try {
			inputStreamReader = new InputStreamReader(new GZIPInputStream(connInputStream), "UTF-8");
			bufferedReader = new BufferReaderWrapper(inputStreamReader);
			String str = null;
			while ((str = bufferedReader.load()) != null) {
				resultStr.append(str);
			}
		} catch (IOException e) {
			logger.error(String.format("从流中获取JSON字符串失败，错误信息是：%s", e));
		} finally {
			if(bufferedReader != null){
				IOUtils.closeQuietly(bufferedReader);
			}
			if(inputStreamReader != null){
				IOUtils.closeQuietly(inputStreamReader);
			}
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

			Map data = (Map) result.get("data");
			JSONArray resultArray = (JSONArray) data.get("result");

			// 查询结果集超过系统设置最大限制 , -1 表示不做限制
			ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
			String queryMaxSeries = valueOperations.get(Constant.QUERY_MAX_SERIES);
			if (StringUtils.isEmpty(queryMaxSeries)) {
				SysConfigEntity configEntity = sysConfigDao.selectOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, Constant.QUERY_MAX_SERIES));
				valueOperations.set(Constant.QUERY_MAX_SERIES, configEntity.getParamValue(), 30, TimeUnit.MINUTES);
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
}
