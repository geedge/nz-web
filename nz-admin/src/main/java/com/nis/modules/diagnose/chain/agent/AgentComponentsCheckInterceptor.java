package com.nis.modules.diagnose.chain.agent;

import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.diagnose.chain.DiagnoseClient;
import com.nis.modules.diagnose.chain.DiagnoseInterceptor;

import java.util.List;
import java.util.Map;

/**
 * AgentComponentsCheckInterceptor
 * 检查 Agent 组件状态
 *
 * @author shizhendong
 * @date 2023/1/4
 */
public class AgentComponentsCheckInterceptor implements DiagnoseInterceptor {

    private static final Log log = Log.get();

    private static final String AGENT_AUTH_SUCCESS = "TRUE";
    private static final String COMPONENT_AVAILABLE_STATUS = "UP";
    private static final String COMPONENT_UNAVAILABLE_STATUS = "DOWN";

    @Override
    public void intercept(Chain chain) {
        boolean continueDiagnosis = chain.isContinueDiagnosis();
        log.info("[AgentComponentsCheckInterceptor] [continue diagnosis: {}]", continueDiagnosis);

        if (continueDiagnosis) {
            // process object
            Agent agent = (Agent) chain.object();

            // check Components Status
            List<Map<Object, Object>> diagnoseMsgList = this.checkComponentsStatus(agent);

            // update check msg
            DiagnoseClient.addDiagnoseMsg(diagnoseMsgList);
        } else {
            // 未检查
            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "diagnose.agent.item.components")
                    .put("state", DiagnoseState.NOT_CHECKED.getValue())
                    .put("resolution", Tool.StrUtil.EMPTY)
                    .map();

            // update check msg
            DiagnoseClient.addDiagnoseMsg(resultMap);
        }
    }

    /**
     * check Components Status
     *
     * @param agent
     * @return
     */
    private List<Map<Object, Object>> checkComponentsStatus(Agent agent) {
        String resultJsonStr = Tool.StrUtil.EMPTY_JSON;
        try {
            resultJsonStr = AgentUtil.requestGet(agent, "/healthy", null, String.class);
        } catch (Exception e) {
            log.error(e, "[checkComponentsStatus] [request agent healthy api error] [agent: {}]", Tool.JSONUtil.toJsonStr(agent));
        }
        log.info("[checkComponentsStatus] [agent healthy api] [result: {}]", resultJsonStr);

        List<Map<Object, Object>> componentsStatusList = Tool.ListUtil.list(true);

        Map resultObj = JSONObject.parseObject(resultJsonStr, Map.class);
        if (Tool.BooleanUtil.or(
                Tool.MapUtil.isEmpty(resultObj),
                Tool.ObjectUtil.notEqual(RCode.SUCCESS.getCode(), resultObj.get("code")))) {
            // nzagent error
            componentsStatusList.add(Tool.MapUtil.builder()
                    .put("item", "diagnose.agent.item.nzagent")
                    .put("state", DiagnoseState.DID_NOT_PASS.getValue())
                    .put("resolution", "diagnose.agent.resolution.nzagent")
                    .map());
        } else {
            Object auth = JSONPath.read(resultJsonStr, "data.auth");
            if (Tool.ObjectUtil.notEqual(AGENT_AUTH_SUCCESS, auth)) {
                componentsStatusList.add(Tool.MapUtil.builder()
                        .put("item", "diagnose.agent.item.auth")
                        .put("state", DiagnoseState.DID_NOT_PASS.getValue())
                        .put("resolution", "diagnose.agent.resolution.auth")
                        .map());
            }

            Object cortex = JSONPath.read(resultJsonStr, "data.cortex");
            if (Tool.ObjectUtil.notEqual(COMPONENT_AVAILABLE_STATUS, cortex)) {
                componentsStatusList.add(Tool.MapUtil.builder()
                        .put("item", "diagnose.agent.item.cortex")
                        .put("state", DiagnoseState.DID_NOT_PASS.getValue())
                        .put("resolution", "diagnose.agent.resolution.cortex")
                        .map());
            }

            Object loki = JSONPath.read(resultJsonStr, "data.loki");
            if (Tool.ObjectUtil.notEqual(COMPONENT_AVAILABLE_STATUS, loki)) {
                componentsStatusList.add(Tool.MapUtil.builder()
                        .put("item", "diagnose.agent.item.loki")
                        .put("state", DiagnoseState.DID_NOT_PASS.getValue())
                        .put("resolution", "diagnose.agent.resolution.loki")
                        .map());
            }

            Object snmpExporter = JSONPath.read(resultJsonStr, "data.snmp_exporter");
            if (Tool.ObjectUtil.notEqual(COMPONENT_AVAILABLE_STATUS, snmpExporter)) {
                componentsStatusList.add(Tool.MapUtil.builder()
                        .put("item", "diagnose.agent.item.snmpExporter")
                        .put("state", DiagnoseState.DID_NOT_PASS.getValue())
                        .put("resolution", "diagnose.agent.resolution.snmpExporter")
                        .map());
            }

            Object blackboxExporter = JSONPath.read(resultJsonStr, "data.blackbox_exporter");
            if (Tool.ObjectUtil.notEqual(COMPONENT_AVAILABLE_STATUS, blackboxExporter)) {
                componentsStatusList.add(Tool.MapUtil.builder()
                        .put("item", "diagnose.agent.item.blackboxExporter")
                        .put("state", DiagnoseState.DID_NOT_PASS.getValue())
                        .put("resolution", "diagnose.agent.resolution.blackboxExporter")
                        .map());
            }

            Object prometheus = JSONPath.read(resultJsonStr, "data.prometheus");
            if (Tool.ObjectUtil.notEqual(COMPONENT_AVAILABLE_STATUS, prometheus)) {
                componentsStatusList.add(Tool.MapUtil.builder()
                        .put("item", "diagnose.agent.item.prometheus")
                        .put("state", DiagnoseState.DID_NOT_PASS.getValue())
                        .put("resolution", "diagnose.agent.resolution.prometheus")
                        .map());
            }

            Object nzagent = JSONPath.read(resultJsonStr, "data.confagent");
            if (Tool.ObjectUtil.notEqual(COMPONENT_AVAILABLE_STATUS, nzagent)) {
                componentsStatusList.add(Tool.MapUtil.builder()
                        .put("item", "diagnose.agent.item.nzagent")
                        .put("state", DiagnoseState.DID_NOT_PASS.getValue())
                        .put("resolution", "diagnose.agent.resolution.nzagent")
                        .map());
            }
        }

        // 所有检查通过 PASS
        if (Tool.CollUtil.isEmpty(componentsStatusList)) {
            componentsStatusList.add(Tool.MapUtil.builder()
                    .put("item", "diagnose.agent.item.components")
                    .put("state", DiagnoseState.PASS.getValue())
                    .put("resolution", Tool.StrUtil.EMPTY)
                    .map());
        }
        return componentsStatusList;
    }
}
