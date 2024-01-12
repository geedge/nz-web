package com.nis.modules.alert.service.impl;

import cn.hutool.cache.impl.CacheObj;
import cn.hutool.cache.impl.FIFOCache;
import cn.hutool.json.JSONArray;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Constant;
import com.nis.common.utils.JSONUtil;
import com.nis.common.utils.LocalCacheManager;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.dao.AlertRuleEvalLogDao;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.entity.AlertRuleEvalLog;
import com.nis.modules.alert.service.AlertRuleEvalLogService;
import com.nis.modules.alert.service.AlertRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service("alertRuleEvalLogService")
public class AlertRuleEvalLogServiceImpl extends ServiceImpl<AlertRuleEvalLogDao,AlertRuleEvalLog> implements AlertRuleEvalLogService{

	private static final Log log = Log.get();

	@Value("${alertRule.evalLog.size:20}")
	private Integer alertRuleEvalLogSize;
	
	@Value("${alertRule.evalLog.timeout:1800}")
	private Integer alertRuleEvalLogTimeout;

	@Autowired
	private AlertRuleService alertRuleService;
	
	@Override
	public boolean save(AlertRuleEvalLog entity) {
		String key = String.format("alertRuleEvalLog:%s", entity.getRuleId());
		FIFOCache<String, Object> fifoCache = LocalCacheManager.getFIFOCache(Constant.ALERT_CACHE_PREFIX, key, alertRuleEvalLogSize);
		fifoCache.put(Tool.StrUtil.toString(System.currentTimeMillis()), Tool.JSONUtil.toJsonStr(entity), TimeUnit.SECONDS.toMillis(alertRuleEvalLogTimeout));
		return true;
	}

	@Override
	public List<AlertRuleEvalLog> queryListByRuleId(Integer id) {
		String key = String.format("alertRuleEvalLog:%s", id);
		FIFOCache<String, Object> fifoCache = LocalCacheManager.getFIFOCache(Constant.ALERT_CACHE_PREFIX, key, alertRuleEvalLogSize);

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

	@Override
	public void syncAlertRuleEvalLogCache(String ruleEvalLogJsonStr) {
		try {
			List<AlertRuleEntity> allRuleList = alertRuleService.list(new LambdaQueryWrapper<AlertRuleEntity>().in(AlertRuleEntity::getType, Tool.ListUtil.of(1, 2)));

			Map dataMap = JSONUtil.toBean(ruleEvalLogJsonStr, Map.class);
			for (AlertRuleEntity rule : allRuleList) {
				String key = String.format("alertRuleEvalLog:%s", rule.getId());
				// remove
				LocalCacheManager.removeCache(Constant.ALERT_CACHE_PREFIX, key);
				// add
				List<AlertRuleEvalLog> evalLogList = JSONUtil.toList((JSONArray) dataMap.get(rule.getId().toString()), AlertRuleEvalLog.class);
				if (Tool.CollUtil.isNotEmpty(evalLogList)) {
					FIFOCache<String, Object> fifoCache = LocalCacheManager.getFIFOCache(Constant.ALERT_CACHE_PREFIX, key, alertRuleEvalLogSize);
					ListIterator<AlertRuleEvalLog> iterator = evalLogList.listIterator(evalLogList.size());
					while (iterator.hasPrevious()) {
						int index = iterator.previousIndex();
						AlertRuleEvalLog element = iterator.previous();
						fifoCache.put(Tool.StrUtil.toString(index), Tool.JSONUtil.toJsonStr(element), TimeUnit.SECONDS.toMillis(alertRuleEvalLogTimeout));
					}
				}
			}
		} catch (Exception e) {
			log.error(e, "[syncAlertRuleEvalLogCache] [error]");
		}
	}
}
