package com.nis.modules.diagnose.chain.endpoint.metrics;


import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.service.PromApiService;
import com.nis.modules.diagnose.chain.DiagnoseClient;
import com.nis.modules.diagnose.chain.DiagnoseInterceptor;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * EndpointScrapeErrorMsgCheckInterceptor
 * 检查 Endpoint Scrape Job 失败原因
 *
 * @author shizhendong
 * @date 2023/1/4
 */
@Component
public class EndpointScrapeErrorMsgCheckInterceptor implements DiagnoseInterceptor {

    private static final Log log = Log.get();

    private static RedisTemplate redisTemplate;

    private static PromApiService promApiService;

    @Autowired
    public void initBean(RedisTemplate redisTemplate, PromApiService promApiService) {
        EndpointScrapeErrorMsgCheckInterceptor.redisTemplate = redisTemplate;
        EndpointScrapeErrorMsgCheckInterceptor.promApiService = promApiService;
    }

    @Override
    public void intercept(Chain chain) {
        boolean continueDiagnosis = chain.isContinueDiagnosis();
        log.info("[EndpointScrapeErrorMsgCheckInterceptor] [continue diagnosis: {}]", continueDiagnosis);

        if (continueDiagnosis) {
            // process object
            MonitorEndpoint endpoint = (MonitorEndpoint) chain.object();

            // check Scrape Job Status
            Map<Object, Object> resultMap = this.checkScrapeJobStatus(endpoint.getId());

            // update check msg
            DiagnoseClient.addDiagnoseMsg(resultMap);
        } else {
            // 未检查
            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "metric.diagnose.endpoint.item.scrape.status")
                    .put("state", DiagnoseState.NOT_CHECKED.getValue())
                    .put("resolution", Tool.StrUtil.EMPTY)
                    .map();

            // update check msg
            DiagnoseClient.addDiagnoseMsg(resultMap);
        }
    }

    /**
     * check Scrape Job Status
     *
     * @param endpointId
     * @return
     */
    private Map<Object, Object> checkScrapeJobStatus(Integer endpointId) {
        HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
        String agentId = opsForHash.get(Constant.REDIS_KEY_ENDPOINT_PROM, Tool.StrUtil.toString(endpointId));
        log.info("[checkScrapeJobStatus] [get agent id from redis] [key: {}] [agentId: {}]", Constant.REDIS_KEY_ENDPOINT_PROM, agentId);

        // nz 系统内已存在 agent & endpoint 对应关系，通过查询 /target api 确认是否为 timeout 问题
        JSONObject targets = promApiService.targets(null, agentId, null, null, Tool.ListUtil.empty(), null);
        Object activeTargets = JSONPath.read(targets.toJSONString(), "data.activeTargets");
        List<JSONObject> activeTargetList = (List) Tool.ObjectUtil.defaultIfNull(activeTargets, Tool.ListUtil.empty());

        JSONObject endpointTargetInfo = activeTargetList.stream()
                .filter(Objects::nonNull)
                .filter(jsonObject -> Tool.StrUtil.equals(Tool.StrUtil.toString(JSONPath.read(jsonObject.toJSONString(), "labels.endpoint_id")), endpointId.toString()))
                .findFirst()
                .orElse(new JSONObject());

        log.info("[checkScrapeJobStatus] [target api result content] [endpointId: {}] [info: {}]", endpointId, Tool.JSONUtil.toJsonStr(endpointTargetInfo));

        Map<Object, Object> resultMap = Tool.MapUtil.builder()
                .put("item", "metric.diagnose.endpoint.item.scrape.status")
                .put("resolution", Tool.StrUtil.EMPTY)
                .map();

        if (Tool.ObjectUtil.equal(endpointTargetInfo.get("health"), "up")) {
            // PASS
            resultMap.put("state", DiagnoseState.PASS.getValue());
        } else {
            String lastError = Tool.StrUtil.toString(endpointTargetInfo.get("lastError"));

            log.warn("[checkScrapeJobStatus] [check failed] [endpointId: {}] [lastError: {}]", endpointId, lastError);
            // DID NOT PASS
            resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());

            if (lastError.contains("connect: no route to host")) {
                // 请求被防火墙阻止
                resultMap.put("resolution", "diagnose.endpoint.resolution.firewall");
            } else if (lastError.contains("context deadline exceeded") || lastError.contains("i/o timeout")) {
                // 连接超时
                resultMap.put("resolution", "diagnose.endpoint.resolution.timeout");
            } else if (lastError.contains("server returned HTTP status 401 Unauthorized")) {
                // 身份验证失败
                resultMap.put("resolution", "diagnose.endpoint.resolution.auth");
            } else if (lastError.contains("connect: connection refused")) {
                // 端口无服务监听
                resultMap.put("resolution", "diagnose.endpoint.resolution.connectable");
            } else {
                resultMap.put("resolution", "diagnose.endpoint.resolution.error");
            }
        }
        return resultMap;
    }

}
