package com.nis.modules.alert.backend;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.log.Log;
import com.nis.common.utils.Constant;
import com.nis.common.utils.LocalCacheManager;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.entity.AlertRuleEntity;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class AlertContext {
	private static final Log log = Log.get();
	/**
	 * 当前生效 event_log.rule
	 */
	private static volatile Integer currentIndex;
	
	public static final String JOB_DATA_KEY_ALERT_RULE_ENTITY = "alert_rule_entity";
	
	public final static Map<String,AlertRuleEntity> ALL_SNMP_ALERT_RULE = Tool.MapUtil.newConcurrentHashMap();

	/**
	 * alert_message_recv_queue cache
	 */
	public static final LinkedBlockingQueue<String> ALERT_MESSAGE_RECV_QUEUE = new LinkedBlockingQueue<>();

	/**
	 * record_data_cache
	 */
	public static final LinkedBlockingQueue<String> RECORD_DATA_CACHE = new LinkedBlockingQueue<>();

	/**
	 * alert_message_notify cache
	 */
	public static final LinkedBlockingQueue<String> ALERT_MESSAGE_NOTIFY_QUEUE = new LinkedBlockingQueue<>();

	/**
	 * alert_message_active_cache
	 */
	public static final TimedCache<String, Object> ALERT_MESSAGE_ACTIVE_CACHE = LocalCacheManager.getNeverExpireTimedCache(Constant.ALERT_CACHE_PREFIX, "alert_message_active");

	/**
	 * alert_message_silence_cache
	 */
	public static final TimedCache<String, Object> ALERT_MESSAGE_SILENCE_CACHE = LocalCacheManager.getNeverExpireTimedCache(Constant.ALERT_CACHE_PREFIX, "alert_message_silence");

	/**
	 * alert 状态 key
	 */
	private static final String ALERT_FOR_STATE_KEY_FORMAT = "alert_for_state:%s";
	
	public static Integer getCurrentIndex() {
		return currentIndex;
	}

	public static void setCurrentIndex(Integer newIndex) {
		currentIndex = newIndex;
	}

	public static String getCacheKey(AlertRuleEntity alertRuleEntity) {
		return getCacheKey(alertRuleEntity.getId());
	}

	public static String getCacheKey(Integer ruleId) {
		return String.format(ALERT_FOR_STATE_KEY_FORMAT, ruleId);
	}
}
