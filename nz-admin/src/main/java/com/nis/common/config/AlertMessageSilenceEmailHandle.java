package com.nis.common.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.dao.AlertNotifiScriptDao;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.AlertNotificationScript;
import com.nis.modules.alert.entity.AlertNotifyLog;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.alert.service.AlertNotifyLogService;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.alert.service.AlertSilenceService;
import com.nis.modules.sys.dao.SysUserNotificationDao;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.entity.SysUserNotification;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserService;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.log.Log;
import freemarker.cache.StringTemplateLoader;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.mail.MessagingException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

@Component
public class AlertMessageSilenceEmailHandle implements CommandLineRunner {

	private static final Log log = Log.get();

    private static final ExecutorService executorService = Executors.newCachedThreadPool();
   
    @Autowired
	private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private AlertSilenceService alertSilenceService;
    
    @Autowired
    private AlertMessageService alertMessageService;
    
    @Autowired
    private SysConfigService configService;
    
    @Autowired
    private AlertRuleService alertRuleService;
    
    @Autowired
    private SysUserService sysUserService;
    
    @Autowired
    private SysUserNotificationDao userNotificationDao;

    @Autowired
    private AlertNotifiScriptDao alertNotifiScriptDao;
    
    @Autowired
    private AlertNotifyLogService alertNotifyLogService;
    
    @Autowired
    private MailUtil mailUtil;
    
    private Template template;
    
