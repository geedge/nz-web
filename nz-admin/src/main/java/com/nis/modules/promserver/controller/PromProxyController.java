package com.nis.modules.promserver.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.RCode;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;

import net.sf.json.util.JSONUtils;

@Controller
@SuppressWarnings("all")
public class PromProxyController {

	private static final Logger logger = LoggerFactory.getLogger(PromProxyController.class);
	/**
	 * global prometheus type
	 */
	private static final Integer PROMSERVER_GLOBAL = 1;
	/**
	 * global prometheus type
	 */
	private static final Integer PROMSERVER_PER_DATACENTER = 2;

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
		// type 默认为 global
		type = (type == null) ? PROMSERVER_GLOBAL : type;
		Promserver server = promserverService.getOneAvaPromServer(promId, idcId, type);
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
			IOUtils.copy(connInputStream, resOutputStream);
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

}
