package com.nis.modules.promserver.controller;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.promserver.service.PromApiService;
import com.nis.modules.promserver.utils.AgentProxyApiUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@SuppressWarnings("all")
public class PromProxyController {

    @Autowired
    private PromApiService promApiService;


    @RequestMapping(value = "/prom/**")
    public R promProxy(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String promPath = request.getServletPath().substring(5);

        switch (promPath) {
            /**
             * GET
             * URL query parameters:
             *      query=<string>: Prometheus expression query string.
             *      time=<rfc3339 | unix_timestamp>: Evaluation timestamp. Optional.
             *      timeout=<duration>: Evaluation timeout. Optional. Defaults to and is capped by the value of the -query.timeout flag.
             * @link https://prometheus.io/docs/prometheus/latest/querying/api/
             */
            case "/api/v1/query": {
                String query = request.getParameter("query");
                ValidateUtils.is(query).notNull(RCode.AGENT_PARAM_QUERY_ISNULL);

                Map<String, String> requestParam = Tool.MapUtil.of(
                        Pair.of("query", URLUtil.decode(query)),
                        Pair.of("time", request.getParameter("time")),
                        Pair.of("timeout", request.getParameter("timeout")));

                HttpEntity httpEntity = AgentProxyApiUtil.buildHttpEntity(request);
                JSONObject resultData = promApiService.query(httpEntity, requestParam);
                return R.ok().put("status", resultData.get("status")).putAllData(resultData);
            }
            /**
             * GET
             * URL query parameters:
             *      query=<string>: Prometheus expression query string.
             *      start=<rfc3339 | unix_timestamp>: Start timestamp, inclusive.
             *      end=<rfc3339 | unix_timestamp>: End timestamp, inclusive.
             *      step=<duration | float>: Query resolution step width in duration format or float number of seconds.
             *      timeout=<duration>: Evaluation timeout. Optional. Defaults to and is capped by the value of the -query.timeout flag.
             * Custom parameter:
             *     nullType=<string>: Automatic filling data type
             *     filter=<string>: The result of restrictions is used to query Alert Message data.
             * @link https://prometheus.io/docs/prometheus/latest/querying/api/
             */
            case "/api/v1/query_range": {
                String query = request.getParameter("query");
                String start = request.getParameter("start");
                String end = request.getParameter("end");
                String step = request.getParameter("step");

                ValidateUtils.is(query).notNull(RCode.AGENT_PARAM_QUERY_ISNULL)
                        .and(start).notNull(RCode.AGENT_PARAM_START_ISNULL)
                        .and(end).notNull(RCode.AGENT_PARAM_END_ISNULL)
                        .and(step).notNull(RCode.AGENT_PARAM_STEP_ISNULL);

                Map<String, String> requestParam = Tool.MapUtil.of(
                        Pair.of("query", URLUtil.decode(query)),
                        Pair.of("start", start),
                        Pair.of("end", end),
                        Pair.of("step", step));

                // Custom parameter
                requestParam.put("nullType", request.getParameter("nullType"));
                requestParam.put("filter", request.getParameter("filter"));

                HttpEntity httpEntity = AgentProxyApiUtil.buildHttpEntity(request);
                JSONObject resultData = promApiService.query_range(httpEntity, requestParam);
                return R.ok().put("status", resultData.get("status")).putAllData(resultData);
            }
            /**
             *  GET
             *  URL query parameters:
             *      start=<rfc3339 | unix_timestamp>: Start timestamp. Optional.
             *      end=<rfc3339 | unix_timestamp>: End timestamp. Optional.
             *      match[]=<series_selector>: Repeated series selector argument that selects the series from which to read the label names. Optional.
             *  @link https://prometheus.io/docs/prometheus/latest/querying/api/
             */
            case "/api/v1/labels": {
                String[] matchs = request.getParameterValues("match[]");

                List<String> matchList = Tool.ListUtil.list(false);
                if (matchs != null) {
                    matchList = Stream.of(matchs).map(s -> URLUtil.decode(s)).collect(Collectors.toList());
                }

                Map<String, Object> requestParam = Tool.MapUtil.of(
                        Pair.of("start", request.getParameter("start")),
                        Pair.of("end", request.getParameter("end")),
                        Pair.of("match[]", matchList));

                HttpEntity httpEntity = AgentProxyApiUtil.buildHttpEntity(request);
                JSONObject resultData = promApiService.labels(promPath, httpEntity, requestParam);
                return R.ok().put("status", resultData.get("status")).putAllData(resultData);
            }
            /**
             *  GET
             *  URL query parameters:
             *      match[]=<series_selector>: Repeated series selector argument that selects the series to return.
             *          At least one match[] argument must be provided.
             *      start=<rfc3339 | unix_timestamp>: Start timestamp.
             *      end=<rfc3339 | unix_timestamp>: End timestamp.
             *  @link https://prometheus.io/docs/prometheus/latest/querying/api/
             */
            case "/api/v1/series": {
                String[] matchs = request.getParameterValues("match[]");
                ValidateUtils.is(matchs).notNull(RCode.AGENT_PARAM_MATCH_ISNULL);

                List<String> matchList = Tool.ListUtil.list(false);
                if (matchs != null) {
                    matchList = Stream.of(matchs).map(s -> URLUtil.decode(s)).collect(Collectors.toList());
                }

                Map<String, Object> requestParam = Tool.MapUtil.of(
                        Pair.of("start", request.getParameter("start")),
                        Pair.of("end", request.getParameter("end")),
                        Pair.of("match[]", matchList));

                HttpEntity httpEntity = AgentProxyApiUtil.buildHttpEntity(request);
                JSONObject resultData = promApiService.series(httpEntity, requestParam);
                return R.ok().put("status", resultData.get("status")).putAllData(resultData);
            }
            /**
             * GET
             * URL query parameters:
             *      state: The state query parameter allows the caller to filter by active or dropped targets,
             *      (e.g., state=active, state=dropped, state=any). Note that an empty array is still returned for targets that are filtered out. Other values are ignored.
             * Custom parameter:
             *      dcId: Datacenter id Optional.
             *      agentId: Nz agent id
             *      type: Nz agent type Optional.
             *      endpointId: Endpoint id.
             *  @link https://prometheus.io/docs/prometheus/latest/querying/api/
             */
            case "/api/v1/targets": {
                Map<String, Object> requestParam = Tool.MapUtil.of(
                        Pair.of("dcId", request.getParameter("dcId")),
                        Pair.of("agentId", request.getParameter("agentId")),
                        Pair.of("endpointId", request.getParameter("endpointId")),
                        Pair.of("type", request.getParameter("type")),
                        Pair.of("state", request.getParameterValues("state") != null ? Arrays.asList(request.getParameterValues("state")) : Tool.ListUtil.empty()),
                        Pair.of("limit", request.getParameter("limit")));

                HttpEntity httpEntity = AgentProxyApiUtil.buildHttpEntity(request);
                JSONObject resultData = promApiService.targets(httpEntity, requestParam);
                return R.ok().put("status", resultData.get("status")).putAllData(resultData);
            }
            /**
             * GET
             * URL query parameters:
             *      limit=<number>: Maximum number of metrics to return.
             *      metric=<string>: A metric name to filter metadata for. All metric metadata is retrieved if left empty.
             * Custom parameter:
             *      dcId: Datacenter id Optional.
             *      agentId: Nz agent id
             *      type: Nz agent type Optional.
             *      endpointId: Endpoint id.
             *  @link https://prometheus.io/docs/prometheus/latest/querying/api/
             */
            case "/api/v1/metadata": {
                Map<String, Object> requestParam = Tool.MapUtil.of(
                        Pair.of("dcId", request.getParameter("dcId")),
                        Pair.of("agentId", request.getParameter("agentId")),
                        Pair.of("endpointId", request.getParameter("endpointId")),
                        Pair.of("type", request.getParameter("type")),
                        Pair.of("metric", request.getParameter("metric")),
                        Pair.of("limit", request.getParameter("limit")));

                HttpEntity httpEntity = AgentProxyApiUtil.buildHttpEntity(request);
                JSONObject resultData = promApiService.metadata(httpEntity, requestParam);
                return R.ok().put("status", resultData.get("status")).putAllData(resultData);
            }
            /**
             * GET
             * URL query parameters:
             *      match_target=<label_selectors>: Label selectors that match targets by their label sets. All targets are selected if left empty.
             *      metric=<string>: A metric name to retrieve metadata for. All metric metadata is retrieved if left empty.
             *      limit=<number>: Maximum number of targets to match.
             * Custom parameter:
             *      dcId: Datacenter id Optional.
             *      agentId: Nz agent id
             *      type: Nz agent type Optional.
             *      endpointId: Endpoint id.
             *  @link https://prometheus.io/docs/prometheus/latest/querying/api/
             */
            case "/api/v1/targets/metadata": {
                Map<String, Object> requestParam = Tool.MapUtil.of(
                        Pair.of("dcId", request.getParameter("dcId")),
                        Pair.of("agentId", request.getParameter("agentId")),
                        Pair.of("endpointId", request.getParameter("endpointId")),
                        Pair.of("type", request.getParameter("type")),
                        Pair.of("match_target", request.getParameter("match_target")),
                        Pair.of("metric", request.getParameter("metric")),
                        Pair.of("limit", request.getParameter("limit")));

                HttpEntity httpEntity = AgentProxyApiUtil.buildHttpEntity(request);
                JSONObject resultData = promApiService.targetsMetadata(httpEntity, requestParam);
                return R.ok().put("status", resultData.get("status")).putAllData(resultData);
            }
            default: {
                promApiService.promApiProxy(request, response, promPath);
                return null;
            }
        }
    }


    @RequestMapping(value = "/prom/api/v1/label/{label_name}/values")
    public R labelValues(HttpServletRequest request) throws IOException {
        String[] matchs = request.getParameterValues("match[]");
        Map<String, Object> requestParam = Tool.MapUtil.of(
                Pair.of("start", request.getParameter("start")),
                Pair.of("end", request.getParameter("end")),
                Pair.of("match[]", matchs == null ? Tool.ListUtil.empty() : Arrays.asList(matchs)));

        HttpEntity httpEntity = AgentProxyApiUtil.buildHttpEntity(request);
        String promPath = request.getServletPath().substring(5);
        JSONObject resultData = promApiService.labels(promPath, httpEntity, requestParam);
        return R.ok().put("status", resultData.get("status")).putAllData(resultData);
    }
}