    @Override
    public void run(String... args) throws RuntimeException {
    	
		executorService.execute(new Runnable() {
			
			@Override
			public void run() {
				log.debug("告警静默匹配规则线程运行");
				while(true) {
					// redis读取 alert:message:recv数据
				    Object obj = redisTemplate.opsForList().rightPop(Constant.ALERT_MESSAGE_RECV);
				    if(ObjectUtil.isEmpty(obj)) {
						//log.debug("缓存中无告警消息入库");
						continue;
					}
				    log.debug("缓存中接收到的告警数据信息为："+JSON.toJSONString(obj));
				    // 告警消息集合
		            List<AlertMessageEntity> recvs = null;
		            try {
		            	recvs = JSONObject.parseArray(obj.toString(), AlertMessageEntity.class);
		            } catch(JSONException e) {
		                log.error("从redis中取告警消息转化失败，错误信息：" +e + " 转化参数是：" + obj.toString());
		            }
					
					// 最终入库的数据
					List<AlertMessageEntity> entities = new ArrayList<AlertMessageEntity>(); 
					
					// 当前激活状态的告警消息
				    List<AlertMessageEntity> pendingAlert = alertMessageService.list(new QueryWrapper<AlertMessageEntity>().lambda().eq(AlertMessageEntity::getState, 1));
				    Map<String, AlertMessageEntity> alertHashKeyAndEntityMap = new HashMap<>();
				    for (AlertMessageEntity entity : pendingAlert) {
					    alertHashKeyAndEntityMap.put(entity.getHashKey(), entity);
				    }
					
					// 接收告警的当前时间
				    Date currentTime = DateUtil.getUTCTimeByConfigTimeZone();
					//获取内置告警规则
					AlertRuleEntity buildRule = alertRuleService.getOne(new QueryWrapper<AlertRuleEntity>().lambda().eq(AlertRuleEntity::getBuildIn, 1));
				    Integer buildRuleId = buildRule.getId();
					
					// redis读取 alert:message:data数据
					Map<Object, Object> silences = redisTemplate.opsForHash().entries(Constant.ALERT_MESSAGE_SILENCE);
					
					for(AlertMessageEntity alertRecv : recvs) {
						try {
							// 匹配静默规则
							boolean checkMatchSilence = alertSilenceService.checkMatchSilence(alertRecv);
							log.debug("静默是否匹配 ："+checkMatchSilence +"告警规则id为："+alertRecv.getRuleId());
							// 匹配上 判断数据是否有缓存
							if(checkMatchSilence) {
								AlertMessageEntity alertCache = JSONObject.parseObject((String)silences.get(alertRecv.getHashKey()),AlertMessageEntity.class);
								log.debug("缓存中静默规则匹配 值为 ："+JSON.toJSONString(alertCache));
								if(ObjectUtil.isEmpty(alertCache)) {
									// 无缓存则 判断库表是否有值  更新状态 加到缓存中去
									AlertMessageEntity alertMessageDb = alertHashKeyAndEntityMap.get(alertRecv.getHashKey());
									log.debug("数据库中的告警数据为："+JSON.toJSONString(alertMessageDb));
									if(ObjectUtil.isEmpty(alertMessageDb)) {
										alertRecv.setState(4);
										entities.add(alertRecv);
									}else {
										// 修改数据库中的状态
										if(!alertMessageDb.getState().equals(4)) {
											alertMessageDb.setState(4);
											entities.add(alertMessageDb);
										}
									}
									// 存入缓存
									redisTemplate.opsForHash().put(Constant.ALERT_MESSAGE_SILENCE, alertRecv.getHashKey(), JSON.toJSONString(alertRecv));
								}else {
									continue;
								}
							}else {
								// 未匹配静默 则判断状态 新增、持续、失效告警
								// 如果是内置告警直接入库
							    if (buildRuleId.equals(alertRecv.getRuleId())) {
							    	log.debug("新增内置规则数据直接入库");
								    entities.add(alertRecv);
							    } else {
								    Date endAt = alertRecv.getEndAt();
								    // 当前时间比结束时间早 告警激活入库
								    if (currentTime.before(endAt)) {
								    	log.debug("新增或持续告警信息 唯一标识为："+alertRecv.getHashKey());
									    // 已存在告警则不保存入库
									    if (alertHashKeyAndEntityMap.get(alertRecv.getHashKey()) != null) continue;
									    alertRecv.setState(1);
									    alertRecv.setEndAt(null);
									    entities.add(alertRecv);
								    } else {
								    	log.debug("失效告警信息 唯一标识为："+alertRecv.getHashKey());
									    // 当前时间比结束时间晚 告警失效
									    AlertMessageEntity expiredAlert = alertHashKeyAndEntityMap.get(alertRecv.getHashKey());
									    // 消息为空或者消息已经过期则不进行操作
									    if(expiredAlert == null || expiredAlert.getState().equals(2)) continue;
									    expiredAlert.setState(2);
									    expiredAlert.setEndAt(alertRecv.getEndAt());
									    entities.add(expiredAlert);
								    }
							    }
							}
						}catch(JSONException e) {
							e.printStackTrace();
							log.error("取出redis中接收告警信息 转换异常："+e.getMessage());
							continue;
						}
					}
					
					if(ObjectUtils.isNotEmpty(entities)) {
						alertMessageService.saveOrUpdateBatch(entities);
						log.debug("入库告警信息为："+JSON.toJSONString(entities));
						//新增以及失效的告警信息存入邮件发送缓存中
					    redisTemplate.opsForList().leftPush(Constant.ALERT_MESSAGE_NOTIFY, JSON.toJSONString(entities));
					}else {
						log.debug("暂无告警消息入库");
					}
				}
			}
		});
    	
		executorService.execute(new Runnable() {
			
			@Override
			public void run() {
				log.debug("告警信息发送线程运行");
				while(true) {
					try {
			            // 加载模板文件  是否匹配silence 发送告警通知  记录通知结果
			            template = this.getTemplate();
			            this.execAlertMessage();
			        }catch (ParseException e) {
			            log.error("告警通知异常",e);
			        } catch (MalformedTemplateNameException e) {
			            log.error("告警通知异常",e);
			        } catch (TemplateNotFoundException e) {
			            log.error("告警通知异常",e);
			        } catch (IOException e) {
			            log.error("告警通知异常",e);
			        }
				}
			}
			
			private void execAlertMessage()  {
		        ListOperations<String, Object> ops = redisTemplate.opsForList();
		        while(true) {
		        	Object obj = ops.rightPop(Constant.ALERT_MESSAGE_NOTIFY);
		            if (ToolUtil.isEmpty(obj)) {
		                continue;
		            }
		            // 告警消息集合
		            List<AlertMessageEntity> entities = null;
		            try {
		                entities = JSONObject.parseArray(obj.toString(), AlertMessageEntity.class);
		            } catch (JSONException e) {
		                log.error("从redis中取告警消息转化失败，错误信息：" +e + " 转化参数是：" + obj.toString());
		            }
		            log.info("取出告警消息，准备邮件通知：" + JSONObject.toJSONString(entities));
		            if(ObjectUtil.isEmpty(entities)){
		                continue;
		            }
		            
		            // 判断是否匹配静默规则
		            for(int i=entities.size()-1;i>=0;i--) {
		            	AlertMessageEntity entity = entities.get(i);
		            	boolean checkMatchSilence = alertSilenceService.checkMatchSilence(entity);
		            	if(checkMatchSilence) {
		            		entities.remove(i);
		            	}
		            }
		            
		            // 匹配完静默判断是否还有告警数据
		            if(CollectionUtils.isEmpty(entities)){
		                continue;
		            }
		            
		            // 规则及通知人信息
		            List<AlertRuleEntity> ruleList = alertRuleService.list();
		            Map<Integer, String> ruleIdAndReceiver = ruleList.stream().filter(entity -> ToolUtil.isNotEmpty(entity.getReceiver())).collect(Collectors.toMap(AlertRuleEntity::getId, AlertRuleEntity::getReceiver));
		
		            // 用户及邮箱信息
		            List<SysUserEntity> userList = sysUserService.list();
		            Map<Long, String> userIdAndEmail = userList.stream().filter(sysUserEntity -> ToolUtil.isNotEmpty(sysUserEntity.getEmail())).collect(Collectors.toMap(SysUserEntity::getUserId, SysUserEntity::getEmail));
		
		            // 用户和关联脚本信息
		            List<SysUserNotification> userNotificationList = userNotificationDao.selectSysUserNotificationInfo();
		            Map<Integer, List<SysUserNotification>> userIdAndNotificationMap = userNotificationList.stream().collect(Collectors.groupingBy(SysUserNotification::getUserId));
		
		            // 脚本信息
		            List<AlertNotificationScript> scriptList = alertNotifiScriptDao.selectList(null);
		            Map<Integer, String> scriptIdAndFilePathMap = scriptList.stream().collect(Collectors.toMap(AlertNotificationScript::getId, AlertNotificationScript::getFilePath));
		            
		            Map<Integer,List<AlertNotifyLog>> alertNotifyLogMap = new HashMap<Integer,List<AlertNotifyLog>>();
		            for (AlertMessageEntity messageEntity : entities) {
		                String receiver = ruleIdAndReceiver.get(messageEntity.getRuleId());
		                // 通知人不为空 发件
		                if(StringUtils.isNotEmpty(receiver)){
		                			                	
		                    log.info("邮件关联用户，进行邮件通知");
		                    List<String> toSendEmails = new ArrayList<>();
		
		                    // 用户关联脚本
		                    List<SysUserNotification> userNotifications = new ArrayList<>();
		
		                    // 发送记录
		                    List<AlertNotifyLog> alertNotifyLogs = new ArrayList<AlertNotifyLog>();
		                    
		                    Arrays.asList(receiver.split(",")).stream().forEach(receiverId -> {
		                        String email = userIdAndEmail.get(Long.valueOf(receiverId));
		                        if(StringUtils.isNotEmpty(email)){
		                            toSendEmails.add(email);
		                        }
		
		                        AlertNotifyLog alertNotifyLog = new AlertNotifyLog();
		                        alertNotifyLog.setMessageId(messageEntity.getId());
		                        alertNotifyLog.setMessageState(messageEntity.getState());
		                        alertNotifyLog.setMethod(email);
		                        alertNotifyLog.setUserId(Long.valueOf(receiverId));
		                        alertNotifyLog.setTime(DateUtil.getUTCTimeByConfigTimeZone());
		                        alertNotifyLogs.add(alertNotifyLog);
		                        alertNotifyLogMap.put(messageEntity.getId(), alertNotifyLogs);
		                        
		                        // 通过关联用户
		                        List<SysUserNotification> sysUserNotifications = userIdAndNotificationMap.get(Integer.valueOf(receiverId));
		                        if(CollectionUtils.isNotEmpty(sysUserNotifications)){
		                            userNotifications.addAll(sysUserNotifications);
		                        }
		                    });
		
		                    log.info("需要发送的邮箱 -> {}",JSON.toJSONString(toSendEmails));
		                    String state = "UNKNOWN";
		                    switch (messageEntity.getState()) {
		                        case 1:state = "Pending";break;
		                        case 2:state = "Expired";break;
		                    }
		                    
		                    if (ToolUtil.isNotEmpty(toSendEmails)) {
		                        try {
		                        	mailUtil.sendHTML(toSendEmails, state + " : " + messageEntity.getSummary(), this.sendMail(messageEntity));
		                        	List<AlertNotifyLog> list = alertNotifyLogMap.get(messageEntity.getId());
		                        	if(ObjectUtil.isNotEmpty(list)) {
		                        		for(AlertNotifyLog log : list) {
		                        			log.setState(1);
		                        		}
		                        	}
		                        } catch (MessagingException | IOException | TemplateException e) {
		                        	List<AlertNotifyLog> list = alertNotifyLogMap.get(messageEntity.getId());
		                        	if(ObjectUtil.isNotEmpty(list)) {
		                        		for(AlertNotifyLog log : list) {
		                        			log.setState(0);
		                        			log.setErrorMsg(e.getMessage());
		                        		}
		                        	}
		                            log.error("邮件发送任务异常",e);
		                        }
		                    }
		
		                    // 通知脚本信息不为空
		                    if (CollectionUtils.isNotEmpty(userNotifications)) {
		                        log.info("邮件进行脚本通知");
		                        Future<?> submit = executorService.submit(() -> this.notifiScript(userNotifications, scriptIdAndFilePathMap, messageEntity));
		                        if(submit.isDone()) {
		                        	log.info("邮件脚本通知已完成");
		                        }
		                    } else {
		                        log.info("邮件没有关联用户脚本，不进行脚本通知");
		                    }
		                }else{
		                    log.info("邮件没有关联用户，不进行邮件通知");
		                }
		            }
		            
		            List<AlertNotifyLog> alertNotifyLogToDb =new ArrayList<AlertNotifyLog>();
		            // 将发送记录批量存入库中
		            for(List<AlertNotifyLog> data:alertNotifyLogMap.values()) {
		            	alertNotifyLogToDb.addAll(data);
		            }
		            alertNotifyLogService.saveBatch(alertNotifyLogToDb);
		        }
		    }
		
			private void notifiScript(List<SysUserNotification> userNotifications, Map<Integer, String> scriptIdAndFilePathMap, AlertMessageEntity alertMsg) {
		        List<AlertNotifyLog> alertNotifyLogs =new ArrayList<AlertNotifyLog>();
				
				String filePath, account;
		        for (SysUserNotification userNotification : userNotifications) {
		        	
		        	AlertNotifyLog alertNotifyLog =new AlertNotifyLog();
		        	alertNotifyLog.setMessageId(alertMsg.getId());
                    alertNotifyLog.setMessageState(alertMsg.getState());
                    alertNotifyLog.setMethod(userNotification.getScriptName());
                    alertNotifyLog.setUserId(Long.valueOf(userNotification.getUserId()));
                    alertNotifyLog.setTime(DateUtil.getUTCTimeByConfigTimeZone());
                    
		            filePath = scriptIdAndFilePathMap.get(userNotification.getScriptId());
		            account = userNotification.getAccount();
		            String[] commands = new String[]{filePath, account, JSONObject.toJSONString(alertMsg)};
		            try {
		                String result = RuntimeUtil.execForStr(commands);
		                if (log.isDebugEnabled())
		                    log.debug(String.format("通知命令执行结束，执行命令是: %s , 结果是: %s", Arrays.asList(commands).toString(), result));
		                alertNotifyLog.setState(1);
		            } catch (IORuntimeException e) {
		                log.debug(String.format("通知命令执行结束，执行命令是: %s , 结果是: %s", Arrays.asList(commands).toString(), "失败"));
		                log.error("alert通知脚本异常,异常消息是:" + e);
		                alertNotifyLog.setState(0);
		                alertNotifyLog.setErrorMsg(e.getMessage());
		            }
		            alertNotifyLogs.add(alertNotifyLog);
		        }
		        alertNotifyLogService.saveBatch(alertNotifyLogs);
		    }
			
			public String sendMail(AlertMessageEntity alertMessage) throws IOException, TemplateException {
		        //创建模型数据
		        Map<String,Object> modelMap = new HashMap<String,Object>();
		        modelMap.put("alertMessage",alertMessage);
		        //执行静态化 方式1  获取静态化内容
		        String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, modelMap);
		        //静态化内容;
		        return content;
		    }
		
		    private Template getTemplate() throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
		        String templateString = configService.getValue("email_template");
				template = TemplateUtil.stringToTemplate(templateString,"emailTemplate");
		        return template;
		    }
		}); 
    }
}