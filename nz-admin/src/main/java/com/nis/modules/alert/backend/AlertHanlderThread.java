package com.nis.modules.alert.backend;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nis.common.protobuf.Types.Label;
import com.nis.common.protobuf.Types.Sample;
import com.nis.common.protobuf.Types.TimeSeries;
import com.nis.common.protobuf.Types.TimeSeries.Builder;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.AlertSilenceConf;
import com.nis.modules.alert.entity.receive.AlertMessageReceiveDTO;
import com.nis.modules.alert.service.AlertHandlerService;
import com.nis.modules.alert.service.RecordRuleService;
import com.nis.modules.election.dao.LeaderElectionDao;
import com.nis.modules.sys.dao.SysLockDao;
import com.nis.modules.sys.entity.SysLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 告警消息处理线程 
 * 1、从 cache 中获取alert message 内容
 * 2、将message 内容解析为 json 
 * 3、根据label 判断endpoint，asset ，dc 是否停用 
 * 4、判断 asset state 是否开启 alert 
 * 5、判断 是否匹配的 alert slience规则 
 * 6、匹配 slience 消息存到 alert_message_slience 表 
 * 7、否则 消息存放到 alert_message_active表 
 * 8、需要告警 将 告警内容 存到 alert_message_notify 队列中
 * 9、将 active message 保存到 prometheus
 * 
 * @author ThinkPad
 *
 */
@Component
public class AlertHanlderThread implements Runnable {
	private static final Log log = Log.get();

	private static final String THREAD_NAME = "Alert Message Handler";
	@Autowired
	private AlertHandlerService messageHandlerService;
	@Autowired
	private RecordRuleService recordRuleService;
	@Autowired
	private SysLockDao sysLockDao;
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
				// 检查 HA_LOCK
				LeaderElectionDao dao = LeaderElectionDao.getInstance();
				if (Tool.BooleanUtil.negate(dao.isLeader(Constant.SYS_HA_LOCK, Constant.SERVER_ID))) {
		        	boolean sleep = Tool.ThreadUtil.sleep(10000);
		        	log.debug("sleep 10000ms :{}",sleep);
		        	continue;
		        }

				// 1、从队列中阻塞获取  等待30s
				log.debug("alert_message_recv size: {}", AlertContext.ALERT_MESSAGE_RECV_QUEUE.size());
				String alertMessageStr = AlertContext.ALERT_MESSAGE_RECV_QUEUE.poll(30, TimeUnit.SECONDS);
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
						Integer severityId = dto.getSeverityId(); //告警级别
						//计算 expired 时间，参照 alert manager实现，当之后一直未收到alert message 时，根据此字段判断alert message 是否已经 expired
						DateTime endTime = null;
						if(Tool.StrUtil.isBlank(endsAt)) {
							endTime = Tool.DateUtil.offsetSecond(nowDate, resolveTimeout);
							dto.setEndsAt(Tool.DateUtil.format(endTime, Constant.ALERT_MESSAGE_DATE_FORMAT));
						} else {
							endTime = Tool.DateUtil.parseUTC(endsAt);//Tool.DateUtil.parse(endsAt,Constant.ALERT_MESSAGE_DATE_FORMAT);
						}
						String hash = dto.genHashKey();// 计算hash 值，不包含 severity_id

						// 本地缓存是否存在，value 为 msg info
						Map<Object, Object> activeMsgMap = AlertContext.ALERT_MESSAGE_ACTIVE_CACHE.get(hash, false);
						String activeRk = Tool.MapUtil.getStr(activeMsgMap, "id");
						if (ObjectUtil.isNull(activeRk)) {
							AlertMessageEntity activeMsgInDb = messageHandlerService.queryAlertMessageByHashKey(Constant.ALERT_MESSAGE_ACTIVE_TABLE, hash);
							activeRk = Optional.ofNullable(activeMsgInDb).map(AlertMessageEntity::getId).map(Object::toString).orElse(null);
							// update cache
							messageHandlerService.updateAlertMessageStateCacheByEntity(Constant.ALERT_MESSAGE_ACTIVE_STATE, activeMsgInDb);
						}

