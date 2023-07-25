package com.nis.modules.diagnose.service.impl;

import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONPath;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant.MonitorConfType;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.AgentService;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.diagnose.chain.DiagnoseClient;
import com.nis.modules.diagnose.chain.agent.AgentComponentsCheckInterceptor;
import com.nis.modules.diagnose.chain.agent.AgentConnectableCheckInterceptor;
import com.nis.modules.diagnose.chain.agent.AgentNetworkCheckInterceptor;
import com.nis.modules.diagnose.chain.asset.AgentStateCheckOfPingInterceptor;
import com.nis.modules.diagnose.chain.asset.AssetNetworkCheckInterceptor;
import com.nis.modules.diagnose.chain.asset.AssetStateConfCheckInterceptor;
import com.nis.modules.diagnose.chain.asset.BlackboxConfigCheckInterceptor;
import com.nis.modules.diagnose.chain.endpoint.logs.AgentStateCheckOfLogsEndpointInterceptor;
import com.nis.modules.diagnose.chain.endpoint.logs.EndpointLogClientStatusCheckInterceptor;
import com.nis.modules.diagnose.chain.endpoint.logs.TalonNetworkCheckInterceptor;
import com.nis.modules.diagnose.chain.endpoint.logs.TalonStatusCheckInterceptor;
import com.nis.modules.diagnose.chain.endpoint.metrics.*;
import com.nis.modules.diagnose.chain.talon.TalonClientEnableCheckInterceptor;
import com.nis.modules.diagnose.chain.talon.TalonComponentsCheckInterceptor;
import com.nis.modules.diagnose.chain.talon.TalonConnectableCheckInterceptor;
import com.nis.modules.diagnose.service.DiagnoseService;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.entity.MonitorEndpointConfig;
import com.nis.modules.endpoint.service.MonitorEndpointConfigService;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.sys.service.SysI18nService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * DiagnoseServiceImpl 故障排查
 *
 * @author shizhendong
 * @date 2023/1/4
 */
@Service
public class DiagnoseServiceImpl implements DiagnoseService {

    private static final Log log = Log.get();

    @Autowired
    private AgentService agentService;

    @Autowired
    private AssetAssetService assetService;

    @Autowired
    private SysI18nService sysI18nService;

    @Autowired
    private MonitorEndpointService monitorEndpointService;

    @Autowired
    private MonitorEndpointConfigService monitorEndpointConfigService;

    @Override
    public List<Map<Object, Object>> assetErrorDiagnosis(Integer assetId, String language) {
        try {
            AssetAsset asset = assetService.getById(assetId);
            ValidateUtils.is(asset).notNull(RCode.ASSET_NOT_EXIST);

            // diagnose client. set diagnose chain
            DiagnoseClient diagnoseClient = new DiagnoseClient.Builder()
                    .addInterceptor(new AssetStateConfCheckInterceptor())
                    .addInterceptor(new AssetNetworkCheckInterceptor())
                    .addInterceptor(new AgentStateCheckOfPingInterceptor())
                    .addInterceptor(new BlackboxConfigCheckInterceptor())
                    .addInterceptor(new TalonClientEnableCheckInterceptor())
                    .addInterceptor(new TalonConnectableCheckInterceptor())
                    .addInterceptor(new TalonComponentsCheckInterceptor())
                    .build();

            // diagnose
            diagnoseClient.diagnose(asset);

            // Diagnose Msg List
            List<Map<Object, Object>> diagnoseMsgList = this.queryDiagnoseMsgByLanguage(language);
            return diagnoseMsgList;
        } finally {
            DiagnoseClient.cleanDiagnoseMsg();
        }
    }

    @Override
    public List<Map<Object, Object>> agentErrorDiagnosis(Integer agentId, String language) {
        try {
            Agent agent = agentService.getById(agentId);
            ValidateUtils.is(agent).notNull(RCode.AGENT_NOTEXSITS);

            // diagnose client. set diagnose chain
            DiagnoseClient diagnoseClient = new DiagnoseClient.Builder()
                    .addInterceptor(new AgentNetworkCheckInterceptor())
                    .addInterceptor(new AgentConnectableCheckInterceptor())
                    .addInterceptor(new AgentComponentsCheckInterceptor())
                    .build();

            // diagnose
            diagnoseClient.diagnose(agent);

            // Diagnose Msg List
            List<Map<Object, Object>> diagnoseMsgList = this.queryDiagnoseMsgByLanguage(language);
            return diagnoseMsgList;
        } finally {
            DiagnoseClient.cleanDiagnoseMsg();
        }
    }

