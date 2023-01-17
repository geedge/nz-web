package com.nis.modules.diagnose.chain.endpoint.logs;


import cn.hutool.log.Log;
import com.nis.common.utils.Tool;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.diagnose.chain.DiagnoseClient;
import com.nis.modules.diagnose.chain.DiagnoseInterceptor;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Map;

/**
 * TalonNetworkCheckInterceptor
 * 检查 NZ-Talon 组件 host，是否可达
 *
 * @author shizhendong
 * @date 2023/1/4
 */
@Component
public class TalonNetworkCheckInterceptor implements DiagnoseInterceptor {

    private static final Log log = Log.get();

    private static AssetAssetService assetService;

    @Autowired
    public void initBean(AssetAssetService assetService) {
        TalonNetworkCheckInterceptor.assetService = assetService;
    }

    @Override
    public void intercept(Chain chain) {
        boolean continueDiagnosis = chain.isContinueDiagnosis();
        log.info("[TalonNetworkCheckInterceptor] [continue diagnosis: {}]", continueDiagnosis);

        if (continueDiagnosis) {
            // process object
            MonitorEndpoint endpoint = (MonitorEndpoint) chain.object();

            Integer assetId = endpoint.getAssetId();
            AssetAsset asset = assetService.queryAssetInfo(assetId);

            // check Ip IsR eachable
            boolean ipIsReachable = this.checkIpIsReachable(asset.getManageIp());

            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "logs.diagnose.endpoint.item.reachable")
                    .put("resolution", Tool.StrUtil.EMPTY)
                    .map();

            if (ipIsReachable) {
                // PASS
                resultMap.put("state", DiagnoseState.PASS.getValue());
            } else {
                // DID NOT PASS
                resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
                // set resolution
                resultMap.put("resolution", "diagnose.endpoint.resolution.reachable");

                // stopDiagnose
                chain.stopDiagnose();
            }

            // update check msg
            DiagnoseClient.addDiagnoseMsg(resultMap);
        } else {
            // 未检查
            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "logs.diagnose.endpoint.item.reachable")
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
     * check Ip IsR eachable
     *
     * @param host
     * @return a {@code boolean} indicating if the address is reachable.
     */
    private boolean checkIpIsReachable(String host) {
        log.info("[checkIpIsReachable] [begin] [host: {}]", host);

        StopWatch sw = new StopWatch();
        sw.start();

        boolean isReachable = false;
        try {
            InetAddress inetAddress = InetAddress.getByName(host);
            // timeout the time, in milliseconds, before the call aborts, default 5s
            isReachable = inetAddress.isReachable(5000);
        } catch (Exception e) {
            log.error(e, "[checkIpIsReachable] [error in check process] [host: {}]", host);
        } finally {
            sw.stop();
        }

        log.info("[checkIpIsReachable] [check completed] [host: {}] [reachable: {}] [Run Time: {}]", host, isReachable, sw.toString());
        return isReachable;
    }

}
