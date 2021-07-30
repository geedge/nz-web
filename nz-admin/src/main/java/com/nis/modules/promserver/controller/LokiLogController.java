package com.nis.modules.promserver.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlPath;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import net.sf.json.util.JSONUtils;
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
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Controller
public class LokiLogController {

    private static final Log log = Log.get();

    @Autowired
    private PromserverService promserverService;

    private static ExecutorService exec = Executors.newCachedThreadPool();

    @RequestMapping(value = "/logs/loki/export")
    public void logExport(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ValidateUtils.is(request.getParameterValues("logql")).notNull(RCode.PROMSERVER_PARAM_QUERY_ISNULL)
                .and(request.getParameter("start")).notNull(RCode.PROMSERVER_PARAM_START_ISNULL)
                .and(request.getParameter("end")).notNull(RCode.PROMSERVER_PARAM_END_ISNULL);

        // 随机 可用 loki
        Promserver server = promserverService.getOneAvaPromServer(null, null, Constant.PromserverType.LOKI.getValue());
        if (ToolUtil.isEmpty(server)) {
            ValidateUtils.is(server).notNull(RCode.PROMSERVER_UNAVAILABLE);
        }
        log.debug("loki server:{}", JSONUtils.valueToString(server));

        String[] logqls = request.getParameterValues("logql");
        List<String> requestUrlList = new ArrayList<>();
        for (String logql : logqls) {
            String queryString = ReflectUtil.invoke(request, "getQueryString");
            logql = logql.replaceAll("&quot;", "\"");
            String url = UrlBuilder.create().setScheme("http").setHost(server.getHost()).setPort(server.getPort()).setPath(UrlPath.of("/loki/api/v1/query_range", Charset.forName("UTF-8"))).toURL().toString() + "?query=" + URLEncoder.encode(logql) + "&" + queryString;
            requestUrlList.add(url);
        }

        List<List<Map>> resultList = this.requestLokiApi(requestUrlList, server, request, response);
        String timeFlag = request.getParameter("timeFlag");
        String direction = request.getParameter("direction");
        int timeFlagIntVal = StrUtil.isEmpty(timeFlag) ? 0 : Integer.valueOf(timeFlag);
        direction = StrUtil.isEmpty(direction) ? "backward" : "forward ";

        String limit = request.getParameter("limit");
        Long limitLongVal = StrUtil.isEmpty(limit) ? 100L : Long.valueOf(limit);

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

        // 根据　uuid　进行去重
        mapList = mapList.stream().collect(
                Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(o -> o.get("uuid").toString()))), ArrayList::new)
        );

        // 按照 direction 进行 排序
        if ("backward".equalsIgnoreCase(direction)) {
            mapList = mapList.stream().sorted((s1, s2) -> Long.valueOf(s2.get("timestamp").toString()).compareTo(Long.valueOf(s1.get("timestamp").toString()))).collect(Collectors.toList());
        } else {
            mapList = mapList.stream().sorted((s1, s2) -> Long.valueOf(s1.get("timestamp").toString()).compareTo(Long.valueOf(s2.get("timestamp").toString()))).collect(Collectors.toList());
        }

        List<String> messageList = mapList.stream().map(map -> (String) map.get("message")).collect(Collectors.toList());
        if (messageList.size() > limit) {
            messageList = messageList.subList(0, limit.intValue());
        }
        return messageList;
    }

    private List<List<Map>> requestLokiApi(List<String> requestUrlList, Promserver server, HttpServletRequest request, HttpServletResponse response) {
        List<Callable<String>> tasks = new ArrayList<>();
        requestUrlList.stream().forEach(url -> {
            Callable task = new Callable() {
                @Override
                public String call() throws Exception {
                    log.debug("loki url: {}", url);
                    HttpResponse execute = HttpUtil.createGet(url).auth(server.getToken()).execute();
                    boolean ok = execute.isOk();
                    if (ok) {
                        return execute.body();
                    }
                    response.sendError(500, execute.body());
                    return "";
                }
            };
            tasks.add(task);
        });

        List<List<Map>> resultList = new ArrayList<>();
        try {
            List<Future<String>> futures = exec.invokeAll(tasks);
            for (Future<String> future : futures) {
                if (!future.isCancelled()) {
                    String result = future.get();
                    if (StrUtil.isNotEmpty(result)) {
                        Map map = JSONObject.parseObject(result, Map.class);
                        Map data = (Map) map.get("data");
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
