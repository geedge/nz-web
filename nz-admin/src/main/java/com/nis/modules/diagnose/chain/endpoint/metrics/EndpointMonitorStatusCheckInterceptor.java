package com.nis.modules.diagnose.chain.endpoint.metrics;


import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.nis.common.utils.Tool;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetStateConf;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.diagnose.chain.DiagnoseClient;
import com.nis.modules.diagnose.chain.DiagnoseInterceptor;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.entity.MonitorEndpointConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * EndpointMonitorStatusCheckInterceptor
 * 检查 Endpoint Monitor 状态是否正常，检查项包括，DC、Asset、Endpoint config
 *
 * @author shizhendong
 * @date 2023/1/4
 */
@Component
public class EndpointMonitorStatusCheckInterceptor implements DiagnoseInterceptor {

    private static final Log log = Log.get();

    private static AssetAssetService assetService;

    @Autowired
    public void initBean(AssetAssetService assetService) {
        EndpointMonitorStatusCheckInterceptor.assetService = assetService;
    }

    @Override
    public void intercept(Chain chain) {
        boolean continueDiagnosis = chain.isContinueDiagnosis();
        log.info("[EndpointMonitorStatusCheckInterceptor] [continue diagnosis: {}]", continueDiagnosis);

        if (continueDiagnosis) {
            // process object
            MonitorEndpoint endpoint = (MonitorEndpoint) chain.object();

            Integer assetId = endpoint.getAssetId();
            AssetAsset asset = assetService.queryAssetInfo(assetId);

            // check dc state
            Map<Object, Object> checkDcStatusResultMap = this.checkDcStatus(asset.getDc());

            // check asset state conf
            Map<Object, Object> checkAssetMonitorStateResultMap = this.checkAssetMonitorState(asset);

            // check endpoint config enable
            Map<Object, Object> checkEndpointConfigEnableResultMap = this.checkEndpointConfigEnable(endpoint);

            if (StrUtil.equalsAny(DiagnoseState.DID_NOT_PASS.getValue().toString(),
                    checkDcStatusResultMap.get("state").toString(),
                    checkAssetMonitorStateResultMap.get("state").toString(),
                    checkEndpointConfigEnableResultMap.get("state").toString())) {
                // stopDiagnose
                chain.stopDiagnose();
            }

            // update check msg
            DiagnoseClient.addDiagnoseMsg(checkDcStatusResultMap);
            DiagnoseClient.addDiagnoseMsg(checkAssetMonitorStateResultMap);
            DiagnoseClient.addDiagnoseMsg(checkEndpointConfigEnableResultMap);
        } else {
            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "metric.diagnose.endpoint.item.monitor.status")
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
     * check Dc Status
     *
     * @param dc
     * @return
     */
    private Map<Object, Object> checkDcStatus(Dc dc) {
        String dcState = dc.getState();
        log.info("[checkDcStatus] [dc state: {}]", dcState);

        Map<Object, Object> resultMap = Tool.MapUtil.builder()
                .put("item", "metric.diagnose.endpoint.item.monitor.dc")
                .put("resolution", Tool.StrUtil.EMPTY)
                .map();

        if (StrUtil.equals("OFF", dcState)) {
            // DID_NOT_PASS
            resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
            resultMap.put("resolution", "diagnose.endpoint.resolution.monitor.dc");
        } else {
            // PASS
            resultMap.put("state", DiagnoseState.PASS.getValue());
        }
        return resultMap;
    }

    /**
     * check Asset Monitor State
     *
     * @param asset
     * @return
     */
    private Map<Object, Object> checkAssetMonitorState(AssetAsset asset) {
        AssetStateConf stateConf = asset.getState();
        Integer monitor = stateConf.getMonitor();
        log.info("[checkAssetMonitorState] [asset monitor state: {}]", monitor);

        Map<Object, Object> resultMap = Tool.MapUtil.builder()
                .put("item", "metric.diagnose.endpoint.item.monitor.asset")
                .put("resolution", Tool.StrUtil.EMPTY)
                .map();

        if (Tool.ObjectUtil.equal(0, monitor)) {
            // DID_NOT_PASS
            resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
            resultMap.put("resolution", "diagnose.endpoint.resolution.monitor.asset");
        } else {
            // PASS
            resultMap.put("state", DiagnoseState.PASS.getValue());
        }
        return resultMap;
    }

    /**
     * check Endpoint Config Enable
     *
     * @param endpoint
     * @return
     */
    private Map<Object, Object> checkEndpointConfigEnable(MonitorEndpoint endpoint) {
        MonitorEndpointConfig endpointConfig = endpoint.getEndpointConfig();
        Integer enable = endpointConfig.getEnable();
        log.info("[checkEndpointConfigEnable] [endpoint config enable: {}]", enable);

        Map<Object, Object> resultMap = Tool.MapUtil.builder()
                .put("item", "metric.diagnose.endpoint.item.monitor.config")
                .put("resolution", Tool.StrUtil.EMPTY)
                .map();

        if (Tool.ObjectUtil.equal(0, enable)) {
            // DID_NOT_PASS
            resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
            resultMap.put("resolution", "diagnose.endpoint.resolution.monitor.config");
        } else {
            // PASS
            resultMap.put("state", DiagnoseState.PASS.getValue());
        }
        return resultMap;
    }
}
