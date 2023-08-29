package com.nis.modules.terminal.backend;

import cn.hutool.log.Log;
import com.nis.common.utils.Constant;
import com.nis.common.utils.DigestUtils;
import com.nis.common.utils.Tool;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;

public class TerminalInteractionRunnable extends Thread {

    private static final Log log = Log.get();

    private TerminalClient client;

    public TerminalInteractionRunnable(TerminalClient client) {
        this.client = client;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024 * 4];
        String clientId = client.getClientId();
        WebSocketSession webSocketSession = client.getWebSocketSession();
        try {
            while (webSocketSession != null && webSocketSession.isOpen()) {
                int size = client.getInputStream().read(buffer);
                // logout
                if (size == Tool.StrUtil.INDEX_NOT_FOUND) {
                    log.warn("[TerminalInteractionRunnable] [no more data because the end of the stream has been reached] [clientid: {}]", clientId);
                    try {
                        // Try sending an exit prompt statement
                        if (webSocketSession.isOpen()) {
                            webSocketSession.sendMessage(new TextMessage("Connection closed \r\n"));
                        }
                    } catch (Exception e) {
                        log.error(e, "[TerminalInteractionRunnable] [Try sending an exit prompt statement error] [clientid: {}]", clientId);
                    }

                    // close terminal
                    TerminalHandler.closeTerminalRescourse(webSocketSession);

                    // update Session Status After Client Closed
                    TerminalHandler.updateSessionStatusAfterClientClosed(clientId);

                    // save Record Content After Client Closed
                    TerminalHandler.saveRecordContentAfterClientClosed(clientId);

                    // remove Terminal Attribute Info
                    TerminalSession.removeTerminalAttributeInfo(clientId);
                    return;
                }
                // update last active time
                // connected last active time
                TerminalSession.TERMINAL_CONNECTED_LAST_ACTIVE_MAP.put(clientId, Tool.DateUtil.currentSeconds());

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < size; i++) {
                    char chr = (char) (buffer[i] & 0xff);
                    sb.append(chr);
                }
                // 取到 linux 远程机器输出的信息发送给前端
                String message = sb.toString();
                message = Tool.StrUtil.str(message.getBytes(DigestUtils.getEncoding(message)), "UTF-8");

                // send Message To Monitor Terminal
                client.sendMessageToMonitorTerminal(clientId, message);

                // save Or Append Terminal Record Message
                File tempFile = Tool.FileUtil.file(Tool.StrUtil.concat(true, Constant.TEMP_PATH, File.separator, clientId, ".log"));
                client.saveOrAppendTerminalRecordMessage(tempFile, message);

                // 处理服务器方向键以及 tab 操作返回的指令信息 便于做日志记录
                client.processCommand(message);

                // send message to terminal
                webSocketSession.sendMessage(new TextMessage(message));
            }
        } catch (Exception e) {
            log.error(e, "[TerminalInteractionRunnable] [Terminal error.]");
            TerminalSession.remove(webSocketSession);
        }
    }
}
