package com.nis.modules.diagnose.chain.endpoint.metrics;


import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONPath;
import com.nis.common.utils.SnmpUtil;
import com.nis.common.utils.Tool;
import com.nis.modules.diagnose.chain.DiagnoseClient;
import com.nis.modules.diagnose.chain.DiagnoseInterceptor;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.entity.MonitorEndpointConfig;
import com.nis.modules.sys.entity.SnmpCredential;
import com.nis.modules.sys.service.SnmpCredentialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * EndpointSNMPAgentCheckInterceptor
 * 检查 Endpoint SNMP Agent 服务是否可用
 *
 * @author shizhendong
 * @date 2023/1/4
 */
@Component
public class EndpointSNMPAgentCheckInterceptor implements DiagnoseInterceptor {

    private static final Log log = Log.get();

    private static SnmpCredentialService snmpCredentialService;

    @Autowired
    public void initBean(SnmpCredentialService snmpCredentialService) {
        EndpointSNMPAgentCheckInterceptor.snmpCredentialService = snmpCredentialService;
    }

    @Override
    public void intercept(Chain chain) {
        boolean continueDiagnosis = chain.isContinueDiagnosis();
        log.info("[EndpointSNMPAgentCheckInterceptor] [continue diagnosis: {}]", continueDiagnosis);

        if (continueDiagnosis) {
            // process object
            MonitorEndpoint endpoint = (MonitorEndpoint) chain.object();
            MonitorEndpointConfig endpointConfig = endpoint.getEndpointConfig();

            String host = JSONPath.read(endpointConfig.getConfig(), "host").toString();
            String port = JSONPath.read(endpointConfig.getConfig(), "port").toString();
            String snmpCredentialsId = JSONPath.read(endpointConfig.getConfig(), "snmpCredentialsId").toString();

            boolean healthyStatus = this.checkSnmpAgentStatus(host, port, snmpCredentialsId);
            log.info("[EndpointSNMPAgentCheckInterceptor] [snmp agent status] [healthy: {}]", healthyStatus);

            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "metric.diagnose.endpoint.item.snmp.agent")
                    .put("resolution", Tool.StrUtil.EMPTY)
                    .map();

            if (healthyStatus) {
                // PASS
                resultMap.put("state", DiagnoseState.PASS.getValue());
            } else {
                // DID_NOT_PASS
                resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
                resultMap.put("resolution", "diagnose.endpoint.resolution.snmp.agent");

                // stopDiagnose
                chain.stopDiagnose();
            }

            // update check msg
            DiagnoseClient.addDiagnoseMsg(resultMap);
        } else {
            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "metric.diagnose.endpoint.item.snmp.agent")
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
     * check Snmp Agent Status
     * 通过执行 snmpwalk 判断 snmp agent 状态是否正常
     *
     * @param host
     * @param port
     * @param snmpCredentialsId
     * @return
     */
    private boolean checkSnmpAgentStatus(String host, String port, String snmpCredentialsId) {
        log.info("[checkSnmpAgentStatus] [host: {}] [port: {}] [credentialsId: {}]", host, port, snmpCredentialsId);

        SnmpCredential snmpCredential = snmpCredentialService.getById(snmpCredentialsId);
        List<Map> snmpResults = Tool.ListUtil.empty();
        SnmpUtil snmpUtil1 = new SnmpUtil(host, Integer.valueOf(port), "", snmpCredential);
        try {
            snmpResults = snmpUtil1.snmpWalk();
        } catch (Exception e) {
            log.error(e, "[checkSnmpAgentStatus] [snmp walk error.]");
        } finally {
            snmpUtil1.close();
        }

        log.info("[checkSnmpAgentStatus] [snmp walk reslut] [size: {}]", snmpResults.size());
        return Tool.CollUtil.isNotEmpty(snmpResults);
    }
}
