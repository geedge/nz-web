package com.nis.modules.promserver.service.impl;

import cn.hutool.core.net.URLEncoder;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlPath;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.Header;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.promserver.dao.PromserverDao;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.promserver.util.PromConstant;
import com.nis.modules.sys.dao.SysConfigDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Service("promserverService")
public class PromserverServiceImpl extends ServiceImpl<PromserverDao, Promserver> implements PromserverService{

	private static Log log = Log.get();

	@Autowired
	private DcService dcService;
	@Autowired
	private PromserverDao promserverDao;
	@Autowired
	private SysConfigService configService;
	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private RedisTemplate redisTemplate;
	@Value("${connect.timeout:30000}")
	private Integer connectTimeout;
	@Autowired
	private SysConfigDao sysConfigDao;
	@Autowired
	private SysConfigService sysConfigService;

	@SuppressWarnings("unchecked")
	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		String ids = (String) params.get("ids");
		if (StrUtil.isNotBlank(ids)) {
			params.put("ids", Arrays.asList(ids.split(",")));
		}
		String dcIds = (String) params.get("dcIds");
		if (StrUtil.isNotBlank(dcIds)) {
			params.put("dcIds", Arrays.asList(dcIds.split(",")));
		}
		String statuss = (String) params.get("statuses");
		if (StrUtil.isNotBlank(statuss)) {
			params.put("statuses", Arrays.asList(statuss.split(",")));
		}

