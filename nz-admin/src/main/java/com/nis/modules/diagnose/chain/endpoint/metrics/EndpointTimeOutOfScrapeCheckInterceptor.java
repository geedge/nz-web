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
 * EndpointTimeOutOfScrapeCheckInterceptor
 * 检查 Endpoint Scrape Job 失败原因是不是 timeout 问题
 *
 * @author shizhendong
 * @date 2023/1/4
 */
@Component
public class EndpointTimeOutOfScrapeCheckInterceptor implements DiagnoseInterceptor {

    private static final Log log = Log.get();

    private static RedisTemplate redisTemplate;

    private static PromApiService promApiService;

    @Autowired
    public void initBean(RedisTemplate redisTemplate, PromApiService promApiService) {
        EndpointTimeOutOfScrapeCheckInterceptor.redisTemplate = redisTemplate;
        EndpointTimeOutOfScrapeCheckInterceptor.promApiService = promApiService;
    }

    @Override
    public void intercept(Chain chain) {
        boolean continueDiagnosis = chain.isContinueDiagnosis();
        log.info("[EndpointTimeOutOfScrapeCheckInterceptor] [continue diagnosis: {}]", continueDiagnosis);

        if (continueDiagnosis) {
            // process object
            MonitorEndpoint endpoint = (MonitorEndpoint) chain.object();

            // check Scrape Time out
            boolean timeOut = this.checkScrapeTimeOut(endpoint.getId());

            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "metric.diagnose.endpoint.item.timeout")
                    .put("resolution", Tool.StrUtil.EMPTY)
                    .map();
            if (!timeOut) {
                // PASS
                resultMap.put("state", DiagnoseState.PASS.getValue());
            } else {
                // DID_NOT_PASS
                resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
                resultMap.put("resolution", "diagnose.endpoint.resolution.timeout");

                // stopDiagnose
                chain.stopDiagnose();
            }

            // update check msg
            DiagnoseClient.addDiagnoseMsg(resultMap);
        } else {
            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "metric.diagnose.endpoint.item.timeout")
                    .put("state", DiagnoseState.NOT_CHECKED.getValue())
                    .put("resolution", Tool.StrUtil.EMPTY)
                    .map();

            // update check msg
            DiagnoseClient.addDiagnoseMsg(resultMap);
        }

        // next chain
        chain.proceed(chain.object());
    }

    /**
     * check Scrape Time Out
     *
     * @param endpointId
     * @return
     */
    private boolean checkScrapeTimeOut(Integer endpointId) {
        HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
        String agentId = opsForHash.get(Constant.REDIS_KEY_ENDPOINT_PROM, Tool.StrUtil.toString(endpointId));
        log.info("[checkScrapeTimeOut] [get agent id from redis] [key: {}] [agentId: {}]", Constant.REDIS_KEY_ENDPOINT_PROM, agentId);

        boolean timeOut = false;
        if (Tool.StrUtil.isNotEmpty(agentId)) {
            // nz 系统内已存在 agent & endpoint 对应关系，通过查询 /target api 确认是否为 timeout 问题
            JSONObject targets = promApiService.targets(null, agentId, null, null, Tool.ListUtil.empty(), null);
            Object activeTargets = JSONPath.read(targets.toJSONString(), "data.activeTargets");
            List<JSONObject> activeTargetList = (List) Tool.ObjectUtil.defaultIfNull(activeTargets, Tool.ListUtil.empty());

            JSONObject endpointTargetInfo = activeTargetList.stream()
                    .filter(Objects::nonNull)
                    .filter(jsonObject -> Tool.StrUtil.equals(Tool.StrUtil.toString(JSONPath.read(jsonObject.toJSONString(), "labels.endpoint_id")), endpointId.toString()))
                    .findFirst()
                    .orElse(new JSONObject());

            if (log.isDebugEnabled()) {
                log.debug("[checkScrapeTimeOut] [target api result content] [endpointId: {}] [info: {}]", endpointId, Tool.JSONUtil.toJsonStr(endpointTargetInfo));
            }

            String health = Tool.StrUtil.toString(endpointTargetInfo.get("health"));
            if (Tool.StrUtil.equalsIgnoreCase("down", health)) {
                String lastError = Tool.StrUtil.toString(endpointTargetInfo.get("lastError"));
                log.info("[checkScrapeTimeOut] [Error Msg] [endpointId: {}] [lastError: {}]", endpointId, lastError);
                if (Tool.StrUtil.isNotEmpty(lastError) && (lastError.contains("context deadline exceeded") || lastError.contains("i/o timeout"))) {
                    timeOut = true;
                }
            }
        }
        return timeOut;
    }

}
