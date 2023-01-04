package com.nis.modules.diagnose.chain.asset;


import cn.hutool.log.Log;
import com.nis.common.utils.Tool;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.diagnose.chain.DiagnoseClient;
import com.nis.modules.diagnose.chain.DiagnoseInterceptor;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Map;

/**
 * AssetNetworkCheckInterceptor
 * 检查 Asset Host 网络情况，是否可达
 *
 * @author shizhendong
 * @date 2023/1/4
 */
@Component
public class AssetNetworkCheckInterceptor implements DiagnoseInterceptor {

    private static final Log log = Log.get();

    @Override
    public void intercept(Chain chain) {
        boolean continueDiagnosis = chain.isContinueDiagnosis();
        log.info("[AssetNetworkCheckInterceptor] [continue diagnosis: {}]", continueDiagnosis);

        if (continueDiagnosis) {
            // process object
            AssetAsset asset = (AssetAsset) chain.object();

            // check Ip IsR eachable
            boolean ipIsReachable = this.checkIpIsReachable(asset.getManageIp());

            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "diagnose.asset.item.reachable")
                    .put("resolution", Tool.StrUtil.EMPTY)
                    .map();

            if (ipIsReachable) {
                // PASS
                resultMap.put("state", DiagnoseState.PASS.getValue());
            } else {
                // DID NOT PASS
                resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
                // set resolution
                resultMap.put("resolution", "diagnose.asset.resolution.reachable");

                // stopDiagnose
                chain.stopDiagnose();
            }

            // update check msg
            DiagnoseClient.addDiagnoseMsg(resultMap);
        } else {
            // 未检查
            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "diagnose.asset.item.reachable")
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
