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

import cn.hutool.core.util.StrUtil;
import com.nis.common.utils.DigestUtils;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.terminal.entity.Account;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.util.ObjectUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.nis.common.utils.CommonUtils.notEmpty;

public class TerminalSession implements Serializable {

    /**
     * 当前websockt连接信息对应的服务器信息   将来可以实现远程连接列表 用以进行主动断开服务器连接功能
     */
    public static final Map<WebSocketSession, TerminalClient> terminalSession = new HashMap<WebSocketSession, TerminalClient>();
    
    /**
     * 保存所有正在连接的服务器列表信息
     */
    public static final Map<String, Set<String>> terminalInfo = new HashMap<String, Set<String>>();

    /**
     * terminal 对应的 monitors
     */
    public static final Map<String, List<WebSocketSession>> terminalMonitorsMap = new ConcurrentHashMap<>();

    /**
     * 当前用户操作的服务器指令信息
     */
    public static final Map<String,String> terminalMessageInfo = new HashMap<String,String>();

    // terminal cmd 信息
    public static final Map<String, List<Character>> terminalCmdInfo = new HashMap();

    public static void removeTerminalCmd(String uuid) {
        terminalCmdInfo.remove(uuid);
    }

    /**
     * 用以保存用户连接websocket后的指令历史记录信息 包含未登录以及登录服务器后的历史指令
     */
    public static final Map<String,List<String>> userInstructions =new HashMap<String,List<String>>();
    
    /**
     * 主要用于存放client相关需要属性
     */
    public static final Map<String,Object> clientAttributeInfo =new HashMap<String,Object>();
    /**
     * 保存用户连接当前websocket的一些配置信息 包括光标位置、未登录时的一些状态信息
     * 每个连接功能会公用一个terminalHandler类 将各自属性根据uuid以及相应的状态信息进行保存
     */
    public static final Map<String,Map> terminalAttributeInfo = new HashMap<String,Map>();
    
    /**
     * 管理前端窗口数量
     */
    public static final Map<String,Integer> windowSize =new HashMap<String,Integer>();
    
    public static Integer getWindowSize(String token) {
    	Integer result = windowSize.get(token);
    	if(ToolUtil.isEmpty(result)) {
    		result=1;
    	}
    	return result;
    }
    
    public static void setWindowSize(String token) {
    	Integer result = windowSize.get(token);
    	if(ToolUtil.isEmpty(result)) {
    		result=1;
    	}else {
    		result+=1;
    	}
    	windowSize.put(token, result);
    }
    
    public static void removeWindowSize(String token) {
    	Integer result = windowSize.get(token);
    	if(ToolUtil.isEmpty(result)||result==0) {
    		windowSize.remove(token);
    	}else {
    		result-=1;
    	}
    	windowSize.put(token, result);
    }
    
    
    public static void setTerminalAttributeInfo(String uuid,String key,Object value) {
    	Map map = terminalAttributeInfo.get(uuid);
    	if(!ObjectUtils.isEmpty(map)) {
    		map.put(key, value);
    		terminalAttributeInfo.put(uuid, map);
    	}
    }
    
    public static Map getTerminalAttributeInfo(String uuid) {
    	Map map = terminalAttributeInfo.get(uuid);
    	if(ObjectUtils.isEmpty(map)) {
    		//第一次获取基础配置的时候 初始化配置信息
    		map=new HashMap();
    		//指令光标位置信息
    		map.put("cursorIndex", 0);
    		//光标是否移动标识
    		map.put("cursorFlag", false);
    		//历史记录位置信息
    		map.put("historyIndex", 0);
    		//ssh连接登录标识
    		map.put("sshLogin", false);
    		//tab键标识
    		map.put("tabFlag", false);

            // 光标位置
            map.put("cmdCharPos", 0);
            terminalAttributeInfo.put(uuid, map);
    	}
    	return map;
    }
    
    public static void removeTerminalAttributeInfo(String uuid) {
    	terminalAttributeInfo.remove(uuid);
    }
    
