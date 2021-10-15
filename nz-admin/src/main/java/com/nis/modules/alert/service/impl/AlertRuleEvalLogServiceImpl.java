package com.nis.modules.alert.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.dao.AlertRuleEvalLogDao;
import com.nis.modules.alert.entity.AlertRuleEvalLog;
import com.nis.modules.alert.service.AlertRuleEvalLogService;

@Service("alertRuleEvalLogService")
public class AlertRuleEvalLogServiceImpl extends ServiceImpl<AlertRuleEvalLogDao,AlertRuleEvalLog> implements AlertRuleEvalLogService{
	
	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	
	@Value("${alertRule.evalLog.size:20}")
	private Integer alertRuleEvalLogSize;
	
	@Override
	public boolean save(AlertRuleEvalLog entity) {
		ListOperations<String, String> listOperations = redisTemplate.opsForList();
		String ruleId = String.format("alertRuleEvalLog:%s", entity.getRuleId());
		Long size = listOperations.leftPush(ruleId, Tool.JSONUtil.toJsonStr(entity));
		if(size > alertRuleEvalLogSize) {
			listOperations.trim(ruleId, 0, alertRuleEvalLogSize-1);
		}
		return true;
	}
	
}
