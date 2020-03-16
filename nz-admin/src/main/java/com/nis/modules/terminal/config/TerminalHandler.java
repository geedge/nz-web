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


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mchange.lang.ByteUtils;
import com.nis.common.utils.CommonUtils;
import com.nis.common.utils.Constant;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.terminal.config.Constants.SshType;
import com.nis.modules.terminal.entity.Account;
import com.nis.modules.terminal.entity.TerminalLog;
import com.nis.modules.terminal.service.AccountService;
import com.nis.modules.terminal.service.TerminalLogService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    private String headerToken;
    
    private String loginUserName;
    
    private Long userId;
    
    private boolean sshLogin=false;
    
    private boolean tabFlag=false;
    
    private StringBuilder terminalCmd;
    
    private TerminalLogService terminalLogService;
    //记录日志
	TerminalLog terminalLog = null;
    
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
        
        RedisTemplate redisTemplate = (RedisTemplate) applicationContext.getBean("redisTemplate");
        redisTemplate.setValueSerializer(RedisSerializer.string());
        //判断用户传入的token是否为项目获取的token信息 
        ValueOperations<String, String>  ops = redisTemplate.opsForValue();
        String value = ops.get(token);
        if(!CommonUtils.notEmpty(value)) {
        	session.sendMessage(new TextMessage("Token error,Please log in to the system first"));
        	session.close();
        	return;
        }
        String json= new String(Base64.getDecoder().decode(value),"UTF-8");
        Map<String,String> map=JSON.parseObject(json, HashMap.class);
        SysUserService sysUserService = (SysUserService) applicationContext.getBean("sysUserService");
        String loginUserName=map.get("username");
        SysUserEntity user = sysUserService.getOne(new QueryWrapper<SysUserEntity>()
				.eq("username", loginUserName));
        userId = user.getUserId();
        
        // 创建登录服务器日志记录相关内容
        terminalLogService=(TerminalLogService) applicationContext.getBean("terminalLogService");
        if(terminalLog==null) {
        	terminalLog=new TerminalLog();
        }
        
        terminalLog.setUserId(userId);
        
        //判断当前连接的数量 防止用户连接过多窗口
        SysConfigService SysconfigService = (SysConfigService) applicationContext.getBean("sysConfigService");
        String maxNum = SysconfigService.getValue(Constant.MAX_TERMINAL_NUM);
        //默认20个
        Integer num=20;
        if(StringUtils.isNotEmpty(maxNum)) {
        	num=Integer.valueOf(maxNum);
        }
        List<TerminalClient> allTerminalClient = TerminalSession.getAllTerminalClient(token);
        if(allTerminalClient.size()>num) {
        	session.sendMessage(new TextMessage("Unable to connect at the maximum number of connections"));
        	session.close();
        	return;
        }
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
            
            terminalLog.setAuthType(terminal.getAuthType());
            terminalLog.setHost(terminal.getAsset().getHost());
            terminalLog.setPort(terminal.getPort());
            terminalLog.setProtocol(terminal.getProtocol());
            try {
                getClient(session, terminal);
                int cols = toInt(session.getAttributes().get("cols").toString());
                int rows = toInt(session.getAttributes().get("rows").toString());
                int width = toInt(session.getAttributes().get("width").toString());
                int height = toInt(session.getAttributes().get("height").toString());
                terminalClient.openTerminal(cols, rows, width, height);
                terminalLog.setCmd("First login");
                terminalLog.setUser(terminal.getUser());
                terminalLog.setStatus(1);
                terminalLog.setTime(new Date());
                terminalLogService.save(terminalLog);
            } catch (Exception e) {
            	logger.error(e.getMessage());
            	e.printStackTrace();
            	if (e.getMessage().toLowerCase().contains("userauth fail")) {
            		session.sendMessage(new TextMessage(" Login fail "));
                } else if (e.getLocalizedMessage().replaceAll("\\s+", "").contentEquals("Operationtimedout")) {
                    session.sendMessage(new TextMessage("Sorry! Connect timed out, please try again. \r\n"));
                } else {
                    session.sendMessage(new TextMessage("Sorry! Operation error, please try again. \r\n"));
                }
            	terminalLog.setStatus(0);
                terminalLog.setTime(new Date());
                terminalLog.setUser(StringUtils.isNotBlank(terminal.getUser())?terminal.getUser():"");
                terminalLogService.save(terminalLog);
                terminalClient.disconnect();
                TerminalSession.remove(session);
                //           session.close();
                unloginUserHandle(session);
            }
        }else {
        	//如果用户没有传入asset信息 则友情提示用户
        	unloginUserHandle(session);
        	logger.info("user stateless access server");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        logger.info("handleTextMessage meesage info : {}",message.getPayload());
        if(CommonUtils.isEmpty(terminalCmd)) {
        	terminalCmd=new StringBuilder();
        }
        try {
            getClient(session, null);
            if (this.terminalClient != null) {
                if (!terminalClient.isClosed()) {
                	if(message.getPayload().equals("\r")) {
                		terminalLog.setCmd(terminalCmd.toString());
                		terminalCmd.delete(0,terminalCmd.length());
                		TerminalSession.removeUnloginMessage("telnetFlag");
                	}else if(message.asBytes()[0]==9){
                		tabFlag=true;
                		terminalCmd.append(" \t");
                	}else if(tabFlag){
                		terminalLog.setCmd(terminalCmd.toString());
                		terminalCmd.delete(0,terminalCmd.length());
                	}else {
                		terminalCmd.append(message.getPayload());
                	}
                    terminalClient.write(message.getPayload());
                    if(message.getPayload().equals("\r")||tabFlag) {
                    	tabFlag=false;
                		terminalLog.setStatus(1);
                        terminalLog.setTime(new Date());
                        terminalLogService.save(terminalLog);
                	}
                }
            }else {
            	/**
            	 * 如果用户未登录 使用uuid为key保存用户发送的指令信息 等到收到回车指令时才执行对应的操作
            	 */
            	if(sshLogin&&message.getPayload().matches("\\r\\n|\\n|\\r|\\n\\r")) {
            		nezhaMessageHandle(session,TerminalSession.getUnloginMessage(uuid));
            	}else if(message.getPayload().matches("\\r\\n|\\n|\\r|\\n\\r")) {
            		logger.info("user send terminal to start");
//            		session.sendMessage(new TextMessage(TerminalSession.getUnloginMessage(uuid)+message.getPayload()));
            		// 处理用户空输入 直接回车操作
    				if(StringUtils.isEmpty(TerminalSession.getUnloginMessage(uuid))&&(!sshLogin)) {
    					session.sendMessage(new TextMessage(message.getPayload()+"\n$:"));
    				}else {
    					nezhaMessageHandle(session,TerminalSession.getUnloginMessage(uuid));
    				}
            		//执行完当前指令后将指令删除
            		TerminalSession.removeUnloginMessage(uuid);
            	}else if(message.asBytes()[0]==127&&sshLogin){
            		// ssh登录时处理用户退格键
            		TerminalSession.backspaceMessage(uuid);
            		return;
            	}else if(message.asBytes()[0]==127) {
            		// 处理用户退格键
            		byte[] fromHexAscii = ByteUtils.fromHexAscii("081b5b4b");
            		boolean backspaceFlage = TerminalSession.backspaceMessage(uuid);
            		if(backspaceFlage) {
            			session.sendMessage(new TextMessage(fromHexAscii));
            		}
            		return;
            	}else{
            		logger.info("terminal unstart  uuid info {}",uuid);
            		TerminalSession.setUnloginMessage(uuid,message.getPayload());
            		// 不返回用户输入的ssh密码信息
            		if(!sshLogin) {
            			session.sendMessage(new TextMessage(message.getPayload()));
            		}
            	}
            }
        } catch (Exception e) {
        	terminalLog.setStatus(0);
        	terminalLog.setTime(new Date());
        	terminalLogService.save(terminalLog);
        	session.sendMessage(new TextMessage(e.getMessage()+"\r\n"));
            session.sendMessage(new TextMessage("Sorry! Nezha Terminal was closed, please try again. \r\n"));
            terminalClient.disconnect();
            TerminalSession.remove(session);
 //           session.close();
            unloginUserHandle(session);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        super.handleTransportError(session, exception);
        this.closeTerminal(session);
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

    public void nezhaMessageHandle(WebSocketSession session, String message) throws Exception {
    	terminalLog.setAuthType(1);
    	terminalLog.setCmd(message);
    	terminalLog.setProtocol("");
    	terminalLog.setStatus(0);
    	logger.info("nezhaMessageHandle message info {}",message);
    	if(StringUtils.isEmpty(message)) {
    		message="";
    	}
    	// 处理用户ssh登录问题
    	if(sshLogin) {
    		terminalLog.setProtocol("SSH");
    		terminalLog.setCmd("SSH Login");
    		terminalLog.setHost(TerminalSession.getUnloginMessage(uuid+"ip"));
    		terminalLog.setPort(Integer.valueOf(TerminalSession.getUnloginMessage(uuid+"port")));
    		sshLogin=false;
    		//获取到用户ssh pwd
    		Asset asset=new Asset();
    		asset.setHost(TerminalSession.getUnloginMessage(uuid+"ip"));
    		Account terminal=new Account();
    		terminal.setUser(TerminalSession.getUnloginMessage(uuid+"user"));
    		terminal.setPort(Integer.valueOf(TerminalSession.getUnloginMessage(uuid+"port")));
    		terminal.setAsset(asset);
    		terminal.setAuthType(SshType.ACCOUNT.getType());
    		terminal.setPwd(Base64.getEncoder().encodeToString(message.getBytes()));
    		terminalLog.setUser(TerminalSession.getUnloginMessage(uuid+"user"));
    		//连接服务
    		try {
				getClient(session, terminal);
				int cols = toInt(session.getAttributes().get("cols").toString());
				int rows = toInt(session.getAttributes().get("rows").toString());
				int width = toInt(session.getAttributes().get("width").toString());
				int height = toInt(session.getAttributes().get("height").toString());
				terminalClient.openTerminal(cols, rows, width, height);
				terminalLog.setStatus(1);
				terminalLog.setTime(new Date());
			} catch (Exception e) {
				// 如果出现异常连接失败 则将session中client信息清空 
				terminalLog.setStatus(0);
				terminalLog.setTime(new Date());
				sshLogin=false;
				TerminalSession.remove(session);
				session.sendMessage(new TextMessage("\r\nCould not connect to '"+TerminalSession.getUnloginMessage(uuid+"ip")+"' (port "+TerminalSession.getUnloginMessage(uuid+"port")+"): Connection failed."+"\r\n$: "));
			}
    		terminalLogService.save(terminalLog);
    	}else if(message.startsWith("ssh")) {
    		sshLoginHandle(session,message);
    	}else if(message.startsWith("telnet")){
    		telnetLoginHandle(session,message);
    	}else if(message.equals("help")) {
    		session.sendMessage(new TextMessage("\r\n$: Internal Commands:\r\n" + 
    				"ssh: Connects to a host using the SSH protocol.\r\n" + 
    				"telnet: Connects to a host using the TELNET protocol.\r\n" + 
    				"set: Sets options.\r\n" + 
    				"clear: Clears the screen/address/command history.\r\n" + 
    				"help: Displays this help. 'help' does the same.\r\n" + 
    				"quit: Quits Local Shell. 'quit' does the same.\r\n" +
    				"$: "));
    	}else if(message.matches("help\\s+ssh")) {
    		session.sendMessage(new TextMessage("\r\n$: NAME \r\n" + 
    				"\n ssh - connects to a host using the SSH protocol. \r\n" + 
    				"SYNOPSYS \r\n" + 
    				"\n ssh user@host[ port] \r\n" + 
    				"OPTIONS \r\n" + 
    				"\n user Indicates the user's login name.\r\n" + 
    				"\n pass If pass is definded, use password authentication.\r\n" + 
    				"\n host Indicates the name, alias, or Internet address of the \r\n" + 
    				"\n remote host.\r\n" + 
    				"\n port Indicates a port number (address of an application).\r\n" + 
    				"\n If the port is not specified, the default ssh port (22) is used.\r\n"+
    				"$: "));
    	}else if(message.matches("help\\s+telnet")) {
    		session.sendMessage(new TextMessage("\r\n$: NAME \r\n" + 
    				"\n telnet - connects to a host using the TELNET protocol. \r\n" + 
    				"SYNOPSYS \r\n" + 
    				"\n telnet [user@]host [port] \r\n" + 
    				"OPTIONS \r\n" + 
    				"\n host Indicates the name, alias, or Internet address of the \r\n" + 
    				"\n remote host.\r\n" + 
    				"\n port Indicates a port number (address of an application).\r\n" + 
    				"\n If the port is not specified, the default telnet port (23) is used.\r\n"+
    				"$: "));
    	}else if(message.matches("help\\s+clear")) {
    		session.sendMessage(new TextMessage("\r\n$: NAME\n" + 
    				"clear - clears the screen \r\n"+
    				"$: "));
    	}else if(message.matches("help\\s+quit")) {
    		session.sendMessage(new TextMessage("\r\n$: NAME \r\n" + 
    				"\n quit - quits application or connection. \r\n" + 
    				"DESCRIPTION \r\n" + 
    				"\n When a connection is not establish, quits application.\r\n" + 
    				"\n Otherwise, quits connection. No parameters are required.\r\n" + 
    				"\n exit does the same. \r\n" + 
    				"\n cf) Press Ctrl + Alt + ] to fall back to Xshell prompt \r\n" + 
    				"\n whlie connection is alive. \r\n"+
    				"$: "));
    	}else if(message.equals("quit")) {
    		session.close();
    	}else if(message.equals("clear")){
    		byte[] fromHexAscii = ByteUtils.fromHexAscii("1b5b333b4a1b5b481b5b324a");
    		session.sendMessage(new TextMessage(fromHexAscii));
    		session.sendMessage(new TextMessage("$: "));
    	}else {
    		// 其它未知指令提示信息
    		session.sendMessage(new TextMessage("\r\n$: command not found...\r\n" + 
    				"Type `help' to learn how to use Nezha Terminal prompt. \r\n"+
    				"$: "));
    	}
    }
    
    public void unloginUserHandle(WebSocketSession session) throws Exception{
    	StringBuilder sb=new StringBuilder();
        sb.append("Welcome to Nezha Terminal! \r\n");
    	sb.append("Type `help' to learn how to use Nezha Terminal prompt. \r\n");
    	sb.append("$: ");
    	session.sendMessage(new TextMessage(sb.toString()));		
    }
    
    public void sshLoginHandle(WebSocketSession session,String message) throws Exception{
    	// 清空之前缓存的ssh登录系信息
    	TerminalSession.removeUnloginMessage(uuid+"user");
    	TerminalSession.removeUnloginMessage(uuid+"ip");
    	TerminalSession.removeUnloginMessage(uuid+"port");
    	String userInfo=null;
		String port=null;
		String user=null;
		String ip=null;
		String[] split = message.split("\\s");
		for(int i =0;i<split.length;i++) {
			if(i==1) {
				userInfo = split[i];
			}else if(i==2) {
				port=split[i];
			}
		}
		if(StringUtils.isNotEmpty(userInfo)&&userInfo.contains("@")) {
			String[] split2 = userInfo.split("@");
			for(int i=0;i<split2.length;i++) {
				if(i==0) {
					user=split2[i];
				}else if(i==1){
					ip=split2[i];
				}
			}
		}
		if(StringUtils.isEmpty(user)||StringUtils.isEmpty(ip)) {
			sshLogin=false;
			session.sendMessage(new TextMessage("\r\nUnable to resolve host Connection failed.\r\nType `help' to learn how to use Xshell prompt.\r\n$: "));
		}else {
			if(StringUtils.isEmpty(port)) {
				port="22";
			}
			//校验端口ip有效性
			if(!(CommonUtils.checkIp(ip)&&CommonUtils.checkPort(port))) {
				session.sendMessage(new TextMessage("\r\nUnable to resolve host Connection failed.\r\nType `help' to learn how to use Xshell prompt.\r\n$: "));
			}else {
				//将用户登录信息存储到当前会话中 记录标识符等待用户输入登录密码完成ssh登录
				List<String> sshLoginInfo =new ArrayList<String>();
				sshLoginInfo.add(user);
				sshLoginInfo.add(ip);
				sshLoginInfo.add(port);
				TerminalSession.setUnloginMessage(uuid+"user",user);
				TerminalSession.setUnloginMessage(uuid+"ip",ip);
				TerminalSession.setUnloginMessage(uuid+"port",port);
				sshLogin=true;
				session.sendMessage(new TextMessage("\r\n"+TerminalSession.getUnloginMessage(uuid+"user")+"@"+TerminalSession.getUnloginMessage(uuid+"ip")+"'s password: "));
			}
		}
    }
    
    public void telnetLoginHandle(WebSocketSession session,String message) throws Exception{
    	// 清空之前缓存的telnet登录系信息
    	TerminalSession.removeUnloginMessage(uuid+"telip");
    	TerminalSession.removeUnloginMessage(uuid+"telport");
    	TerminalSession.removeUnloginMessage(uuid+"teluser");
		String port=null;
		String ip=null;
		String userInfo=null;
		String user=null;
		String[] split = message.split("\\s");
		for(int i =0;i<split.length;i++) {
			if(i==1) {
				userInfo = split[i];
			}else if(i==2) {
				port=split[i];
			}
		}
		if(StringUtils.isNotEmpty(userInfo)&&userInfo.contains("@")) {
			String[] split2 = userInfo.split("@");
			for(int i=0;i<split2.length;i++) {
				if(i==0&&split2.length==2) {
					user=split2[i];
				}else if(i==1&&split2.length==2){
					ip=split2[i];
				}else if(i==0&&split2.length==1) {
					ip=split2[i];
				}
			}
		}else {
			ip=split[1];
		}
		if(StringUtils.isBlank(ip)||!CommonUtils.checkIp(ip)) {
			session.sendMessage(new TextMessage("\r\nPlease check your Ip is correct \r\n$: "));
			return;
		}
		if(StringUtils.isBlank(port)) {
			port="23";
		}
		if(!CommonUtils.checkPort(port)) {
			session.sendMessage(new TextMessage("\r\nPlease check your port is correct \r\n$: "));
			return;
		}
		if(StringUtils.isBlank(user)) {
			user="";
		}
		TerminalSession.setUnloginMessage(uuid+"teluser",user);
		TerminalSession.setUnloginMessage(uuid+"telip",ip);
		TerminalSession.setUnloginMessage(uuid+"telport",port);
		//获取到用户ssh pwd
		Asset asset=new Asset();
		asset.setHost(ip);
		Account terminal=new Account();
		terminal.setPort(Integer.valueOf(port));
		terminal.setAsset(asset);
		terminal.setUser(user);
		//连接服务
		terminalLog.setAuthType(1);
		terminalLog.setCmd("Telnet Login");
		terminalLog.setHost(ip);
		terminalLog.setPort(Integer.valueOf(port));
		terminalLog.setProtocol("TELNET");
		try {
			getClient(session, terminal);
			int cols = toInt(session.getAttributes().get("cols").toString());
			int rows = toInt(session.getAttributes().get("rows").toString());
			int width = toInt(session.getAttributes().get("width").toString());
			int height = toInt(session.getAttributes().get("height").toString());
			terminalClient.openTelnetTerminal(cols, rows, width, height);
			terminalLog.setStatus(1);
			terminalLog.setTime(new Date());
			terminalLogService.save(terminalLog);
		} catch (Exception e) {
			// 如果出现异常连接失败 则将session中client信息清空 
			terminalLog.setStatus(0);
			terminalLog.setTime(new Date());
			terminalLogService.save(terminalLog);
			sshLogin=false;
			TerminalSession.remove(session);
			session.sendMessage(new TextMessage("\r\nCould not connect to '"+TerminalSession.getUnloginMessage(uuid+"ip")+"' (port "+TerminalSession.getUnloginMessage(uuid+"port")+"): Connection failed."+"\r\n$: "));
		}
		
    }
}

