package com.nis.modules.alert.backend;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.AlertSilenceConf;
import com.nis.modules.alert.entity.receive.AlertMessageReceiveDTO;
import com.nis.modules.alert.service.AlertHandlerService;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;

/**
 * 告警消息处理线程 
 * 1、从 redis中获取alert message 内容
 * 2、将message 内容解析为 json 
 * 3、根据label 判断endpoint，asset ，dc 是否停用 
 * 4、判断 asset state 是否开启 alert 
 * 5、判断 是否匹配的 alert slience规则 
 * 6、匹配 slience 消息存到 alert_message_slience 表 
 * 7、否则 消息存放到 alert_message_active表 
 * 8、需要告警 将 告警内容 存到 redis alert_message_send 队列中
 * 
 * @author ThinkPad
 *
 */
@Component
public class AlertHanlderThread implements Runnable {
	private static final Log log = Log.get();

	private static final String THREAD_NAME = "Alert Message Handler";
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
	@Autowired
	private AlertHandlerService messageHandlerService;
	
	/**
	 * alert message 超时时间
	 */
	@Value("${alert.message.resolve.timeout:300}")
	private Integer resolveTimeout;
	
	@Override
	public void run() {
		Thread.currentThread().setName(THREAD_NAME);
		log.info("AlertHanlderRunner 告警消息处理线程启动");
		while (true) {
			try {
				// 检查 haMode
		    	String serverId = (String) redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
		        if(!StrUtil.equals(serverId, Constant.SERVER_ID)) {
					Thread.sleep(10000);
		        	continue;
		        }
				ValueOperations<String, Object> operations = redisTemplate.opsForValue();
				ListOperations<String, Object> opsForList = redisTemplate.opsForList();
				if(redisTemplate.hasKey(Constant.ALERT_MESSAGE_RECV)) {
					Long size = operations.size(Constant.ALERT_MESSAGE_RECV);
		    		log.debug("alert_message_recv size : {}",size);
				}
				// 1、从redis中阻塞获取  等待30s
				String alertMessageStr = (String)opsForList.rightPop(Constant.ALERT_MESSAGE_RECV,30,TimeUnit.SECONDS);
				if (Tool.StrUtil.isBlank(alertMessageStr)) {
					continue;
				}
				if (log.isDebugEnabled()) {
					log.debug("get a alert message,content:{}", alertMessageStr);
				}
				// 2、将message 内容解析为 json
				List<AlertMessageReceiveDTO> amList = JSON.parseArray(alertMessageStr, AlertMessageReceiveDTO.class);
				if (amList == null) {
					continue;
				}
				StopWatch sw = StopWatch.create("alert");
				sw.start();
				for (AlertMessageReceiveDTO dto : amList) {
					try {
						if(log.isDebugEnabled()) {
							log.debug("alert message content: {}	", Tool.JSONUtil.toJsonStr(dto));
						}
						// 3、判断是否为已经失效的告警消息
						String endsAt = dto.getEndsAt();
						DateTime nowDate = Tool.DateUtil.date();
						//计算 expired 时间，参照 alert manager实现，当之后一直未收到alert message 时，根据此字段判断alert message 是否已经 expired
						DateTime endTime = null;
						if(Tool.StrUtil.isBlank(endsAt)) {
							endTime = Tool.DateUtil.offsetSecond(nowDate, resolveTimeout);
							dto.setEndsAt(Tool.DateUtil.format(endTime, Constant.ALERT_MESSAGE_DATE_FORMAT));
						} else {
							endTime = Tool.DateUtil.parse(endsAt,Constant.ALERT_MESSAGE_DATE_FORMAT);
						}
						String hash = dto.genHashKey();// 计算hash 值
						//查找 redis 是否存在,value 为id
						String activeRk = (String)operations.get(Tool.StrUtil.concat(true, Constant.ALERT_MESSAGE_ACTIVE_PRIX, hash));
						String silenceRk = (String)operations.get(Tool.StrUtil.concat(true, Constant.ALERT_MESSAGE_SILENCE_PRIX, hash));
						if(log.isDebugEnabled()) {
							log.debug("activeRedisValue: {}, silenceRedisValue:{}", activeRk,silenceRk);
						}
						
						AlertMessageEntity message = new AlertMessageEntity();
						message.setEndAt(endTime);
						message.setHashKey(hash);
						/**
						 * endtime < now 超时过期 alert message 处理
						 * redis key 存在说明首次处理
						 *     将数据移动到 history 表
						 *     删除  redis key 
						 * redis key 不存在说明之前处理过，直接continue
						 */
						if (endTime != null && Tool.DateUtil.compare(nowDate, endTime)>0) {//判断是否已过期
							if(Tool.StrUtil.isAllBlank(activeRk,silenceRk)) {//不存在说明已经处理过，首次接收到 expired 消息会删除缓存key
								continue;
							}
							//首次收到 active 消息变更为expired 消息，更新状态
							if(Tool.StrUtil.isNotBlank(activeRk)) {
								message.setId(Tool.NumberUtil.toBigInteger(activeRk).longValue());
								//删除 active 表数据，history新增一条，推送邮件通知队列
								messageHandlerService.handleMessageExpiredBatch(Tool.ListUtil.toList(message), Constant.ALERT_MESSAGE_ACTIVE_STATE);
							}
							//首次收到 silence消息变更为expired 消息，更新状态
							if(Tool.StrUtil.isNotBlank(silenceRk)) {
								message.setId(Tool.NumberUtil.toBigInteger(silenceRk).longValue());
								messageHandlerService.handleMessageExpiredBatch(Tool.ListUtil.toList(message), Constant.ALERT_MESSAGE_SILENCE_STATE);
								continue;
							}
						}
						
						/**
						 * 之前已经接收过的消息，重新更新 redis expired 和 表中 endat 即可
						 */
						if(Tool.StrUtil.isNotBlank(activeRk)|| Tool.StrUtil.isNotBlank(silenceRk)){
							//非首次接收的告警消息，只更新 redis 失效时间和 数据库中 endat 字段
							if(Tool.StrUtil.isNotBlank(activeRk)) {//非首次接收的状态为 active 的告警消息
								message.setId(Tool.NumberUtil.toBigInteger(activeRk).longValue());
								message.setTableName(Constant.ALERT_MESSAGE_ACTIVE_TABLE);
								//更新表中数据同时更新 redis 超时时间
								messageHandlerService.updateMessageEndAt(message);
								continue;
							}
							if(Tool.StrUtil.isNotBlank(silenceRk)) {//非首次接收的状态为 silence 的告警消息
								message.setId(Tool.NumberUtil.toBigInteger(silenceRk).longValue());
								message.setTableName(Constant.ALERT_MESSAGE_SILENCE_TABLE);
								//更新表中数据同时更新 redis 超时时间
								messageHandlerService.updateMessageEndAt(message);
								continue;
							}
						}
						
						/**
						 * 以下为首次收到告警消息，或 首次状态由 active 变更为 expired
						 * 1、ative 变更为 expired需要发送邮件
						 * 2、首次接收到 告警消息 则需要判断是否符合 告警静默规则
						 * 3、符合告警静默规则，存到 alert_message_silence 表，缓存到 ALERT_MESSAGE_SILENCE_PRIX+hash  
						 * 4、不符合告警静默规则，存到 alert_message_active 表，缓存到 ALERT_MESSAGE_ACTIVE_PRIX+hash
						 */
						// 转成 AlertMessageEntity
						message = messageHandlerService.transAlertMessageEntity(dto);
						if (message == null) {// 直接丢弃
							if (log.isDebugEnabled()) {
								log.debug("drop alert message", alertMessageStr);
							}
							continue;
						}
						message.setHashKey(hash);//message 保存 hashkey

						//补充id
						Long messageId = this.getAlertMessageId();
						message.setId(messageId);
						// 判断 是否符合alert slience 规则
						List<AlertSilenceConf> silenceList = messageHandlerService.judgeAlertMessageSilence(message);
						if (Tool.CollectionUtil.isEmpty(silenceList)) {// 未匹配任何告警静默规则,
							// 判断是否已经新产生告警
							message.setTableName(Constant.ALERT_MESSAGE_ACTIVE_TABLE);
							messageHandlerService.handleMessageActiveBatch(Tool.ListUtil.toList(message));
							continue;
						}
						// 匹配到告警静默规则
						// 写入到 alert_message_silence 库 和 silence 关系表
						message.setTableName(Constant.ALERT_MESSAGE_SILENCE_TABLE);
						message.setSilenceList(silenceList);
						messageHandlerService.handleMessageSilenceBatch(Tool.ListUtil.toList(message));
					} catch (RuntimeException e) {
						log.warn(e, "alert message foreach error");
					}
				}
				sw.stop();
				if(log.isDebugEnabled()) {
					log.debug("耗时统计:{}", sw.prettyPrint());
				}
			} catch (Throwable e) {
				if(!(e instanceof QueryTimeoutException)) {
					log.error(e, "alert message handle error");
				}
			}
		}
	}


	/**
	 * 使用 redis 维护 alert message id，确保三个表中全局唯一
	 * @return
	 */
	public Long getAlertMessageId() {
		DateTime now = Tool.DateUtil.date();
		//yyyyMMddHHmm
		String datePrefix = Tool.DateUtil.format(now, Constant.ALERT_MESSAGE_ID_FORMAT);
		String idKey = Tool.StrUtil.concat(true, Constant.ALERT_MESSAGE_ID_PRIX,datePrefix);
		Object val = redisTemplate.opsForValue().get(idKey);
		Long increment = redisTemplate.opsForValue().increment(idKey);
		if(Tool.ObjectUtil.isNull(val)) {//设置超时时间，有效期一天，超时时间不是很精准
			redisTemplate.expire(idKey,Duration.ofMinutes(2).toMillis(),TimeUnit.MILLISECONDS);
		}
		String middleId = Tool.StrUtil.concat(true, "0000",String.valueOf(increment));
		middleId = StrUtil.sub(middleId,middleId.length()-5, middleId.length());
		return Tool.NumberUtil.parseLong(Tool.StrUtil.concat(true, datePrefix,middleId));
	}


}
