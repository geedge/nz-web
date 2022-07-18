package com.nis.modules.promserver.service.impl;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.promserver.dao.AgentDao;
import com.nis.modules.promserver.entity.Agent;
import com.nis.modules.promserver.service.PromApiService;
import com.nis.modules.promserver.utils.AgentProxyApiUtil;
import com.nis.modules.sys.service.SysConfService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class PromApiServiceImpl extends ServiceImpl<AgentDao, Agent> implements PromApiService {

    private static Log log = Log.get();

    @Autowired
    private AgentProxyApiUtil agentProxyApiUtil;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SysConfService sysConfService;

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
            HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
            String serverId = opsForHash.get(Constant.REDIS_KEY_ENDPOINT_PROM, endpointId.toString());
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

    @Override
    public JSONObject query(HttpEntity httpEntity, Map<String, String> requestParam) {
        String proxyPathPrefix = this.getPromApiProxyPathPrefix();
        Agent server = this.getOneAvaliableGlobalAgent();

        String url = String.format("%s%s", proxyPathPrefix, "/api/v1/query");

        String query = requestParam.get("query");
        requestParam.put("query", this.handleQueryExprIntervalVal(query, StrUtil.EMPTY));

        return agentProxyApiUtil.getForObject(server, url, httpEntity, requestParam);
    }

    @Override
    public JSONObject query_range(HttpEntity httpEntity, Map<String, String> requestParam) {
        String proxyPathPrefix = this.getPromApiProxyPathPrefix();
        Agent server = this.getOneAvaliableGlobalAgent();

        String url = String.format("%s%s", proxyPathPrefix, "/api/v1/query_range");

        String query = requestParam.get("query");
        String step = requestParam.get("step");

        requestParam.put("query", this.handleQueryExprIntervalVal(query, step));
        JSONObject resultData = agentProxyApiUtil.getForObject(server, url, httpEntity, requestParam);

        if ("success".equals(resultData.get("status"))) {
            // 处理缺失时间数据
            String nullType = requestParam.get("nullType");
            nullType = StrUtil.isEmpty(nullType) ? "connected" : nullType;

            if (Tool.MapUtil.isNotEmpty(resultData) && resultData.get("data") != null) {
                Integer start = Integer.valueOf(requestParam.get("start"));
                Integer end = Integer.valueOf(requestParam.get("end"));

                String filter = requestParam.get("filter");
                Map data = (Map) resultData.get("data");
                JSONArray resultArray = (JSONArray) data.get("result");
                // 去除 prometheus 返回结果中不符合条件的数据
                if (StrUtil.isNotBlank(filter)) {
                    filter = URLUtil.decode(filter);
                    resultArray = this.filterResult(resultArray, filter);
                }
                // 查询结果集超过系统设置最大限制 , -1 表示不做限制
                ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
                String queryMaxSeries = valueOperations.get(Constant.METRICS_QUERY_MAX_SERIES);
                if (StringUtils.isEmpty(queryMaxSeries)) {
                    queryMaxSeries = sysConfService.getValue(Constant.METRICS_QUERY_MAX_SERIES);
                    valueOperations.set(Constant.METRICS_QUERY_MAX_SERIES, queryMaxSeries, 30, TimeUnit.MINUTES);
                }

                Integer queryMaxSeriesIntVal = Integer.valueOf(queryMaxSeries);
                if (!queryMaxSeriesIntVal.equals(-1) && resultArray.size() > queryMaxSeriesIntVal) {
                    data.put("result", Collections.emptyList());
                    resultData.put("message", String.format("Configure the maximum number of Series: %s, query the total number of Series: %s", queryMaxSeriesIntVal, resultArray.size()));
                } else {
                    data.put("result", addMissingData(nullType, start, end, step, resultArray));
                }
                resultData.put("data", data);
            }
        }
        return resultData;
    }

    @Override
    public JSONObject labels(String path, HttpEntity httpEntity, Map<String, Object> requestParam) {
        String proxyPathPrefix = this.getPromApiProxyPathPrefix();
        Agent server = this.getOneAvaliableGlobalAgent();

        String url = String.format("%s%s", proxyPathPrefix, path);
        JSONObject resultData = agentProxyApiUtil.getForObject(server, url, httpEntity, requestParam);
        return resultData;
    }

    @Override
    public JSONObject series(HttpEntity httpEntity, Map<String, Object> requestParam) {
        String proxyPathPrefix = this.getPromApiProxyPathPrefix();
        Agent server = this.getOneAvaliableGlobalAgent();

        String url = String.format("%s%s", proxyPathPrefix, "/api/v1/series");
        JSONObject resultData = agentProxyApiUtil.getForObject(server, url, httpEntity, requestParam);
        return resultData;
    }

    @Override
    public JSONObject targets(HttpEntity httpEntity, Map<String, Object> requestParam) {
        // 获取 targets 代理 path
        String agentPath = sysConfService.getValue(Constant.CONFAGENT_PATH);
        String promPathPrefix = String.format("%s%s", agentPath, "/prometheus/proxy");

        String url = String.format("%s%s", promPathPrefix, "/api/v1/targets");

        Integer agentId = requestParam.get("agentId") != null ? Integer.valueOf(requestParam.get("agentId").toString()) : null;
        Integer dcId = requestParam.get("dcId") != null ? Integer.valueOf(requestParam.get("dcId").toString()) : null;
        Integer endpointId = requestParam.get("endpointId") != null ? Integer.valueOf(requestParam.get("endpointId").toString()) : null;
        Integer type = requestParam.get("type") != null ? Integer.valueOf(requestParam.get("type").toString()) : null;

        Agent server = this.getOneAvaPromServer(agentId, dcId, endpointId, type);

        Tool.MapUtil.removeAny(requestParam, "agentId", "dcId", "endpointId", "type");
        JSONObject resultData = agentProxyApiUtil.getForObject(server, url, httpEntity, requestParam);
        return resultData;
    }

    @Override
    public JSONObject targetsMetadata(HttpEntity httpEntity, Map<String, Object> requestParam) {
        // 获取 /targets/metadata 代理 path
        String agentPath = sysConfService.getValue(Constant.CONFAGENT_PATH);
        String promPathPrefix = String.format("%s%s", agentPath, "/prometheus/proxy");

        String url = String.format("%s%s", promPathPrefix, "/api/v1/targets/metadata");

        Integer agentId = requestParam.get("agentId") != null ? Integer.valueOf(requestParam.get("agentId").toString()) : null;
        Integer dcId = requestParam.get("dcId") != null ? Integer.valueOf(requestParam.get("dcId").toString()) : null;
        Integer endpointId = requestParam.get("endpointId") != null ? Integer.valueOf(requestParam.get("endpointId").toString()) : null;
        Integer type = requestParam.get("type") != null ? Integer.valueOf(requestParam.get("type").toString()) : null;

        Agent server = this.getOneAvaPromServer(agentId, dcId, endpointId, type);

        Tool.MapUtil.removeAny(requestParam, "agentId", "dcId", "endpointId", "type");
        JSONObject resultData = agentProxyApiUtil.getForObject(server, url, httpEntity, requestParam);
        return resultData;
    }

    @Override
    public JSONObject metadata(HttpEntity httpEntity, Map<String, Object> requestParam) {
        String proxyPathPrefix = this.getPromApiProxyPathPrefix();
        String url = String.format("%s%s", proxyPathPrefix, "/api/v1/metadata");

        Integer agentId = requestParam.get("agentId") != null ? Integer.valueOf(requestParam.get("agentId").toString()) : null;
        Integer dcId = requestParam.get("dcId") != null ? Integer.valueOf(requestParam.get("dcId").toString()) : null;
        Integer endpointId = requestParam.get("endpointId") != null ? Integer.valueOf(requestParam.get("endpointId").toString()) : null;
        Integer type = requestParam.get("type") != null ? Integer.valueOf(requestParam.get("type").toString()) : null;
        Agent server = this.getOneAvaPromServer(agentId, dcId, endpointId, type);

        Tool.MapUtil.removeAny(requestParam, "agentId", "dcId", "endpointId", "type");
        JSONObject resultData = agentProxyApiUtil.getForObject(server, url, httpEntity, requestParam);
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

        agentProxyApiUtil.agentApiProxy(server, url, request, response);
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
