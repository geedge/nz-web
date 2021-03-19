
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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.google.common.primitives.Bytes;
import com.jcraft.jsch.*;
import com.nis.common.utils.*;
import com.nis.modules.terminal.entity.*;
import com.nis.modules.terminal.service.TerminalCmdService;
import com.nis.modules.terminal.service.TerminalLogService;
import com.nis.modules.terminal.service.TerminalRecordService;
import com.nis.modules.terminal.service.TerminalSessionService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.telnet.TelnetClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.nis.common.utils.CommonUtils.notEmpty;

public class TerminalClient {
	private static final Log logger = Log.get();
    private String clientId;//每次生成的唯一值token
    private String httpSessionId;//打开该终端的SessionId
    private WebSocketSession webSocketSession;
    private JSch jSch;
    private ChannelShell channelShell;
    private Session session;
    private Account terminal;
    private InputStream inputStream;
    private OutputStream outputStream;
    private BufferedWriter writer;

    //获取路径相关变量
    private String pwd;
    private boolean sendTempCmd = false;
    private String sendTempCmdId;

    //控制命令(exit)终端退出相关变量
    private boolean sendEnter;
    private boolean receiveEnd = false;
    private StringBuffer sendBuffer = new StringBuffer();

    //连接时长相关变量
    public static final int SERVER_ALIVE_INTERVAL = 60 * 1000;
    public static final int SESSION_TIMEOUT = 60000;
    public static final int CHANNEL_TIMEOUT = 60000;

    private boolean closed = false;
    // 是否可进行上传下载操作
    private boolean transfer =true;
    // telnet连接次数
    private Integer telnetConnectCount=0;
    // 标识telnet连接状态 true是正在尝试连接  false为连接成功或失败
    private boolean telnetConnect=false;
    private boolean telnetUser=false;
    private StringBuilder sb=new StringBuilder();
    public TerminalClient(WebSocketSession webSocketSession, Account terminal, String clientId) {
        this.webSocketSession = webSocketSession;
        this.terminal = terminal;
        this.clientId = clientId;

        //解决分布式环境下,不同的server为用户打开的终端实例会话不一致问题,导致根据会话Id查找不到另一个server打开的终端实例的问题
        List<String> userAgents = webSocketSession.getHandshakeHeaders().get(HttpHeaders.USER_AGENT);
        if (CommonUtils.notEmpty(userAgents)) {
            String userAgent = StringUtils.joinString(userAgents);
            userAgent = userAgent.replaceAll("\\s+", "");
            this.httpSessionId = DigestUtils.md5Hex(userAgent);
        } else {
            this.httpSessionId = (String) webSocketSession.getAttributes().get(Constants.PARAM_HTTP_SESSION_ID_KEY);
        }
        this.sendTempCmdId = this.clientId + this.httpSessionId;
        this.jSch = new JSch();
    }

