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
import com.nis.modules.alert.dao.ReocrdRuleEvalLogDao;
import com.nis.modules.alert.entity.RecordRuleEntity;
import com.nis.modules.alert.entity.RecordRuleEvalLog;
import com.nis.modules.alert.service.RecordRuleEvalLogService;
import com.nis.modules.alert.service.RecordRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service("recordRuleEvalLogService")
public class RecordRuleEvalLogServiceImpl extends ServiceImpl<ReocrdRuleEvalLogDao,RecordRuleEvalLog> implements RecordRuleEvalLogService{

	private static final Log log = Log.get();

	@Value("${recordRule.evalLog.size:20}")
	private Integer recordRuleEvalLogSize;
	
	@Value("${recordRule.evalLog.timeout:1800}")
	private Integer recordRuleEvalLogTimeout;

	@Autowired
	private RecordRuleService recordRuleService;
	
	@Override
	public boolean save(RecordRuleEvalLog entity) {
 		String key = String.format("recordRuleEvalLog:%s", entity.getRuleId());
		FIFOCache<String, Object> fifoCache = LocalCacheManager.getFIFOCache(Constant.ALERT_CACHE_PREFIX, key, recordRuleEvalLogSize);
		fifoCache.put(Tool.StrUtil.toString(System.currentTimeMillis()), Tool.JSONUtil.toJsonStr(entity), TimeUnit.SECONDS.toMillis(recordRuleEvalLogTimeout));
		return true;
	}

	@Override
	public List<RecordRuleEvalLog> queryListByRuleId(Integer id) {
		String key = String.format("recordRuleEvalLog:%s", id);
		FIFOCache<String, Object> fifoCache = LocalCacheManager.getFIFOCache(Constant.ALERT_CACHE_PREFIX, key, recordRuleEvalLogSize);

		List<RecordRuleEvalLog> results = Tool.ListUtil.list(true);
		Iterator<CacheObj<String, Object>> iterator = fifoCache.cacheObjIterator();
		while (iterator.hasNext()) {
			CacheObj<String, Object> next = iterator.next();
			Object value = next.getValue();
			if (value != null) {
				RecordRuleEvalLog recordRuleEvalLog = JSONObject.parseObject((String) value, RecordRuleEvalLog.class);
				results.add(recordRuleEvalLog);
			}
		}
		return Tool.CollUtil.reverse(results);
	}

	@Override
	public void syncRecordRuleEvalLogCache(String ruleEvalLogJsonStr) {
		try {
			List<RecordRuleEntity> allRuleList = recordRuleService.list(new LambdaQueryWrapper<RecordRuleEntity>().eq(RecordRuleEntity::getState, 1));
			Map dataMap = JSONUtil.toBean(ruleEvalLogJsonStr, Map.class);

			for (RecordRuleEntity rule : allRuleList) {
				String key = String.format("recordRuleEvalLog:%s", rule.getId());
				// remove
				LocalCacheManager.removeCache(Constant.ALERT_CACHE_PREFIX, key);
				// add
				List<RecordRuleEvalLog> evalLogList = JSONUtil.toList((JSONArray) dataMap.get(rule.getId().toString()), RecordRuleEvalLog.class);
				if (Tool.CollUtil.isNotEmpty(evalLogList)) {
					FIFOCache<String, Object> fifoCache = LocalCacheManager.getFIFOCache(Constant.ALERT_CACHE_PREFIX, key, recordRuleEvalLogSize);
					ListIterator<RecordRuleEvalLog> iterator = evalLogList.listIterator(evalLogList.size());
					while (iterator.hasPrevious()) {
						int index = iterator.previousIndex();
						RecordRuleEvalLog element = iterator.previous();
						fifoCache.put(Tool.StrUtil.toString(index), Tool.JSONUtil.toJsonStr(element), TimeUnit.SECONDS.toMillis(recordRuleEvalLogTimeout));
					}
				}
			}
		} catch (Exception e) {
			log.error(e, "[syncRecordRuleEvalLogCache] [error]");
		}
	}
}
