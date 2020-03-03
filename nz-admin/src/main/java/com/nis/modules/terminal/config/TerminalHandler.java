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
 */

package com.nis.modules.terminal.config;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.terminal.entity.Account;
import com.nis.modules.terminal.service.AccountService;

import java.io.IOException;

import static com.nis.common.utils.CommonUtils.toInt;
@Component
public class TerminalHandler extends TextWebSocketHandler {
	
	private final Logger logger = LoggerFactory.getLogger(TerminalHandler.class);
	
    private TerminalClient terminalClient;

    private static ApplicationContext applicationContext;

    public static void setApplicationContext(ApplicationContext applicationContext) {
        TerminalHandler.applicationContext = applicationContext;
    }

    private TerminalContext terminalContext;

    // 当前用户登录认证唯一标识
    private String uuid;
    // 当前登录设备信息
    private String assetId;
    
    private String token;
    
    private String accountId;
    
    @Override
    public synchronized void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        if(terminalContext==null){
            terminalContext= (TerminalContext) applicationContext.getBean("terminalContext");
        }
        assetId=(String)session.getAttributes().get("assetId");
        accountId=(String)session.getAttributes().get("accountId");
        uuid=(String)session.getAttributes().get("uuid");
        token=(String)session.getAttributes().get("token");
        
        if (StringUtils.isNotBlank(assetId) && StringUtils.isNotBlank(accountId)) {
        	// 从缓存中取出之前保存的客户端实例
        	Account terminalInfo = terminalContext.get(uuid);
            if (terminalInfo == null) {
            	AssetService assetService = (AssetService) applicationContext.getBean("assetService");
            	AccountService accountService = (AccountService) applicationContext.getBean("accountService");
            	terminalInfo = accountService.getById(accountId);
            	// 查询asset相关信息
            	Asset asset = assetService.getById(assetId);
            	terminalInfo.setAsset(asset);
            	terminalContext.put(uuid,terminalInfo);
            }
            final Account terminal =terminalInfo;
            try {
                getClient(session, terminal);
                int cols = toInt(session.getAttributes().get("cols").toString());
                int rows = toInt(session.getAttributes().get("rows").toString());
                int width = toInt(session.getAttributes().get("width").toString());
                int height = toInt(session.getAttributes().get("height").toString());
                terminalClient.openTerminal(cols, rows, width, height);
            } catch (Exception e) {
            	logger.error(e.getMessage());
            	e.printStackTrace();
            	if (e.getMessage().toLowerCase().contains("userauth fail")) {
            		session.sendMessage(new TextMessage(" Login fail "));
                } else if (e.getLocalizedMessage().replaceAll("\\s+", "").contentEquals("Operationtimedout")) {
                    session.sendMessage(new TextMessage("Sorry! Connect timed out, please try again. "));
                } else {
                    session.sendMessage(new TextMessage("Sorry! Operation error, please try again. "));
                }
                terminalClient.disconnect();
                session.close();
            }
        }else {
        	//如果用户没有传入asset信息 则友情提示用户
        	session.sendMessage(new TextMessage("Welcome to Nezha Terminal!  Type `help' to learn how to use Nezha Terminal prompt. \r\n"));
        	logger.info("user stateless access server");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        logger.info("handleTextMessage meesage info : {}",message.getPayload());
        try {
            getClient(session, null);
            if (this.terminalClient != null) {
                if (!terminalClient.isClosed()) {
                    terminalClient.write(message.getPayload());
                } else {
                    session.close();
                }
            }else {
            	nezhaMessageHandle(session,message.getPayload());
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
    }

    private TerminalClient getClient(WebSocketSession session, Account terminal) {
        this.terminalClient = TerminalSession.get(session);
        if (this.terminalClient == null && terminal != null) {
            this.terminalClient = new TerminalClient(session, terminal, uuid);
            TerminalSession.put(session, this.terminalClient);
            TerminalSession.putTerminalInfo(token,uuid);
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

    public void nezhaMessageHandle(WebSocketSession session, String message) throws IOException {
    	if(message.equals("help")) {
    		session.sendMessage(new TextMessage("Internal Commands:\n" + 
    				"\n" + 
    				"ssh: Connects to a host using the SSH protocol.\n" + 
    				"telnet: Connects to a host using the TELNET protocol.\n" + 
    				"set: Sets options.\n" + 
    				"clear: Clears the screen/address/command history.\n" + 
    				"help: Displays this help. '?' does the same.\n" + 
    				"quit: Quits Local Shell. 'exit' does the same."));
    	}else if(message.matches("help\\s+ssh")) {
    		session.sendMessage(new TextMessage("NAME\n" + 
    				"ssh - connects to a host using the SSH protocol.\n" + 
    				"\n" + 
    				"SYNOPSYS\n" + 
    				"ssh [user:pass@]host[ port]\n" + 
    				"\n" + 
    				"OPTIONS\n" + 
    				"user Indicates the user's login name.\n" + 
    				"pass If pass is definded, use password authentication.\n" + 
    				"host Indicates the name, alias, or Internet address of the\n" + 
    				"remote host.\n" + 
    				"port Indicates a port number (address of an application).\n" + 
    				"If the port is not specified, the default ssh port\n" + 
    				"(22) is used."));
    	}else if(message.matches("help\\s+telnet")) {
    		session.sendMessage(new TextMessage("NAME\n" + 
    				"telnet - connects to a host using the TELNET protocol.\n" + 
    				"\n" + 
    				"SYNOPSYS\n" + 
    				"telnet [user@]host [port]\n" + 
    				"\n" + 
    				"OPTIONS\n" + 
    				"host Indicates the name, alias, or Internet address of the\n" + 
    				"remote host.\n" + 
    				"port Indicates a port number (address of an application).\n" + 
    				"If the port is not specified, the default telnet port\n" + 
    				"(23) is used."));
    	}else if(message.matches("help\\s+clear")) {
    		session.sendMessage(new TextMessage("NAME\n" + 
    				"clear - clears the screen"));
    	}else if(message.matches("help\\s+quit")) {
    		session.sendMessage(new TextMessage("NAME\n" + 
    				"quit - quits application or connection.\n" + 
    				"\n" + 
    				"DESCRIPTION\n" + 
    				"When a connection is not establish, quits application.\n" + 
    				"Otherwise, quits connection. No parameters are required.\n" + 
    				"exit does the same.\n" + 
    				"cf) Press Ctrl + Alt + ] to fall back to Xshell prompt\n" + 
    				"whlie connection is alive."));
    	}else if(message.equals("quit")) {
    		session.close();
    	}
    }
}

