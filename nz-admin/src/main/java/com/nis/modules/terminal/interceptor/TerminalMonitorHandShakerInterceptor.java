package com.nis.modules.terminal.interceptor;

import com.nis.modules.terminal.utils.TerminalConstant;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * HttpSessionHandshakeInterceptor
 */
@Component
public class TerminalMonitorHandShakerInterceptor extends HttpSessionHandshakeInterceptor {

    @Override
    public synchronized boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpSession session = servletRequest.getServletRequest().getSession();
            attributes.put("uuid", servletRequest.getServletRequest().getParameter("uuid"));
            attributes.put("terminalToken", servletRequest.getServletRequest().getParameter("token"));
            attributes.put(TerminalConstant.PARAM_HTTP_SESSION_ID_KEY, session.getId());
        }
        return super.beforeHandshake(request, response, wsHandler, attributes);
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        super.afterHandshake(request, response, wsHandler, exception);
    }

}
