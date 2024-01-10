package com.nis.modules.alert.service.impl;

import cn.hutool.cache.impl.CacheObj;
import cn.hutool.cache.impl.FIFOCache;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Constant;
import com.nis.common.utils.TimedCacheManager;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.dao.AlertRuleEvalLogDao;
import com.nis.modules.alert.entity.AlertRuleEvalLog;
import com.nis.modules.alert.service.AlertRuleEvalLogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service("alertRuleEvalLogService")
public class AlertRuleEvalLogServiceImpl extends ServiceImpl<AlertRuleEvalLogDao,AlertRuleEvalLog> implements AlertRuleEvalLogService{
	
	@Value("${alertRule.evalLog.size:20}")
	private Integer alertRuleEvalLogSize;
	
	@Value("${alertRule.evalLog.timeout:1800}")
	private Integer alertRuleEvalLogTimeout;
	
	@Override
	public boolean save(AlertRuleEvalLog entity) {
		String key = String.format("alertRuleEvalLog:%s", entity.getRuleId());
		FIFOCache<String, Object> fifoCache = TimedCacheManager.getFIFOCache(Constant.ALERT_CACHE_PREFIX, key, alertRuleEvalLogSize);
		fifoCache.put(Tool.StrUtil.toString(System.currentTimeMillis()), Tool.JSONUtil.toJsonStr(entity), TimeUnit.SECONDS.toMillis(alertRuleEvalLogTimeout));
		return true;
	}

	@Override
	public List<AlertRuleEvalLog> queryListByRuleId(Integer id) {
		String key = String.format("alertRuleEvalLog:%s", id);
		FIFOCache<String, Object> fifoCache = TimedCacheManager.getFIFOCache(Constant.ALERT_CACHE_PREFIX, key, alertRuleEvalLogSize);

		List<AlertRuleEvalLog> results = Tool.ListUtil.list(true);
		Iterator<CacheObj<String, Object>> iterator = fifoCache.cacheObjIterator();
		while (iterator.hasNext()) {
			CacheObj<String, Object> next = iterator.next();
			Object value = next.getValue();
			if (value != null) {
				AlertRuleEvalLog alertRuleEvalLog = JSONObject.parseObject((String) value, AlertRuleEvalLog.class);
				results.add(alertRuleEvalLog);
			}
		}
		return Tool.CollUtil.reverse(results);
	}
}