		IPage page = new Query(Promserver.class).getPage(params);
		List<Promserver> promserverList = promserverDao.queryList(page, params);
		// 获取缓存中检查时间信息
		Map promserverCaches = redisTemplate.opsForHash().entries(Constant.AGENT_STATE_CACHE_KEY);
		if(ToolUtil.isNotEmpty(promserverCaches)) {
			for(Promserver prom : promserverList) {
				Object promCache = promserverCaches.get(prom.getId().toString());
				if(ToolUtil.isNotEmpty(promCache)) {
					prom.setCheckTime(JSON.parseObject((String)promCache, Promserver.class).getCheckTime());
					prom.setTs(JSON.parseObject((String) promCache, Promserver.class).getTs());
				}
			}
		}
		page.setRecords(promserverList);
		return new PageUtils(page);
	}


	public void validateParam(Promserver promserverEntity) {
		// host port type 唯一
		List<Promserver> pList = this.list(new LambdaQueryWrapper<Promserver>()
				.eq(Promserver::getHost, promserverEntity.getHost())
				.eq(Promserver::getPort, promserverEntity.getPort())
				.eq(Promserver::getType, promserverEntity.getType()));

		boolean flag = false;
		if ((pList == null || pList.size() == 0) || (pList != null && pList.size() == 1 && pList.get(0).getId().equals(promserverEntity.getId()))) {
			flag = true;
		}

		if (!flag) {
			throw new NZException(RCode.PROMSERVER_EXIST_ERROR);
		}

		Promserver promserver = null;
		if (promserverEntity.getId() == null){
			promserver = this.getOne(new LambdaQueryWrapper<Promserver>().eq(Promserver::getName, promserverEntity.getName()));
			if (Tool.ObjectUtil.isNotEmpty(promserver)){
				throw new NZException(RCode.PROMSERVER_NAME_EXISTED);
			}
		}else {
			promserver = this.getOne(new LambdaQueryWrapper<Promserver>().eq(Promserver::getName, promserverEntity.getName()));
			if (Tool.ObjectUtil.isNotEmpty(promserver) && !promserver.getId().equals(promserverEntity.getId())){
				throw new NZException(RCode.PROMSERVER_NAME_EXISTED);
			}
		}

		Integer type = promserverEntity.getType();
		String host = promserverEntity.getHost();
		Integer port = promserverEntity.getPort();
		ValidateUtils.is(promserverEntity.getDcId()).notNull(RCode.PROMSERVER_DCID_ISNULL)
				.and(host).notNull(RCode.PROMSERVER_HOST_ISNULL).maxLength(64)
				.and(type).notNull(RCode.PROMSERVER_TYPE_ISNULL)
				.and(port).notNull(RCode.PROMSERVER_PORT_ISNULL).maxLength(10)
				.and(promserverEntity.getToken()).notNull(RCode.PROMSERVER_TOKEN_ISNULL).maxLength(64)
		        .and(promserverEntity.getName()).notNull(RCode.PROMSERVER_NAME_ISNULL).maxLength(256);

		// 校验ip是否合法
		boolean ipFlag = CommonUtils.checkIp(host);
		if (!ipFlag) {
			throw new NZException(RCode.PROMSERVER_HOST_ERROR);
		}
		// 校验port是否合法
		boolean portFlag = CommonUtils.checkPort(port.toString());
		if (!portFlag) {
			throw new NZException(RCode.PROMSERVER_PORT_ERROR);
		}

		// dc 存在校验
		Dc dc = dcService.getById(promserverEntity.getDcId());
		ValidateUtils.is(dc).notNull(RCode.DC_NOT_EXISTS);

		// 校验 type 合法性
		Constant.PromserverType[] values = Constant.PromserverType.values();
		List<Integer> list = Arrays.asList(values).stream().map(Constant.PromserverType::getValue).collect(Collectors.toList());
		if (!list.contains(type)) {
			throw new NZException(RCode.PROMSERVER_TYPE_INVALIDE);
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Promserver getOneAvaliableGlobalAgent() {
		Integer type = Constant.PromserverType.GLOBAL.getValue();
		//按照id 排序，暂时 使用相对固定的 健康 global
		return this.getOne(new QueryWrapper<Promserver>().lambda()
				.eq(Promserver::getStatus, 1)
				.eq(Promserver::getType, type)
				.orderByAsc(Promserver::getId)
				.last("limit 1"));
	}
	
	@Override
	public Promserver getOneAvaPromServer(Integer promId,Integer idcId,Integer type) {
		type = (type == null) ? 1 :type;
		List<Promserver> list = this.list(new QueryWrapper<Promserver>().lambda()
				.eq(true, Promserver::getStatus, 1)
				.eq(promId!=null, Promserver::getId, promId)
				.eq(promId==null&&type!=null, Promserver::getType, type)
				.eq(promId==null&&idcId != null, Promserver::getDcId, idcId));
		// 校验是否有可用 prometheus server
		// ValidateUtils.is(list).notNull(RCode.PROMSERVER_HOST_ISNULL);
		// Promserver server = list.get(RandomUtils.nextInt(0, list.size()));

		Promserver server = null;
		if (CollectionUtils.isNotEmpty(list)) {
			server = list.get(RandomUtils.nextInt(0, list.size()));
		}
		return server;
	}

	/**
	 * 获取 prometheus 请求路径前缀
	 * 本地存储使用prometheus，直接请求prometheus
	 * @return
	 */
	@Override
	public String getPromPathPrefix() {
		String storageType = configService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_TYPE);
		String agentPath = configService.getValueOrDefault(Constant.CONFAGENT_PATH, "");
		String prePath = "1".equals(storageType) ? "/prometheus/proxy" :"/cortex/proxy/prometheus";
		String proxyPath = String.format("%s%s",agentPath, prePath);
		return proxyPath;
	}
	
	
	@Override
	public Integer saveOrUpdatePromserver(Promserver promserverEntity, boolean modify) {
        // 校验
		this.validateParam(promserverEntity);

		// 添加设置默认值
		if(!modify){
			promserverEntity.setStatus(1);
			promserverEntity.setStatusInfo(JSONObject.toJSONString(new HashMap<>()));
		}

		// 保存/修改
		this.saveOrUpdate(promserverEntity);
		return promserverEntity.getId();
	}

	@Override
	public void updateStatusInfo(Promserver promserver) {
		this.baseMapper.updateStatusInfo(promserver);
	}

	/**
	 * 详情
	 * @param id
	 * @return
	 */
	@Override
	public Promserver queryPromServerDetails(Integer id) {
		Promserver promserver = this.getById(id);
		ValidateUtils.is(promserver).notNull(RCode.PROMSERVER_NOTEXSITS);

		Dc dc = dcService.getById(promserver.getDcId());
		promserver.setDc(dc);
		return promserver;
	}

	@Override
	public JSONObject requestAgent(Promserver server, String urlPath, String requestAgent) {
		String urlStr = UrlBuilder.create().setScheme(server.getProtocol()).setHost(server.getHost())
				.setPort(server.getPort()).setPath(UrlPath.of(urlPath, Charset.forName("UTF-8")))
				.setQuery(UrlQuery.of(URLEncoder.createQuery().encode(requestAgent, Charset.forName("UTF-8")), Charset.forName("UTF-8"))).toString();

		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.AUTHORIZATION, server.getToken());
		HttpEntity httpEntity = new HttpEntity(headers);
		ResponseEntity<String> responseEntity = null;
		try {
			responseEntity = restTemplate.exchange(new URI(urlStr), HttpMethod.GET, httpEntity, String.class);
		} catch (URISyntaxException e) {
			log.error(e);
		}
		log.info("agent request url:{},responseStatus:{}",urlStr,responseEntity.getStatusCodeValue());
		if (log.isDebugEnabled()) {
			log.debug("agent request body:{}",urlStr,responseEntity.getStatusCodeValue(),responseEntity.getBody());
		}
		return JSONObject.parseObject(responseEntity.getBody());
	}

	@Override
	public Map promServerProxy(HttpServletRequest request, HttpServletResponse response, String promPath, Map<String, String> paramData){
		Integer agentId = request.getParameter("agentId") != null ? Integer.valueOf(request.getParameter("agentId")):null;
		Integer dcId = request.getParameter("dcId") != null ? Integer.valueOf(request.getParameter("dcId")):null;
		Integer type = request.getParameter("type") != null ? Integer.valueOf(request.getParameter("type")):null;

		Promserver server = getOneAvaPromServer(agentId, dcId, type);
		if (ToolUtil.isEmpty(server)) {
			// type 其次使用 global
			type = PromConstant.PROMSERVER_GLOBAL;
			server = getOneAvaPromServer(agentId, dcId, type);
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
			byte[] body = null;
			String queryString = request.getQueryString();
			queryString = StrUtil.isNotBlank(queryString)?queryString:"";
			queryString = URLUtil.decode(queryString);
			String step = request.getParameter("step");
			if(ToolUtil.isNotEmpty(paramData)){
				queryString = paramData.get("queryString");
				step = paramData.get("step");
			}else{
				body = Tool.IoUtil.readBytes(request.getInputStream());
			}
			// 重新构造请求参数内容
			HttpEntity<byte[]> httpEntity = new HttpEntity<byte[]>(body, headers);
			if(StrUtil.equals(promPath, "/api/v1/query_range") || StrUtil.equals(promPath, "/api/v1/query")) {
				if(ReUtil.contains("\\[\\s*\\$interval\\s*]", URLUtil.decode(queryString))) {
					// 判断step是否小于两个周期的scrape interval
					Integer defaultScrapeInterval = Integer.valueOf(sysConfigService.getValue(Constant.SYSCONFIG_KEY_SCRAPEINTERVAL));
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
			String promPathPrefix = getPromPathPrefix();
			if (promPath.endsWith("targets")){
				//获取 targets代理 path
				String agentPath = sysConfigService.getValue(Constant.CONFAGENT_PATH);
				promPathPrefix = String.format("%s%s",agentPath, "/prometheus/proxy");
				//获取本身 agent
				server = getOne(new LambdaQueryWrapper<Promserver>()
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
			/*
			 * query_range 补零
			 */
			if ("/api/v1/query_range".equals(promPath) && statusCode < 300) {
				// 处理缺失时间数据
				List<String> list = httpHeaders.get(HttpHeaders.CONTENT_ENCODING);
				String contentEncoding = Tool.CollUtil.isNotEmpty(list)?list.get(0):"";
				boolean isGzipStream = StrUtil.equalsIgnoreCase("gzip", contentEncoding);
				bais = new ByteArrayInputStream(responseBody);
				Map map = handleResultData(request,bais,isGzipStream, paramData);
				if(ToolUtil.isNotEmpty(paramData)){
					map.put("eleIndex",paramData.get("eleIndex"));
					map.put("chartId",paramData.get("chartId"));
					return map;
				}
				// 压缩
				responseBody = isGzipStream ? compressedIntoGzipBytes(JSONObject.toJSONString(map), "UTF-8") : JSONObject.toJSONString(map).getBytes("UTF-8");
			}else if(statusCode > 200 && ToolUtil.isNotEmpty(paramData)){
				return null;
			}
			response.reset();
			response.setStatus(statusCode);
			Set<Map.Entry<String,List<String>>> entrySet = httpHeaders.entrySet();
			//设置cors响应头
			Constant.CORS_HEADER.forEach((k,v)->{
				response.setHeader(k, v);
			});
			for(Map.Entry<String,List<String>> en : entrySet) {
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
			response.setContentLength(Tool.ArrayUtil.length(responseBody));
			response.getOutputStream().write(responseBody);
			response.flushBuffer();
		} catch (Exception e) {
			log.error(e, "prometheus proxy error,path : {}", promPath);
		} finally {
			Tool.IoUtil.close(bais);
		}
		return null;
	}

	private Map handleResultData(HttpServletRequest request, InputStream connInputStream, Boolean isGzipStream, Map<String, String> paramData) {
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
			log.error(String.format("从流中获取JSON字符串失败，错误信息是：%s", e));
		} finally {
			Tool.IoUtil.close(bufferedReader);
			Tool.IoUtil.close(inputStreamReader);
		}

		Map result = new HashMap();
		if (org.apache.commons.lang.StringUtils.isNotEmpty(resultStr.toString())) {
			result = JSONObject.parseObject(resultStr.toString(), Map.class);
		}

		if (result != null && result.get("data") != null) {
			String nullType = request.getParameter("nullType");
			nullType = org.apache.commons.lang.StringUtils.isEmpty(nullType) ? "connected" : nullType;
			Integer start = Integer.valueOf(request.getParameter("start"));
			Integer end = Integer.valueOf(request.getParameter("end"));
			String step = request.getParameter("step");
			String filter = request.getParameter("filter");
			if(ToolUtil.isNotEmpty(paramData)){
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
			log.error(e);
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