    public static String getHistoryInstruction(String uuid,Integer index) {
    	List<String> lists = userInstructions.get(uuid);
    	if(ObjectUtils.isEmpty(lists)) {
    		return "";
    	}else if(index>lists.size()){
    		return "";
    	}else if(lists.size()-index==lists.size()){
    		return "";
    	}else {
    		return lists.get(lists.size()-index);
    	}
    }
    
    public static void setHistoryInstruction(String uuid,String message) {
    	List<String> lists = userInstructions.get(uuid);
    	if(ObjectUtils.isEmpty(lists)) {
    		lists=new ArrayList<String>();
    		lists.add(message);
    		userInstructions.put(uuid, lists);
    	}else {
    		lists.add(message);
    	}
    }
    
    public static String getTerminalMessage(String uuid) {
    	return terminalMessageInfo.get(uuid);
    }
    
    public static void setTerminalMessage(String uuid,String message,Integer cursorIndex) {
    	String string = terminalMessageInfo.get(uuid);
        if (StrUtil.isNotBlank(string)) {
    		// 根据光标位置来追加字符出现的位置
    		if(cursorIndex==null||cursorIndex==string.length()) {
    			string=string+message;
    		}else {
    			String prefix = string.substring(0, cursorIndex);
    			String suffix = string.substring(cursorIndex, string.length());
    			string=prefix+message+suffix;
    		}
    	}else {
    		string=message;
    	}
    	terminalMessageInfo.put(uuid,string);
    }
    /**
     * 清空当前用户指令
     * @param uuid
     */
    public static void removeTerminalMessage(String uuid) {
    	terminalMessageInfo.remove(uuid);
    }
    
    public static boolean backspaceMessage(String uuid,Integer cursorIndex) {
    	boolean flag=false;
    	String string = terminalMessageInfo.get(uuid);
    	if(StringUtils.isNotBlank(string)) {
    		if(cursorIndex==string.length()) {
    			string=string.substring(0, string.length()-1);
    		}else if(cursorIndex==0&&string.length()>0){
    			// 如果用户将光标移到最开始的地方 则不进行任何删除操作
    			return false;
    		}else {
    			String prefix = string.substring(0, cursorIndex-1);
    			String suffix = string.substring(cursorIndex, string.length());
    			string=prefix+suffix;
    		}
    		if(StringUtils.isNotBlank(string)) {
    			terminalMessageInfo.put(uuid, string);
    			flag=true;
    		}else {
    			terminalMessageInfo.remove(uuid);
    			flag=true;
    		}    		
    	}
    	return flag;
    }
    
    public static TerminalClient get(WebSocketSession key) {
        return terminalSession.get(key);
    }

    public static TerminalClient get(String key) {
        for (Map.Entry<WebSocketSession, TerminalClient> entry : terminalSession.entrySet()) {
            TerminalClient client = entry.getValue();
            if(StrUtil.equals(client.getClientId(),key)) {
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
    	if(ToolUtil.isNotEmpty(terminalInfos)) {
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
                    if (StrUtil.equals(terminalClient.getHttpSessionId(),sessionId.toString())) {
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

    /**
     * 通过 uuid 得到 webSocketSession 对象
     * @param uuid
     * @return
     */
    public static WebSocketSession findSession(String uuid) {
        for (Map.Entry<WebSocketSession, TerminalClient> entry : terminalSession.entrySet()) {
            TerminalClient client = entry.getValue();
            if (client.getClientId().equalsIgnoreCase(uuid)) {
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
                if(StrUtil.equals(terminalClient.getHttpSessionId(),request.getSession().getId())
                        || terminalClient.getHttpSessionId().equals(userAgent)) {
                    terminalClient.disconnect();
                    terminalClient.getWebSocketSession().sendMessage(new TextMessage("Sorry! Session was invalidated, so Nezha Terminal changed to closed. "));
                    terminalClient.getWebSocketSession().close();
                }
            }
        }
    }

}
