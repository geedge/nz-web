package com.nis.modules.alert.backend;

import java.util.Map;

import com.nis.common.utils.Tool;
import com.nis.modules.alert.entity.AlertRuleEntity;

import cn.hutool.log.Log;

public class AlertContext {
	private static final Log log = Log.get();
	/**
	 * 当前生效 event_log.rule
	 */
	private static volatile Integer currentIndex;
	
	public static final String JOB_DATA_KEY_ALERT_RULE_ENTITY = "alert_rule_entity";
	
	public final static Map<String,AlertRuleEntity> ALL_SNMP_ALERT_RULE = Tool.MapUtil.newConcurrentHashMap();

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

	public static String getRedisCacheKey(AlertRuleEntity alertRuleEntity) {
		return getRedisCacheKey(alertRuleEntity.getId());
	}

	public static String getRedisCacheKey(Integer ruleId) {
		return String.format(ALERT_FOR_STATE_KEY_FORMAT, ruleId);
	}
}
