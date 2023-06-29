package com.nis.modules.terminal.backend;

import cn.hutool.log.Log;
import com.nis.common.utils.Tool;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class TerminalMonitorHandler extends TextWebSocketHandler {

    private static final Log log = Log.get();

    @Override
    public synchronized void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        Map<String, Object> attributes = session.getAttributes();
        String token = (String) attributes.get("terminalToken");

        Map<String, SysUserEntity> shiroSessionInfoMap = ShiroUtils.getShiroSessionInfoMap();
        SysUserEntity user = shiroSessionInfoMap.get(token);
        if (user == null) {
            session.sendMessage(new TextMessage("Token error,Please log in to the system first"));
            session.close();
            log.warn("terminal error user token Incorrect token info is : {}", token);
            return;
        }

        String uuid = (String) attributes.get("uuid");
        WebSocketSession webSocketSession = TerminalSession.findSession(uuid);
        TerminalClient terminalClient = TerminalSession.TERMINAL_SESSION.get(webSocketSession);
        if (terminalClient == null) {
            session.sendMessage(new TextMessage("Connection closed \r\n"));
            session.close();
            log.warn("The monitored terminal connection has been closed. sessionId : {}", uuid);
            return;
        }

        List<WebSocketSession> webSocketSessions = TerminalSession.TERMINAL_MONITORS_MAPPING.get(uuid);
        webSocketSessions = Tool.CollUtil.defaultIfEmpty(webSocketSessions, Tool.ListUtil.list(true));
        webSocketSessions.add(session);
        TerminalSession.TERMINAL_MONITORS_MAPPING.put(uuid, webSocketSessions);

        // 打开 monitor 返回的第一条消息
        AssetAsset asset = terminalClient.getAsset();
        String firstMessage = String.format("Connection established, Start monitoring.  %s@%s \r\n", asset.getAuthUsername(), asset.getManageIp());
        session.sendMessage(new TextMessage(firstMessage));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);

        for (Map.Entry<String, List<WebSocketSession>> entry : TerminalSession.TERMINAL_MONITORS_MAPPING.entrySet()) {
            List<WebSocketSession> value = entry.getValue();
            if (CollectionUtils.isEmpty(value)) {
                continue;
            }
            Iterator<WebSocketSession> iterator = value.iterator();
            while (iterator.hasNext()) {
                WebSocketSession next = iterator.next();
                if (session.getId().equals(next.getId())) {
                    iterator.remove();
                }
            }
            entry.setValue(value);
        }
    }
}
