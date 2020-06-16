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

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.google.common.primitives.Bytes;
import com.jcraft.jsch.*;
import com.mchange.lang.ByteUtils;
import com.nis.common.utils.*;
import com.nis.modules.terminal.entity.Account;
import com.nis.modules.terminal.entity.TerminalLog;
import com.nis.modules.terminal.service.TerminalLogService;
import org.apache.commons.net.telnet.TelnetClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

import static com.nis.common.utils.CommonUtils.notEmpty;

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

    public void openTerminal(final int cols, int rows, int width, int height) throws Exception {
        Map<String, String> params = JSONObject.parseObject((String) terminal.getParams(), Map.class);
        if (ToolUtil.isEmpty(params)) {
            params = new HashMap<>();
        }
        this.session = jSch.getSession(params.get("user"), terminal.getAsset().getHost(), terminal.getPort());

        String sshType = params.get("method");
        switch (sshType) {
            case "key": {
                String privateKey = params.get("key");
                String keyPassword = params.get("keyPassword");
                if (notEmpty(privateKey)) {
                    File keyFile = new File(terminal.getPrivateKeyPath());
                    if (!keyFile.exists()) {
                        //将数据库中的私钥写到用户的机器上
                        IOUtils.writeFile(keyFile, new ByteArrayInputStream(privateKey.getBytes()));
                    }
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
                session.setPassword(new String(Base64.getDecoder().decode(params.get("password"))));
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
        sb.append("Login success，use user " + params.get("user") + " \r\n");
    	webSocketSession.sendMessage(new TextMessage(sb.toString()));
        
        new Thread(new Runnable() {
            @Override
            public void run() {
            	TerminalLogService terminalLogService = (TerminalLogService) TerminalSession.clientAttributeInfo.get("terminalLogService");
            	TerminalLog terminalLog=(TerminalLog) TerminalSession.getTerminalAttributeInfo(clientId).get("terminalLog");
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
                        clientMessageHandle(terminalLogService,terminalLog,message);
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
                            	String unloginInfo = getUnloginInfo();
                            	webSocketSession.sendMessage(new TextMessage(unloginInfo));	
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
                	logger.error("TerminalClient error info :",e);
                	TerminalSession.remove(webSocketSession);
                }
            }
        }).start();

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
            	TerminalLog terminalLog=(TerminalLog) TerminalSession.getTerminalAttributeInfo(clientId).get("terminalLog");
                byte[] buffer = new byte[1024 * 4];
                StringBuilder builder = new StringBuilder();
                telnetUser=true;
                try {
                    while (webSocketSession != null && webSocketSession.isOpen()) {
                        Map<String, String> params = JSONObject.parseObject((String) terminal.getParams(), Map.class);
                        if (ToolUtil.isEmpty(params)) {
                            params = new HashMap<>();
                        }
                        String user = params.get("user");

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
                        clientMessageHandle(terminalLogService,terminalLog,message);
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
                            	String unloginInfo = getUnloginInfo();
                            	webSocketSession.sendMessage(new TextMessage(unloginInfo));	
                            }else {
                            	//telnet登录状态修改
                            	if(message.contains("Last login")) {
                            		terminalLog.setCmd("Login");
                            		terminalLog.setStatus(1);
                            		terminalLog.setTime(DateUtil.getUTCTimeByConfigTimeZone());
                            		terminalLog.setUser(user);
                            		terminalLogService.save(terminalLog);
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
                                    String s = new String(Base64.getDecoder().decode(password));
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
                            		String unloginInfo = getUnloginInfo();
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
                            			TerminalSession.remove(webSocketSession);
                            			closed=true;
                            			String unloginInfo = getUnloginInfo();
                                    	webSocketSession.sendMessage(new TextMessage(unloginInfo));
                            		}
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
                	logger.error("TerminalClient error info :",e);
                	TerminalSession.remove(webSocketSession);
                }
            }
        }).start();
    }
    
    /**
     * 处理服务器方向键以及tab操作返回的指令信息 便于做日志记录
     * @param terminalLogService    
     * @param terminalLog       日志信息
     * @param serverResponse    服务器响应内容
     */
    public void clientMessageHandle(TerminalLogService terminalLogService,TerminalLog terminalLog,String serverResponse) {
    	Map terminalAttributeInfo = TerminalSession.getTerminalAttributeInfo(clientId);
    	Integer cursorIndex = (Integer) terminalAttributeInfo.get("cursorIndex");
        boolean cursorFlag = (boolean) terminalAttributeInfo.get("cursorFlag");
        Integer  historyIndex = (Integer) terminalAttributeInfo.get("historyIndex");
    	// 前端每次传给后端服务器的指令字符
    	TextMessage message = (TextMessage) TerminalSession.clientAttributeInfo.get(clientId+"shell");
    	if(ObjectUtils.isNotEmpty(message) && message.asBytes()[0]!=3) {
    		// 处理前端方向键
    		if(message.asBytes()[0]==27) {
        		String operation = ByteUtils.toHexAscii(message.asBytes());
        		if(operation.equals("1B5B44")){
        			// 左方向操作
        			if(cursorIndex!=0) {
        				// 光标在初始位置不可移动
        				cursorIndex--;
        				cursorFlag=true;
        			}else if(TerminalSession.getTerminalMessage(clientId)==null || cursorIndex==TerminalSession.getTerminalMessage(clientId).length()){
        				cursorFlag=false;
        			}
                    TerminalSession.setTerminalAttributeInfo(clientId, "cursorIndex", cursorIndex);
                    TerminalSession.setTerminalAttributeInfo(clientId, "cursorFlag", cursorFlag);
        			return;
        		} else if(operation.equals("1B5B43")) {
        			// 右方向操作
        			if((cursorIndex==0 && (TerminalSession.getTerminalMessage(clientId)==null || TerminalSession.getTerminalMessage(clientId).length()==0))  || cursorIndex==TerminalSession.getTerminalMessage(clientId).length()) {
        				// 光标位置在初始位置 或者已经为当前输入内容最右处
        				cursorFlag=false;
        			}else {
        				cursorIndex++;
        				cursorFlag=true;
        			}
                    TerminalSession.setTerminalAttributeInfo(clientId, "cursorIndex", cursorIndex);
                    TerminalSession.setTerminalAttributeInfo(clientId, "cursorFlag", cursorFlag);
        			return;
        		} else if(operation.equals("1B5B41")||operation.equals("1B5B42")) {
        			// 上方向操作 获取用户之前保存的指令信息
        			String data = terminalResponseHandle(serverResponse);
        			TerminalSession.removeTerminalMessage(clientId);
        			TerminalSession.setTerminalMessage(clientId, data, null);
        			cursorIndex=data.length();
                    TerminalSession.setTerminalAttributeInfo(clientId, "cursorIndex", cursorIndex);
        		} 
    		}else if(message.asBytes()[0]==9) {
    			//处理用户tab键操作
    			String data = terminalResponseHandle(serverResponse);
    			TerminalSession.setTerminalMessage(clientId, data, cursorIndex);
    			cursorIndex+=data.length();
    			TerminalSession.setTerminalAttributeInfo(clientId, "cursorIndex", cursorIndex);
    		}else if(message.getPayload().equals("\r")) {
                Map<String, String> params = JSONObject.parseObject((String) terminal.getParams(), Map.class);
                if (ToolUtil.isEmpty(params)) {
                    params = new HashMap<>();
                }
    			// 服务器执行完命令 保存日志
    			if(!telnetConnect) {
    				String cmd = TerminalSession.getTerminalMessage(clientId);
    				if(StringUtils.isNotEmpty(cmd)) {
    					terminalLog.setCmd(TerminalSession.getTerminalMessage(clientId));
                		terminalLog.setStatus(1);
                		terminalLog.setTime(DateUtil.getUTCTimeByConfigTimeZone());
                		terminalLog.setUser(params.get("user"));
                		terminalLogService.save(terminalLog);
                		TerminalSession.removeTerminalMessage(clientId);
                		TerminalSession.setTerminalAttributeInfo(clientId, "cursorIndex", 0);
                		TerminalSession.clientAttributeInfo.put(clientId+"shell",null);
    				}
    			}
    		}else if(message.asBytes()[0]==127) {
    			// 处理用户删除退格操作
    			boolean flag = TerminalSession.backspaceMessage(clientId,cursorIndex);
    			if(flag) {
    				cursorIndex--;
        			TerminalSession.setTerminalAttributeInfo(clientId, "cursorIndex", cursorIndex);
    			}
    		}else{
    			TerminalSession.setTerminalMessage(clientId, message.getPayload(), cursorIndex);
        		cursorIndex++;
        		TerminalSession.setTerminalAttributeInfo(clientId, "cursorIndex", cursorIndex);
    		}    		
    	}
    }
    
	/**
	 * 处理终端响应信息
	 * @param param
	 * @return
	 */
	public String terminalResponseHandle(String param) {
		byte[] bytes = param.getBytes();
		List<Byte> asList = Bytes.asList(bytes);
		List<Byte> asList2=new ArrayList(asList);
		for(int i=asList2.size()-1;i>=0;i--) {
			if(asList2.get(i)==8){
				asList2.remove(i);
			}else if(asList2.get(i)==13) {
				asList2.remove(i);
			}else if(asList2.get(i)==10) {
				asList2.remove(i);
			}else if(asList2.get(i)==27) {
				asList2.remove(i);
			}else if(asList2.get(i)==91) {
				asList2.remove(i);
			}else if(i!=0&&asList2.get(i-1)==91) {
				asList2.remove(i);
			}else if(asList2.get(i)==7) {
				asList2.remove(i);
			}
		}
		byte[] array = Bytes.toArray(asList2);
		return new String(array);
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
        System.out.println(telnetUser + "----"+telnetConnect+"0---"+params.get("user"));
        if (telnetUser && telnetConnect && StringUtils.isEmpty(params.get("user"))) {
            sb.append(message);
            webSocketSession.sendMessage(new TextMessage(message));
        } else if (StringUtils.isNotEmpty(sb.toString())) {
            params.put("user", sb.toString());
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
}