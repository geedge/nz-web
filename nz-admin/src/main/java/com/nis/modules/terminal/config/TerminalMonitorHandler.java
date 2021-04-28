package com.nis.modules.terminal.config;


import cn.hutool.log.Log;
import com.nis.common.utils.SpringContextUtils;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class TerminalMonitorHandler extends TextWebSocketHandler {

    private static final Log logger = Log.get();

    @Override
    public synchronized void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        Map<String, Object> attributes = session.getAttributes();
        String token = (String) attributes.get("terminalToken");

        RedisConnectionFactory factory = SpringContextUtils.getBean(RedisConnectionFactory.class);
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.afterPropertiesSet();

        ValueOperations ops = redisTemplate.opsForValue();
        Object obj = ops.get(ShiroUtils.REDIS_KEYPREFIX + token);
        if (obj == null) {
            session.sendMessage(new TextMessage("Token error,Please log in to the system first"));
            session.close();
            logger.error("terminal error user token Incorrect token info is : {}", token);
            return;
        }

        String uuid = (String) attributes.get("uuid");
        WebSocketSession webSocketSession = TerminalSession.findSession(uuid);
        TerminalClient terminalClient = TerminalSession.terminalSession.get(webSocketSession);
        if (terminalClient == null) {
            session.sendMessage(new TextMessage("Connection closed \r\n"));
            session.close();
            logger.error("被监控的 terminal 连接已经关闭 sessionId : {}", uuid);
            return;
        }

        List<WebSocketSession> webSocketSessions = TerminalSession.terminalMonitorsMap.get(uuid);
        webSocketSessions = (CollectionUtils.isEmpty(webSocketSessions)) ? new ArrayList<>() : webSocketSessions;
        webSocketSessions.add(session);
        TerminalSession.terminalMonitorsMap.put(uuid, webSocketSessions);

        // 打开 monitor 返回的第一条消息
        AssetAsset terminal = terminalClient.getTerminal();
        String firstMessage = String.format("Connection established, Start monitoring.  %s@%s \r\n", terminal.getAuthUsername(), terminal.getManageIp());
        session.sendMessage(new TextMessage(firstMessage));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);

        for (Map.Entry<String, List<WebSocketSession>> entry : TerminalSession.terminalMonitorsMap.entrySet()) {
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
