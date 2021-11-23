package com.nis.modules.alert.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.alert.dao.AlertRuleEvalLogDao;
import com.nis.modules.alert.entity.AlertRuleEvalLog;
import com.nis.modules.alert.service.AlertRuleEvalLogService;

@Service("alertRuleEvalLogService")
public class AlertRuleEvalLogServiceImpl extends ServiceImpl<AlertRuleEvalLogDao,AlertRuleEvalLog> implements AlertRuleEvalLogService{
	
	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	
	@Value("${alertRule.evalLog.size:20}")
	private Integer alertRuleEvalLogSize;
	
	@Value("${alertRule.evalLog.timeout:1800}")
	private Integer alertRuleEvalLogTimeout;
	
	@Override
	public boolean save(AlertRuleEvalLog entity) {
		ListOperations<String, String> listOperations = redisTemplate.opsForList();
		String ruleId = String.format("alertRuleEvalLog:%s", entity.getRuleId());
		Long size = listOperations.leftPush(ruleId, Tool.JSONUtil.toJsonStr(entity));
		if(size > alertRuleEvalLogSize) {
			listOperations.trim(ruleId, 0, alertRuleEvalLogSize-1);
		}
		redisTemplate.expire(ruleId, alertRuleEvalLogTimeout, TimeUnit.SECONDS);
		return true;
	}

	@Override
	public List<AlertRuleEvalLog> queryListByRuleId(Integer id) {
		ListOperations<String, String> opsForList = redisTemplate.opsForList();
		String key = String.format("alertRuleEvalLog:%s", id);
		List<String> datas = opsForList.range(key, 0, -1);
		if(ToolUtil.isNotEmpty(datas)) {
			List<AlertRuleEvalLog> results = datas.stream().map(data -> JSONObject.parseObject(data, AlertRuleEvalLog.class)).collect(Collectors.toList());
			return results;
		}else {
			return new ArrayList<AlertRuleEvalLog>();
		}
	}
}