    public void openTerminal(final int cols, int rows, int width, int height) throws JSchException, IOException {
        Map<String, String> params = JSONObject.parseObject((String) terminal.getParams(), Map.class);
        if (ToolUtil.isEmpty(params)) {
            params = new HashMap<>();
        }
        this.session = jSch.getSession(params.get("username"), terminal.getAsset().getHost(), terminal.getPort());

        String sshType = params.get("method");
        switch (sshType) {
            case "key": {
                String privateKey = params.get("key");
                String keyPassword = params.get("keyPassword");
                if (notEmpty(privateKey)) {
                	File keyFile = FileUtil.file(terminal.getPrivateKeyPath());
//                    if (!keyFile.exists()) {
                    //将数据库中的私钥写到用户的机器上
                    FileUtil.writeFromStream(new ByteArrayInputStream(privateKey.getBytes()),keyFile);
//                    }
                    if (notEmpty(keyPassword)) {
                        //设置带口令的密钥
                        jSch.addIdentity(terminal.getPrivateKeyPath(), keyPassword);
                    } else {
                        //设置不带口令的密钥
                        jSch.addIdentity(terminal.getPrivateKeyPath());
                    }
                    UserInfo userInfo = new SshUserInfo();
                    session.setUserInfo(userInfo);
                }
                session.setConfig("StrictHostKeyChecking", "no");
                break;
            }
            case "password": {
                session.setConfig("StrictHostKeyChecking", "no");
                session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
                session.setPassword(StringUtils.str(Base64.getDecoder().decode(params.get("password"))));
                break;
            }
            default: {
            }
        }
        this.session.setServerAliveInterval(SERVER_ALIVE_INTERVAL);
        this.session.connect(SESSION_TIMEOUT);
        this.channelShell = (ChannelShell) session.openChannel("shell");
        this.channelShell.setPtyType("xterm", cols, rows, width, height);
        this.inputStream = this.channelShell.getInputStream();
        this.outputStream = this.channelShell.getOutputStream();
        this.writer = new BufferedWriter(new OutputStreamWriter(this.outputStream, "UTF-8"));
        this.channelShell.connect();

        StringBuilder sb = new StringBuilder();
        sb.append("\r\nConnecting to " + terminal.getAsset().getHost() + ":" + terminal.getPort() + "\r\n");
        sb.append("Connection established. \r\n");
        sb.append("Login success，use user " + params.get("username") + " \r\n");
    	webSocketSession.sendMessage(new TextMessage(sb.toString()));
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                TerminalCmdService terminalCmdService = (TerminalCmdService) TerminalSession.clientAttributeInfo.get("terminalCmdService");
                TerminalRecordService terminalRecordService = (TerminalRecordService) TerminalSession.clientAttributeInfo.get("terminalRecordService");
                byte[] buffer = new byte[1024 * 4];
                StringBuilder builder = new StringBuilder();
                try {
                    while (webSocketSession != null && webSocketSession.isOpen()) {
                        builder.setLength(0);
                        int size = inputStream.read(buffer);
                        if (size == -1) {
                            return;
                        }
                        for (int i = 0; i < size; i++) {
                            char chr = (char) (buffer[i] & 0xff);
                            builder.append(chr);
                        }
                        //取到linux远程机器输出的信息发送给前端
                        String message = builder.toString();

                        // terminal record 信息梳理
                        File file = FileUtil.file(Constant.TEMP_PATH, clientId + ".log");
                        Path path = file.toPath();

                        message = StrUtil.str(message.getBytes(DigestUtils.getEncoding(message)),"UTF-8");

                        List<WebSocketSession> webSocketSessions = TerminalSession.terminalMonitorsMap.get(clientId);
                        if(CollectionUtils.isNotEmpty(webSocketSessions)){
                            for (WebSocketSession socketSession : webSocketSessions) {
                                socketSession.sendMessage(new TextMessage(message));
                            }
                        }

                        TerminalClient.handleTerminalRecordMessage(file, path, terminalRecordService, clientId, message);

                        processCommand(terminalCmdService, message);
                        //获取pwd的结果输出,不能发送给前台
                        if (sendTempCmd) {
                            if (message.contains(sendTempCmdId)) {
                                if (pwd != null || message.contains("echo")) {
                                    continue;
                                }
                                pwd = message.replace(sendTempCmdId, "").replaceAll("\r\n.*", "") + "/";
                                if (logger.isInfoEnabled()) {
                                    logger.info(" Sftp upload file target path:{}", pwd);
                                }
                            }
                        } else {
                            if((message.replaceAll("\r\n", "")).equals("logout")) {
                                // 已结束连接
                            	String unloginInfo = getUnloginInfo(Constants.SessionStatus.OVER.getValue());
                                webSocketSession.sendMessage(new TextMessage(unloginInfo));

                                try {
                                    // 将结束信息保存入库
                                    TerminalRecord terminalRecord = new TerminalRecord();
                                    terminalRecord.setUuid(clientId);
                                    terminalRecord.setContent(Files.readAllBytes(path));
                                    TerminalRecord lastRecord = terminalRecordService.getOne(new LambdaQueryWrapper<TerminalRecord>().eq(TerminalRecord::getUuid, clientId).orderByDesc(TerminalRecord::getId).last("limit 0,1"));
                                    // 之前没有文件记录
                                    if(lastRecord == null){
                                        terminalRecord.setStartTime(0);
                                    }else{
                                        terminalRecord.setStartTime(lastRecord.getEndTime());
                                    }
                                    long terminalSessionStartTime = (long) TerminalSession.getTerminalAttributeInfo(clientId).get("terminalSessionStartTime");
                                    terminalRecord.setEndTime((int) (System.currentTimeMillis() - terminalSessionStartTime));
                                    terminalRecordService.save(terminalRecord);

                                    file.delete();
                                } catch (IOException e) {
                                    logger.error(String.format("SSH 连接已关闭，terminal record 结尾临时文件入库失，文件路径是: %s，错误信息是: %s", path.toString(), e.getMessage()), e);
                                }
                            }else {
                            	webSocketSession.sendMessage(new TextMessage(message));
                            }
                            if (sendEnter) {
                                String trimMessage = message.replaceAll("\r\n", "");
                                if (CommonUtils.isEmpty(trimMessage)) {
                                    receiveEnd = false;
                                } else {
                                    if (sendBuffer.toString().equals("exit") && trimMessage.equals("logout")) {
                                        channelShell.disconnect();
                                        webSocketSession.close();
                                    	TerminalSession.remove(webSocketSession);
                                        closed = true;
                                    }
                                    receiveEnd = true;
                                }
                                if (receiveEnd) {
                                    sendBuffer.setLength(0);
                                }
                            }
                        }
                    }
                } catch (UnsupportedEncodingException e) {
                    logger.error("TerminalClient error info :",e);
                    TerminalSession.remove(webSocketSession);
                } catch (IOException e) {
                    logger.error("TerminalClient error info :",e);
                    TerminalSession.remove(webSocketSession);
                }

            }
        }).start();

    }

    /**
     * terminal record 信息入库
     *
     * @param file
     * @param path
     * @param terminalRecordService
     * @param clientId
     * @param message
     */
    public static void handleTerminalRecordMessage(File file, Path path, TerminalRecordService terminalRecordService, String clientId, String message) {
        // 文件大于 定义临时文件大小，
        if (file.exists() && file.length() > Constants.RECORD_TEMPFILE_MAX_LENGTH) {
            logger.info("临时文件大于定义大小，保存 record 文件入库，文件路径 是:" + path.toString());
            // 将本次内容保存至数据库
            try {
                List<TerminalRecord> list = terminalRecordService.list(new LambdaQueryWrapper<TerminalRecord>().eq(TerminalRecord::getUuid, clientId).orderByAsc(TerminalRecord::getId));
                TerminalRecord terminalRecord = new TerminalRecord();
                terminalRecord.setUuid(clientId);
                // 如果是第一次保存到库的话 ， start time 为 0
                if (CollectionUtils.isEmpty(list)) {
                    terminalRecord.setStartTime(0);
                } else {
                    // 其他的开始时间为上一次的结束时间
                    terminalRecord.setStartTime(list.get(list.size() - 1).getEndTime());
                }
                long terminalSessionStartTime = (long) TerminalSession.getTerminalAttributeInfo(clientId).get("terminalSessionStartTime");
                terminalRecord.setEndTime((int) (System.currentTimeMillis() - terminalSessionStartTime));
                terminalRecord.setContent(Files.readAllBytes(path));
                terminalRecordService.save(terminalRecord);
                // 清空文件内容
                file.delete();
            } catch (Exception e) {
                logger.error(String.format("terminal record 文件入库失败，文件路径是: %s，错误信息是: %s", path.toString(), e.getMessage()), e);
            }
        }
        // 以追加的方式追加服务器返回内容
        try {
            long terminalSessionStartTime = (long) TerminalSession.getTerminalAttributeInfo(clientId).get("terminalSessionStartTime");
            Map<String, Object> map = new HashMap(4);
            map.put("t",(int) (System.currentTimeMillis() - terminalSessionStartTime));
            map.put("c",message);
            String s = JSONObject.toJSONString(map);
            s += ",";
            FileUtil.writeBytes(s.getBytes(), file, 0, s.getBytes().length, true);
        } catch (IORuntimeException e) {
            logger.error(String.format("追加 terminal record 文件失败，文件路径是: %s,错误信息是: %s", path.toString(), e.getMessage()), e);
        }
    }

    public void openTelnetTerminal(final int cols, int rows, int width, int height) throws Exception {
    	TelnetClient telnet = new TelnetClient("VT100");// VT100 VT52 VT220 VTNT ANSI  
        telnet.connect(terminal.getAsset().getHost(), terminal.getPort());  
        // telnet 不允许上传下载操作
        this.transfer=false;
        this.inputStream=telnet.getInputStream();
        this.outputStream=telnet.getOutputStream();
        this.writer = new BufferedWriter(new OutputStreamWriter(this.outputStream, "UTF-8"));
        //当前正在进行telnet连接  相关日志不记录
        this.telnetConnect=true;

        Map<String, String> params = JSONObject.parseObject((String) terminal.getParams(), Map.class);
        if (ToolUtil.isEmpty(params)) {
            params = new HashMap<>();
        }
        String password = params.get("password");
        new Thread(new Runnable() {
            @Override
            public void run() {
            	TerminalLogService terminalLogService = (TerminalLogService) TerminalSession.clientAttributeInfo.get("terminalLogService");
                TerminalSessionService terminalSessionService = (TerminalSessionService) TerminalSession.clientAttributeInfo.get("terminalSessionService");
                TerminalCmdService terminalCmdService = (TerminalCmdService) TerminalSession.clientAttributeInfo.get("terminalCmdService");
                TerminalRecordService terminalRecordService = (TerminalRecordService) TerminalSession.clientAttributeInfo.get("terminalRecordService");
                TerminalLog terminalLog = (TerminalLog) TerminalSession.getTerminalAttributeInfo(clientId).get("terminalLog");
                TerminalSessionEntity sessionEntity = (TerminalSessionEntity) TerminalSession.getTerminalAttributeInfo(clientId).get("terminalSessionEntity");
                byte[] buffer = new byte[1024 * 4];
                StringBuilder builder = new StringBuilder();
                telnetUser=true;
                try {
                    while (webSocketSession != null && webSocketSession.isOpen()) {
                        Map<String, String> params = JSONObject.parseObject((String) terminal.getParams(), Map.class);
                        if (ToolUtil.isEmpty(params)) {
                            params = new HashMap<>();
                        }
                        String user = params.get("username");

                        builder.setLength(0);
                        int size = inputStream.read(buffer);
                        if (size == -1) {
                            return;
                        }
                        for (int i = 0; i < size; i++) {
                            char chr = (char) (buffer[i] & 0xff);
                            builder.append(chr);
                        }
                        //取到linux远程机器输出的信息发送给前端
                        String message = builder.toString();

                        // terminal record 信息梳理
                        String fileName = Constant.TEMP_PATH + File.separator + clientId + ".log";
                        File file = new File(fileName);
                        Path path = file.toPath();

                        message = StrUtil.str(message.getBytes(DigestUtils.getEncoding(message)),"UTF-8");

                        List<WebSocketSession> webSocketSessions = TerminalSession.terminalMonitorsMap.get(clientId);
                        if(CollectionUtils.isNotEmpty(webSocketSessions)){
                            for (WebSocketSession socketSession : webSocketSessions) {
                                socketSession.sendMessage(new TextMessage(message));
                            }
                        }

                        TerminalClient.handleTerminalRecordMessage(file, path, terminalRecordService, clientId, message);

                        processCommand(terminalCmdService, message);
                        //获取pwd的结果输出,不能发送给前台
                        if (sendTempCmd) {
                            if (message.contains(sendTempCmdId)) {
                                if (pwd != null || message.contains("echo")) {
                                    continue;
                                }
                                pwd = message.replace(sendTempCmdId, "").replaceAll("\r\n.*", "") + "/";
                                if (logger.isInfoEnabled()) {
                                    logger.info(" Sftp upload file target path:{}", pwd);
                                }
                            }
                        } else {
                            if((message.replaceAll("\r\n", "")).equals("logout")) {
                                // 已结束连接
                                String unloginInfo = getUnloginInfo(Constants.SessionStatus.OVER.getValue());
                                webSocketSession.sendMessage(new TextMessage(unloginInfo));

                                try {
                                    // 将结束信息保存入库
                                    TerminalRecord terminalRecord = new TerminalRecord();
                                    terminalRecord.setUuid(clientId);
                                    terminalRecord.setContent(Files.readAllBytes(path));
                                    TerminalRecord lastRecord = terminalRecordService.getOne(new LambdaQueryWrapper<TerminalRecord>().eq(TerminalRecord::getUuid, clientId).orderByDesc(TerminalRecord::getId).last("limit 0,1"));
                                    // 之前没有文件记录
                                    if(lastRecord == null){
                                        terminalRecord.setStartTime(0);
                                    }else{
                                        terminalRecord.setStartTime(lastRecord.getEndTime());
                                    }
                                    long terminalSessionStartTime = (long) TerminalSession.getTerminalAttributeInfo(clientId).get("terminalSessionStartTime");
                                    terminalRecord.setEndTime((int) (System.currentTimeMillis() - terminalSessionStartTime));
                                    terminalRecordService.save(terminalRecord);

                                    file.delete();
                                } catch (IOException e) {
                                    logger.error(String.format("Telnet 连接已关闭，terminal record 结尾临时文件入库失，文件路径是: %s，错误信息是: %s", path.toString(), e.getMessage()), e);
                                }
                            }else {
                            	//telnet登录状态修改
                            	if(message.contains("Last login")) {
                            		terminalLog.setCmd("Login");
                            		terminalLog.setStatus(1);
                            		terminalLog.setTime(DateUtil.getUTCTimeByConfigTimeZone());
                            		terminalLog.setUser(user);
                            		terminalLogService.save(terminalLog);

                                    // cmd 信息
                                    TerminalCmd terminalCmd = new TerminalCmd();
                                    terminalCmd.setUuid(clientId);
                                    terminalCmd.setCmd("Login");
                                    terminalCmd.setTime(0);
                                    terminalCmdService.saveOrUpdate(terminalCmd);

                                    // session 信息
                                    sessionEntity.setStatus(Constants.SessionStatus.CONNECTING.getValue());
                                    sessionEntity.setStartTime(DateUtil.getUTCTimeByConfigTimeZone());
                                    terminalSessionService.saveOrUpdate(sessionEntity);

                            		telnetConnect=false;
                            		// 登录成功后清空 前端输入字符 防止日志保存空数据
                            		TerminalSession.clientAttributeInfo.put(clientId+"shell", null);
                            	}
                            
                            	//判断用户登录telnet连接次数 
                            	if(message.contains("Login incorrect")) {
                        			telnetConnectCount++;
                        			telnetUser=true;
                        		}
                                if (message.replace(" ", "").matches(".*wwwlogin:") && StringUtils.isNotEmpty(user)) {
                                    webSocketSession.sendMessage(new TextMessage(message + user + "\r\n"));
                                    write(user + "\r");
                                } else if (message.matches("Password:.*") && StringUtils.isBlank(user)) {
                                    telnetUser = false;
                                    webSocketSession.sendMessage(new TextMessage("\r\n" + message));
                                } else if (message.matches("Password:.*") && StringUtils.isNotEmpty(user) && StringUtils.isNotEmpty(password)) {
                                	String s = StringUtils.str(Base64.getDecoder().decode(password));
                                    telnetUser = false;
                                    webSocketSession.sendMessage(new TextMessage("\r\n" + message));
                                    write(s + "\r");
                                } else if (message.contains("timed out after 60 seconds")) {
                            		webSocketSession.sendMessage(new TextMessage(message+"\r\nConnection closed by foreign host.\r\n$: "));
                            		// 连接超时
                            		terminalLog.setCmd("Login");
                            		terminalLog.setStatus(0);
                            		terminalLog.setTime(DateUtil.getUTCTimeByConfigTimeZone());
                            		terminalLog.setUser(user);
                            		terminalLogService.save(terminalLog);
                            		TerminalSession.remove(webSocketSession);
                            		closed=true;
                            		telnetConnect=false;
                            		// 连接失败
                                    String unloginInfo = getUnloginInfo(Constants.SessionStatus.CONNECTION_FAILED.getValue());
                                	webSocketSession.sendMessage(new TextMessage(unloginInfo));
                                } else {
                            		webSocketSession.sendMessage(new TextMessage(message));
                            		if(telnetConnectCount==3) {
                            			// 连接错误达到限制 关闭连接 连接失败
                            			terminalLog.setCmd("Login");
                                		terminalLog.setStatus(0);
                                		terminalLog.setTime(DateUtil.getUTCTimeByConfigTimeZone());
                                		terminalLog.setUser(user);
                                		terminalLogService.save(terminalLog);

                                        // cmd 信息
                                        TerminalCmd terminalCmd = new TerminalCmd();
                                        terminalCmd.setUuid(clientId);
                                        terminalCmd.setCmd("Login");
                                        terminalCmd.setTime(0);
                                        terminalCmdService.save(terminalCmd);

                                        // session 信息
                                        sessionEntity.setLoginUser(user);
                                        sessionEntity.setStatus(Constants.SessionStatus.CONNECTING.getValue());
                                        sessionEntity.setStartTime(DateUtil.getUTCTimeByConfigTimeZone());
                                        terminalSessionService.saveOrUpdate(sessionEntity);

                                        // 强制退出 错误达到限制
                            			String unloginInfo = getUnloginInfo(Constants.SessionStatus.FORCE_QUIT.getValue());
                                    	webSocketSession.sendMessage(new TextMessage(unloginInfo));

                                        webSocketSession.close();
                                        TerminalSession.remove(webSocketSession);
                                        closed=true;
                            		}
                            	}
                            }
                            if (sendEnter) {
                                String trimMessage = message.replaceAll("\r\n", "");
                                if (CommonUtils.isEmpty(trimMessage)) {
                                    receiveEnd = false;
                                } else {
                                    if (sendBuffer.toString().equals("exit") && trimMessage.equals("logout")) {
                                        webSocketSession.close();
                                        TerminalSession.remove(webSocketSession);
                                        closed = true;
                                    }
                                    receiveEnd = true;
                                }
                                if (receiveEnd) {
                                    sendBuffer.setLength(0);
                                }
                            }
                        }
                    }
                } catch (UnsupportedEncodingException e) {
                    logger.error("TerminalClient error info :",e);
                    TerminalSession.remove(webSocketSession);
                } catch (IOException e) {
                    logger.error("TerminalClient error info :",e);
                    TerminalSession.remove(webSocketSession);
                }

            }
        }).start();
    }
    
    /**
     * 处理服务器方向键以及tab操作返回的指令信息 便于做日志记录
     * @param serverResponse    服务器响应内容
     */
    public void processCommand(TerminalCmdService terminalCmdService,  String serverResponse) {
        Map terminalAttributeInfo = TerminalSession.getTerminalAttributeInfo(clientId);
        Integer cmdCharPos = (Integer) terminalAttributeInfo.get("cmdCharPos");

        List<Character> list = TerminalSession.terminalCmdInfo.get(clientId);
        list = (list == null) ? new ArrayList<>() : list;

        char[] charArray = serverResponse.toCharArray();

        // 转义字符
        boolean esc_mode = false;
        int esc_arg = 0;

        boolean maybeCmd = false;
        if (charArray.length >= 2) {
            maybeCmd = (charArray[0] == 13 && charArray[1] == 10); // 0x0d 13 \r  0x0a 10 \n
        }

        boolean client_single_char = false;
        if (charArray.length == 1) {
            int c = charArray[0];
            if (Constants.INVISIBLE_CHAR.contains(c)) {
                client_single_char = true;
            }
        }

        int offset = 0;
        int len = charArray.length;
        for (; offset < charArray.length; ) {
            char ch = charArray[offset];
            if (esc_mode) {
                switch (ch) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9': {
                        // 字符转数字
                        esc_arg = esc_arg * 10 + (ch - '0');
                        break;
                    }
                    case 63:
                    case ';':
                    case '>': {
                        TerminalSession.terminalCmdInfo.put(clientId, new ArrayList<>());
                        TerminalSession.setTerminalAttributeInfo(clientId, "cmdCharPos", 0);
                        return;
                    }
                    case 75: { // K
                        if (0 == esc_arg) {
                            list = list.subList(0, cmdCharPos);
                            cmdCharPos = list.size();
                        } else if (1 == esc_arg) {
                            list = list.subList(cmdCharPos, list.size());
                            cmdCharPos = list.size();
                        } else if (2 == esc_arg) {
                            list = new ArrayList<>();
                            cmdCharPos = 0;
                        }
                        TerminalSession.terminalCmdInfo.put(clientId, list);
                        TerminalSession.setTerminalAttributeInfo(clientId, "cmdCharPos", cmdCharPos);
                        esc_mode = false;
                        break;
                    }
                    case 67: {    // ^[C
                        // 光标右移
                        if (esc_arg == 0) {
                            esc_arg = 1;
                        }

                        for (int j = 0; j < esc_arg; ++j) {
                            if (cmdCharPos != list.size()) {
                                cmdCharPos++;
                            }
                        }
                        TerminalSession.setTerminalAttributeInfo(clientId, "cmdCharPos", cmdCharPos);
                        esc_mode = false;
                        break;
                    }
                    case 68: { // ^[D
                        // 光标左移
                        if (esc_arg == 0) {
                            esc_arg = 1;
                        }
                        for (int j = 0; j < esc_arg; ++j) {
                            if (cmdCharPos != 0) {
                                cmdCharPos--;
                            }
                        }
                        TerminalSession.setTerminalAttributeInfo(clientId, "cmdCharPos", cmdCharPos);
                        esc_mode = false;
                        break;
                    }
                    case 80: { // 'P' 删除指定数量的字符
                        if (esc_arg == 0) {
                            esc_arg = 1;
                        }

                        for (int j = 0; j < esc_arg; ++j) {
                            // 不是最后一个
                            if (cmdCharPos != list.size()) {
                                int index = cmdCharPos;
                                list.remove(index);
                            }
                        }
                        TerminalSession.terminalCmdInfo.put(clientId, list);
                        TerminalSession.setTerminalAttributeInfo(clientId, "cmdCharPos", cmdCharPos);
                        esc_mode = false;
                        break;
                    }
                    case 64:{
                        if (esc_arg == 0){
                            esc_arg = 1;
                        }
                        for (int j = 0; j < esc_arg; ++j){
                            list.add(cmdCharPos, ' ');
                            cmdCharPos++;
                        }
                        TerminalSession.terminalCmdInfo.put(clientId, list);
                        TerminalSession.setTerminalAttributeInfo(clientId, "cmdCharPos", cmdCharPos);
                        esc_mode = false;
                        break;
                    }
                    default:
                        esc_mode = false;
                        break;
                }

                offset++;
                continue;
            }

            switch (ch) {
                case 7:
                    // 响铃
                    break;
                case 8: {
                    // 光标左移
                    if (cmdCharPos != 0 && CollectionUtils.isNotEmpty(list)) {
                        cmdCharPos--;
                        TerminalSession.terminalCmdInfo.put(clientId, list);
                        TerminalSession.setTerminalAttributeInfo(clientId, "cmdCharPos", cmdCharPos);
                    }
                    break;
                }
                case 27: {  // 0x1b esc
                    if (offset + 1 < len) {
                        if (charArray[offset + 1] == 91 || charArray[offset + 1] == 93) {
                            if (offset == 0 && client_single_char) {
                                TerminalSession.removeTerminalCmd(clientId);
                                TerminalSession.setTerminalAttributeInfo(clientId, "cmdCharPos", 0);
                                maybeCmd = false;
                                client_single_char = false;
                                return;
                            }
                        }

                        if (charArray[offset + 1] == 91 || charArray[offset + 1] == 93) {
                            esc_mode = true;

                            esc_arg = 0;
                            offset += 1;
                        }
                    }
                    break;
                }
                // 0x0d 13 \r  0x0a 10 \n
                case 13: {
                    if (offset + 1 < len && charArray[offset + 1] == 10) {
                        if (maybeCmd) {
                            if (CollectionUtils.isNotEmpty(list) && !telnetConnect) {
                                String cmd = "";
                                for (Character character : list) {
                                    cmd += character;
                                }
                                TerminalCmd terminalCmd = new TerminalCmd();
                                terminalCmd.setCmd(cmd);
                                terminalCmd.setUuid(clientId);
                                long terminalSessionStartTime = (long) TerminalSession.getTerminalAttributeInfo(clientId).get("terminalSessionStartTime");
                                terminalCmd.setTime((int) (System.currentTimeMillis() - terminalSessionStartTime));
                                terminalCmdService.save(terminalCmd);
                            }
                            TerminalSession.removeTerminalCmd(clientId);
                            TerminalSession.setTerminalAttributeInfo(clientId, "cmdCharPos", 0);
                            maybeCmd = false;
                        }
                    }
                    TerminalSession.removeTerminalCmd(clientId);
                    TerminalSession.setTerminalAttributeInfo(clientId, "cmdCharPos", 0);
                    return;
                }
                default: {
                    if (cmdCharPos != list.size()) {
                        int index = cmdCharPos;
                        list.remove(index);
                        list.add(cmdCharPos, ch);
                        cmdCharPos++;
                    } else {
                        list.add(ch);
                        cmdCharPos = list.size();
                    }
                    TerminalSession.terminalCmdInfo.put(clientId, list);
                    TerminalSession.setTerminalAttributeInfo(clientId, "cmdCharPos", cmdCharPos);
                }
            }
            offset++;
        }
    }
    
	/**
	 * 处理终端响应信息
	 * @param param
	 * @return
	 */
	public String terminalResponseHandle(String param) {
		byte[] bytes = param.getBytes();
		List<Byte> asList = new ArrayList<>(Bytes.asList(bytes));
		for(int i=asList.size()-1;i>=0;i--) {
			if(asList.get(i)==8){
                asList.remove(i);
			}else if(asList.get(i)==13) {
                asList.remove(i);
			}else if(asList.get(i)==10) {
                asList.remove(i);
			}else if(asList.get(i)==27) {
                asList.remove(i);
			}else if(asList.get(i)==91) {
                asList.remove(i);
			}else if(i!=0&&asList.get(i-1)==91) {
                asList.remove(i);
			}else if(asList.get(i)==7) {
                asList.remove(i);
			}
		}
		byte[] array = Bytes.toArray(asList);
		return StringUtils.str(array);
	}
	
    /**
     * 向ssh终端输入内容
     *
     * @param message
     * @throws IOException
     */
    public void write(String message) throws IOException {
        Map<String, String> params = JSONObject.parseObject((String) terminal.getParams(), Map.class);
        if (ToolUtil.isEmpty(params)) {
            params = new HashMap<>();
        }
        // 获取telnet登录用户名信息
        if (telnetUser && telnetConnect && StringUtils.isEmpty(params.get("username"))) {
            sb.append(message);
            webSocketSession.sendMessage(new TextMessage(message));
        } else if (StringUtils.isNotEmpty(sb.toString())) {
            params.put("username", sb.toString());
            terminal.setParams(JSONObject.toJSONString(params));
        }
        if (message.equals("\r")) {
            if (closed) {
                disconnect();
            } else {
                sendEnter = true;
            }
        } else {
        	// 防止用户输入ctrl+c出现程序异常 以及记录日志异常
        	if(ObjectUtils.isNotEmpty(message) && message.getBytes()[0]!=3) {
        		sendEnter = false;
                sendBuffer.append(message);
        	}
        }

        if (outputStream != null) {
            IoUtil.write(outputStream, false, message.getBytes());
            outputStream.flush();
        }
    }

    public void upload(String src, String dst, long fileSize) throws Exception {
    	FileInputStream file =null;
    	ChannelSftp channelSftp = null;
        try {
        	file = IoUtil.toStream(FileUtil.file(src));
			channelSftp = (ChannelSftp) this.session.openChannel("sftp");
			channelSftp.connect(CHANNEL_TIMEOUT);
			//当前路径下,获取用户终端的当前位置
			if (dst.startsWith("./")) {
			    //不出绝招不行啊....很神奇
			    sendTempCmd = true;
			    write(String.format("echo %s$(pwd)\r", this.sendTempCmdId));
			    //等待获取返回的路径...
			    Thread.sleep(100);
			    sendTempCmd = false;
			    if (pwd == null) {
			        throw new RuntimeException("Sftp upload file target path error!");
			    }
			    dst = dst.replaceFirst("\\./", pwd);
			    pwd = null;
			} else if (dst.startsWith("~")) {
			    dst = dst.replaceAll("~/|~", "");
			}
			channelSftp.put(file, dst, new SftpMonitor(fileSize));
			
		} catch (Exception e) {
			throw e;
		} finally {
			//exit
			if (channelSftp != null) {
			    channelSftp.exit();
			}
			//disconnect
			if (channelSftp != null) {
			    channelSftp.disconnect();
			}
			if(file!=null) {
				IOUtils.closeQuietly(file);
			}
		}
    }

    public void download(String path,HttpServletResponse res) throws Exception {
    	ChannelSftp channelSftp=null;
    	InputStream is =null;
    	OutputStream outputStream =null;
    	try {
			logger.info("TerminalClient download param is {}",path);
			channelSftp = (ChannelSftp) this.session.openChannel("sftp");
			channelSftp.connect(CHANNEL_TIMEOUT);
			//当前路径下,获取用户终端的当前位置
			if (path.startsWith("./")) {
			    //不出绝招不行啊....很神奇
			    sendTempCmd = true;
			    write(String.format("echo %s$(pwd)\r", this.sendTempCmdId));
			    //等待获取返回的路径...
			    Thread.sleep(100);
			    sendTempCmd = false;
			    if (pwd == null) {
			        throw new RuntimeException("Sftp upload file target path error!");
			    }
			    path = path.replaceFirst("\\./", pwd);
			    pwd = null;
			} else if (path.startsWith("~")) {
			    path = path.replaceAll("~/|~", "");
			}
			//截取路径中的文件名称
			String fileName = path.substring(path.lastIndexOf("/")+1);
			String pathExceptFileName = path.substring(0,path.lastIndexOf("/"));
			logger.info("TerminalClient download fileName is {}",fileName);
			logger.info("TerminalClient download pathExceptFileName is {}",pathExceptFileName);
			channelSftp.cd(pathExceptFileName);
			is = channelSftp.get(fileName);
			res.setCharacterEncoding("utf-8");
			res.setContentType("multipart/form-data");
			res.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
			res.setHeader("Access-Control-Expose-Headers","Content-Disposition");
			// 发送给客户端的数据
			outputStream = res.getOutputStream();
			byte[] buff = new byte[1024];
			int i = is.read(buff);
			while (i != -1) {
			    outputStream.write(buff, 0, i);
			    outputStream.flush();
			    i = is.read(buff);
			}
		} catch (Exception e) {
			throw e;
		}  finally {
			if(outputStream!=null) {
				IOUtils.closeQuietly(outputStream);
			}
			if(is!=null) {
				IOUtils.closeQuietly(is);
			}
			//exit
			if (channelSftp != null) {
			    channelSftp.exit();
			}
			//disconnect
			if (channelSftp != null) {
			    channelSftp.disconnect();
			}
		}
    }

    
    
    public void disconnect() throws IOException {
        if (writer != null) {
            writer.close();
            writer = null;
        }
        if (session != null) {
            session.disconnect();
            session = null;
        }
        if (jSch != null) {
            jSch = null;
        }
        closed = true;
    }

    public void resize(Integer cols, Integer rows, Integer width, Integer height) throws IOException {
        channelShell.setPtySize(cols, rows, width, height);
    }

    public Account getTerminal() {
        return terminal;
    }

    public void setTerminal(Account terminal) {
        this.terminal = terminal;
    }

    public boolean isClosed() {
        return closed;
    }

    public WebSocketSession getWebSocketSession() {
        return webSocketSession;
    }

    public String getHttpSessionId() {
        return httpSessionId;
    }

    public String getClientId() {
        return clientId;
    }

	public boolean isTransfer() {
		return transfer;
	}

	public void setTransfer(boolean transfer) {
		this.transfer = transfer;
	}
    
	public String getUnloginInfo() {
		StringBuilder sb=new StringBuilder();
    	sb.append("\r\nlogout\r\n");
        sb.append("Welcome to Nezha Terminal! \r\n");
    	sb.append("Type `help' to learn how to use Nezha Terminal prompt. \r\n");
    	sb.append("$: ");
    	return sb.toString();
	}

    public String getUnloginInfo(Integer status) {
        /**
         * session 已经结束，更新状态，设置结束时间
         */
        TerminalSessionEntity sessionEntity = (TerminalSessionEntity) TerminalSession.getTerminalAttributeInfo(clientId).get("terminalSessionEntity");
        if (sessionEntity.getStartTime() == null) {
            sessionEntity.setStartTime(DateUtil.getUTCTimeByConfigTimeZone());
        }
        sessionEntity.setStatus(status);
        sessionEntity.setEndTime(DateUtil.getUTCTimeByConfigTimeZone());
        TerminalSessionService terminalSessionService = (TerminalSessionService) TerminalSession.clientAttributeInfo.get("terminalSessionService");
        terminalSessionService.saveOrUpdate(sessionEntity);

        StringBuilder sb = new StringBuilder();
        sb.append("\r\nlogout\r\n");
        sb.append("Connection closed \r\n");
        return sb.toString();
    }
}