    @Override
    public List<Map<Object, Object>> endpointErrorDiagnosis(Integer endpointId, String language) {
        try {
            MonitorEndpoint endpoint = monitorEndpointService.getById(endpointId);
            ValidateUtils.is(endpoint).notNull(RCode.ENDPOINT_NOTFOUND);

            List<MonitorEndpointConfig> configList = monitorEndpointConfigService.list(new LambdaQueryWrapper<MonitorEndpointConfig>().eq(MonitorEndpointConfig::getEndpointId, endpoint.getId()));

            // metrics
            MonitorEndpointConfig metricsEndpointConfig = configList.stream().filter(endpointConfig -> Tool.StrUtil.equals(endpointConfig.getType(), MonitorConfType.METRICS.getValue())).findFirst().get();
            endpoint.setEndpointConfig(metricsEndpointConfig);

            String protocol = JSONPath.read(metricsEndpointConfig.getConfig(), "protocol").toString();
            // diagnose
            if (Tool.StrUtil.equalsAny(protocol, "http", "https")) {
                DiagnoseClient metricsEndpointDiagnoseClient = new DiagnoseClient.Builder()
                        .addInterceptor(new AgentStateCheckOfMetricsEndpointInterceptor())
                        .addInterceptor(new EndpointMonitorStatusCheckInterceptor())
                        .addInterceptor(new EndpointScrapeJobConfExistCheckInterceptor())
                        .addInterceptor(new EndpointNetworkCheckInterceptor())
                        .addInterceptor(new EndpointConnectableCheckInterceptor())
                        .addInterceptor(new EndpointTimeOutOfScrapeCheckInterceptor())
                        .addInterceptor(new EndpointScrapeErrorMsgCheckInterceptor())
                        .build();
                metricsEndpointDiagnoseClient.diagnose(endpoint);
            } else if (Tool.StrUtil.equalsAny(protocol, "snmp")) {
                DiagnoseClient metricsEndpointDiagnoseClient = new DiagnoseClient.Builder()
                        .addInterceptor(new AgentStateCheckOfMetricsEndpointInterceptor())
                        .addInterceptor(new EndpointMonitorStatusCheckInterceptor())
                        .addInterceptor(new EndpointScrapeJobConfExistCheckInterceptor())
                        .addInterceptor(new EndpointNetworkCheckInterceptor())
                        .addInterceptor(new EndpointConnectableCheckInterceptor())
                        // snmp agent
                        .addInterceptor(new EndpointSNMPAgentCheckInterceptor())
                        .addInterceptor(new EndpointTimeOutOfScrapeCheckInterceptor())
                        .addInterceptor(new EndpointScrapeErrorMsgCheckInterceptor())
                        .build();
                metricsEndpointDiagnoseClient.diagnose(endpoint);
            }

            // logs
            MonitorEndpointConfig logsEndpointConfig = configList.stream().filter(endpointConfig -> Tool.StrUtil.equals(endpointConfig.getType(), MonitorConfType.LOGS.getValue())).findFirst().get();
            endpoint.setEndpointConfig(logsEndpointConfig);

            DiagnoseClient logsEndpointDiagnoseClient = new DiagnoseClient.Builder()
                    .addInterceptor(new AgentStateCheckOfLogsEndpointInterceptor())
                    .addInterceptor(new EndpointLogClientStatusCheckInterceptor())
                    .addInterceptor(new TalonNetworkCheckInterceptor())
                    .addInterceptor(new TalonStatusCheckInterceptor())
                    .build();
            logsEndpointDiagnoseClient.diagnose(endpoint);

            // Diagnose Msg List
            List<Map<Object, Object>> diagnoseMsgList = this.queryDiagnoseMsgByLanguage(language);
            return diagnoseMsgList;
        } finally {
            DiagnoseClient.cleanDiagnoseMsg();
        }
    }

    /**
     * query Diagnose Msg By Language
     *
     * @param language
     * @return
     */
    private List<Map<Object, Object>> queryDiagnoseMsgByLanguage(String language) {
        List<Map<Object, Object>> diagnoseMsgList = DiagnoseClient.queryDiagnoseMsg();

        for (Map<Object, Object> map : diagnoseMsgList) {
            String itemCode = map.get("item").toString();
            String itemValue = sysI18nService.queryValue(itemCode, language);
            map.put("item", itemValue);

            String resolutionCode = map.get("resolution").toString();
            String resolutionValue = sysI18nService.queryValue(resolutionCode, language);
            map.put("resolution", resolutionValue);
        }

        return diagnoseMsgList;
    }
}
