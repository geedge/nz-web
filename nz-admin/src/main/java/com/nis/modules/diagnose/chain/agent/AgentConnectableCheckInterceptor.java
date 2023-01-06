package com.nis.modules.diagnose.chain.agent;

import cn.hutool.log.Log;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.diagnose.chain.DiagnoseClient;
import com.nis.modules.diagnose.chain.DiagnoseInterceptor;
import org.apache.commons.lang3.time.StopWatch;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;

/**
 * AgentConnectableCheckInterceptor
 * 检查 Agent Host&Port 网络情况，端口是否开放
 *
 * @author shizhendong
 * @date 2023/1/4
 */
public class AgentConnectableCheckInterceptor implements DiagnoseInterceptor {

    private static final Log log = Log.get();

    @Override
    public void intercept(Chain chain) {
        boolean continueDiagnosis = chain.isContinueDiagnosis();
        log.info("[AgentConnectableCheckInterceptor] [continue diagnosis: {}]", continueDiagnosis);

        if (continueDiagnosis) {
            // process object
            Agent agent = (Agent) chain.object();

            // check Host Is Connectable
            boolean isConnectable = this.checkHostIsConnectable(agent.getHost(), agent.getPort());

            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "diagnose.agent.item.connectable")
                    .put("resolution", Tool.StrUtil.EMPTY)
                    .map();

            if (isConnectable) {
                // PASS
                resultMap.put("state", DiagnoseState.PASS.getValue());
            } else {
                // DID NOT PASS
                resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
                // set resolution
                resultMap.put("resolution", "diagnose.agent.resolution.connectable");

                // stopDiagnose
                chain.stopDiagnose();
            }

            // update check msg
            DiagnoseClient.addDiagnoseMsg(resultMap);
        } else {
            // 未检查
            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "diagnose.agent.item.connectable")
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
     * check Host Is Connectable
     *
     * @param host
     * @param port
     * @return
     */
    public boolean checkHostIsConnectable(String host, int port) {
        log.info("[checkHostIsConnectable] [begin] [host: {}] [port: {}]", host, port);

        StopWatch sw = new StopWatch();
        sw.start();

        boolean isConnectable = false;

        Socket socket = new Socket();
        try {
            InetSocketAddress socketAddress = new InetSocketAddress(host, port);
            socket.connect(socketAddress, 5000);
            isConnectable = socket.isConnected();
        } catch (Exception e) {
            log.error(e, "[checkHostIsConnectable] [error in check process] [host: {}] [port: {}]", host, port);
        } finally {
            Tool.IoUtil.close(socket);
            sw.stop();
        }

        log.info("[checkHostIsConnectable] [check completed] [host: {}] [port: {}] [connected: {}] [Run Time: {}]", host, port, isConnectable, sw.toString());
        return isConnectable;
    }

}
