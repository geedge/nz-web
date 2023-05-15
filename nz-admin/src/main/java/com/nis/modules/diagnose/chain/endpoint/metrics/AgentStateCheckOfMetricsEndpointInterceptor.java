package com.nis.modules.diagnose.chain.endpoint.metrics;


import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.AgentService;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.diagnose.chain.DiagnoseClient;
import com.nis.modules.diagnose.chain.DiagnoseInterceptor;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.sys.service.SysConfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AgentStateCheckOfMetricsEndpointInterceptor
 * 检查负责采集 Metrics Endpoint 数据的 Nz-Agent 是否健康
 *
 * @author shizhendong
 * @date 2023/1/4
 */
@Component
public class AgentStateCheckOfMetricsEndpointInterceptor implements DiagnoseInterceptor {

    private static final Log log = Log.get();

    private static AgentService agentService;

    private static SysConfService sysConfService;

    private static AssetAssetService assetService;

    @Autowired
    public void initBean(AgentService agentService, SysConfService sysConfService, AssetAssetService assetService) {
        AgentStateCheckOfMetricsEndpointInterceptor.agentService = agentService;
        AgentStateCheckOfMetricsEndpointInterceptor.assetService = assetService;
        AgentStateCheckOfMetricsEndpointInterceptor.sysConfService = sysConfService;
    }

    @Override
    public void intercept(Chain chain) {
        // process object
        MonitorEndpoint endpoint = (MonitorEndpoint) chain.object();

        // agent State Check Of Metrics
        Map<Object, Object> map = this.agentStateCheckOfMetrics(endpoint);

        Object state = map.get("state");
        if (Tool.ObjectUtil.equal(state, DiagnoseState.DID_NOT_PASS.getValue())) {
            // stopDiagnose
            chain.stopDiagnose();
        }

        // update check msg
        DiagnoseClient.addDiagnoseMsg(map);

        // next chain
        chain.proceed(chain.object());
    }

    /**
     * agent State Check Of Metrics Endpoint
     *
     * @param endpoint
     * @return
     */
    private Map<Object, Object> agentStateCheckOfMetrics(MonitorEndpoint endpoint) {
        // federation enable
        String federationEnabled = sysConfService.getValueOrDefault(Constant.SYSCONFIG_KEY_PROM_FEDER_ENABLED, "1");
        log.info("[agentStateCheckOfMetrics] [prometheus_federation_enabled] [value: {}]", federationEnabled);

        List<Agent> healthyGlobalList = agentService.list(new LambdaUpdateWrapper<Agent>()
                .eq(Agent::getType, Constant.AgentType.GLOBAL.getValue())
                .eq(Agent::getStatus, "1"));

        log.info("[agentStateCheckOfMetrics] [query prom_api by global agent] [healthy global agent size: {}]", healthyGlobalList.size());

        List<Agent> healthyDatacenterList = Tool.ListUtil.list(true);
        List<Agent> totalAgentList = agentService.list();

        //result
        Map<Object, Object> resultMap = Tool.MapUtil.builder()
                .put("item", "metric.diagnose.endpoint.item.agent.status")
                .put("resolution", Tool.StrUtil.EMPTY)
                .map();

        if (StrUtil.equals("0", federationEnabled)) {
            // federation 模式关闭  ，Global scrape endpoint jobs
            log.info("[agentStateCheckOfMetrics] [federation is not enabled. get global agents]");

            if (Tool.BooleanUtil.and(Tool.CollUtil.isNotEmpty(totalAgentList), Tool.CollUtil.isEmpty(healthyGlobalList))) {
                // has down agent
                resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
                resultMap.put("resolution", "diagnose.endpoint.resolution.agent.status");
            } else if (Tool.CollUtil.isNotEmpty(healthyGlobalList)) {
                // PASS
                resultMap.put("state", DiagnoseState.PASS.getValue());
            } else if (Tool.CollUtil.isEmpty(totalAgentList)) {
                // 没有配置 Agent 信息
                resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
                resultMap.put("resolution", "diagnose.asset.resolution.agent.config");
            }

        }
        if (StrUtil.equals("1", federationEnabled)) {
            // federation 模式开启  ，Per-datacenter scrape endpoint jobs
            Integer assetId = endpoint.getAssetId();
            AssetAsset asset = assetService.getById(assetId);

            // per-datacenter agent list
            healthyDatacenterList = agentService.list(new LambdaUpdateWrapper<Agent>()
                    .eq(Agent::getType, Constant.AgentType.PER_DATACENTER.getValue())
                    .eq(Agent::getDcId, asset.getDcId())
                    .eq(Agent::getStatus, "1"));

            log.info("[agentStateCheckOfMetrics] [per-datacenter agent scrapes endpoint metrics] [healthy per-datacenter agent size: {}] [healthy global agent size: {}]", healthyDatacenterList.size(), healthyGlobalList.size());

            if (Tool.BooleanUtil.and(Tool.CollUtil.isNotEmpty(healthyGlobalList), Tool.CollUtil.isNotEmpty(healthyDatacenterList))) {
                // PASS
                resultMap.put("state", DiagnoseState.PASS.getValue());
            } else if (Tool.CollUtil.isNotEmpty(totalAgentList) && (Tool.CollUtil.isEmpty(healthyGlobalList) || Tool.CollUtil.isEmpty(healthyDatacenterList))) {
                // has down agent
                resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
                resultMap.put("resolution", "diagnose.asset.resolution.agent.status");
            } else if (Tool.CollUtil.isEmpty(totalAgentList)) {
                // 没有配置 Agent 信息
                resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
                resultMap.put("resolution", "diagnose.asset.resolution.agent.config");
            }
        }
        return resultMap;
    }

}
