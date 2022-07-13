package com.nis.modules.promserver.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.promserver.entity.Agent;
import org.springframework.http.HttpEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public interface LokiApiService extends IService<Agent> {

    /**
     * GET /loki/api/v1/query allows for doing queries against a single point in time.
     * The URL query parameters support the following values:
     * query: The LogQL query to perform
     * limit: The max number of entries to return. It defaults to 100. Only applies to query types which produce a stream(log lines) response.
     * time: The evaluation time for the query as a nanosecond Unix epoch or another supported format. Defaults to now.
     * direction: Determines the sort order of logs. Supported values are forward or backward. Defaults to backward.
     *
     * @param httpEntity
     * @param requestParam
     */
    JSONObject query(HttpEntity httpEntity, Map<String, String> requestParam);

    /**
     * GET /loki/api/v1/query_range is used to do a query over a range of time and
     * accepts the following query parameters in the URL:
     * query: The LogQL query to perform
     * limit: The max number of entries to return. It defaults to 100. Only applies to query types which produce a stream(log lines) response.
     * start: The start time for the query as a nanosecond Unix epoch or another supported format. Defaults to one hour ago.
     * end: The end time for the query as a nanosecond Unix epoch or another supported format. Defaults to now.
     * step: Query resolution step width in duration format or float number of seconds. duration refers to Prometheus duration strings of the form [0-9]+[smhdwy]. For example, 5m refers to a duration of 5 minutes. Defaults to a dynamic value based on start and end. Only applies to query types which produce a matrix response.
     * interval: This parameter is experimental; see the explanation under Step versus interval. Only return entries at (or greater than) the specified interval, can be a duration format or float number of seconds. Only applies to queries which produce a stream response.
     * direction: Determines the sort order of logs. Supported values are forward or backward. Defaults to backward.
     * Custom parameter:
     * format: The result of restrictions is used to query Alert Message data.
     * filter: 可选值：1/0,默认：0 resultType = streams  时有效
     *
     * @param httpEntity
     * @param requestParam
     * @return
     */
    JSONObject query_range(HttpEntity httpEntity, Map<String, String> requestParam);


    /**
     * GET /loki/api/v1/labels
     * retrieves the list of known labels within a given time span. Loki may use a larger time span than the one specified.
     * It accepts the following query parameters in the URL:
     * start: The start time for the query as a nanosecond Unix epoch. Defaults to 6 hours ago.
     * end: The end time for the query as a nanosecond Unix epoch. Defaults to now.
     *
     * @param proxyPath
     * @param httpEntity
     * @param requestParam
     * @return
     */
    JSONObject labels(String proxyPath, HttpEntity httpEntity, Map<String, String> requestParam);


    /**
     * other api proxy
     *
     * @param request
     * @param response
     * @param proxyPath
     */
    void lokiServerProxy(HttpServletRequest request, HttpServletResponse response, String proxyPath);

}
