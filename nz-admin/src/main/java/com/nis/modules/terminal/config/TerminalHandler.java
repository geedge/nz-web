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
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
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
    
    /*// 光标位置
    private Integer cursorIndex=0;
    // 判断光标是否被移动过
    private boolean cursorFlag=false;
    
    private Integer historyIndex=0;
    
    private boolean sshLogin=false;
        
    private boolean tabFlag=false;
    
    private StringBuilder terminalCmd;*/
    
    private TerminalLogService terminalLogService;
    //记录日志
	/*TerminalLog terminalLog = null;*/
    
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
        TerminalSession.clientAttributeInfo.put("terminalLogService", terminalLogService);
        // 初始化服务器连接基本属性配置
        Map terminalAttribute = TerminalSession.getTerminalAttributeInfo(uuid);
        terminalAttribute.put("userId", userId);
        
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
            TerminalLog terminalLog = new TerminalLog();
            terminalLog.setAuthType(terminal.getAuthType());
            terminalLog.setHost(terminal.getAsset().getHost());
            terminalLog.setPort(terminal.getPort());
            terminalLog.setProtocol(terminal.getProtocol());
            terminalLog.setUserId(userId);
            terminalLog.setUuid(uuid);
            try {
                getClient(session, terminal);
                int cols = toInt(session.getAttributes().get("cols").toString());
                int rows = toInt(session.getAttributes().get("rows").toString());
                int width = toInt(session.getAttributes().get("width").toString());
                int height = toInt(session.getAttributes().get("height").toString());
                terminalLog.setCmd("Login");
                terminalLog.setUser(terminal.getUser());
                terminalLog.setStatus(1);
                terminalLog.setTime(new Date());
                TerminalSession.setTerminalAttributeInfo(uuid, "terminalLog", terminalLog);
                terminalClient.openTerminal(cols, rows, width, height);
                terminalLogService.save(terminalLog);
            } catch (Exception e) {
            	logger.error("error info :",e);
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
    	// 每次请求获取对应的uuid信息
    	Map<String, Object> attributes = session.getAttributes();
    	uuid = (String) attributes.get("uuid");
        super.handleTextMessage(session, message);
        logger.info("handleTextMessage meesage info : {}",message.getPayload());
        
        /**
         * 优先处理用户前端输入方向内容保存到session中 以便于执行以及日志记录
         */
        Map terminalAttribute = TerminalSession.getTerminalAttributeInfo(uuid);
        boolean sshLogin = (boolean) terminalAttribute.get("sshLogin");
        Integer cursorIndex = (Integer) terminalAttribute.get("cursorIndex");
        boolean cursorFlag = (boolean) terminalAttribute.get("cursorFlag");
        Integer  historyIndex = (Integer) terminalAttribute.get("historyIndex");
               
        try {
            getClient(session, null);
            if (this.terminalClient != null) {
            	// 保存当前输入字符信息  以便client端获取对应的操作字符
            	TerminalSession.clientAttributeInfo.put(uuid+"shell", message);
                terminalClient.write(message.getPayload());
                    
            }else {
            	//处理用户上下左右方向键操作
            	if(message.asBytes()[0]==27) {
            		// 判断用户是在ssh登录输入密码时操作  则直接返回不进行任何操作
            		if(sshLogin) {
            			return;
            		}
            		String operation = ByteUtils.toHexAscii(message.asBytes());
            		if(operation.equals("1B5B44")){
            			// 左方向操作
            			if(cursorIndex!=0) {
            				// 光标在初始位置不可移动
            				cursorIndex--;
            				session.sendMessage(new TextMessage(message.getPayload()));
            				cursorFlag=true;
            			}else if(TerminalSession.getTerminalMessage(uuid)==null || cursorIndex==TerminalSession.getTerminalMessage(uuid).length()){
            				cursorFlag=false;
            			}
                        TerminalSession.setTerminalAttributeInfo(uuid, "cursorIndex", cursorIndex);
                        TerminalSession.setTerminalAttributeInfo(uuid, "cursorFlag", cursorFlag);
            			return;
            		} else if(operation.equals("1B5B43")) {
            			// 右方向操作
            			if((cursorIndex==0 && (TerminalSession.getTerminalMessage(uuid)==null || TerminalSession.getTerminalMessage(uuid).length()==0))  || cursorIndex==TerminalSession.getTerminalMessage(uuid).length()) {
            				// 光标位置在初始位置 或者已经为当前输入内容最右处
            				cursorFlag=false;
            			}else {
            				cursorIndex++;
            				session.sendMessage(new TextMessage(message.getPayload()));
            				cursorFlag=true;
            			}
                        TerminalSession.setTerminalAttributeInfo(uuid, "cursorIndex", cursorIndex);
                        TerminalSession.setTerminalAttributeInfo(uuid, "cursorFlag", cursorFlag);
            			return;
            		} else if(operation.equals("1B5B41")) {
            			// 上方向操作 获取用户之前保存的指令信息
            			// 获取当前字符串位置 将光标移到开头 同时清空信息 补充历史指令信息
            			List<String> list = TerminalSession.userInstructions.get(uuid);
            			if(ObjectUtils.isEmpty(list)||list.size()==historyIndex) {
            				// 用户初进入没有任何历史指令 或者历史指令已经到最后一条位置
            				return;
            			}
            			String historyInstruction = TerminalSession.getHistoryInstruction(uuid, historyIndex+1);
        				if(historyIndex==0) {
        					String TerminalMessage = TerminalSession.getTerminalMessage(uuid);
            				if(StringUtils.isEmpty(TerminalMessage)) {
            					// 用户并无任何输入情况
            					session.sendMessage(new TextMessage(historyInstruction));
            				}else {
            					int positionIndex=0;
            					positionIndex=TerminalMessage.length();
            					if(cursorFlag) {
            						// 存在光标位置移动情况 将光标位置复原情况
            						for(int i=0;i<TerminalMessage.length()-cursorIndex;i++) {
            							byte[] fromHexAscii = ByteUtils.fromHexAscii("1B5B43");
            							session.sendMessage(new TextMessage(fromHexAscii));
            						}
            					}else {
            						// 光标未移动用户使用回退操作情况
            						if(TerminalMessage.length()!=cursorIndex) {
            							positionIndex=cursorIndex;
            						}
            					}
            					for(int i=0;i<positionIndex-1;i++) {
            						session.sendMessage(new TextMessage("\b"));
            					}
            					byte[] fromHexAscii = ByteUtils.fromHexAscii("081b5b4b");
            					session.sendMessage(new TextMessage(fromHexAscii));
            					session.sendMessage(new TextMessage(historyInstruction));
            				}
        				}else {
        					// 无改变--->上一条历史信息    改变---->当前指令信息
        					String prefixInstruction = TerminalSession.getTerminalMessage(uuid);
        					int positionIndex=0;
        					positionIndex=prefixInstruction.length();
        					if(cursorFlag) {
        						// 存在光标位置移动情况 将光标位置复原情况
        						for(int i=0;i<prefixInstruction.length()-cursorIndex;i++) {
        							byte[] fromHexAscii = ByteUtils.fromHexAscii("1B5B43");
        							session.sendMessage(new TextMessage(fromHexAscii));
        						}
        					}else {
        						// 光标未移动用户使用回退操作情况或者用户新增输入操作
        						if(prefixInstruction.length()!=cursorIndex) {
        							positionIndex=cursorIndex;
        						}
        					}
        					for(int i=0;i<positionIndex-1;i++) {
        						session.sendMessage(new TextMessage("\b"));
        					}
        					byte[] fromHexAscii = ByteUtils.fromHexAscii("081b5b4b");
        					session.sendMessage(new TextMessage(fromHexAscii));
        					session.sendMessage(new TextMessage(historyInstruction));
            			}
        				/*String historyInstruction = TerminalSession.getHistoryInstruction(uuid, historyIndex+1);
            			if(historyIndex==0) {
            				String TerminalMessage = TerminalSession.getTerminalMessage(uuid);
            				if(StringUtils.isEmpty(TerminalMessage)) {
            					session.sendMessage(new TextMessage(historyInstruction));
            				}else {
            					for(int i=0;i<TerminalMessage.length()-1;i++) {
            						session.sendMessage(new TextMessage("\b"));
            					}
            					byte[] fromHexAscii = ByteUtils.fromHexAscii("081b5b4b");
            					session.sendMessage(new TextMessage(fromHexAscii));
            					session.sendMessage(new TextMessage(historyInstruction));
            				}
            				if(StringUtils.isNoneBlank(historyInstruction)) {
            					historyIndex++;
            				}
            			}else {
            				if(StringUtils.isNoneBlank(historyInstruction)) {
            					String prefixInstruction = TerminalSession.getHistoryInstruction(uuid, historyIndex);
                				for(int i=0;i<prefixInstruction.length()-1;i++) {
            						session.sendMessage(new TextMessage("\b"));
            					}
            					byte[] fromHexAscii = ByteUtils.fromHexAscii("081b5b4b");
            					session.sendMessage(new TextMessage(fromHexAscii));
            					session.sendMessage(new TextMessage(historyInstruction));
            					historyIndex++;
            				}
            			}*/
        				historyIndex++;
            			TerminalSession.removeTerminalMessage(uuid);
            			TerminalSession.setTerminalMessage(uuid, historyInstruction, null);
            			cursorIndex=historyInstruction.length();
                        TerminalSession.setTerminalAttributeInfo(uuid, "cursorIndex", cursorIndex);
                        TerminalSession.setTerminalAttributeInfo(uuid, "historyIndex", historyIndex);
            			return;
            		} else if(operation.equals("1B5B42")) {
            			// 下方向操作
            			if(historyIndex==0||historyIndex==1) {
            				return;
            			}
            			// 要回溯的历史指令
        				String historyInstruction = TerminalSession.getHistoryInstruction(uuid, historyIndex-1);
        				// 当前指令信息
        				String currentInstruction = TerminalSession.getTerminalMessage(uuid);
        				int positionIndex=0;
    					positionIndex=currentInstruction.length();
        				if(cursorFlag) {
        					// 存在光标位置移动情况 将光标位置复原情况
    						for(int i=0;i<currentInstruction.length()-cursorIndex;i++) {
    							byte[] fromHexAscii = ByteUtils.fromHexAscii("1B5B43");
    							session.sendMessage(new TextMessage(fromHexAscii));
    						}
    						
        				}else {
        					if(currentInstruction.length()!=cursorIndex) {
    							positionIndex=cursorIndex;
    						}
        				}
        				for(int i=0;i<positionIndex-1;i++) {
    						session.sendMessage(new TextMessage("\b"));
    					}
    					byte[] fromHexAscii = ByteUtils.fromHexAscii("081b5b4b");
    					session.sendMessage(new TextMessage(fromHexAscii));
    					session.sendMessage(new TextMessage(historyInstruction));
        				/*if(StringUtils.isEmpty(historyInstruction)) {
        					String prefixInstruction = TerminalSession.getHistoryInstruction(uuid, historyIndex);
            				for(int i=0;i<prefixInstruction.length()-1;i++) {
        						session.sendMessage(new TextMessage("\b"));
        					}
        					byte[] fromHexAscii = ByteUtils.fromHexAscii("081b5b4b");
        					session.sendMessage(new TextMessage(fromHexAscii));
        					String TerminalMessage = TerminalSession.getTerminalMessage(uuid);
        					if(StringUtils.isEmpty(TerminalMessage)) {
        						TerminalMessage="";
        					}
        					session.sendMessage(new TextMessage(TerminalMessage));
        				}else {
        					String prefixInstruction = TerminalSession.getHistoryInstruction(uuid, historyIndex);
            				for(int i=0;i<prefixInstruction.length()-1;i++) {
        						session.sendMessage(new TextMessage("\b"));
        					}
        					byte[] fromHexAscii = ByteUtils.fromHexAscii("081b5b4b");
        					session.sendMessage(new TextMessage(fromHexAscii));
        					session.sendMessage(new TextMessage(historyInstruction));
        				}*/
            			TerminalSession.removeTerminalMessage(uuid);
    					TerminalSession.setTerminalMessage(uuid, historyInstruction, null);
    					cursorIndex=historyInstruction.length();
    					historyIndex--;
            			TerminalSession.setTerminalAttributeInfo(uuid, "cursorIndex", cursorIndex);
                        TerminalSession.setTerminalAttributeInfo(uuid, "historyIndex", historyIndex);
            			return;
            		}
            	}
            	/**
            	 * 如果用户未登录 使用uuid为key保存用户发送的指令信息 等到收到回车指令时才执行对应的操作
            	 */
            	if(sshLogin&&message.getPayload().matches("\\r\\n|\\n|\\r|\\n\\r")) {
            		/**
            		 * 这里进行ssh登录操作
            		 */
            		String sshPwd = TerminalSession.getTerminalMessage(uuid);
            		//将当前执行指令删除 防止用户保存ssh登录密码  同时将光标等属性重置
            		TerminalSession.removeTerminalMessage(uuid);
            		//将sshlogin登录状态外的其它基础配置恢复默认
                    TerminalSession.setTerminalAttributeInfo(uuid, "cursorIndex", 0);
                    TerminalSession.setTerminalAttributeInfo(uuid, "cursorFlag", false);
                    TerminalSession.setTerminalAttributeInfo(uuid, "historyIndex", 0);
            		nezhaMessageHandle(session,sshPwd);
            	}else if(message.getPayload().matches("\\r\\n|\\n|\\r|\\n\\r")) {
            		logger.info("user send terminal to start");
            		// 处理用户空输入 直接回车操作
            		String TerminalMessage = TerminalSession.getTerminalMessage(uuid);
            		if(historyIndex!=0) {
            			if(StringUtils.isEmpty(TerminalMessage)) {
            				TerminalMessage = TerminalSession.getHistoryInstruction(uuid, historyIndex);
            			}
            		}
    				if(StringUtils.isEmpty(TerminalMessage)&&(!sshLogin)) {
    					session.sendMessage(new TextMessage(message.getPayload()+"\n$:"));
    				}else {
    					// 将用户已经执行的指令存储到历史记录里
						if(!sshLogin) {
							TerminalSession.setHistoryInstruction(uuid,TerminalMessage);
						}
						nezhaMessageHandle(session,TerminalMessage);
						//将当前执行指令删除
	            		TerminalSession.removeTerminalMessage(uuid);
	            		//将sshlogin登录状态外的其它基础配置恢复默认
	                    TerminalSession.setTerminalAttributeInfo(uuid, "cursorIndex", 0);
	                    TerminalSession.setTerminalAttributeInfo(uuid, "cursorFlag", false);
	                    TerminalSession.setTerminalAttributeInfo(uuid, "historyIndex", 0);
    				}
            	}else if(message.asBytes()[0]==127&&sshLogin){
            		// ssh登录时处理用户退格键
            		TerminalSession.backspaceMessage(uuid,cursorIndex);
            		return;
            	}else if(message.asBytes()[0]==127) {
            		String TerminalMessage = TerminalSession.getTerminalMessage(uuid);
            		if(historyIndex!=0 && cursorIndex != 0) {
            			if(StringUtils.isEmpty(TerminalMessage)) {
            				TerminalMessage = TerminalSession.getHistoryInstruction(uuid, historyIndex);
                			TerminalSession.setTerminalMessage(uuid, TerminalMessage, null);
                			cursorIndex=TerminalMessage.length();
            			}
            		}
            		if(StringUtils.isEmpty(TerminalMessage)) {
            			return;
            		}
            		// 处理用户退格键
            		byte[] fromHexAscii = ByteUtils.fromHexAscii("081b5b4b");
            		boolean backspaceFlag = TerminalSession.backspaceMessage(uuid,cursorIndex);
            		if(backspaceFlag) {
            			session.sendMessage(new TextMessage(fromHexAscii));
            			if(cursorFlag) {
            				String substring = TerminalMessage.substring(cursorIndex, TerminalMessage.length());
            				session.sendMessage(new TextMessage(substring));
            				// 将光标移到对应位置
            				for(int i =0;i<Integer.valueOf(TerminalMessage.length())-cursorIndex;i++) {
            					fromHexAscii = ByteUtils.fromHexAscii("1B5B44");
                				session.sendMessage(new TextMessage(fromHexAscii));
            				}
            			}
            			cursorIndex--;
            		}
            		TerminalSession.setTerminalAttributeInfo(uuid, "cursorIndex", cursorIndex);
            		return;
            	}else{
            		logger.info("terminal unstart  uuid info {}",uuid);
            		TerminalSession.setTerminalMessage(uuid,message.getPayload(),cursorIndex);
            		//光标位置
            		cursorIndex++;
            		// 不返回用户输入的ssh密码信息
            		if(!sshLogin) {
            			session.sendMessage(new TextMessage(message.getPayload()));
            			if(cursorFlag) { 
            				String TerminalMessage = TerminalSession.getTerminalMessage(uuid);
            				String substring = TerminalMessage.substring(cursorIndex, TerminalMessage.length());
            				session.sendMessage(new TextMessage(substring));
            				// 将光标移到对应位置
            				for(int i =0;i<Integer.valueOf(TerminalMessage.length())-cursorIndex;i++) {
            					byte[] fromHexAscii = ByteUtils.fromHexAscii("1B5B44");
                				session.sendMessage(new TextMessage(fromHexAscii));
            				}
            			}
            		}
            		TerminalSession.setTerminalAttributeInfo(uuid, "cursorIndex", cursorIndex);
            	}
            }
        } catch (Exception e) {
        	// 出现异常将光标信息重置防止下一次无法正常执行命令
        	logger.error("error info :",e);
        	TerminalLog terminalLog = (TerminalLog) TerminalSession.getTerminalAttributeInfo(uuid).get("terminalLog");
        	terminalLog.setUuid(uuid);
        	terminalLog.setUserId((Long)TerminalSession.getTerminalAttributeInfo(uuid).get("userId"));
        	terminalLog.setStatus(0);
        	terminalLog.setTime(new Date());
        	terminalLog.setCmd("Terminal due to abnormal disconnection");
        	terminalLogService.save(terminalLog);
        	TerminalSession.removeTerminalAttributeInfo(uuid);
        	TerminalSession.removeTerminalMessage(uuid+"telnetFlag");
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
        TerminalSession.removeTerminalAttributeInfo(uuid);
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
    	TerminalLog terminalLog = new TerminalLog();
    	terminalLog.setUuid(uuid);
    	terminalLog.setUserId((Long)TerminalSession.getTerminalAttributeInfo(uuid).get("userId"));
    	terminalLog.setAuthType(1);
    	terminalLog.setCmd(message);
    	terminalLog.setProtocol("");
    	terminalLog.setStatus(0);
    	logger.info("nezhaMessageHandle message info {}",message);
    	if(StringUtils.isEmpty(message)) {
    		message="";
    	}
    	// 处理用户ssh登录问题
    	Map terminalAttributeInfo = TerminalSession.getTerminalAttributeInfo(uuid);
    	boolean sshLogin = (boolean) terminalAttributeInfo.get("sshLogin");
    	if(sshLogin) {
    		terminalLog.setProtocol("SSH");
    		terminalLog.setCmd("Login");
    		terminalLog.setHost(TerminalSession.getTerminalMessage(uuid+"ip"));
    		terminalLog.setPort(Integer.valueOf(TerminalSession.getTerminalMessage(uuid+"port")));
    		sshLogin=false;
    		//获取到用户ssh pwd
    		Asset asset=new Asset();
    		asset.setHost(TerminalSession.getTerminalMessage(uuid+"ip"));
    		Account terminal=new Account();
    		terminal.setUser(TerminalSession.getTerminalMessage(uuid+"user"));
    		terminal.setPort(Integer.valueOf(TerminalSession.getTerminalMessage(uuid+"port")));
    		terminal.setAsset(asset);
    		terminal.setAuthType(SshType.ACCOUNT.getType());
    		terminal.setPwd(Base64.getEncoder().encodeToString(message.getBytes()));
    		terminalLog.setUser(TerminalSession.getTerminalMessage(uuid+"user"));
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
				TerminalSession.setTerminalAttributeInfo(uuid, "terminalLog", terminalLog);
			} catch (Exception e) {
				// 如果出现异常连接失败 则将session中client信息清空 
				terminalLog.setStatus(0);
				terminalLog.setTime(new Date());
				sshLogin=false;
				TerminalSession.remove(session);
				session.sendMessage(new TextMessage("\r\nCould not connect to '"+TerminalSession.getTerminalMessage(uuid+"ip")+"' (port "+TerminalSession.getTerminalMessage(uuid+"port")+"): Connection failed."+"\r\n$: "));
			}
    		//连接后（不论成功失败）将基础设置重置
    		TerminalSession.setTerminalAttributeInfo(uuid, "cursorIndex", 0);
            TerminalSession.setTerminalAttributeInfo(uuid, "cursorFlag", false);
            TerminalSession.setTerminalAttributeInfo(uuid,"historyIndex",0);
            TerminalSession.setTerminalAttributeInfo(uuid, "sshLogin", false);
    		terminalLogService.save(terminalLog);
    	}else if(message.startsWith("ssh")) {
    		sshLoginHandle(session,message,sshLogin);
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
    
    public void sshLoginHandle(WebSocketSession session,String message,boolean sshLogin) throws Exception{
    	// 清空之前缓存的ssh登录系信息
    	TerminalSession.removeTerminalMessage(uuid+"user");
    	TerminalSession.removeTerminalMessage(uuid+"ip");
    	TerminalSession.removeTerminalMessage(uuid+"port");
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
				TerminalSession.setTerminalMessage(uuid+"user",user,null);
				TerminalSession.setTerminalMessage(uuid+"ip",ip,null);
				TerminalSession.setTerminalMessage(uuid+"port",port,null);
				sshLogin=true;
				session.sendMessage(new TextMessage("\r\n"+TerminalSession.getTerminalMessage(uuid+"user")+"@"+TerminalSession.getTerminalMessage(uuid+"ip")+"'s password: "));
			}
		}
		TerminalSession.setTerminalAttributeInfo(uuid, "sshLogin", sshLogin);
    }
    
    public void telnetLoginHandle(WebSocketSession session,String message) throws Exception{
    	// 清空之前缓存的telnet登录系信息
    	TerminalSession.removeTerminalMessage(uuid+"telip");
    	TerminalSession.removeTerminalMessage(uuid+"telport");
    	TerminalSession.removeTerminalMessage(uuid+"teluser");
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
		TerminalSession.setTerminalMessage(uuid+"teluser",user,null);
		TerminalSession.setTerminalMessage(uuid+"telip",ip,null);
		TerminalSession.setTerminalMessage(uuid+"telport",port,null);
		//获取到用户ssh pwd
		Asset asset=new Asset();
		asset.setHost(ip);
		Account terminal=new Account();
		terminal.setPort(Integer.valueOf(port));
		terminal.setAsset(asset);
		terminal.setUser(user);
		//连接服务
		TerminalLog terminalLog = new TerminalLog();
		terminalLog.setUuid(uuid);
    	terminalLog.setUserId((Long)TerminalSession.getTerminalAttributeInfo(uuid).get("userId"));
		terminalLog.setAuthType(1);
		terminalLog.setCmd("Login");
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
			TerminalSession.setTerminalAttributeInfo(uuid, "terminalLog", terminalLog);
		} catch (Exception e) {
			// 如果出现异常连接失败 则将session中client信息清空 
			terminalLog.setStatus(0);
			terminalLog.setTime(new Date());
			terminalLogService.save(terminalLog);
			session.sendMessage(new TextMessage("\r\nCould not connect to '"+TerminalSession.getTerminalMessage(uuid+"telip")+"' (port "+TerminalSession.getTerminalMessage(uuid+"telport")+"): Connection failed."+"\r\n$: "));
			TerminalSession.remove(session);
		}
		
    }
}

