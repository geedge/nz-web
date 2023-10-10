package com.nis.modules.diagnose.chain.asset;


import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.AgentService;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.diagnose.chain.DiagnoseClient;
import com.nis.modules.diagnose.chain.DiagnoseInterceptor;
import com.nis.modules.sys.service.SysConfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AgentStateCheckOfPingInterceptor
 * 检查负责采集 Asset Ping Metrics 数据的 Nz-Agent 是否健康
 *
 * @author shizhendong
 * @date 2023/1/4
 */
@Component
public class AgentStateCheckOfPingInterceptor implements DiagnoseInterceptor {

    private static final Log log = Log.get();

    private static AgentService agentService;

    private static SysConfService sysConfService;

    @Autowired
    public void initBean(AgentService agentService, SysConfService sysConfService) {
        AgentStateCheckOfPingInterceptor.agentService = agentService;
        AgentStateCheckOfPingInterceptor.sysConfService = sysConfService;
    }

    @Override
    public void intercept(Chain chain) {
        boolean continueDiagnosis = chain.isContinueDiagnosis();
        log.info("[AgentStateCheckOfPingInterceptor] [continue diagnosis: {}]", continueDiagnosis);

        if (continueDiagnosis) {
            // process object
            AssetAsset asset = (AssetAsset) chain.object();

            // check Agent State Of Ping
            Map<Object, Object> map = this.checkAgentStateOfPing(asset);

            Object state = map.get("state");
            if (Tool.ObjectUtil.equal(state, DiagnoseState.DID_NOT_PASS.getValue())) {
                // stopDiagnose
                chain.stopDiagnose();
            }

            // update check msg
            DiagnoseClient.addDiagnoseMsg(map);
        } else {
            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "diagnose.asset.item.agent.status")
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
     * check Agent State Of Ping
     *
     * @param asset
     * @return
     */
    private Map<Object, Object> checkAgentStateOfPing(AssetAsset asset) {
        // blackbox source，  1: global    2: per-datacenter
        String assetPingFromConf = sysConfService.getValueOrDefault(Constant.SYSCONFIG_KEY_ASSET_PING_FROM, Constant.AssetPingFrom.GLOBAL.getValue());
        log.info("[checkAgentStateOfPing] [asset_ping_from: {}]", assetPingFromConf);

        // federation enable
        boolean federationEnabled = StrUtil.equals("1", sysConfService.getValueOrDefault(Constant.SYSCONFIG_KEY_PROM_FEDER_ENABLED, "1"));

        List<Agent> healthyDatacenterList = Tool.ListUtil.list(true);

        // global agent list
        // ping_from is global || federation is false  global scrape endpoint job
        List<Agent> healthyGlobalList = agentService.list(new LambdaUpdateWrapper<Agent>()
                .eq(Agent::getType, Constant.AgentType.GLOBAL.getValue())
                .eq(Agent::getStatus, "1"));

        log.info("[checkAgentStateOfPing] [total healthy global agent size: {}]", healthyGlobalList.size());

        List<Agent> totalAgentLists = agentService.list();

        //result
        Map<Object, Object> resultMap = Tool.MapUtil.builder()
                .put("item", "diagnose.asset.item.agent.status")
                .put("resolution", Tool.StrUtil.EMPTY)
                .map();


        if (federationEnabled) {
            // federation 模式关闭  ，Global scrape endpoint jobs
            log.info("[agentStateCheckOfMetrics] [federation is not enabled. get global agents]");

            // ping source is per-datacenter
            if (Tool.StrUtil.equals(Constant.AssetPingFrom.PER_DATACENTER.getValue(), assetPingFromConf)) {
                Integer dcId = asset.getDcId();

                // per-datacenter agent list
                healthyDatacenterList = agentService.list(new LambdaUpdateWrapper<Agent>()
                        .eq(Agent::getType, Constant.AgentType.PER_DATACENTER.getValue())
                        .eq(Agent::getDcId, dcId)
                        .eq(Agent::getStatus, "1")
                        .last("limit 1"));

                log.info("[checkAgentStateOfPing] [asset ping from per-datacenter] [healthy global agent size: {}] [healthy per-datacenter agent size: {}]", healthyGlobalList.size(), healthyDatacenterList.size());

                if (Tool.BooleanUtil.and(Tool.CollUtil.isNotEmpty(healthyGlobalList), Tool.CollUtil.isNotEmpty(healthyDatacenterList))) {
                    // PASS
                    resultMap.put("state", DiagnoseState.PASS.getValue());
                } else if (Tool.CollUtil.isNotEmpty(totalAgentLists) && (Tool.CollUtil.isEmpty(healthyGlobalList) || Tool.CollUtil.isEmpty(healthyDatacenterList))) {
                    // has down agent
                    resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
                    resultMap.put("resolution", "diagnose.asset.resolution.agent.status");
                } else if (Tool.CollUtil.isEmpty(totalAgentLists)) {
                    // 没有配置 Agent 信息
                    resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
                    resultMap.put("resolution", "diagnose.asset.resolution.agent.config");
                }
            } else {
                if (Tool.CollUtil.isNotEmpty(healthyGlobalList)) {
                    // PASS
                    resultMap.put("state", DiagnoseState.PASS.getValue());
                } else if (Tool.CollUtil.isNotEmpty(totalAgentLists) && Tool.CollUtil.isEmpty(healthyGlobalList)) {
                    // has down agent
                    resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
                    resultMap.put("resolution", "diagnose.asset.resolution.agent.status");
                }
                if (Tool.CollUtil.isEmpty(totalAgentLists)) {
                    // 没有配置 Agent 信息
                    resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
                    resultMap.put("resolution", "diagnose.asset.resolution.agent.config");
                }
            }
        } else {
            if (Tool.CollUtil.isNotEmpty(healthyGlobalList)) {
                // PASS
                resultMap.put("state", DiagnoseState.PASS.getValue());
            } else if (Tool.CollUtil.isNotEmpty(totalAgentLists) && Tool.CollUtil.isEmpty(healthyGlobalList)) {
                // has down agent
                resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
                resultMap.put("resolution", "diagnose.asset.resolution.agent.status");
            }
            if (Tool.CollUtil.isEmpty(totalAgentLists)) {
                // 没有配置 Agent 信息
                resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
                resultMap.put("resolution", "diagnose.asset.resolution.agent.config");
            }
        }
        return resultMap;
    }

}
