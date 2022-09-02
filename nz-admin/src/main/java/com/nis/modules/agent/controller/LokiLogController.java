package com.nis.modules.agent.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.service.LokiApiService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Controller
public class LokiLogController {

	private static final Log log = Log.get();

	@Autowired
	private LokiApiService lokiApiService;

	private static ExecutorService exec = Executors.newCachedThreadPool();

	@RequestMapping(value = "/logs/loki/export")
	public void logExport(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String[] logqlArr = request.getParameterValues("logql[]");
		String start = request.getParameter("start");
		String end = request.getParameter("end");

		ValidateUtils.is(logqlArr).notNull(RCode.AGENT_PARAM_QUERY_ISNULL)
				.and(start).notNull(RCode.AGENT_PARAM_START_ISNULL)
				.and(end).notNull(RCode.AGENT_PARAM_END_ISNULL);

		String direction = request.getParameter("direction");
		direction = StrUtil.isEmpty(direction) ? "backward" : direction;

		String limit = request.getParameter("limit");
		Long limitLongVal = StrUtil.isEmpty(limit) ? 100L : Long.valueOf(limit);

		String[] logqls = request.getParameterValues("logql[]");
		List<Map> requestParamList = new ArrayList<>();
		for (String logql : logqls) {
			logql = logql.replaceAll("&quot;", "\"");

			Map requestParam = Tool.MapUtil.newHashMap(2);
			requestParam.put("query",  URLUtil.decode(logql));
			requestParam.put("start", start);
			requestParam.put("end", end);
			requestParam.put("limit", limit);
			requestParam.put("interval", request.getParameter("interval"));
			requestParam.put("direction", direction);
			requestParamList.add(requestParam);
		}

		List<List<Map>> resultList = this.requestLokiApi(requestParamList);
		String timeFlag = request.getParameter("timeFlag");
		int timeFlagIntVal = StrUtil.isEmpty(timeFlag) ? 0 : Integer.valueOf(timeFlag);

		ServletOutputStream resOutputStream = null;
		File file = null;
		try {
			resOutputStream = response.getOutputStream();

			// 处理并返回
			List<String> messageList = this.handleResultList(resultList, timeFlagIntVal == 1, direction, limitLongVal);

			file = new File(Constant.TEMP_PATH + File.separator + "lokilog_" + UUID.randomUUID());
			FileUtil.writeLines(messageList, file, "UTF-8");

			byte[] bytes = FileUtil.readBytes(file);
			String transfer = response.getHeader("Transfer-Encoding");
			if (StringUtils.isEmpty(transfer)) {
				response.setContentLength(bytes.length);
			}
			resOutputStream.write(bytes);
			resOutputStream.flush();
		} catch (IOException e) {
			try {
				response.sendError(500, "request error");
			} catch (IOException e1) {
				log.error("proxy request error", e1);
			}
			log.error("request error : ", e);
		} finally {
			if (resOutputStream != null) {
				resOutputStream.close();
			}
			// 删除临时文件
			if (file.exists()) {
				log.info("file delete:" + file.delete());
			}
		}
	}

	private List<String> handleResultList(List<List<Map>> resultList, boolean timeFlag, String direction, Long limit) {
		List<Map> allResultList = resultList.stream().flatMap(Collection::stream).collect(Collectors.toList());
		List<Map> mapList = new ArrayList<>();
		for (Map logInfoMap : allResultList) {
			Map stream = (Map) logInfoMap.get("stream");
			List<List> values = (List) logInfoMap.get("values");

			for (int i = 0; i < values.size(); i++) {
				Map map = new HashMap();
				JSONArray timestampAndMsgArray = (JSONArray) values.get(i);
				String timestamp = (String) timestampAndMsgArray.get(0);
				String format = DateFormatUtils.format(Long.valueOf(timestamp) / 1000000, "yyyy-MM-dd HH:mm:ss");
				String message = (String) timestampAndMsgArray.get(1);
				map.put("timestamp", timestamp);
				map.put("message", timeFlag ? format + " " + message : message);
				map.put("uuid", MD5.create().digestHex16(timestamp + message + JSONObject.toJSONString(stream)));
				mapList.add(map);
			}
		}

		// 根据 uuid 进行去重
		mapList = mapList.stream()
				.collect(Collectors.collectingAndThen(
						Collectors
								.toCollection(() -> new TreeSet<>(Comparator.comparing(o -> o.get("uuid").toString()))),
						ArrayList::new));

		// 按照 direction 进行 排序
		if ("backward".equalsIgnoreCase(direction)) {
			mapList = mapList.stream().sorted((s1, s2) -> Long.valueOf(s2.get("timestamp").toString())
					.compareTo(Long.valueOf(s1.get("timestamp").toString()))).collect(Collectors.toList());
		} else {
			mapList = mapList.stream().sorted((s1, s2) -> Long.valueOf(s1.get("timestamp").toString())
					.compareTo(Long.valueOf(s2.get("timestamp").toString()))).collect(Collectors.toList());
		}

		List<String> messageList = mapList.stream().map(map -> (String) map.get("message"))
				.collect(Collectors.toList());
		if (messageList.size() > limit) {
			messageList = messageList.subList(0, limit.intValue());
		}
		return messageList;
	}


	private List<List<Map>> requestLokiApi(List<Map> requestParamList) {
		List<Callable<JSONObject>> tasks = new ArrayList<>();
		for (Map<String, String> requestParam : requestParamList) {
			Callable task = new Callable() {
				@Override
				public JSONObject call() throws Exception {
					JSONObject jsonObject = null;
					try {
						String query = requestParam.get("query");
						String start = requestParam.get("start");
						String end = requestParam.get("end");
						String limit = requestParam.get("limit");
						String interval = requestParam.get("interval");
						String direction = requestParam.get("direction");
						jsonObject = lokiApiService.query_range(query, start, end, limit, null, interval, direction, null, null);
					} catch (Exception e) {
						log.error(e);
					}
					return jsonObject;
				}
			};
			tasks.add(task);
		}

		List<List<Map>> resultList = new ArrayList<>();
		try {
			List<Future<JSONObject>> futures = exec.invokeAll(tasks);
			for (Future<JSONObject> future : futures) {
				if (!future.isCancelled()) {
					JSONObject result = future.get();
					if (result != null) {
						Map data = (Map) result.get("data");
						if (data != null && !"matrix".equals(data.get("resultType").toString())) {
							List<Map> resultArray = (List<Map>) data.get("result");
							resultList.add(resultArray);
						}
					}
				}
			}
		} catch (InterruptedException | ExecutionException e) {
			log.error(e);
		}
		return resultList;
	}

}
