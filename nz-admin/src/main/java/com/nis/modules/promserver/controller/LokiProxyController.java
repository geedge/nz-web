package com.nis.modules.promserver.controller;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.promserver.service.LokiApiService;
import com.nis.modules.promserver.utils.AgentProxyApiUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@RestController
@SuppressWarnings("all")
public class LokiProxyController {

    @Autowired
    private LokiApiService lokiApiService;

    @RequestMapping(value = "/logs/loki/**")
    public R logLokiProxy(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String proxyPath = request.getServletPath().substring(5);

        switch (proxyPath) {
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
             */
            case "/loki/api/v1/query": {
                String query = request.getParameter("query");
                ValidateUtils.is(query).notNull(RCode.AGENT_PARAM_QUERY_ISNULL);

                Map<String, String> requestParam = Tool.MapUtil.of(
                        Pair.of("query", URLUtil.decode(query)),
                        Pair.of("limit", request.getParameter("limit")),
                        Pair.of("time", request.getParameter("time")),
                        Pair.of("direction", request.getParameter("direction")),
                        Pair.of("format", request.getParameter("format")),
                        Pair.of("filter", request.getParameter("filter")));

                HttpEntity httpEntity = AgentProxyApiUtil.buildHttpEntity(request);
                JSONObject resultData = lokiApiService.query(httpEntity, requestParam);
                return R.ok().put("status", resultData.get("status")).putAllData(resultData);
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
             */
            case "/loki/api/v1/query_range": {
                String query = request.getParameter("query");
                String start = request.getParameter("start");
                String end = request.getParameter("end");

                ValidateUtils.is(query).notNull(RCode.AGENT_PARAM_QUERY_ISNULL)
                        .and(start).notNull(RCode.AGENT_PARAM_START_ISNULL)
                        .and(end).notNull(RCode.AGENT_PARAM_END_ISNULL);

                Map<String, String> requestParam = Tool.MapUtil.of(
                        Pair.of("query", URLUtil.decode(query)),
                        Pair.of("start", start), Pair.of("end", end),
                        Pair.of("limit", request.getParameter("limit")),
                        Pair.of("step", request.getParameter("step")),
                        Pair.of("direction", request.getParameter("direction")),
                        Pair.of("format", request.getParameter("format")),
                        Pair.of("filter", request.getParameter("filter")));

                HttpEntity httpEntity = AgentProxyApiUtil.buildHttpEntity(request);
                JSONObject resultData = lokiApiService.query_range(httpEntity, requestParam);
                return R.ok().put("status", resultData.get("status")).putAllData(resultData);
            }
            /**
             * GET /loki/api/v1/labels
             *  retrieves the list of known labels within a given time span. Loki may use a larger time span than the one specified.
             *  It accepts the following query parameters in the URL:
             *      start: The start time for the query as a nanosecond Unix epoch. Defaults to 6 hours ago.
             *      end: The end time for the query as a nanosecond Unix epoch. Defaults to now.
             */
            case "/loki/api/v1/labels": {
                Map<String, String> requestParam = Tool.MapUtil.of(
                        Pair.of("start", request.getParameter("start")),
                        Pair.of("start", request.getParameter("start")),
                        Pair.of("end", request.getParameter("end")));

                HttpEntity httpEntity = AgentProxyApiUtil.buildHttpEntity(request);
                JSONObject resultData = lokiApiService.labels(proxyPath, httpEntity, requestParam);
                return R.ok().put("status", resultData.get("status")).putAllData(resultData);
            }
            default: {
                lokiApiService.lokiServerProxy(request, response, proxyPath);
                return null;
            }
        }
    }

    @RequestMapping(value = "/logs/loki/api/v1/label/{label_name}/values")
    public R labelValues(HttpServletRequest request) throws IOException {
        Map<String, String> requestParam = Tool.MapUtil.of(
                Pair.of("start", request.getParameter("start")),
                Pair.of("start", request.getParameter("start")),
                Pair.of("end", request.getParameter("end")));

        HttpEntity httpEntity = AgentProxyApiUtil.buildHttpEntity(request);
        String proxyPath = request.getServletPath().substring(5);

        JSONObject resultData = lokiApiService.labels(proxyPath, httpEntity, requestParam);
        return R.ok().put("status", resultData.get("status")).putAllData(resultData);
    }
}
