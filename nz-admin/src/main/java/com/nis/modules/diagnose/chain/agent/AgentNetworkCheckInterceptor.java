package com.nis.modules.diagnose.chain.agent;

import cn.hutool.log.Log;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.diagnose.chain.DiagnoseClient;
import com.nis.modules.diagnose.chain.DiagnoseInterceptor;
import org.apache.commons.lang3.time.StopWatch;

import java.net.InetAddress;
import java.util.Map;

/**
 * AgentNetworkCheckInterceptor
 * 检查 Agent Host 网络情况，是否可达
 *
 * @author shizhendong
 * @date 2023/1/4
 */
public class AgentNetworkCheckInterceptor implements DiagnoseInterceptor {

    private static final Log log = Log.get();

    @Override
    public void intercept(Chain chain) {
        // process object
        Agent agent = (Agent) chain.object();

        // check Ip IsR eachable
        boolean ipIsReachable = this.checkIpIsReachable(agent.getHost());

        Map<Object, Object> resultMap = Tool.MapUtil.builder()
                .put("item", "diagnose.agent.item.reachable")
                .put("resolution", Tool.StrUtil.EMPTY)
                .map();

        if (ipIsReachable) {
            // PASS
            resultMap.put("state", DiagnoseState.PASS.getValue());
        } else {
            // DID NOT PASS
            resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
            // set resolution
            resultMap.put("resolution", "diagnose.agent.resolution.reachable");

            // stopDiagnose
            chain.stopDiagnose();
        }

        // update check msg
        DiagnoseClient.addDiagnoseMsg(resultMap);

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
