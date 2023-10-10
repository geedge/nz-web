/**
 * Copyright (c) 2015 The Nezha Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.nis.modules.terminal.interceptor;

import com.nis.modules.terminal.utils.TerminalConstant;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;

/**
 * HttpSessionHandshakeInterceptor
 */
@Component
public class TerminalHandShakerInterceptor extends HttpSessionHandshakeInterceptor {

    @Override
    public synchronized boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            attributes.put("cols", servletRequest.getServletRequest().getParameter("cols"));
            attributes.put("rows", servletRequest.getServletRequest().getParameter("rows"));
            attributes.put("width", servletRequest.getServletRequest().getParameter("width"));
            attributes.put("height", servletRequest.getServletRequest().getParameter("height"));

            attributes.put("uuid", servletRequest.getServletRequest().getParameter("uuid"));
            attributes.put("terminalToken", servletRequest.getServletRequest().getParameter("token"));

            HttpSession session = servletRequest.getServletRequest().getSession();
            attributes.put(TerminalConstant.PARAM_HTTP_SESSION_ID_KEY, session.getId());
        }
        return super.beforeHandshake(request, response, wsHandler, attributes);
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        super.afterHandshake(request, response, wsHandler, exception);
    }

}
