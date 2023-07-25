package com.nis.modules.diagnose.chain.talon;

import cn.hutool.log.Log;
import com.nis.common.utils.Tool;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.diagnose.chain.DiagnoseClient;
import com.nis.modules.diagnose.chain.DiagnoseInterceptor;
import org.apache.commons.lang3.time.StopWatch;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;

/**
 * TalonConnectableCheckInterceptor
 * 检查  talon Host&Port 网络情况，端口是否开放
 *
 * @author shuai
 * @date 2023/7/25
 */
public class TalonConnectableCheckInterceptor implements DiagnoseInterceptor {

    private static final Log log = Log.get();

    @Override
    public void intercept(Chain chain) {
        // process object
        AssetAsset asset = (AssetAsset) chain.object();

        boolean continueDiagnosis = chain.isContinueDiagnosis();
        log.info("[TalonConnectableCheckInterceptor] [continue diagnosis: {}]", continueDiagnosis);

        if (continueDiagnosis) {
            Integer port = asset.getClientPort();
            String host = asset.getManageIp();
            // check Host Is Connectable
            boolean isConnectable = this.checkHostIsConnectable(host, port);

            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "diagnose.talon.item.connectable")
                    .put("resolution", Tool.StrUtil.EMPTY)
                    .map();

            if (isConnectable) {
                // PASS
                resultMap.put("state", DiagnoseState.PASS.getValue());
            } else {
                // DID NOT PASS
                resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
                // set resolution
                resultMap.put("resolution", "diagnose.talon.resolution.connectable");

                // stopDiagnose
                chain.stopDiagnose();
            }

            // update check msg
            DiagnoseClient.addDiagnoseMsg(resultMap);
        } else {
            // 未检查
            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "diagnose.talon.item.connectable")
                    .put("state", DiagnoseState.NOT_CHECKED.getValue())
                    .put("resolution", Tool.StrUtil.EMPTY)
                    .map();

            // update check msg
            DiagnoseClient.addDiagnoseMsg(resultMap);
        }

        // next chain
        chain.proceed(chain.object());
    }

    private boolean checkHostIsConnectable(String host, Integer port) {
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