						Map<Object, Object> silenceMsgMap = AlertContext.ALERT_MESSAGE_SILENCE_CACHE.get(hash, false);
						String silenceRk = Tool.MapUtil.getStr(silenceMsgMap, "id");
						if (ObjectUtil.isNull(silenceRk)) {
							AlertMessageEntity silenceMsgInDb = messageHandlerService.queryAlertMessageByHashKey(Constant.ALERT_MESSAGE_SILENCE_TABLE, hash);
							silenceRk = Optional.ofNullable(silenceMsgInDb).map(AlertMessageEntity::getId).map(Object::toString).orElse(null);
							// update cache
							messageHandlerService.updateAlertMessageStateCacheByEntity(Constant.ALERT_MESSAGE_SILENCE_STATE, silenceMsgInDb);
						}
						if(log.isDebugEnabled()) {
							log.debug("activeCacheValue: {}, silenceCacheValue:{}", activeRk,silenceRk);
						}
						
						AlertMessageEntity message = new AlertMessageEntity();
						message.setEndAt(endTime);
						message.setHashKey(hash);
						message.setLastAt(nowDate);//设置当前时间为 最近一次 告警接收时间
						if( Tool.ObjectUtil.isNotNull(severityId) ) {
							message.setSeverityId(severityId);
						}
						/**
						 * endtime < now 超时过期 alert message 处理
						 * cache key 存在说明首次处理
						 *     将数据移动到 history 表
						 *     删除  cache key
						 * cache key 不存在说明之前处理过，直接continue
						 */
						if (endTime != null && Tool.DateUtil.compare(nowDate, endTime)>0) {//判断是否已过期
							// 当 endTime 小于 当前时间，需要将 lastAt 时间和 endtime 保持一致
							message.setLastAt(endTime);
							//首次收到 active 消息变更为expired 消息，更新状态
							if(Tool.StrUtil.isNotBlank(activeRk)) {
								log.info("active->expired, {}", JSONObject.toJSONString(message));
								message.setId(Tool.NumberUtil.toBigInteger(activeRk).longValue());
								//删除 active 表数据，history新增一条，推送邮件通知队列
								messageHandlerService.handleMessageExpiredBatch(Tool.ListUtil.toList(message), Constant.ALERT_MESSAGE_ACTIVE_STATE);
							}
							//首次收到 silence消息变更为expired 消息，更新状态
							if(Tool.StrUtil.isNotBlank(silenceRk)) {
								log.info("silence->expired, {}", JSONObject.toJSONString(message));
								message.setId(Tool.NumberUtil.toBigInteger(silenceRk).longValue());
								messageHandlerService.handleMessageExpiredBatch(Tool.ListUtil.toList(message), Constant.ALERT_MESSAGE_SILENCE_STATE);
							}
							continue;
						}
						
