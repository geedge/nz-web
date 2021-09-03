package com.nis.modules.alert.backend;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.nis.common.utils.Tool;

import cn.hutool.log.Log;

public class AlertContext {
	private static final Log log = Log.get();
	/**
	 * 当前生效 event_log.rule
	 */
	private static Integer currentIndex;
	/**
	 * 注册当前生效的 alert rule
	 */
	private final static Map<String, AlertRuleJudgeRunnable> ALERT_RULE_TABLE = Tool.MapUtil.newConcurrentHashMap();

	private final static ScheduledExecutorService EXECUTORS = Executors
			.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

	public static Integer getCurrentIndex() {
		return currentIndex;
	}

	public static void setCurrentIndex(Integer newIndex) {
		currentIndex = newIndex;
	}

	public static Map<String, AlertRuleJudgeRunnable> getAlertRuleMap() {
		return ALERT_RULE_TABLE;
	}

	/**
	 * 注册 judge 线程
	 * 
	 * @param judge
	 */
	public static void registerJudgeRunnable(String hashId, AlertRuleJudgeRunnable judge) {
		Integer interval = judge.getAlertRuleEntity().getInr();
		ScheduledFuture<?> future = EXECUTORS.scheduleAtFixedRate(judge, Tool.RandomUtil.randomLong(6), interval,TimeUnit.SECONDS);
		judge.setFuture(future);
		ALERT_RULE_TABLE.put(hashId, judge);
		log.info("register: {},rule :{}", judge.getAlertRuleEntity().getId());
	}

	/**
	 * 注销 judge 线程
	 * 
	 * @param hashId 
	 * @param cleanCache 是否清楚 缓存key
	 */
	public static void unRegisterJudgeRunnable(String hashId, boolean cleanCache) {
		AlertRuleJudgeRunnable judge = AlertContext.ALERT_RULE_TABLE.get(hashId);
		if(judge != null) {
			judge.stop(cleanCache);
			// 删掉 注册的 线程
			AlertContext.ALERT_RULE_TABLE.remove(hashId);
		}
		log.info("unRegister: {},judge exist:{},cleanCache:{}", hashId,judge == null,cleanCache);
	}

}
