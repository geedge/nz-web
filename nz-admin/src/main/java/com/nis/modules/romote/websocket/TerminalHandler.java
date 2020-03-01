/**
 * Copyright (c) 2015 The JobX Project
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
 *//*

package com.nis.modules.romote.websocket;


import com.nis.modules.romote.entity.Account;
import com.nis.modules.romote.service.impl.AccountServiceImpl;
import com.nis.modules.romote.support.TerminalClient;
import com.nis.modules.romote.support.TerminalContext;
import com.nis.modules.romote.support.TerminalSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

import static com.nis.modules.romote.util.CommonUtils.toInt;
@Component
public class TerminalHandler extends TextWebSocketHandler {
    private TerminalClient terminalClient;

    private static ApplicationContext applicationContext;

    public static void setApplicationContext(ApplicationContext applicationContext) {
        TerminalHandler.applicationContext = applicationContext;
    }

    private TerminalContext terminalContext;

//    @Autowired
//    private TerminalClusterProcessor terminalClusterProcessor;

    private String terminalToken;

    @Override
    public synchronized void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        if(terminalContext==null){
            terminalContext= (TerminalContext) applicationContext.getBean("terminalContext");
        }
//        terminalToken = terminalContext.getToken();
        terminalToken=(String)session.getAttributes().get("token");
        if (terminalToken != null) {
            final Account terminal = terminalContext.get(terminalToken);
            if (terminal != null) {
                try {
                    session.sendMessage(new TextMessage("Welcome to login Terminal! Connect Starting."));
                    getClient(session, terminal);
                    int cols = toInt(session.getAttributes().get("cols").toString());
                    int rows = toInt(session.getAttributes().get("rows").toString());
                    int width = toInt(session.getAttributes().get("width").toString());
                    int height = toInt(session.getAttributes().get("height").toString());
                    terminalClient.openTerminal(cols, rows, width, height);
                } catch (Exception e) {
                    if (e.getLocalizedMessage().replaceAll("\\s+", "").contentEquals("Operationtimedout")) {
                        session.sendMessage(new TextMessage("Sorry! Connect timed out, please try again. "));
                    } else {
                        session.sendMessage(new TextMessage("Sorry! Operation error, please try again. "));
                    }
                    terminalClient.disconnect();
                    session.close();
                }
            } else {
                this.terminalClient.disconnect();
                session.sendMessage(new TextMessage("Sorry! Connect failed, please try again. "));
                session.close();
            }
        }

        System.out.println("afterOpen-->"+terminalContext.terminalContext.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        try {
            getClient(session, null);
            if (this.terminalClient != null) {
                if (!terminalClient.isClosed()) {
                    terminalClient.write(message.getPayload());
                } else {
                    session.close();
                }
            }
        } catch (Exception e) {
            session.sendMessage(new TextMessage("Sorry! jobx Terminal was closed, please try again. "));
            terminalClient.disconnect();
            session.close();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        super.handleTransportError(session, exception);
        this.closeTerminal(session);
        session.close();

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        this.closeTerminal(session);
        System.out.println("afterClose-->"+terminalContext.terminalContext.size());
    }

    private TerminalClient getClient(WebSocketSession session, Account terminal) {
        this.terminalClient = TerminalSession.get(session);
        if (this.terminalClient == null && terminal != null) {
            this.terminalClient = new TerminalClient(session, terminal, terminalToken);
            TerminalSession.put(session, this.terminalClient);
        }
        return this.terminalClient;
    }

    private void closeTerminal(WebSocketSession session) throws IOException {
        terminalClient = TerminalSession.remove(session);
        if (terminalClient != null) {
            Account account=terminalClient.getTerminal();
            if(account!=null){
                String rekey=account.getId() + "_" + terminalClient.getClientId();
                terminalContext.remove(rekey);
            }

            terminalClient.disconnect();
        }
        //解除实例
//        terminalClusterProcessor.unregistry(terminalToken);
    }

}

*/