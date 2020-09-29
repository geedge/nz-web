package com.nis.modules.promserver.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import net.sf.json.util.JSONUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Controller
@SuppressWarnings("all")
public class PromProxyController {

	private static final Logger logger = LoggerFactory.getLogger(PromProxyController.class);
	/**
	 * global prometheus type
	 */
	private static final Integer PROMSERVER_GLOBAL = 1;

	/**
	 * per_datacenter type
	 */
	private static final Integer PROMSERVER_PER_DATACENTER = 2;

	/**
	 * thanos_query type
	 */
	private static final Integer PROMSERVER_THANOS_QUERY = 3;

	@Autowired
	private PromserverService promserverService;

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
		type = (type == null) ? PROMSERVER_THANOS_QUERY : type;
		Promserver server = promserverService.getOneAvaPromServer(promId, idcId, type);
		if (ToolUtil.isEmpty(server)) {
			// type 其次使用 global
			type = PROMSERVER_GLOBAL;
			server = promserverService.getOneAvaPromServer(promId, idcId, type);
			ValidateUtils.is(server).notNull(RCode.PROMSERVER_UNAVAILABLE);
		}
		// type 默认为 global
		// type = (type == null) ? PROMSERVER_GLOBAL : type;
		// Promserver server = promserverService.getOneAvaPromServer(promId, idcId, type);
		logger.debug("path:{},prometheus server:{}", promPath, JSONUtils.valueToString(server));
		// 请求 prometheus server
		requestProm(server.getHost(), server.getPort(), promPath, request, response);
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
	public static void requestProm(String host, int port, String path, HttpServletRequest request,
			HttpServletResponse response)  {
		String queryString = request.getQueryString();
		// 请求 prometheus url
		String url = String.format("http://%s:%s%s?%s", host, port, path, queryString);
		logger.debug("prometheus url: {}", url);
		String method = request.getMethod();
		HttpURLConnection conn = null;
		ServletInputStream reqInputStream = null;
		ServletOutputStream resOutputStream = null;
		OutputStream connOutputStream = null;
		InputStream connInputStream = null;
		try {
			conn = (HttpURLConnection) new URL(url).openConnection();
			reqInputStream = request.getInputStream();
			resOutputStream = response.getOutputStream();
			conn.setRequestMethod(method);
			// 复制请求头
			Enumeration<String> headerNames = request.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String hn = headerNames.nextElement();
				conn.addRequestProperty(hn, request.getHeader(hn));
			}
			if (!"GET".equalsIgnoreCase(method)) {
				conn.setDoOutput(true);
				conn.setDoInput(true);
				conn.setUseCaches(false);
				connOutputStream = conn.getOutputStream();
				IOUtils.copy(reqInputStream, connOutputStream);
			}
			conn.connect();
			int responseCode = conn.getResponseCode();
			connInputStream = (responseCode < 400)? conn.getInputStream():conn.getErrorStream();
			String responseMessage = conn.getResponseMessage();
			Map<String, List<String>> responseHeaders = conn.getHeaderFields();
			//复制响应头
			for(Entry<String, List<String>> en : responseHeaders.entrySet()) {
				String key = en.getKey();
				List<String> value = en.getValue();
				response.addHeader(key, StringUtils.join(value, "; "));
			}
			response.setStatus(responseCode, responseMessage);

			String promPath = request.getServletPath().substring(5);
			String nullType = request.getParameter("nullType");
			if ("/api/v1/query_range".equals(promPath) && responseCode < 400 && ("null".equals(nullType) || "zero".equals(nullType))) {
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
		} catch (Exception e) {
			try {
				response.sendError(500, "request error");
			} catch (IOException e1) {
				logger.error("proxy request error", e1);
			}
			logger.error("request error : ",e);
		}finally {
			IOUtils.closeQuietly(reqInputStream,resOutputStream,connOutputStream,connInputStream);
			conn.disconnect();
		}
	}

	private static Map handleResultData(HttpServletRequest request, InputStream connInputStream) {
		StringBuffer resultStr = new StringBuffer();
		try (InputStreamReader inputStreamReader = new InputStreamReader(new GZIPInputStream(connInputStream), "UTF-8");
		     BufferedReader bufferedReader = new BufferedReader(inputStreamReader);) {
			String str = null;
			while ((str = bufferedReader.readLine()) != null) {
				resultStr.append(str);
			}
		} catch (IOException e) {
			logger.error(String.format("从流中获取JSON字符串失败，错误信息是：%s", e.getMessage()), e);
		}

		Map result = new HashMap();
		if (StringUtils.isNotEmpty(resultStr.toString())) {
			result = JSONObject.parseObject(resultStr.toString(), Map.class);
		}

		if (result != null && result.get("data") != null) {
			String nullType = request.getParameter("nullType");
			String regEx = "[^0-9]";
			Integer start = Integer.valueOf(request.getParameter("start"));
			Integer end = Integer.valueOf(request.getParameter("end"));
			Matcher matcher = Pattern.compile(regEx).matcher(request.getParameter("step"));
			Integer step = Integer.valueOf(matcher.replaceAll("").trim());

			Map data = (Map) result.get("data");
			JSONArray resultArray = (JSONArray) data.get("result");
			data.put("result", addMissingData(nullType, start, end, step, resultArray));
			result.put("data", data);
		}
		return result;
	}

	private static JSONArray addMissingData(String nullType, Integer start, Integer end, Integer step, JSONArray resultArray) {
		Map<String, Object> nullTypeAndValueMap = new HashMap(2);
		switch (nullType) {
			case "null": {
				nullTypeAndValueMap.put(nullType, "null");
				break;
			}
			case "zero": {
				nullTypeAndValueMap.put(nullType, "0");
				break;
			}
			default:
				break;
		}

		List<Integer> timeList = new ArrayList<>();
		for (int j = start; j < end; j = j + step) {
			timeList.add(j);
		}

		JSONObject metric = null;
		List<JSONArray> values = null;
		Map<Integer, Object> valuesMap;
		JSONArray tempArray;
		for (int i = 0; i < resultArray.size(); i++) {
			metric = (JSONObject) resultArray.get(i);
			values = (List<JSONArray>) metric.get("values");
			// 如果缺失数据 进行补全
			if (values.size() != timeList.size()) {
				valuesMap = new HashMap<>();
				for (JSONArray objects : values) {
					valuesMap.put(Integer.valueOf(objects.get(0).toString()), 1);
				}
				for (Integer time : timeList) {
					if (valuesMap.get(time) == null) {
						tempArray = new JSONArray();
						tempArray.add(time);
						tempArray.add(nullTypeAndValueMap.get(nullType));
						values.add(tempArray);
					}
				}
			}
		}
		return resultArray;
	}

	public static byte[] compressedIntoGzipBytes(String str, String encoding) {
		if (str == null || str.length() == 0) {
			return null;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GZIPOutputStream gzip;
		try {
			gzip = new GZIPOutputStream(out);
			gzip.write(str.getBytes());
			gzip.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return out.toByteArray();
	}
}
