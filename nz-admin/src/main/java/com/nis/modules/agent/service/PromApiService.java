package com.nis.modules.agent.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.agent.entity.Agent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface PromApiService extends IService<Agent> {


    /**
     * 根据 configuration metrics_storage_type 获取 nz-agent cortex/promehteus 请求前缀
     *
     * @return
     */
    String getPromApiProxyPathPrefix();

    /**
     * 获取一个可用的 Global Agent
     *
     * @return
     */
    Agent getOneAvaliableGlobalAgent();

    /**
     * 获取一个可用的 Global Agent
     *
     * @param agentId
     * @param dcId
     * @param endpointId
     * @param type
     * @return
     */
    Agent getOneAvaPromServer(Integer agentId, Integer dcId, Integer endpointId, Integer type);

    /**
     * GET
     * URL query parameters:
     *      query=<string>: Prometheus expression query string.
     *      time=<rfc3339 | unix_timestamp>: Evaluation timestamp. Optional.
     *      timeout=<duration>: Evaluation timeout. Optional. Defaults to and is capped by the value of the -query.timeout flag.
     *
     * @param query
     * @param time
     * @param timeout
     * @return
     */
    JSONObject query(String query, String time, String timeout);

    /**
     * GET
     * URL query parameters:
     *      query=<string>: Prometheus expression query string.
     *      start=<rfc3339 | unix_timestamp>: Start timestamp, inclusive.
     *      end=<rfc3339 | unix_timestamp>: End timestamp, inclusive.
     *      step=<duration | float>: Query resolution step width in duration format or float number of seconds.
     *      timeout=<duration>: Evaluation timeout. Optional. Defaults to and is capped by the value of the -query.timeout flag.
     * Custom parameter:
     *      nullType=<string>: Automatic filling data type
     *      filter=<string>: The result of restrictions is used to query Alert Message data.
     *
     * @param query
     * @param start
     * @param end
     * @param step
     * @param nullType
     * @param filter
     * @return
     */
    JSONObject query_range(String query, String start, String end, String step, String nullType, String filter);

    /**
     * GET
     * URL query parameters:
     *      start=<rfc3339 | unix_timestamp>: Start timestamp. Optional.
     *      end=<rfc3339 | unix_timestamp>: End timestamp. Optional.
     *      match[]=<series_selector>: Repeated series selector argument that selects the series from which to read the label names. Optional.
     *
     * @param path
     * @param start
     * @param end
     * @param matchList
     * @return
     */
    JSONObject labels(String path, String start, String end, List<String> matchList);


    /**
     * GET
     * URL query parameters:
     *      match[]=<series_selector>: Repeated series selector argument that selects the series to return.
     *          At least one match[] argument must be provided.
     *      start=<rfc3339 | unix_timestamp>: Start timestamp.
     *      end=<rfc3339 | unix_timestamp>: End timestamp.
     *
     * @param start
     * @param end
     * @param matchList
     * @return
     */
    JSONObject series(String start, String end, List<String> matchList);

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
     *
     * @param dcId
     * @param agentId
     * @param endpointId
     * @param type
     * @param stateList
     * @param limit
     * @return
     */
    JSONObject targets(String dcId, String agentId, String endpointId, String type, List<String> stateList, String limit);


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
     *
     * @param dcId
     * @param agentId
     * @param endpointId
     * @param type
     * @param metric
     * @param limit
     * @return
     * @link https://prometheus.io/docs/prometheus/latest/querying/api/
     */
    JSONObject metadata(String dcId, String agentId, String endpointId, String type, String metric, String limit);


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
     *
     * @param dcId
     * @param agentId
     * @param endpointId
     * @param type
     * @param matchTarget
     * @param metric
     * @param limit
     * @return
     * @link https://prometheus.io/docs/prometheus/latest/querying/api/
     */
    JSONObject targetsMetadata(String dcId, String agentId, String endpointId, String type, String matchTarget, String metric, String limit);

    /**
     * other
     *
     * @param request
     * @param response
     * @param Agent
     */
    void promApiProxy(HttpServletRequest request, HttpServletResponse response, String Agent);

}
