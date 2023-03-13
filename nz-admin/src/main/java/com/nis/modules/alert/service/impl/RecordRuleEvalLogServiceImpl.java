package com.nis.modules.alert.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.alert.dao.ReocrdRuleEvalLogDao;
import com.nis.modules.alert.entity.RecordRuleEvalLog;
import com.nis.modules.alert.service.RecordRuleEvalLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service("recordRuleEvalLogService")
public class RecordRuleEvalLogServiceImpl extends ServiceImpl<ReocrdRuleEvalLogDao,RecordRuleEvalLog> implements RecordRuleEvalLogService{
	
	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	
	@Value("${recordRule.evalLog.size:20}")
	private Integer recordRuleEvalLogSize;
	
	@Value("${recordRule.evalLog.timeout:1800}")
	private Integer recordRuleEvalLogTimeout;
	
	@Override
	public boolean save(RecordRuleEvalLog entity) {
		ListOperations<String, String> listOperations = redisTemplate.opsForList();
		String ruleId = String.format("recordRuleEvalLog:%s", entity.getRuleId());
		Long size = listOperations.leftPush(ruleId, Tool.JSONUtil.toJsonStr(entity));
		if(size > recordRuleEvalLogSize) {
			listOperations.trim(ruleId, 0, recordRuleEvalLogSize-1);
		}
		redisTemplate.expire(ruleId, recordRuleEvalLogTimeout, TimeUnit.SECONDS);
		return true;
	}

	@Override
	public List<RecordRuleEvalLog> queryListByRuleId(Integer id) {
		ListOperations<String, String> opsForList = redisTemplate.opsForList();
		String key = String.format("recordRuleEvalLog:%s", id);
		List<String> datas = opsForList.range(key, 0, -1);
		if(ToolUtil.isNotEmpty(datas)) {
			List<RecordRuleEvalLog> results = datas.stream().map(data -> JSONObject.parseObject(data, RecordRuleEvalLog.class)).collect(Collectors.toList());
			return results;
		}else {
			return new ArrayList<RecordRuleEvalLog>();
		}
	}
}
