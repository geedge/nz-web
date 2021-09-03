package com.nis.modules.alert.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.alert.dao.AlertRuleEvalLogDao;
import com.nis.modules.alert.entity.AlertRuleEvalLog;
import com.nis.modules.alert.service.AlertRuleEvalLogService;

@Service("alertRuleEvalLogService")
public class AlertRuleEvalLogServiceImpl extends ServiceImpl<AlertRuleEvalLogDao,AlertRuleEvalLog> implements AlertRuleEvalLogService{
	
}
