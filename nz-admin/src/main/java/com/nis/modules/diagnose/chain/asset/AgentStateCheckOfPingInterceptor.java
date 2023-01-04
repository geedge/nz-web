package com.nis.modules.diagnose.chain.asset;


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
import java.util.stream.Collectors;

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

        List<Agent> totalAgentList = Tool.ListUtil.list(true);
        List<Agent> downAgentList = Tool.ListUtil.list(true);

        // ping source is global
        if (Tool.StrUtil.equals(Constant.AssetPingFrom.GLOBAL.getValue(), assetPingFromConf)) {
            totalAgentList = agentService.list(new LambdaUpdateWrapper<Agent>()
                    .eq(Agent::getType, Constant.AgentType.GLOBAL.getValue()));

            downAgentList = totalAgentList.stream().filter(agent -> Tool.ObjectUtil.notEqual(1, agent.getStatus())).collect(Collectors.toList());
            log.info("[checkAgentStateOfPing] [asset ping from global] [total agent size: {}] [down agent size: {}]", totalAgentList.size(), downAgentList.size());
        }

        // ping source is per-datacenter
        if (Tool.StrUtil.equals(Constant.AssetPingFrom.PER_DATACENTER.getValue(), assetPingFromConf)) {
            Integer dcId = asset.getDcId();
            // per-datacenter agent list
            totalAgentList = agentService.list(new LambdaUpdateWrapper<Agent>()
                    .eq(Agent::getType, Constant.AgentType.PER_DATACENTER.getValue())
                    .eq(Agent::getDcId, dcId));

            List<Agent> downPerDcAgents = totalAgentList.stream().filter(agent -> Tool.ObjectUtil.notEqual(1, agent.getStatus())).collect(Collectors.toList());

            // per-datacenter scrape asset_ping job, query prom_api by global agent, so down agent include global agent
            List<Agent> downGlobalAgents = agentService.list(new LambdaUpdateWrapper<Agent>().eq(Agent::getType, Constant.AgentType.GLOBAL.getValue()).ne(Agent::getStatus, 1));
            log.info("[checkAgentStateOfPing] [asset ping from per-datacenter] [total per-datacenter agent size: {}] [down per-datacenter agent size: {}] [down global agent size: {}]", totalAgentList.size(), downPerDcAgents.size(), downGlobalAgents.size());

            // down agent list include per-datacenter and global agent
            downAgentList.addAll(downPerDcAgents);
            downAgentList.addAll(downGlobalAgents);
        }

        Map<Object, Object> resultMap = Tool.MapUtil.builder()
                .put("item", "diagnose.asset.item.agent.status")
                .put("resolution", Tool.StrUtil.EMPTY)
                .map();

        if (Tool.BooleanUtil.and(Tool.CollUtil.isNotEmpty(totalAgentList),
                Tool.CollUtil.isNotEmpty(downAgentList))) {
            // has down agent
            resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
            resultMap.put("resolution", "diagnose.asset.resolution.agent.status");
        } else if (Tool.BooleanUtil.and(Tool.CollUtil.isNotEmpty(totalAgentList),
                Tool.CollUtil.isEmpty(downAgentList))) {
            // PASS
            resultMap.put("state", DiagnoseState.PASS.getValue());
        } else if (Tool.CollUtil.isEmpty(totalAgentList)) {
            // 没有配置 Agent 信息
            resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
            resultMap.put("resolution", "diagnose.asset.resolution.agent.config");
        }
        return resultMap;
    }

}
