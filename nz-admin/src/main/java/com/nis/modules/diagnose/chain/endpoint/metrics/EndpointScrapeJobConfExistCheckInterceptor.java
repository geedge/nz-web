package com.nis.modules.diagnose.chain.endpoint.metrics;


import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
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
import java.util.stream.Collectors;

/**
 * EndpointScrapeJobConfExistCheckInterceptor
 * 检查 Endpoint Scrape Job 是否已下发
 *
 * @author shizhendong
 * @date 2023/1/4
 */
@Component
public class EndpointScrapeJobConfExistCheckInterceptor implements DiagnoseInterceptor {

    private static final Log log = Log.get();

    private static RedisTemplate redisTemplate;

    private static PromApiService promApiService;

    @Autowired
    public void initBean(RedisTemplate redisTemplate, PromApiService promApiService) {
        EndpointScrapeJobConfExistCheckInterceptor.redisTemplate = redisTemplate;
        EndpointScrapeJobConfExistCheckInterceptor.promApiService = promApiService;
    }

    @Override
    public void intercept(Chain chain) {
        boolean continueDiagnosis = chain.isContinueDiagnosis();
        log.info("[EndpointScrapeJobConfExistCheckInterceptor] [continue diagnosis: {}]", continueDiagnosis);

        if (continueDiagnosis) {
            // process object
            MonitorEndpoint endpoint = (MonitorEndpoint) chain.object();

            // check Scrape Job Config
            boolean configured = this.checkScrapeJobConfig(endpoint.getId());

            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "metric.diagnose.endpoint.item.scrape.config")
                    .put("resolution", Tool.StrUtil.EMPTY)
                    .map();

            if (configured) {
                // PASS
                resultMap.put("state", DiagnoseState.PASS.getValue());
            } else {
                // DID_NOT_PASS
                resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
                resultMap.put("resolution", "diagnose.endpoint.resolution.scrape.config");

                // stopDiagnose
                chain.stopDiagnose();
            }

            // update check msg
            DiagnoseClient.addDiagnoseMsg(resultMap);
        } else {
            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "metric.diagnose.endpoint.item.scrape.config")
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
     * check Scrape Job Config
     *
     * @param endpointId
     * @return
     */
    private boolean checkScrapeJobConfig(Integer endpointId) {
        HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
        String agentId = opsForHash.get(Constant.REDIS_KEY_ENDPOINT_PROM, Tool.StrUtil.toString(endpointId));
        log.info("[checkScrapeJobConfig] [get agent id from redis] [key: {}] [agentId: {}]", Constant.REDIS_KEY_ENDPOINT_PROM, agentId);

        boolean configured = false;
        if (Tool.StrUtil.isNotEmpty(agentId)) {
            // nz 系统内已存在 agent & endpoint 对应关系，通过查询 /target api 再次确认
            JSONObject targets = promApiService.targets(null, agentId, null, null, Tool.ListUtil.empty(), null);
            Object activeTargets = JSONPath.read(targets.toJSONString(), "data.activeTargets");
            List<JSONObject> activeTargetList = (List) Tool.ObjectUtil.defaultIfNull(activeTargets, Tool.ListUtil.empty());

            List<String> promServerConfiguredEndpoints = activeTargetList.stream()
                    .filter(Objects::nonNull)
                    .filter(jsonObject -> ObjectUtil.isNotNull(JSONPath.read(jsonObject.toJSONString(), "labels.endpoint_id")))
                    .map(jsonObject -> JSONPath.read(jsonObject.toJSONString(), "labels.endpoint_id").toString())
                    .collect(Collectors.toList());

            if (promServerConfiguredEndpoints.contains(StrUtil.toString(endpointId))) {
                configured = true;
            }
        }

        log.info("[checkScrapeJobConfig] [configuration exist in prometheus] [flag: {}]", configured);
        return configured;
    }

}
