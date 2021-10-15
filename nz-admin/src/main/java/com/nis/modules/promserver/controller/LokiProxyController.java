package com.nis.modules.promserver.controller;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlPath;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.http.Header;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import net.sf.json.util.JSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
@SuppressWarnings("all")
public class LokiProxyController {

	
    private static final Log log = Log.get();

    static Map<Pattern, String> logPatternAndLevelMap = new LinkedHashMap<>();

    static {
        Map<String, String> logLevel = new LinkedHashMap<>();
        logLevel.put("emerg", "fatal");
        logLevel.put("fatal", "fatal");
        // logLevel.put("alert", "fatal");
        logLevel.put("crit", "fatal");
        logLevel.put("critical", "fatal");
        logLevel.put("warn", "warn");
        logLevel.put("warning", "warn");
        logLevel.put("err", "error");
        // logLevel.put("eror", "error");
        logLevel.put("error", "error");
        logLevel.put("info", "info");
        logLevel.put("information", "info");
        logLevel.put("informational", "info");
        logLevel.put("notice", "info");
        logLevel.put("dbug", "debug");
        logLevel.put("debug", "debug");
        logLevel.put("trace", "trace");
        logLevel.put("unknown", "unknown");

        for (Entry<String, String> entry : logLevel.entrySet()) {
            logPatternAndLevelMap.put(Pattern.compile("\\b" + entry.getKey() + "\\b", Pattern.CASE_INSENSITIVE), entry.getValue());
        }
    }

    @Autowired
    private PromserverService promserverService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private RedisTemplate redisTemplate;

	@Autowired
	private RestTemplate restTemplate;
    @Value("${connect.timeout:30000}")
    private Integer connectTimeout;

