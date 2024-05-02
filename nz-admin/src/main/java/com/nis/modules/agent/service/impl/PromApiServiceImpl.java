package com.nis.modules.agent.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.agent.dao.AgentDao;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.PromApiService;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.endpoint.service.MonitorEndpointAgentMapService;
import com.nis.modules.sys.service.SysConfService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PromApiServiceImpl extends ServiceImpl<AgentDao, Agent> implements PromApiService {

    private static Log log = Log.get();

    @Autowired
    private SysConfService sysConfService;

    @Autowired
    private MonitorEndpointAgentMapService endpointAgentMapService;

    @Override
    public String getPromApiProxyPathPrefix() {
        String metricsStorageType = sysConfService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_TYPE);
        String agentPath = sysConfService.getValue(Constant.CONFAGENT_PATH);
        // 根据 configuration metrics_storage_type 获取 nz-agent cortex/promehteus 请求前缀
        String prePath = Tool.StrUtil.equals("1", metricsStorageType) ? "/prometheus/proxy" : "/cortex/proxy/prometheus";
        String proxyPath = String.format("%s%s", Tool.StrUtil.emptyToDefault(agentPath, Tool.StrUtil.EMPTY), prePath);
        return proxyPath;
    }

    @Override
    public Agent getOneAvaliableGlobalAgent() {
        Integer type = Constant.AgentType.GLOBAL.getValue();

        // 按照id 排序，暂时 使用相对固定的 健康 global
        Agent avaliableGlobalAgent = this.getOne(new QueryWrapper<Agent>().lambda()
                .eq(Agent::getStatus, 1)
                .eq(Agent::getType, type)
                .orderByDesc(Agent::getPriority)
                .orderByAsc(Agent::getId)
                .last("limit 1"));

        // 校验
        ValidateUtils.is(avaliableGlobalAgent).notNull(RCode.AGENT_UNAVAILABLE);
        return avaliableGlobalAgent;
    }

    @Override
    public Agent getOneAvaPromServer(Integer agentId, Integer dcId, Integer endpointId, Integer type) {
        // endpointId 不为空 先根据 endpointId 获取
        Agent server = null;
        if (endpointId != null) {
            Integer serverId = endpointAgentMapService.getAgentId(endpointId);
            server = this.getById(serverId);
        }

        if (server == null) {
            // 根据 agentId dcId type 再次获取
            type = (type == null) ? 1 : type;
            List<Agent> list = this.list(new QueryWrapper<Agent>().lambda()
                    .eq(true, Agent::getStatus, 1)
                    .eq(agentId != null, Agent::getId, agentId)
                    .eq(agentId == null && type != null, Agent::getType, type)
                    .eq(agentId == null && dcId != null, Agent::getDcId, dcId));


            if (CollectionUtils.isNotEmpty(list)) {
                server = list.get(RandomUtils.nextInt(0, list.size()));
            }
        }

        // 校验
        ValidateUtils.is(server).notNull(RCode.AGENT_UNAVAILABLE);
        return server;
    }


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
    @Override
    public JSONObject query(String query, String time, String timeout) {
        String proxyPathPrefix = this.getPromApiProxyPathPrefix();
        Agent server = this.getOneAvaliableGlobalAgent();

        String url = String.format("%s%s", proxyPathPrefix, "/api/v1/query");

        query = this.handleQueryExprIntervalVal(query, StrUtil.EMPTY);
        Map<Object, Object> requestParam = Tool.MapUtil.builder()
                .put("query", query)
                .put("time", time)
                .put("timeout", timeout)
                .build();

        return AgentUtil.getForObject(server, url, requestParam);
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
    @Override
    public JSONObject query_range(String query, String start, String end, String step, String nullType, String filter) {
        String proxyPathPrefix = this.getPromApiProxyPathPrefix();
        Agent server = this.getOneAvaliableGlobalAgent();

        String url = String.format("%s%s", proxyPathPrefix, "/api/v1/query_range");

        query = this.handleQueryExprIntervalVal(query, step);

        Map<Object, Object> requestParam = Tool.MapUtil.builder()
                .put("query", query)
                .put("start", start)
                .put("end", end)
                .put("step", step)
                .build();

        JSONObject resultData = AgentUtil.getForObject(server, url, requestParam);

        if ("success".equals(resultData.get("status"))) {
            // 处理缺失时间数据
            nullType = StrUtil.isEmpty(nullType) ? "connected" : nullType;

            if (Tool.MapUtil.isNotEmpty(resultData) && resultData.get("data") != null) {
                Map data = (Map) resultData.get("data");
                JSONArray resultArray = (JSONArray) data.get("result");
                // 去除 prometheus 返回结果中不符合条件的数据
                if (StrUtil.isNotBlank(filter)) {
                    filter = URLUtil.decode(filter);
                    resultArray = this.filterResult(resultArray, filter);
                }
                // 查询结果集超过系统设置最大限制 , -1 表示不做限制
                String queryMaxSeries = sysConfService.getValue(Constant.METRICS_QUERY_MAX_SERIES);
                Integer queryMaxSeriesIntVal = Integer.valueOf(queryMaxSeries);
                if (!queryMaxSeriesIntVal.equals(-1) && resultArray.size() > queryMaxSeriesIntVal) {
                    data.put("result", Collections.emptyList());
                    resultData.put("message", String.format("Configure the maximum number of Series: %s, query the total number of Series: %s", queryMaxSeriesIntVal, resultArray.size()));
                } else {
                    data.put("result", addMissingData(nullType, Integer.valueOf(start), Integer.valueOf(end), step, resultArray));
                }
                resultData.put("data", data);
            }
        }
        return resultData;
    }

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
    @Override
    public JSONObject labels(String path, String start, String end, List<String> matchList) {
        String proxyPathPrefix = this.getPromApiProxyPathPrefix();
        Agent server = this.getOneAvaliableGlobalAgent();

        String url = String.format("%s%s", proxyPathPrefix, path);

        Map<Object, Object> requestParam = Tool.MapUtil.builder()
                .put("start", start)
                .put("end", end)
                .put("match[]", matchList)
                .build();

        JSONObject resultData = AgentUtil.getForObject(server, url, requestParam);
        return resultData;
    }

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
    @Override
    public JSONObject series(String start, String end, List<String> matchList) {
        String proxyPathPrefix = this.getPromApiProxyPathPrefix();
        Agent server = this.getOneAvaliableGlobalAgent();

        String url = String.format("%s%s", proxyPathPrefix, "/api/v1/series");

        Map<Object, Object> requestParam = Tool.MapUtil.builder()
                .put("start", start)
                .put("end", end)
                .put("match[]", matchList)
                .build();

        JSONObject resultData = AgentUtil.getForObject(server, url, requestParam);
        return resultData;
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
     *
     * @param dcId
     * @param agentId
     * @param endpointId
     * @param type
     * @param stateList
     * @param limit
     * @return
     */
    @Override
    public JSONObject targets(String dcId, String agentId, String endpointId, String type, List<String> stateList, String limit) {
        // 获取 targets 代理 path
        String agentPath = sysConfService.getValue(Constant.CONFAGENT_PATH);
        String promPathPrefix = String.format("%s%s", agentPath, "/prometheus/proxy");

        String url = String.format("%s%s", promPathPrefix, "/api/v1/targets");

        Integer agentIdInt = StrUtil.isNotEmpty(agentId) ? Integer.valueOf(agentId) : null;
        Integer dcIdInt = StrUtil.isNotEmpty(dcId) ? Integer.valueOf(dcId) : null;
        Integer endpointIdInt = StrUtil.isNotEmpty(endpointId) ? Integer.valueOf(endpointId) : null;
        Integer typeInt = StrUtil.isNotEmpty(type) ? Integer.valueOf(type) : null;

        Agent server = this.getOneAvaPromServer(agentIdInt, dcIdInt, endpointIdInt, typeInt);

        Map<Object, Object> requestParam = Tool.MapUtil.builder()
                .put("state", stateList)
                .put("limit", limit)
                .build();

        JSONObject resultData = AgentUtil.getForObject(server, url, requestParam);
        return resultData;
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
     *
     * @param dcId
     * @param agentId
     * @param endpointId
     * @param type
     * @param matchTarget
     * @param metric
     * @param limit
     * @return
     */
    @Override
    public JSONObject targetsMetadata(String dcId, String agentId, String endpointId, String type, String matchTarget, String metric, String limit) {
        // 获取 /targets/metadata 代理 path
        String agentPath = sysConfService.getValue(Constant.CONFAGENT_PATH);
        String promPathPrefix = String.format("%s%s", agentPath, "/prometheus/proxy");

        String url = String.format("%s%s", promPathPrefix, "/api/v1/targets/metadata");

        Integer agentIdInt = StrUtil.isNotEmpty(agentId) ? Integer.valueOf(agentId) : null;
        Integer dcIdInt = StrUtil.isNotEmpty(dcId) ? Integer.valueOf(dcId) : null;
        Integer endpointIdInt = StrUtil.isNotEmpty(endpointId) ? Integer.valueOf(endpointId) : null;
        Integer typeInt = StrUtil.isNotEmpty(type) ? Integer.valueOf(type) : null;

        Agent server = this.getOneAvaPromServer(agentIdInt, dcIdInt, endpointIdInt, typeInt);

        Map<Object, Object> requestParam = Tool.MapUtil.builder()
                .put("match_target", matchTarget)
                .put("metric", metric)
                .put("limit", limit)
                .build();

        JSONObject resultData = AgentUtil.getForObject(server, url, requestParam);
        return resultData;
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
    @Override
    public JSONObject metadata(String dcId, String agentId, String endpointId, String type, String metric, String limit) {
        String proxyPathPrefix = this.getPromApiProxyPathPrefix();
        String url = String.format("%s%s", proxyPathPrefix, "/api/v1/metadata");

        Integer agentIdInt = StrUtil.isNotEmpty(agentId) ? Integer.valueOf(agentId) : null;
        Integer dcIdInt = StrUtil.isNotEmpty(dcId) ? Integer.valueOf(dcId) : null;
        Integer endpointIdInt = StrUtil.isNotEmpty(endpointId) ? Integer.valueOf(endpointId) : null;
        Integer typeInt = StrUtil.isNotEmpty(type) ? Integer.valueOf(type) : null;

        Agent server = this.getOneAvaPromServer(agentIdInt, dcIdInt, endpointIdInt, typeInt);

        Map<Object, Object> requestParam = Tool.MapUtil.builder()
                .put("metric", metric)
                .put("limit", limit)
                .build();

        JSONObject resultData = AgentUtil.getForObject(server, url, requestParam);
        return resultData;
    }


    /**
     * other
     *
     * @param request
     * @param response
     * @param promPath
     */
    @Override
    public void promApiProxy(HttpServletRequest request, HttpServletResponse response, String promPath) {
        Integer agentId = request.getParameter("agentId") != null ? Integer.valueOf(request.getParameter("agentId")) : null;
        Integer dcId = request.getParameter("dcId") != null ? Integer.valueOf(request.getParameter("dcId")) : null;
        Integer endpointId = request.getParameter("endpointId") != null ? Integer.valueOf(request.getParameter("endpointId")) : null;
        Integer type = request.getParameter("type") != null ? Integer.valueOf(request.getParameter("type")) : null;

        Agent server = this.getOneAvaPromServer(agentId, dcId, endpointId, type);

        // 构造请求路径
        String promPathPrefix = this.getPromApiProxyPathPrefix();
        String url = String.format("%s%s", promPathPrefix, promPath);

        AgentUtil.agentApiProxy(server, url, request, response);
    }

    /**
     * GET
     * URL query parameters:
     * query=<string>: Prometheus expression query string.
     * time=<rfc3339 | unix_timestamp>: Evaluation timestamp. Optional.
     * timeout=<duration>: Evaluation timeout. Optional. Defaults to and is capped by the value of the -query.timeout flag.
     *
     * @param queryStr
     * @param time
     * @param timeout
     * @return
     */
    @Override
    public JSONObject query_instant(String queryStr, String time, String timeout) {
        JSONObject resultData = this.query(queryStr, time, timeout);
        // format conversion
        resultData = this.queryResultToQueryRange(resultData);
        return resultData;
    }

    private JSONObject queryResultToQueryRange(JSONObject resultData) {
        // 获取 resultType
        JSONObject data = resultData.getJSONObject("data");
        String resultType = data.getString("resultType");
        String jsonStr = JSONUtil.toJsonStr(resultData);
        switch (resultType) {
            case "vector":
                List result = (List) JSONPath.read(jsonStr, "$.data.result");
                for (int i = 0; i < result.size(); i++) {
                    //value to values
                    JSONObject jsonObject = (JSONObject) result.get(i);
                    JSONArray newValuesArr = new JSONArray();
                    newValuesArr.add(jsonObject.remove("value"));
                    jsonObject.put("values", newValuesArr);
                }
                data.put("result", result);
                break;
            case "string":
            case "scalar": {
                Object value = JSONPath.read(jsonStr, "$.data.result");
                JSONPath.remove(resultData, "$.data.result");
                JSONPath.set(resultData, "$.data.result[0].metric", MapUtil.newHashMap());
                JSONPath.set(resultData, "$.data.result[0].values", Arrays.asList(value));
                break;
            }
            default:
                break;
        }
        return resultData;
    }

    /**
     * query | query_range
     * 在 chart 渲染时，step 参数会动态计算，当表达式中 包含 irate(aaa[5m]) 等函数时，会因为 step 变化从而导致 结果不准确
     * 查询前 根据正则匹配，将 [$interval] 替换为 [xx] xx 为step 的值
     *
     * @param expr
     * @param step
     * @return
     */
    private String handleQueryExprIntervalVal(String expr, String step) {
        if (ReUtil.contains("\\[\\s*\\$interval\\s*]", URLUtil.decode(expr))) {
            // 判断step是否小于两个周期的 scrape interval
            Integer defaultScrapeInterval = Integer.valueOf(sysConfService.getValue(Constant.SYSCONFIG_KEY_SCRAPEINTERVAL));
            if (StrUtil.isNotBlank(step)) {
                Integer stepVal = Integer.valueOf(StrUtil.sub(step, 0, step.length() - 1));
                if (step.endsWith("s")) {
                    if (stepVal < defaultScrapeInterval * 2) {
                        step = defaultScrapeInterval * 2 + "s";
                    }
                } else if (step.endsWith("m")) {
                    if (stepVal * 60 < defaultScrapeInterval * 2) {
                        step = defaultScrapeInterval * 2 + "s";
                    }
                } else {
                    if (stepVal * 60 * 60 < defaultScrapeInterval * 2) {
                        step = defaultScrapeInterval * 2 + "s";
                    }
                }
            } else {
                step = defaultScrapeInterval * 2 + "s";
            }
            // 正则替换 当查询语句中有[]时  将时间替换为step
            expr = URLUtil.decode(expr).replaceAll("\\[\\s*\\$interval\\s*]", "[" + step + "]");
        }
        return expr;
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


    /**
     * 获取完整时间序列
     *
     * @param start
     * @param end
     * @param stepStr
     * @return
     */
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

    /**
     * 补充缺失时间数据
     *
     * @param nullType
     * @param start
     * @param end
     * @param step
     * @param resultArray
     * @return
     */
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
        int timeListSize = timeList.size();
        for (int i = 0; i < resultArray.size(); i++) {
            JSONObject metric = (JSONObject) resultArray.get(i);
            List<JSONArray> values = (List<JSONArray>) metric.get("values");
            // 如果缺失数据 进行补全 只有 nullType 为 null zero 进行补充值
            if (values.size() < timeList.size() && !"connected".equals(nullType)) {
                Map<Integer, Object> valuesMap = new HashMap<>(2);
                for (JSONArray objects : values) {
                    valuesMap.put(Integer.valueOf(objects.get(0).toString()), 1);
                }
                for (int j = 0; j < timeListSize; j++) {
                    Integer time = timeList.get(j);
                    if (valuesMap.get(time) == null) {
                        JSONArray tempArray = new JSONArray();
                        tempArray.add(time);
                        tempArray.add(nullTypeAndValueMap.get(nullType));
                        values.add(j, tempArray);
                    }
                }
            }
        }
        return resultArray;
    }
}
