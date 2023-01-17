package com.nis.modules.diagnose.chain.endpoint.metrics;

import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONPath;
import com.nis.common.utils.Tool;
import com.nis.modules.diagnose.chain.DiagnoseClient;
import com.nis.modules.diagnose.chain.DiagnoseInterceptor;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.entity.MonitorEndpointConfig;
import org.apache.commons.lang3.time.StopWatch;

import java.net.*;
import java.util.Map;

/**
 * EndpointConnectableCheckInterceptor
 * 检查 Endpoint Host&Port 网络情况，端口是否开放
 *
 * @author shizhendong
 * @date 2023/1/4
 */
public class EndpointConnectableCheckInterceptor implements DiagnoseInterceptor {

    private static final Log log = Log.get();

    @Override
    public void intercept(Chain chain) {
        boolean continueDiagnosis = chain.isContinueDiagnosis();
        log.info("[EndpointConnectableCheckInterceptor] [continue diagnosis: {}]", continueDiagnosis);

        if (continueDiagnosis) {
            // process object
            MonitorEndpoint endpoint = (MonitorEndpoint) chain.object();
            MonitorEndpointConfig endpointConfig = endpoint.getEndpointConfig();

            String host = JSONPath.read(endpointConfig.getConfig(), "host").toString();
            String port = JSONPath.read(endpointConfig.getConfig(), "port").toString();

            boolean isConnectable = false;
            String protocol = JSONPath.read(endpointConfig.getConfig(), "protocol").toString();

            // check Host Is Connectable
            if (Tool.StrUtil.equalsAny(protocol, "http", "https")) {
                // TCP
                isConnectable = this.checkHttpHostIsConnectable(host, port);
            } else if (Tool.StrUtil.equalsAny(protocol, "snmp")) {
                // UDP
                isConnectable = this.checkSnmpHostIsConnectable(host, port);
            }


            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "metric.diagnose.endpoint.item.connectable")
                    .put("resolution", Tool.StrUtil.EMPTY)
                    .map();

            if (isConnectable) {
                // PASS
                resultMap.put("state", DiagnoseState.PASS.getValue());
            } else {
                // DID NOT PASS
                resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
                // set resolution
                resultMap.put("resolution", "diagnose.endpoint.resolution.connectable");

                // stopDiagnose
                chain.stopDiagnose();
            }

            // update check msg
            DiagnoseClient.addDiagnoseMsg(resultMap);
        } else {
            // 未检查
            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "metric.diagnose.endpoint.item.connectable")
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
     * check Snmp Host Is Connectable
     *
     * @param host
     * @param port
     * @return
     */
    private boolean checkSnmpHostIsConnectable(String host, String port) {
        log.info("[checkSnmpHostIsConnectable] [begin] [host: {}] [port: {}]", host, port);

        StopWatch sw = new StopWatch();
        sw.start();

        boolean isConnectable = true;

        DatagramSocket d = null;
        try {
            d = new DatagramSocket();
            String msg = "\n";
            byte[] buffer = new byte[msg.length()];
            buffer = msg.getBytes();

            InetAddress target = InetAddress.getByName(host);
            d.connect(target, Integer.valueOf(port));
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, target, Integer.valueOf(port));

            d.send(packet);
            d.setSoTimeout(5000);
            d.receive(packet);
        } catch (PortUnreachableException e) {
            log.error(e, "[checkSnmpHostIsConnectable] [ICMP Port Unreachable]");
            // Unreachable
            isConnectable = false;
        } catch (Exception e) {
            // 只有明确的 PortUnreachableException 异常为端口不可用，捕获的其他异常均视为端口可用，只输出日志
            log.error(e, "[checkSnmpHostIsConnectable] [error]");
        } finally {
            Tool.IoUtil.close(d);
            sw.stop();
        }

        log.info("[checkSnmpHostIsConnectable] [check completed] [host: {}] [port: {}] [connected: {}] [Run Time: {}]", host, port, isConnectable, sw.toString());
        return isConnectable;
    }


    /**
     * check http Host Is Connectable
     *
     * @param host
     * @param port
     * @return
     */
    private boolean checkHttpHostIsConnectable(String host, String port) {
        log.info("[checkHttpHostIsConnectable] [begin] [host: {}] [port: {}]", host, port);

        StopWatch sw = new StopWatch();
        sw.start();

        boolean isConnectable = false;

        Socket socket = new Socket();
        try {
            InetSocketAddress socketAddress = new InetSocketAddress(host, Integer.valueOf(port));
            socket.connect(socketAddress, 5000);
            isConnectable = socket.isConnected();
        } catch (Exception e) {
            log.error(e, "[checkHttpHostIsConnectable] [error in check process] [host: {}] [port: {}]", host, port);
        } finally {
            Tool.IoUtil.close(socket);
            sw.stop();
        }

        log.info("[checkHttpHostIsConnectable] [check completed] [host: {}] [port: {}] [connected: {}] [Run Time: {}]", host, port, isConnectable, sw.toString());
        return isConnectable;
    }
}