						/**
						 * 之前已经接收过的消息，重新更新 cache expired 和 表中 endat 即可
						 */
						if(Tool.StrUtil.isNotBlank(activeRk)|| Tool.StrUtil.isNotBlank(silenceRk)){
							//非首次接收的告警消息，只更新 cache 失效时间和 数据库中 endat 字段
							if(Tool.StrUtil.isNotBlank(activeRk)) {//非首次接收的状态为 active 的告警消息
								message.setId(Tool.NumberUtil.toBigInteger(activeRk).longValue());
								message.setTableName(Constant.ALERT_MESSAGE_ACTIVE_TABLE);
								//更新表中数据同时更新 cache 超时时间
								messageHandlerService.updateMessageEndAt(message);
								// 在prometheus中保存一份
								Map<Object, Object> activeMsgCache = AlertContext.ALERT_MESSAGE_ACTIVE_CACHE.get(hash, false);
								this.sendToTimeSeries(message.getId(), nowDate, "active", activeMsgCache, dto);
								continue;
							}
							if(Tool.StrUtil.isNotBlank(silenceRk)) {//非首次接收的状态为 silence 的告警消息
								message.setId(Tool.NumberUtil.toBigInteger(silenceRk).longValue());
								message.setTableName(Constant.ALERT_MESSAGE_SILENCE_TABLE);
								//更新表中数据同时更新 cache 超时时间
								messageHandlerService.updateMessageEndAt(message);
								// 在prometheus中保存一份
								Map<Object, Object> silenceMsgCache = AlertContext.ALERT_MESSAGE_SILENCE_CACHE.get(hash, false);
								this.sendToTimeSeries(message.getId(), nowDate, "silence", silenceMsgCache, dto);
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
						message.setLastAt(nowDate);

						//补充id
						Long messageId = this.getAlertMessageId();
						message.setId(messageId);
						// 判断 是否符合alert slience 规则
						List<AlertSilenceConf> silenceList = messageHandlerService.judgeAlertMessageSilence(message);
						log.info("first alert message: {}, match silence size: {}", Tool.JSONUtil.toJsonStr(message),Tool.CollUtil.size(silenceList));
						if (Tool.CollectionUtil.isEmpty(silenceList)) {// 未匹配任何告警静默规则,
							// 判断是否已经新产生告警
							message.setTableName(Constant.ALERT_MESSAGE_ACTIVE_TABLE);
							messageHandlerService.handleMessageActiveBatch(Tool.ListUtil.toList(message));
							// 在prometheus中保存一份
							Map<Object, Object> activeMsgCache = AlertContext.ALERT_MESSAGE_ACTIVE_CACHE.get(hash, false);
							this.sendToTimeSeries(message.getId(), nowDate, "active", activeMsgCache, dto);
							continue;
						}
						// 匹配到告警静默规则
						// 写入到 alert_message_silence 库 和 silence 关系表
						message.setTableName(Constant.ALERT_MESSAGE_SILENCE_TABLE);
						message.setSilenceList(silenceList);
						messageHandlerService.handleMessageSilenceBatch(Tool.ListUtil.toList(message));
						// 在prometheus中保存一份
						Map<Object, Object> silenceMsgCache = AlertContext.ALERT_MESSAGE_SILENCE_CACHE.get(hash, false);
						this.sendToTimeSeries(message.getId(), nowDate, "silence", silenceMsgCache, dto);
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
	 * 使用 mysql 乐观锁实现 alert message 表中 id 全局唯一
	 * @return
	 */
	public Long getAlertMessageId() {
		SysLock sysLock = sysLockDao.selectOne(new LambdaQueryWrapper<SysLock>().eq(SysLock::getLockKey, Constant.SYS_LOCK_ALERT_MESSAGE_ID));
		Integer lockValue = Integer.valueOf(sysLock.getLockValue());

		Integer seqId = (lockValue == 99999) ? 1 : ++lockValue;
		// updateWithOptimisticLock
		int update = sysLockDao.update(new SysLock(), new LambdaUpdateWrapper<SysLock>()
				.set(SysLock::getLockValue, seqId)
				.set(SysLock::getVersion, sysLock.getVersion() + 1)
				.eq(SysLock::getLockKey, Constant.SYS_LOCK_ALERT_MESSAGE_ID)
				.eq(SysLock::getVersion, sysLock.getVersion()));

		if (update == 0) {
			// version 冲突，再次尝试获取
			return this.getAlertMessageId();
		} else {
			DateTime now = Tool.DateUtil.date();
			// yyyyMMddHHmm
			String datePrefix = Tool.DateUtil.format(now, Constant.ALERT_MESSAGE_ID_FORMAT);

			String middleId = Tool.StrUtil.concat(true, "0000", String.valueOf(seqId));
			middleId = StrUtil.sub(middleId, middleId.length() - 5, middleId.length());
			return Tool.NumberUtil.parseLong(Tool.StrUtil.concat(true, datePrefix, middleId));
		}
	}

	/**
	 * 将 active message 保存到 prometheus
	 *
	 * @param messageId
	 * @param nowDate
	 * @param state
	 * @param stateCacheMap
	 * @param dto
	 */
	private void sendToTimeSeries(Long messageId, Date nowDate, String state, Map<Object, Object> stateCacheMap, AlertMessageReceiveDTO dto) {
		Label idLabel = Label.newBuilder().setName("id").setValue(String.valueOf(messageId)).build();
		Label metricName = Label.newBuilder().setName("__name__").setValue("nz_alert_message_event").build();
		Label stateLabel = Label.newBuilder().setName("state").setValue(state).build();
		Sample sample = Sample.newBuilder().setTimestamp(nowDate.getTime()).setValue(Tool.NumberUtil.parseDouble(Tool.StrUtil.emptyToDefault(dto.getValue(), "1"))).build();
		Builder builder = TimeSeries.newBuilder().addLabels(idLabel).addLabels(metricName).addLabels(stateLabel).addSamples(sample);
		if (Tool.MapUtil.isNotEmpty(stateCacheMap)) {
			for (Map.Entry<Object, Object> entry : stateCacheMap.entrySet()) {
				String key = StrUtil.utf8Str(entry.getKey());
				if (StrUtil.equalsAny(key, "labels", "id")) {
					continue;
				}
				Label l = Label.newBuilder().setName(key).setValue(StrUtil.toString(entry.getValue())).build();
				builder.addLabels(l);
			}
			TimeSeries timeSeries = builder.build();
			recordRuleService.rightPushRecordToCache(timeSeries);
		}
	}

}
