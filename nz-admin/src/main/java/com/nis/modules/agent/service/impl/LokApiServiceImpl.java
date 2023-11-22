package com.nis.modules.agent.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.agent.dao.AgentDao;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.LokiApiService;
import com.nis.modules.agent.service.PromApiService;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.sys.service.SysConfService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class LokApiServiceImpl extends ServiceImpl<AgentDao, Agent> implements LokiApiService {

	private static final Log log = Log.get();

	@Autowired
	private SysConfService sysConfService;

	@Value("${connect.timeout:30000}")
	private Integer connectTimeout;

	@Autowired
	private PromApiService promApiService;

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

		for (Map.Entry<String, String> entry : logLevel.entrySet()) {
			logPatternAndLevelMap.put(Pattern.compile("\\b" + entry.getKey() + "\\b", Pattern.CASE_INSENSITIVE), entry.getValue());
		}
	}

	/**
	 * GET /loki/api/v1/query allows for doing queries against a single point in time.
	 * The URL query parameters support the following values:
	 *      query: The LogQL query to perform
	 *      limit: The max number of entries to return. It defaults to 100. Only applies to query types which produce a stream(log lines) response.
	 *      time: The evaluation time for the query as a nanosecond Unix epoch or another supported format. Defaults to now.
	 *      direction: Determines the sort order of logs. Supported values are forward or backward. Defaults to backward.
	 * Custom parameter:
	 *      format: The result of restrictions is used to query Alert Message data.
	 *      filter: 可选值：1/0,默认：0 resultType = streams  时有效
	 * @param query
	 * @param limit
	 * @param time
	 * @param direction
	 * @param format
	 * @param filter
	 */
	@Override
	public JSONObject query(String query, String limit, String time, String direction, String format, String filter) {
		Agent server = promApiService.getOneAvaliableGlobalAgent();

		// 请求 prometheus server
		String agentPath = sysConfService.getValue(Constant.CONFAGENT_PATH);
		String url = String.format("%s%s%s", agentPath, "/loki/proxy", "/loki/api/v1/query");

		Map<Object, Object> requestParam = Tool.MapUtil.builder()
				.put("query", query)
				.put("limit", limit)
				.put("time", time)
				.put("direction", direction)
				.build();

		JSONObject resultData = AgentUtil.getForObject(server, url, requestParam);
		resultData = this.handleResultData(resultData, direction, format, filter);
		return resultData;
	}

	/**
	 * GET /loki/api/v1/query_range is used to do a query over a range of time and
	 * accepts the following query parameters in the URL:
	 *      query: The LogQL query to perform
	 *      limit: The max number of entries to return. It defaults to 100. Only applies to query types which produce a stream(log lines) response.
	 *      start: The start time for the query as a nanosecond Unix epoch or another supported format. Defaults to one hour ago.
	 *      end: The end time for the query as a nanosecond Unix epoch or another supported format. Defaults to now.
	 *      step: Query resolution step width in duration format or float number of seconds. duration refers to Prometheus duration strings of the form [0-9]+[smhdwy]. For example, 5m refers to a duration of 5 minutes. Defaults to a dynamic value based on start and end. Only applies to query types which produce a matrix response.
	 *      interval: This parameter is experimental; see the explanation under Step versus interval. Only return entries at (or greater than) the specified interval, can be a duration format or float number of seconds. Only applies to queries which produce a stream response.
	 *      direction: Determines the sort order of logs. Supported values are forward or backward. Defaults to backward.
	 * Custom parameter:
	 *      format: The result of restrictions is used to query Alert Message data.
	 *      filter: 可选值：1/0,默认：0 resultType = streams  时有效
	 *
	 * @param query
	 * @param start
	 * @param end
	 * @param limit
	 * @param step
	 * @param interval
	 * @param direction
	 * @param format
	 * @param filter
	 * @return
	 */
	@Override
	public JSONObject query_range(String query, String start, String end, String limit, String step, String interval, String direction, String format, String filter) {
		Agent server = promApiService.getOneAvaliableGlobalAgent();

		// 请求 prometheus server
		String agentPath = sysConfService.getValue(Constant.CONFAGENT_PATH);
		String url = String.format("%s%s%s", agentPath, "/loki/proxy", "/loki/api/v1/query_range");

		Map<Object, Object> requestParam = Tool.MapUtil.builder()
				.put("query", query)
				.put("start", start)
				.put("end", end)
				.put("limit", limit)
				.put("step", step)
				.put("direction", direction)
				.put("interval", interval)
				.build();

		JSONObject resultData = AgentUtil.getForObject(server, url, requestParam);
		resultData = this.handleResultData(resultData, direction, format, filter);
		return resultData;
	}

	@Override
	public JSONObject labels(String proxyPath, String start, String end) {
		Agent server = promApiService.getOneAvaliableGlobalAgent();

		// 请求 prometheus server
		String agentPath = sysConfService.getValue(Constant.CONFAGENT_PATH);
		String url = String.format("%s%s%s", agentPath, "/loki/proxy", proxyPath);

		Map<Object, Object> requestParam = Tool.MapUtil.builder()
				.put("start", start)
				.put("end", end)
				.build();

		return AgentUtil.getForObject(server, url, requestParam);
	}


	@Override
	public void lokiServerProxy(HttpServletRequest request, HttpServletResponse response, String proxyPath) {
		Agent server = promApiService.getOneAvaliableGlobalAgent();

		// 请求 prometheus server
		String agentPath = sysConfService.getValue(Constant.CONFAGENT_PATH);
		String url = String.format("%s%s%s", agentPath, "/loki/proxy", proxyPath);

		AgentUtil.agentApiProxy(server, url, request, response);
	}

	private JSONObject handleResultData(JSONObject resultData, String direction, String format, String filter) {
		if ("success".equals(resultData.get("status"))) {
			if (Tool.MapUtil.isNotEmpty(resultData) && resultData.get("data") != null) {
				format = StrUtil.emptyToDefault(format, "0");

				Map<String, Object> data = (Map) resultData.get("data");
				String resultType = (String) data.get("resultType");
				if (StrUtil.isNotBlank(filter) && ToolUtil.isNotEmpty(data.get("result"))) {
					filter = URLUtil.decode(filter);
					JSONArray resultArray = (JSONArray) data.get("result");
					resultArray = this.filterResult(resultArray, filter);
					data.put("result", resultArray);
				}

				// 重新整理数据结构
				if (format.equals("1") && "streams".equals(resultType)) {
					List<Map> resultArray = (List<Map>) data.get("result");
					if (ToolUtil.isEmpty(resultArray)) {
						data.put("resultType", "streamsFormat");
						resultData.put("data", data);
						return resultData;
					}

					List<Object> valuesList = (List<Object>) resultArray.stream().map(map -> (List) map.get("values")).flatMap(Collection::stream).collect(Collectors.toList());
					// 查询结果集超过系统设置最大限制 , -1 表示不做限制
					String queryMaxSeriesConfStr = sysConfService.getValue(Constant.LOKILOG_QUERY_MAX_SERIES);
					Integer queryMaxSeriesIntVal = Integer.valueOf(queryMaxSeriesConfStr);
					if (!queryMaxSeriesIntVal.equals(-1) && valuesList.size() > queryMaxSeriesIntVal) {
						data.put("result", Collections.emptyList());
						resultData.put("message", String.format("Configure the maximum number of Series: %s, query the total number of Series: %s", queryMaxSeriesIntVal, valuesList.size()));
					} else {
						// labels series
						handleDataStructure(data, resultArray, direction);
						data.put("resultType", "streamsFormat");
					}
					resultData.put("data", data);
				}
			}
			return resultData;
		}
		return resultData;
	}

	/**
	 * The result of restrictions is used to query Alert Message data.
	 *
	 * @param resultArray
	 * @param filter
	 * @return
	 */
	public JSONArray filterResult(JSONArray resultArray, String filter) {
		Map parse = (Map) JSONObject.parse(filter);
		JSONObject metric = null;
		for (int i = resultArray.size() - 1; i >= 0; i--) {
			boolean flag = false;
			metric = (JSONObject) resultArray.get(i);
			JSONObject object = (JSONObject) metric.get("metric");
			Map javaObject = JSONObject.toJavaObject(object, Map.class);
			for (Object key : parse.keySet()) {
				if (!ToolUtil.equals(parse.get(key), javaObject.get(key))) {
					flag = true;
				}
			}
			if (flag) {
				resultArray.remove(i);
			}
		}
		return resultArray;
	}

	private void handleDataStructure(Map data, List<Map> resultArray, String direction) {
		direction = StrUtil.isNotBlank(direction) ? direction : "backward";
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

		// LOKI_LOGS_QUERY_STAT_MAX_POINTS
		String logsQueryStatMaxPointsConfStr = sysConfService.getValue(Constant.LOKI_LOGS_QUERY_STAT_MAX_POINTS);
		Integer nodeNum = Integer.valueOf(logsQueryStatMaxPointsConfStr);

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
		for (Map.Entry<Object, List<Map>> entry : listMapGroupByLevel.entrySet()) {
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
		for (Map.Entry<Pattern, String> entry : logPatternAndLevelMap.entrySet()) {
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
