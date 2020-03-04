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

package com.nis.modules.terminal.config;

import com.nis.common.utils.DigestUtils;
import com.nis.modules.terminal.entity.Account;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.nis.common.utils.CommonUtils.notEmpty;

public class TerminalSession implements Serializable {

    //key--->WebSocketSession value--->TerminalClient
    public static Map<WebSocketSession, TerminalClient> terminalSession = new HashMap<WebSocketSession, TerminalClient>();
    
    //key--->token value---->terminalInfo
    public static Map<String, Set<String>> terminalInfo = new HashMap<String, Set<String>>();
    
    public static Map<String,String> unloginMessageInfo = new HashMap<String,String>();
    
    public static String getUnloginMessage(String uuid) {
    	return unloginMessageInfo.get(uuid);
    }
    
    public static void setUnloginMessage(String uuid,String message) {
    	String string = unloginMessageInfo.get(uuid);
    	if(StringUtils.isNoneBlank(string)) {
    		string=string+message;
    	}else {
    		string=message;
    	}
    	unloginMessageInfo.put(uuid,string);
    }
    
    public static void removeUnloginMessage(String uuid) {
    	unloginMessageInfo.remove(uuid);
    }
    
    public static TerminalClient get(WebSocketSession key) {
        return terminalSession.get(key);
    }

    public static TerminalClient get(String key) {
        for (Map.Entry<WebSocketSession, TerminalClient> entry : terminalSession.entrySet()) {
            TerminalClient client = entry.getValue();
            if (client.getClientId().equals(key)) {
                return client;
            }
        }
        return null;
    }

    public static List<TerminalClient> getAllTerminalClient(String token){
    	List<TerminalClient> terminalClients=new ArrayList<TerminalClient>();
    	Set<String> terminalInfos = terminalInfo.get(token);
    	Map<String,TerminalClient> map=new HashMap<String,TerminalClient>();
    	for (Map.Entry<WebSocketSession, TerminalClient> entry : terminalSession.entrySet()) {
            TerminalClient client = entry.getValue();
            map.put(client.getClientId(), client);
        }
    	if(terminalInfos!=null&&terminalInfos.size()>0) {
    		for(String terminalInfo : terminalInfos) {
    			TerminalClient terminalClient = map.get(terminalInfo);
    			if(terminalClient!=null) {
    				terminalClients.add(terminalClient);
    			}
    		}
    	}
    	return terminalClients;
    }
    
    public static void put(WebSocketSession key, TerminalClient terminalClient) {
        terminalSession.put(key, terminalClient);
    }

    public static void putTerminalInfo(String token,String uuid) {
    	Set<String> terminalInfos = terminalInfo.get(token);
    	if(terminalInfos==null) {
    		terminalInfos=new HashSet<String>();
    		terminalInfos.add(uuid);
    		terminalInfo.put(token, terminalInfos);
    	}else {
    		terminalInfos.add(uuid);
    	}
    }
    
    public static TerminalClient remove(WebSocketSession key) {
        return terminalSession.remove(key);
    }

    public static boolean isOpened(Account terminal) {
        for (Map.Entry<WebSocketSession, TerminalClient> entry : terminalSession.entrySet()) {
            if (entry.getValue().getTerminal().equals(terminal)) {
                return true;
            }
        }
        return false;
    }

    public static List<TerminalClient> findClient(Serializable sessionId) throws IOException {
        List<TerminalClient> terminalClients = new ArrayList<TerminalClient>(0);
        if (notEmpty(terminalSession)) {
            for (Map.Entry<WebSocketSession, TerminalClient> entry : terminalSession.entrySet()) {
                TerminalClient terminalClient = entry.getValue();
                if (terminalClient != null && terminalClient.getTerminal() != null) {
                    if (sessionId.equals(terminalClient.getHttpSessionId())) {
                        terminalClients.add(terminalClient);
                    }
                }
            }
        }
        return terminalClients;
    }

    public static List<TerminalClient> findClient(){
        List<TerminalClient> terminalClients = new ArrayList<TerminalClient>(0);
        if (notEmpty(terminalSession)) {
            for (Map.Entry<WebSocketSession, TerminalClient> entry : terminalSession.entrySet()) {
                TerminalClient terminalClient = entry.getValue();
                terminalClients.add(terminalClient);
            }
        }
        return terminalClients;
    }

    public static WebSocketSession findSession(Account terminal) {
        for (Map.Entry<WebSocketSession, TerminalClient> entry : terminalSession.entrySet()) {
            TerminalClient client = entry.getValue();
            if (client.getTerminal().equals(terminal)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static void exit(HttpServletRequest request) throws IOException {
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        if (userAgent != null) {
            userAgent = userAgent.replaceAll("\\s+", "");
            userAgent = DigestUtils.md5Hex(userAgent);
        }

        if (notEmpty(terminalSession)) {
            for (Map.Entry<WebSocketSession, TerminalClient> entry : terminalSession.entrySet()) {
                TerminalClient terminalClient = entry.getValue();
                if (terminalClient.getHttpSessionId().equals(request.getSession().getId())
                        || terminalClient.getHttpSessionId().equals(userAgent)) {
                    terminalClient.disconnect();
                    terminalClient.getWebSocketSession().sendMessage(new TextMessage("Sorry! Session was invalidated, so Nezha Terminal changed to closed. "));
                    terminalClient.getWebSocketSession().close();
                }
            }
        }
    }

}