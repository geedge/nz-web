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

import com.jcraft.jsch.*;
import com.nis.common.utils.CommonUtils;
import com.nis.common.utils.DigestUtils;
import com.nis.common.utils.IOUtils;
import com.nis.common.utils.StringUtils;
import com.nis.modules.terminal.entity.Account;
import static com.nis.common.utils.CommonUtils.notEmpty;

import org.apache.commons.net.telnet.TelnetClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.*;
import java.util.Base64;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

public class TerminalClient {

    private final Logger logger = LoggerFactory.getLogger(TerminalClient.class);

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

    public void openTerminal(final int cols, int rows, int width, int height) throws Exception {
        this.session = jSch.getSession(terminal.getUser(), terminal.getAsset().getHost(), terminal.getPort());
        Constants.SshType sshType = Constants.SshType.getByType(terminal.getAuthType());
        switch (sshType) {
            case SSHKEY:
                if (notEmpty(terminal.getPrivateKey())) {
                    File keyFile = new File(terminal.getPrivateKeyPath());
                    if (!keyFile.exists()) {
                        //将数据库中的私钥写到用户的机器上
                        IOUtils.writeFile(keyFile, new ByteArrayInputStream(terminal.getPrivateKey().getBytes()));
                    }
                    if (notEmpty(terminal.getPwd())) {
                        //设置带口令的密钥
                        jSch.addIdentity(terminal.getPrivateKeyPath(), terminal.getPwd());
                    } else {
                        //设置不带口令的密钥
                        jSch.addIdentity(terminal.getPrivateKeyPath());
                    }
                    UserInfo userInfo = new SshUserInfo();
                    session.setUserInfo(userInfo);
                }
                session.setConfig("StrictHostKeyChecking", "no");
                break;
            case ACCOUNT:
                session.setConfig("StrictHostKeyChecking", "no");
                session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
                session.setPassword(new String(Base64.getDecoder().decode(terminal.getPwd())));
                break;
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
    	sb.append("\r\nConnecting to "+terminal.getAsset().getHost()+":"+terminal.getPort()+"\r\n");
    	sb.append("Connection established. \r\n");
    	sb.append("Login success，use user "+terminal.getUser()+" \r\n");
    	webSocketSession.sendMessage(new TextMessage(sb.toString()));
        
        new Thread(new Runnable() {
            @Override
            public void run() {
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
                        message = new String(message.getBytes(DigestUtils.getEncoding(message)), "UTF-8");
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
                            	System.out.println(session);
                            	StringBuilder sb=new StringBuilder();
                            	sb.append("\r\nlogout\r\n");
                                sb.append("Welcome to Nezha Terminal! \r\n");
                            	sb.append("Type `help' to learn how to use Nezha Terminal prompt. \r\n");
                            	sb.append("$: ");
                            	webSocketSession.sendMessage(new TextMessage(sb.toString()));	
                            }else {
                            	webSocketSession.sendMessage(new TextMessage(message));
                            }
                            if (sendEnter) {
                                String trimMessage = message.replaceAll("\r\n", "");
                                if (CommonUtils.isEmpty(trimMessage)) {
                                    receiveEnd = false;
                                } else {
                                    if (sendBuffer.toString().equals("exit") && trimMessage.equals("logout")) {
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
                } catch (Exception e) {
                }
            }
        }).start();

    }

    public void openTelnetTerminal(final int cols, int rows, int width, int height) throws Exception {
    	TelnetClient telnet = new TelnetClient("VT100");// VT100 VT52 VT220 VTNT ANSI  
        telnet.connect(terminal.getAsset().getHost(), terminal.getPort());  
        this.inputStream=telnet.getInputStream();
        this.outputStream=telnet.getOutputStream();
        this.writer = new BufferedWriter(new OutputStreamWriter(this.outputStream, "UTF-8"));
        
        new Thread(new Runnable() {
            @Override
            public void run() {
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
                        message = new String(message.getBytes(DigestUtils.getEncoding(message)), "UTF-8");
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
                            	System.out.println(session);
                            	StringBuilder sb=new StringBuilder();
                            	sb.append("\r\nlogout\r\n");
                                sb.append("Welcome to Nezha Terminal! \r\n");
                            	sb.append("Type `help' to learn how to use Nezha Terminal prompt. \r\n");
                            	sb.append("$: ");
                            	webSocketSession.sendMessage(new TextMessage(sb.toString()));	
                            }else {
                            	// 防止用户登录日志记录用户输入的密码信息
                            	if(message.matches("Password:.*")) {
                            		TerminalSession.setUnloginMessage("telnetFlag", "1");
                            	}
                            	if(message.replace(" ","").matches(".*wwwlogin:")&&StringUtils.isNotEmpty(terminal.getUser())) {
                            		webSocketSession.sendMessage(new TextMessage(message+terminal.getUser()+"\r\n"));
                            		write(terminal.getUser()+"\r");
                            	}else if(message.matches("Password:.*")&&StringUtils.isBlank(terminal.getUser())){
                            		webSocketSession.sendMessage(new TextMessage("\r\n"+message));
                            	}else if(message.contains("timed out after 60 seconds")) {
                            		webSocketSession.sendMessage(new TextMessage(message+"\r\nConnection closed by foreign host.\r\n$: "));
                            		TerminalSession.remove(webSocketSession);
                            	}else{
                            		webSocketSession.sendMessage(new TextMessage(message));
                            	}
                            }
                            if (sendEnter) {
                                String trimMessage = message.replaceAll("\r\n", "");
                                if (CommonUtils.isEmpty(trimMessage)) {
                                    receiveEnd = false;
                                } else {
                                    if (sendBuffer.toString().equals("exit") && trimMessage.equals("logout")) {
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
                } catch (Exception e) {
                }
            }
        }).start();

    }
    
    
    /**
     * 向ssh终端输入内容
     *
     * @param message
     * @throws IOException
     */
    public void write(String message) throws IOException {
        if (message.equals("\r")) {
            if (closed) {
                disconnect();
            } else {
                sendEnter = true;
            }
        } else {
            sendEnter = false;
            sendBuffer.append(message);
        }

        if (writer != null) {
            writer.write(message);
            writer.flush();
        }
    }

    public void upload(String src, String dst, long fileSize) throws Exception {
        FileInputStream file = new FileInputStream(src);
        ChannelSftp channelSftp = (ChannelSftp) this.session.openChannel("sftp");
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
        //exit
        if (channelSftp != null) {
            channelSftp.exit();
        }
        //disconnect
        if (channelSftp != null) {
            channelSftp.disconnect();
        }
    }

    public void download(String path,HttpServletResponse res) throws Exception {
    	logger.info("TerminalClient download param is {}",path);
        ChannelSftp channelSftp = (ChannelSftp) this.session.openChannel("sftp");
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
        InputStream is = channelSftp.get(fileName);
        res.setHeader("Content-Disposition","attachment; filename="+fileName);
        // 发送给客户端的数据
        OutputStream outputStream = res.getOutputStream();
        byte[] buff = new byte[1024];
        int i = is.read(buff);
        while (i != -1) {
            outputStream.write(buff, 0, i);
            outputStream.flush();
            i = is.read(buff);
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

}