    @RequestMapping(value = "/logs/loki/**")
    public void logLokiProxy(HttpServletRequest request, HttpServletResponse response) {
        String proxyPath = request.getServletPath().substring(5);
        Integer promId = request.getParameter("promId") != null ? Integer.valueOf(request.getParameter("promId")) : null;

        switch (proxyPath) {
            case "/loki/api/v1/query":
                // 验证query 参数不能为空
                ValidateUtils.is(request.getParameter("query")).notNull(RCode.PROMSERVER_PARAM_QUERY_ISNULL);
                break;
            case "/loki/api/v1/query_range":
                // 验证query 参数不能为空
                ValidateUtils.is(request.getParameter("query")).notNull(RCode.PROMSERVER_PARAM_QUERY_ISNULL);
                // 验证start 参数不能为空
                ValidateUtils.is(request.getParameter("start")).notNull(RCode.PROMSERVER_PARAM_START_ISNULL);
                // 验证end 参数不能为空
                ValidateUtils.is(request.getParameter("end")).notNull(RCode.PROMSERVER_PARAM_END_ISNULL);
                break;
            default:
                break;
        }
        // 随机 可用 loki
        Promserver server = promserverService.getOneAvaPromServer(promId, null, Constant.PromserverType.GLOBAL.getValue());
        if (ToolUtil.isEmpty(server)) {
            ValidateUtils.is(server).notNull(RCode.PROMSERVER_UNAVAILABLE);
        }
        log.debug("path:{},loki server:{}", proxyPath, JSONUtils.valueToString(server));
        
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
            // 构造请求路径
         // 请求 prometheus server
            String agentPath = sysConfigService.getValue(Constant.CONFAGENT_PATH);
            String realPath = agentPath + "/loki/proxy" + proxyPath;
            String urlString = UrlBuilder.create().setScheme(server.getProtocol()).setHost(server.getHost())
                    .setPort(server.getPort()).setPath(UrlPath.of(realPath, Charset.forName("UTF-8")))
                    .setQuery(UrlQuery.of(queryString, Charset.forName("UTF-8"))).toString();
            String method = request.getMethod();
            //发送 请求
            // ResponseEntity<byte[]> exchange = restTemplate.exchange(url, HttpMethod.resolve(method), httpEntity, byte[].class);
            ResponseEntity<byte[]> exchange = restTemplate.exchange(new URI(urlString), HttpMethod.resolve(method), httpEntity, byte[].class);
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
    		if (("/loki/api/v1/query".equals(proxyPath) || "/loki/api/v1/query_range".equals(proxyPath)) && statusCode < 300) {
    			// 处理数据格式
    			bais = new ByteArrayInputStream(responseBody);
                Map resultData = handleResultData(request, bais);
                responseBody = JSONObject.toJSONString(resultData, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.MapSortField).getBytes();
			} 
    		response.setContentLength(responseBody.length);
    		response.getOutputStream().write(responseBody);
    		response.flushBuffer();
		} catch (Exception e) {
			log.error(e, "loki proxy error,path : {}", proxyPath);
		} finally {
			Tool.IoUtil.close(bais);
		}
    }


    private Map handleResultData(HttpServletRequest request, InputStream connInputStream) {
        StringBuffer resultStr = new StringBuffer();
        InputStreamReader inputStreamReader = null;
        BufferReaderWrapper bufferedReader = null;
        try {
            inputStreamReader = new InputStreamReader(connInputStream, "UTF-8");
            bufferedReader = new BufferReaderWrapper(inputStreamReader);
            String str = null;
            while ((str = bufferedReader.load()) != null) {
                resultStr.append(str);
            }
        } catch (IOException e) {
            log.error(String.format("从流中获取JSON字符串失败，错误信息是：%s", e.getMessage()), e);
        } finally {
        	Tool.IoUtil.close(bufferedReader);
			Tool.IoUtil.close(inputStreamReader);
        }

        Map result = StrUtil.isNotEmpty(resultStr.toString()) ? JSONObject.parseObject(resultStr.toString(), Map.class) : new HashMap();

        if (result != null && result.get("data") != null) {
            Integer format = request.getParameter("format") != null ? Integer.valueOf(request.getParameter("format")) : 0;

            Map<String, Object> data = (Map) result.get("data");
            String resultType = (String) data.get("resultType");
            // 重新整理数据结构
            if (format == 1 && "streams".equals(resultType)) {
                List<Map> resultArray = (List<Map>) data.get("result");
                if (ToolUtil.isEmpty(resultArray)) {
                    data.put("resultType", "streamsFormat");
                    result.put("data", data);
                    return result;
                }

                List<Object> valuesList = (List<Object>) resultArray.stream().map(map -> (List) map.get("values")).flatMap(Collection::stream).collect(Collectors.toList());
                // 查询结果集超过系统设置最大限制 , -1 表示不做限制
                SysConfigEntity configEntity = sysConfigService.getOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, Constant.LOKILOG_QUERY_MAX_SERIES));
                Integer queryMaxSeriesIntVal = Integer.valueOf(configEntity.getParamValue());
                if (!queryMaxSeriesIntVal.equals(-1) && valuesList.size() > queryMaxSeriesIntVal) {
                    data.put("result", Collections.emptyList());
                    result.put("message", String.format("Configure the maximum number of Series: %s, query the total number of Series: %s", queryMaxSeriesIntVal, valuesList.size()));
                } else {
                    // labels series
                    handleDataStructure(data, resultArray, request);
                    data.put("resultType", "streamsFormat");
                }
                result.put("data", data);
            }
        }
        return result;
    }

    private void handleDataStructure(Map data, List<Map> resultArray, HttpServletRequest request) {
        String direction = request.getParameter("direction") != null ? request.getParameter("direction") : "backward";
        boolean directionFlag = "backward".equalsIgnoreCase(direction) ? true : false;

        List<Map> mapList = new ArrayList<>();
        for (Map logInfoMap : resultArray) {
            Map stream = (Map) logInfoMap.get("stream");
            List<List> values = (List) logInfoMap.get("values");

            for (int i = 0; i < values.size(); i++) {
                Map map = new HashMap();
                JSONArray timestampAndMsgArray = (JSONArray) values.get(i);
                String timestamp = (String) timestampAndMsgArray.get(0);
                String message = (String) timestampAndMsgArray.get(1);
                map.put("labels", ObjectUtil.clone(stream));
                map.put("timestamp", timestamp);
                map.put("message", message);
                map.put("uuid", MD5.create().digestHex16(timestamp + message + JSONObject.toJSONString(stream, SerializerFeature.MapSortField)));
                map.put("level", getLogLevelInfo(message));
                mapList.add(map);
            }
        }

        if (directionFlag) {
            mapList = mapList.stream().sorted((s1, s2) -> Long.valueOf(s2.get("timestamp").toString()).compareTo(Long.valueOf(s1.get("timestamp").toString()))).collect(Collectors.toList());
        } else {
            mapList = mapList.stream().sorted((s1, s2) -> Long.valueOf(s1.get("timestamp").toString()).compareTo(Long.valueOf(s2.get("timestamp").toString()))).collect(Collectors.toList());
        }

        SysConfigEntity configEntity = sysConfigService.getOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, Constant.LOKI_LOGS_QUERY_STAT_MAX_POINTS));
        Integer nodeNum = Integer.valueOf(configEntity.getParamValue());

        Long first, end;
        if (directionFlag) {
            first = Long.valueOf((String) mapList.get(mapList.size() - 1).get("timestamp"));
            end = Long.valueOf((String) mapList.get(0).get("timestamp"));
        } else {
            first = Long.valueOf((String) mapList.get(0).get("timestamp"));
            end = Long.valueOf((String) mapList.get(mapList.size() - 1).get("timestamp"));
        }

        SortedMap<Long, Integer> timeAndIndexMap = new TreeMap<>();

        // 总时间 秒
        BigDecimal elapsedsSecondsTime = BigDecimal.valueOf(end - first).divide(BigDecimal.valueOf(1000000000.0), 0);
        long step;
        if (elapsedsSecondsTime.longValue() < nodeNum) {
            step = 1000000000; // 一秒
            nodeNum = elapsedsSecondsTime.intValue();
        } else {
            BigDecimal stepDecimal = BigDecimal.valueOf(end - first).divide(BigDecimal.valueOf(nodeNum), 0);
            step = stepDecimal.longValue();
        }

        List<Long[]> timeStampAndCounterList = new ArrayList();
        timeStampAndCounterList.add(new Long[]{first, 0L});
        timeAndIndexMap.put(first, 0);

        for (int i = 0; i < nodeNum - 1; i++) {
            Long[] timeAndCountArray = {first += step, 0L};
            timeStampAndCounterList.add(timeAndCountArray);
            timeAndIndexMap.put(timeAndCountArray[0], i);
        }

        List<Map> seriesList = new ArrayList<>();
        Map<Object, List<Map>> listMapGroupByLevel = mapList.stream().collect(Collectors.groupingBy(map -> map.get("level")));
        for (Entry<Object, List<Map>> entry : listMapGroupByLevel.entrySet()) {
            Map levelCountInfoMap = new HashMap();
            levelCountInfoMap.put("name", entry.getKey());

            List cloneList = JSONObject.parseArray(JSONObject.toJSONString(timeStampAndCounterList));

            for (Map map : entry.getValue()) {
                Long timestamp = Long.valueOf((String) map.get("timestamp"));
                SortedMap<Long, Integer> tailMap = timeAndIndexMap.tailMap(timestamp);
                Integer index;
                // 可能为空, 放到最后一个节点
                if (tailMap.isEmpty()) {
                    index = cloneList.size() - 1;
                } else {
                    index = timeAndIndexMap.get(tailMap.firstKey());
                }
                Object obj = cloneList.get(index);
                List<Object> objects = JSONObject.parseArray(obj.toString());
                Integer count = (Integer) objects.get(1);
                objects.set(1, ++count);
                cloneList.set(index, objects);
            }

            if (directionFlag) {
                cloneList = (List) cloneList.stream().sorted((o1, o2) -> {
                    List<Object> obj1 = JSONObject.parseArray(o1.toString());
                    List<Object> obj2 = JSONObject.parseArray(o2.toString());
                    return ((Long) obj2.get(0)).compareTo(((Long) obj1.get(0)));
                }).collect(Collectors.toList());
            } else {
                cloneList = (List) cloneList.stream().sorted((o1, o2) -> {
                    List<Object> obj1 = JSONObject.parseArray(o1.toString());
                    List<Object> obj2 = JSONObject.parseArray(o2.toString());
                    return ((Long) obj1.get(0)).compareTo(((Long) obj2.get(0)));
                }).collect(Collectors.toList());
            }

            levelCountInfoMap.put("data", cloneList);
            seriesList.add(levelCountInfoMap);
        }

        data.put("result", mapList);
        data.put("series", seriesList);
    }

    private static String getLogLevelInfo(String message) {
        Integer currentIndex = null;
        String level = "";
        for (Entry<Pattern, String> entry : logPatternAndLevelMap.entrySet()) {
            Pattern pattern = entry.getKey();
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                String group = matcher.group(0);
                int i = message.indexOf(group);
                if (currentIndex == null || (i != 1 && i < currentIndex)) {
                    level = entry.getValue();
                    currentIndex = i;
                }
            }
        }
        if (StrUtil.isNotEmpty(level))
            return level;
        return "unknown";
    }
}
